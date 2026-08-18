"""
ScarfBench behavioural oracle — application family: quarkus-htmx-todos.

Concretization of oracle/quarkus-htmx-todos.feature. One test function per Gherkin scenario,
in the same order, asserting only at the HTTP boundary: routes, status codes, response
headers, the server-rendered markup a browser actually receives, and the state a later
request reads back (Gate G5).

Nothing here names a framework mechanism (G3/G6) — no template engine, no bean scope, no
persistence API, no package or class name. The same file is shipped verbatim in the quarkus
and spring variants and must pass against both (G16/G17).

    BASE_URL       overrides the target, default http://localhost:8080   (G13)
    SMOKE_TIMEOUT  per-request timeout in seconds, default 20            (G14)

Each test starts from an empty list: the `clean_slate` fixture deletes every remaining item
first, using only routes this specification already covers. The suite is therefore
order-independent, but it is still run with -p no:randomly so a failure is reproducible.

Two normalisations are applied before comparing markup, and both are deliberate. Whitespace
between tags is collapsed, because indentation is a template-engine artifact and not
behaviour. Numeric character references for the apostrophe are decoded, because one engine
escapes `'` inside an attribute value and the other does not — the DOM the browser builds is
identical either way. Neither normalisation can hide a wrong route, a wrong status, a wrong
title or a wrong flag. See FINDINGS.md §2.

Run:  pytest -v -p no:randomly smoke.py
"""

import os
import re
import urllib.error
import urllib.parse
import urllib.request
import uuid

import pytest

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
TIMEOUT = float(os.environ.get("SMOKE_TIMEOUT", "20"))

# The header the hypermedia client sets on every request it makes. Two routes answer with a
# bare item fragment when it is present and a redirect when it is not.
HX_HEADER = {"HX-Request": "true"}

PAGE_TITLE = "Quarkus/htmx • TodoMVC"


# --------------------------------------------------------------------------
# HTTP
# --------------------------------------------------------------------------

class NoRedirect(urllib.request.HTTPRedirectHandler):
    """Redirects are an assertion target here, so they are never followed."""

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


OPENER = urllib.request.build_opener(NoRedirect)


class Result:
    """A response. Transport failures are reported separately, never as a status (G14)."""

    def __init__(self, status, headers, body, url):
        self.status = status
        self.headers = headers
        self.body = body
        self.url = url

    def header(self, name):
        for key, value in self.headers.items():
            if key.lower() == name.lower():
                return value
        return None

    def __str__(self):
        return f"{self.url} -> HTTP {self.status}, body: {self.body[:500]!r}"


def call(method, path, form=None, hx=False):
    url = path if path.startswith("http") else BASE_URL + path
    data = None
    headers = {"Accept": "text/html,*/*"}
    if hx:
        headers.update(HX_HEADER)
    if form is not None:
        data = urllib.parse.urlencode(form).encode("utf-8")
        headers["Content-Type"] = "application/x-www-form-urlencoded"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with OPENER.open(request, timeout=TIMEOUT) as response:
            return Result(response.status, dict(response.headers),
                          response.read().decode("utf-8", "replace"), url)
    except urllib.error.HTTPError as error:
        # 3xx arrives here too, because redirects are not followed.
        return Result(error.code, dict(error.headers),
                      error.read().decode("utf-8", "replace"), url)
    except urllib.error.URLError as error:
        pytest.fail(f"network error contacting {url}: {error.reason}")
    except OSError as error:
        pytest.fail(f"network error contacting {url}: {error}")


def get(path, hx=False):
    return call("GET", path, hx=hx)


def post(path, form=None, hx=False):
    return call("POST", path, form=form if form is not None else {}, hx=hx)


# --------------------------------------------------------------------------
# Reading the rendered markup
# --------------------------------------------------------------------------

def normalise(html):
    """Collapse inter-tag whitespace and decode the escaped apostrophe. See the module
    docstring: both are engine artifacts, neither can mask a behavioural difference."""
    html = html.replace("&#39;", "'").replace("&#039;", "'").replace("&apos;", "'")
    html = re.sub(r">\s+<", "><", html)
    return re.sub(r"\s+", " ", html).strip()


def text_of(html):
    """The visible text of a markup fragment, whitespace-collapsed."""
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", html)).strip()


ITEM_ID = re.compile(r'id="item-([0-9a-fA-F-]{36})"')


class Item:
    """One rendered row, read from the markup rather than from any internal state."""

    def __init__(self, markup):
        self.markup = markup
        opening = markup[: markup.index(">") + 1] if ">" in markup else markup
        self.id = (ITEM_ID.search(opening) or ITEM_ID.search(markup)).group(1)
        self.completed = 'class="completed"' in opening
        label = re.search(r"<label[^>]*>(.*?)</label>", markup, re.S)
        self.title = text_of(label.group(1)) if label else None
        toggle = re.search(r"<input[^>]*class=\"toggle\"[^>]*>", markup, re.S)
        self.checked = bool(toggle) and "checked" in toggle.group(0)
        edit = re.search(r"<input[^>]*class=\"edit\"[^>]*>", markup, re.S)
        self.edit_value = None
        if edit:
            value = re.search(r'value="([^"]*)"', edit.group(0))
            self.edit_value = value.group(1) if value else None

    def __repr__(self):
        return f"Item(id={self.id}, title={self.title!r}, completed={self.completed})"


def items_of(html):
    """The rows of a rendered list page, in document order."""
    html = normalise(html)
    match = re.search(r'<ul class="todo-list" id="todo-list">(.*?)</ul>', html, re.S)
    assert match, f"no todo list found in response: {html[:600]!r}"
    return [Item("<li" + chunk) for chunk in match.group(1).split("<li")[1:]]


def fragment_item(body):
    """The single row an htmx response consists of."""
    return Item(normalise(body))


def counter_of(html):
    match = re.search(r'<span class="todo-count">(.*?)</span>', normalise(html), re.S)
    assert match, f"no item counter found in response: {html[:600]!r}"
    return text_of(match.group(1))


def selected_filter(html):
    """Which of the three filter links carries the selected marker."""
    html = normalise(html)
    selected = []
    for label, href in (("All", "/todos"), ("Active", "/todos/active"),
                        ("Completed", "/todos/completed")):
        anchor = re.search(r'<a[^>]*href="%s"[^>]*>' % re.escape(href), html)
        assert anchor, f"no {label!r} filter link in response"
        if "selected" in anchor.group(0):
            selected.append(label)
    return selected


def is_whole_document(body):
    lowered = body.lower()
    return "<html" in lowered or "<!doctype" in lowered


# --------------------------------------------------------------------------
# Operations, expressed only through the documented routes
# --------------------------------------------------------------------------

def listing(path="/todos"):
    result = get(path)
    assert result.status == 200, str(result)
    return result.body


def titles(path="/todos"):
    return [item.title for item in items_of(listing(path))]


def add(title, hx=False):
    return post("/todos", {"title": title}, hx=hx)


def add_all(*titles_):
    for title in titles_:
        result = add(title)
        assert result.status == 302, str(result)


def find(title, path="/todos"):
    for item in items_of(listing(path)):
        if item.title == title:
            return item
    raise AssertionError(f"no item titled {title!r} in {path}; have {titles(path)}")


def toggle(title, hx=False):
    return post(f"/todos/{find(title).id}/toggle", hx=hx)


def rename(title, new_title):
    return post(f"/todos/{find(title).id}", {"title": new_title})


def delete(title):
    return post(f"/todos/{find(title).id}/delete")


MISSING_ID = "00000000-0000-4000-8000-000000000000"


@pytest.fixture(autouse=True)
def clean_slate():
    """Every scenario starts from an empty list (feature file Background)."""
    for item in items_of(listing()):
        post(f"/todos/{item.id}/delete")
    remaining = items_of(listing())
    assert remaining == [], f"could not empty the list; {remaining} remain"
    yield


# --------------------------------------------------------------------------
# 1–5  entry points
# --------------------------------------------------------------------------

def test_01_root_redirects_to_the_todo_list():
    result = get("/")
    assert result.status == 307, str(result)
    assert result.header("Location") == "/todos", str(result)


def test_02_todo_list_page_renders_the_application_shell():
    result = get("/todos")
    assert result.status == 200, str(result)
    body = normalise(result.body)
    assert is_whole_document(result.body), str(result)
    assert f"<title>{PAGE_TITLE}</title>" in body, body[:400]
    assert "<h1>todos</h1>" in body, body[:800]
    new_todo = re.search(r'<input[^>]*class="new-todo"[^>]*>', body)
    assert new_todo, body[:800]
    assert 'name="title"' in new_todo.group(0), new_todo.group(0)
    assert 'placeholder="What needs to be done?"' in new_todo.group(0), new_todo.group(0)
    section = re.search(r'<section class="todoapp"[^>]*>', body)
    assert section and 'hx-boost="true"' in section.group(0), body[:800]
    assert "Double-click to edit a todo" in body
    assert "todomvc.com" in body


def test_03_front_end_assets_are_served():
    script = get("/todos.js")
    assert script.status == 200, str(script)
    assert "clear-add-todo" in script.body, script.body[:300]

    library = get("/webjars/htmx.org/dist/htmx.min.js")
    assert library.status == 200, str(library)
    assert len(library.body) > 1000, len(library.body)

    stylesheet = get("/webjars/todomvc-app-css/index.css")
    assert stylesheet.status == 200, str(stylesheet)
    assert "todoapp" in stylesheet.body, stylesheet.body[:300]


def test_04_unknown_path_is_not_served():
    result = get("/no-such-page")
    assert result.status == 404, str(result)


def test_05_every_route_is_reachable_with_a_trailing_slash():
    result = get("/todos/")
    assert result.status == 200, str(result)
    assert "<h1>todos</h1>" in normalise(result.body), result.body[:400]
    for path in ("/todos/active/", "/todos/completed/"):
        result = get(path)
        assert result.status == 200, str(result)
    result = post("/todos/toggle-all/")
    assert result.status == 302, str(result)


# --------------------------------------------------------------------------
# 6–13  creating
# --------------------------------------------------------------------------

def test_06_plain_add_redirects_to_the_list():
    result = add("Buy milk")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    assert titles() == ["Buy milk"]


def test_07_a_new_todo_is_active():
    add("Buy milk")
    item = find("Buy milk")
    assert item.completed is False, item.markup
    assert item.checked is False, item.markup


def test_08_hypermedia_add_returns_only_the_new_item():
    result = add("Buy milk", hx=True)
    assert result.status == 200, str(result)
    assert not is_whole_document(result.body), result.body[:400]
    assert normalise(result.body).startswith("<li"), result.body[:200]
    item = fragment_item(result.body)
    assert item.title == "Buy milk", item.markup
    assert uuid.UUID(item.id)
    # and the fragment really is the row the list now shows
    assert find("Buy milk").id == item.id


def test_09_hypermedia_add_tells_the_client_to_clear_the_input():
    result = add("Buy milk", hx=True)
    assert result.header("HX-Trigger") == "clear-add-todo", str(result)


def test_10_plain_add_carries_no_client_trigger():
    result = add("Buy milk")
    assert result.header("HX-Trigger") is None, str(result)


def test_11_add_without_a_title_is_rejected():
    result = post("/todos", {})
    assert result.status >= 400, str(result)
    assert items_of(listing()) == []


def test_12_todos_are_listed_oldest_first():
    add_all("one", "two", "three")
    assert titles() == ["one", "two", "three"]


def test_13_every_item_carries_the_routes_the_client_needs():
    add("Buy milk")
    item = find("Buy milk")
    markup = item.markup
    assert f'action="/todos/{item.id}/toggle"' in markup, markup
    assert f'action="/todos/{item.id}/delete"' in markup, markup
    assert f'action="/todos/{item.id}"' in markup, markup
    assert f'hx-post="/todos/{item.id}/toggle"' in markup, markup
    assert f'hx-target="#item-{item.id}"' in markup, markup
    # the checkbox submits the toggle form rather than navigating
    toggle_input = re.search(r'<input[^>]*class="toggle"[^>]*>', markup, re.S).group(0)
    assert f"toggle-{item.id}" in toggle_input, toggle_input
    assert "requestSubmit()" in toggle_input, toggle_input


# --------------------------------------------------------------------------
# 14–17  the counter
# --------------------------------------------------------------------------

def test_14_counter_reports_the_number_of_active_items():
    add_all("one", "two")
    assert counter_of(listing()) == "2 items left"


def test_15_counter_is_singular_for_one_active_item():
    add("one")
    assert counter_of(listing()) == "1 item left"


def test_16_counter_counts_only_active_items():
    add_all("one", "two")
    toggle("one")
    assert counter_of(listing()) == "1 item left"


def test_17_counter_reads_zero_for_an_empty_list():
    assert counter_of(listing()) == "0 items left"


# --------------------------------------------------------------------------
# 18–25  toggling
# --------------------------------------------------------------------------

def test_18_plain_toggle_redirects_and_completes_the_item():
    add("Buy milk")
    result = toggle("Buy milk")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    item = find("Buy milk")
    assert item.completed is True, item.markup
    assert item.checked is True, item.markup


def test_19_hypermedia_toggle_returns_only_that_item():
    add("Buy milk")
    result = toggle("Buy milk", hx=True)
    assert result.status == 200, str(result)
    assert not is_whole_document(result.body), result.body[:400]
    item = fragment_item(result.body)
    assert item.title == "Buy milk", item.markup
    assert item.completed is True, item.markup
    assert item.checked is True, item.markup


def test_20_toggling_a_completed_todo_makes_it_active_again():
    add("Buy milk")
    toggle("Buy milk")
    result = toggle("Buy milk")
    assert result.status == 302, str(result)
    assert find("Buy milk").completed is False
    assert counter_of(listing()) == "1 item left"


def test_21_toggling_an_unknown_identifier_is_rejected():
    result = post(f"/todos/{MISSING_ID}/toggle")
    assert result.status >= 400, str(result)


def test_22_toggle_all_completes_every_item():
    add_all("one", "two")
    result = post("/todos/toggle-all")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    assert all(item.completed for item in items_of(listing()))
    assert counter_of(listing()) == "0 items left"


def test_23_toggle_all_again_reactivates_every_item():
    add_all("one", "two")
    post("/todos/toggle-all")
    result = post("/todos/toggle-all")
    assert result.status == 302, str(result)
    assert not any(item.completed for item in items_of(listing()))
    assert counter_of(listing()) == "2 items left"


def test_24_toggle_all_completes_the_rest_when_some_are_completed():
    add_all("one", "two")
    toggle("one")
    post("/todos/toggle-all")
    assert all(item.completed for item in items_of(listing()))


def test_25_toggle_all_on_an_empty_list_changes_nothing():
    result = post("/todos/toggle-all")
    assert result.status == 302, str(result)
    assert items_of(listing()) == []


# --------------------------------------------------------------------------
# 26–29  filters
# --------------------------------------------------------------------------

def test_26_active_filter_lists_only_active_todos():
    add_all("one", "two")
    toggle("one")
    result = get("/todos/active")
    assert result.status == 200, str(result)
    assert [item.title for item in items_of(result.body)] == ["two"]


def test_27_completed_filter_lists_only_completed_todos():
    add_all("one", "two")
    toggle("one")
    result = get("/todos/completed")
    assert result.status == 200, str(result)
    assert [item.title for item in items_of(result.body)] == ["one"]


def test_28_each_filter_page_marks_its_own_filter_as_selected():
    assert selected_filter(listing("/todos")) == ["All"]
    assert selected_filter(listing("/todos/active")) == ["Active"]
    assert selected_filter(listing("/todos/completed")) == ["Completed"]


def test_29_every_filter_page_renders_the_same_shell():
    for path in ("/todos/active", "/todos/completed"):
        body = normalise(listing(path))
        assert f"<title>{PAGE_TITLE}</title>" in body, path
        assert "<h1>todos</h1>" in body, path
        assert 'class="new-todo"' in body, path


# --------------------------------------------------------------------------
# 30–32  renaming
# --------------------------------------------------------------------------

def test_30_renaming_a_todo_replaces_its_title():
    add("Buy milk")
    result = rename("Buy milk", "Buy oat milk")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    assert titles() == ["Buy oat milk"]


def test_31_the_edit_field_is_prefilled_with_the_current_title():
    add("Buy milk")
    assert find("Buy milk").edit_value == "Buy milk"


def test_32_renaming_an_unknown_identifier_is_rejected():
    result = post(f"/todos/{MISSING_ID}", {"title": "nope"})
    assert result.status >= 400, str(result)


# --------------------------------------------------------------------------
# 33–36  deleting
# --------------------------------------------------------------------------

def test_33_deleting_a_todo_removes_it_and_leaves_the_others():
    add_all("one", "two")
    result = delete("one")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    assert titles() == ["two"]


def test_34_deleting_an_unknown_identifier_changes_nothing():
    add("one")
    result = post(f"/todos/{MISSING_ID}/delete")
    assert result.status == 302, str(result)
    assert titles() == ["one"]


def test_35_clearing_completed_removes_only_the_completed_ones():
    add_all("one", "two", "three")
    toggle("one")
    toggle("three")
    result = post("/todos/clear-completed")
    assert result.status == 302, str(result)
    assert result.header("Location") == "/todos", str(result)
    assert titles() == ["two"]


def test_36_clearing_completed_with_none_completed_changes_nothing():
    add_all("one", "two")
    result = post("/todos/clear-completed")
    assert result.status == 302, str(result)
    assert titles() == ["one", "two"]


# --------------------------------------------------------------------------
# 37  persistence
# --------------------------------------------------------------------------

def test_37_every_change_survives_into_later_requests():
    add("Buy milk")
    created_id = find("Buy milk").id
    rename("Buy milk", "Buy oat milk")
    toggle("Buy oat milk")
    item = find("Buy oat milk")
    assert titles() == ["Buy oat milk"]
    assert item.completed is True, item.markup
    assert counter_of(listing()) == "0 items left"
    assert item.id == created_id, "the row was replaced rather than updated"
