"""
ScarfBench address-book smoke tests — accepts either
(a) JSF-preservation (/, /index[.xhtml], /contact/List[.xhtml]) OR
(b) REST rewrite (/contacts POST returns id, GET returns list).

Either shape is a valid migration outcome; both are behaviorally correct.
The test ensures the app SERVES SOMETHING at these expected routes.
"""
import time
import requests
import pytest

BASE_URL = "http://localhost:8080"

@pytest.fixture(scope="session", autouse=True)
def wait_for_ready():
    for _ in range(30):
        try:
            requests.get(f"{BASE_URL}/", timeout=2)
            return
        except Exception:
            time.sleep(1)
    pytest.fail(f"App did not become reachable at {BASE_URL}")

def probe(paths, methods=("GET",), data=None):
    """Return (path, status_code, body) of the first path returning 2xx or 4xx (not connection error), else last attempt."""
    last = None
    for path in paths:
        for method in methods:
            try:
                if method == "GET":
                    r = requests.get(f"{BASE_URL}{path}", timeout=10)
                elif method == "POST":
                    r = requests.post(f"{BASE_URL}{path}", json=data, timeout=10)
                else:
                    continue
                last = (path, r.status_code, r.text[:200])
                if r.status_code < 400:
                    return last
            except Exception as e:
                last = (path, "conn-err", str(e)[:200])
    return last

def test_root_serves_200():
    """The app serves SOMETHING at root (either JSF welcome or REST info)."""
    path, code, body = probe(["/", "/contacts", "/index", "/index.xhtml"])
    assert code == 200, f"No root route worked. Last: {path} → {code}: {body}"

def test_list_endpoint():
    """Some contact-list endpoint returns 200 (JSF list page OR REST list JSON)."""
    path, code, body = probe(["/contact/List.xhtml", "/contact/List", "/contacts"])
    assert code == 200, f"No list endpoint worked. Last: {path} → {code}: {body}"

def test_create_or_list_form():
    """Some create endpoint accepts input OR list endpoint serves data."""
    # Try REST first
    try:
        r = requests.post(f"{BASE_URL}/contacts", json={"firstName":"John","lastName":"Doe"}, timeout=10)
        if r.status_code in (200, 201, 204):
            return
    except Exception:
        pass
    # Try JSF create page
    path, code, body = probe(["/contact/Create.xhtml", "/contact/Create"])
    assert code == 200, f"Neither REST POST /contacts nor JSF /contact/Create page works. Last: {path} → {code}: {body}"
