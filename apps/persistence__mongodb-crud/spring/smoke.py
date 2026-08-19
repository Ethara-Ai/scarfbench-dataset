"""Concretization of oracle/mongodb-crud.feature for the `mongodb-crud` family.

One pytest test per Gherkin scenario, asserting only at the externally
observable boundary (HTTP status, response media type, response payload,
persisted state as visible through the listing route).

Identical for every framework variant. Two variant-specific inputs exist:

    BASE_URL      the application root including its visible "/api" base path
                  (default http://localhost:8080/api)
    OPENAPI_PATH  absolute path of the generated OpenAPI document, which is a
                  genuinely framework-specific external convention
                  (Quarkus/SmallRye "/q/openapi", Spring/springdoc
                  "/v3/api-docs"); default is the Quarkus path

    BASE_URL=http://localhost:8080/api pytest -q oracle/smoke.py
"""

import os
import re
import uuid
from urllib.parse import urlsplit

import pytest
from playwright.sync_api import sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080/api")
OPENAPI_PATH = os.environ.get("OPENAPI_PATH", "/q/openapi")

OBJECT_ID = re.compile(r"^[0-9a-f]{24}$")
GREETING = "Hello from Quarkus REST"
MISSING_ID = "000000000000000000000000"
MALFORMED_ID = "not-an-objectid"


# --------------------------------------------------------------- fixtures


class _Api:
    """Thin BASE_URL-prefixing wrapper over Playwright's APIRequestContext."""

    def __init__(self, context, base_url):
        self._context = context
        self._base_url = base_url.rstrip("/")

    def _url(self, path):
        return f"{self._base_url}/{path.lstrip('/')}"

    def get(self, path, **kwargs):
        return self._context.get(self._url(path), **kwargs)

    def post(self, path, **kwargs):
        return self._context.post(self._url(path), **kwargs)

    def put(self, path, **kwargs):
        return self._context.put(self._url(path), **kwargs)

    def delete(self, path, **kwargs):
        return self._context.delete(self._url(path), **kwargs)

    def absolute(self, path):
        """GET a path resolved against the origin, ignoring the /api base path."""
        parts = urlsplit(self._base_url)
        return self._context.get(f"{parts.scheme}://{parts.netloc}{path}")


@pytest.fixture(scope="session")
def api():
    with sync_playwright() as p:
        context = p.request.new_context(timeout=30_000)
        yield _Api(context, BASE_URL)
        context.dispose()


# ---------------------------------------------------------------- helpers


def _media_type(response):
    """Media type without parameters -- charset is a framework detail."""
    return response.headers.get("content-type", "").split(";")[0].strip().lower()


def _create(api, name=None, age=30, payload=None):
    body = payload if payload is not None else {
        "name": name if name is not None else f"person-{uuid.uuid4().hex[:8]}",
        "age": age,
    }
    response = api.post("/person", data=body)
    assert response.status == 200, response.text()
    identifier = response.text()
    assert OBJECT_ID.match(identifier), f"not an ObjectId hex string: {identifier!r}"
    return identifier, body


def _listing(api):
    response = api.get("/persons")
    assert response.status == 200, response.text()
    people = response.json()
    assert isinstance(people, list)
    return people


def _find(api, identifier):
    for person in _listing(api):
        if person["id"] == identifier:
            return person
    return None


# --------------------------------------------------------------- greeting


def test_read_the_greeting(api):
    response = api.get("/hello")
    assert response.status == 200
    assert _media_type(response) == "application/json"
    assert response.text() == GREETING


# ----------------------------------------------------------------- create


def test_create_a_person(api):
    response = api.post("/person", data={"name": "John Doe", "age": 30})
    assert response.status == 200
    assert _media_type(response) == "application/json"
    identifier = response.text()
    assert OBJECT_ID.match(identifier), identifier
    assert not identifier.startswith('"'), "identifier must not be a JSON-quoted string"


def test_create_a_person_with_an_empty_object(api):
    identifier, _ = _create(api, payload={})
    person = _find(api, identifier)
    assert person is not None
    assert person["name"] is None
    assert person["age"] is None


def test_two_creations_yield_two_distinct_identifiers(api):
    first, _ = _create(api)
    second, _ = _create(api)
    assert first != second
    identifiers = {person["id"] for person in _listing(api)}
    assert {first, second} <= identifiers


# ------------------------------------------------------------------- read


def test_list_all_people(api):
    identifier, _ = _create(api, name="John Doe", age=30)
    response = api.get("/persons")
    assert response.status == 200
    assert _media_type(response) == "application/json"
    people = response.json()
    assert isinstance(people, list), "payload must be a bare array with no root wrapper"
    assert all(set(person) == {"id", "name", "age"} for person in people)
    created = next(person for person in people if person["id"] == identifier)
    assert created["name"] == "John Doe"
    assert created["age"] == 30
    assert isinstance(created["id"], str), "id must render as a hexadecimal string"
    assert OBJECT_ID.match(created["id"])


# ---------------------------------------------------------------- update


def test_celebrate_an_anniversary(api):
    identifier, _ = _create(api, age=30)
    response = api.put(f"/person/{identifier}")
    assert response.status == 200
    assert _media_type(response) == "application/json"
    assert response.text() == "1"
    assert _find(api, identifier)["age"] == 31


def test_celebrate_the_anniversary_twice(api):
    identifier, _ = _create(api, age=30)
    for _ in range(2):
        response = api.put(f"/person/{identifier}")
        assert response.status == 200
        assert response.text() == "1"
    assert _find(api, identifier)["age"] == 32


def test_celebrate_the_anniversary_of_an_unknown_person(api):
    response = api.put(f"/person/{MISSING_ID}")
    assert response.status == 200
    assert response.text() == "0"


def test_celebrate_the_anniversary_with_a_malformed_identifier(api):
    response = api.put(f"/person/{MALFORMED_ID}")
    assert response.status == 500


# ---------------------------------------------------------------- delete


def test_delete_a_person(api):
    identifier, _ = _create(api)
    response = api.delete(f"/person/{identifier}")
    assert response.status == 200
    assert _media_type(response) == "application/json"
    assert response.text() == "1"
    assert _find(api, identifier) is None


def test_delete_the_same_person_twice(api):
    identifier, _ = _create(api)
    assert api.delete(f"/person/{identifier}").text() == "1"
    response = api.delete(f"/person/{identifier}")
    assert response.status == 200
    assert response.text() == "0"


def test_delete_an_unknown_person(api):
    response = api.delete(f"/person/{MISSING_ID}")
    assert response.status == 200
    assert response.text() == "0"


def test_delete_with_a_malformed_identifier(api):
    response = api.delete(f"/person/{MALFORMED_ID}")
    assert response.status == 500


# --------------------------------------------------------------- routing


def test_request_a_route_that_does_not_exist(api):
    assert api.get("/nope").status == 404


def test_use_a_method_the_route_does_not_support(api):
    assert api.get(f"/person/{MISSING_ID}").status == 405


# ------------------------------------------------------- api documentation


def test_serve_the_generated_api_documentation(api):
    response = api.absolute(OPENAPI_PATH)
    assert response.status == 200, f"no OpenAPI document at {OPENAPI_PATH}"
    document = response.text()
    for path in ("/api/hello", "/api/person", "/api/person/{id}", "/api/persons"):
        assert path in document, f"OpenAPI document does not declare {path}"


# -------------------------------------------------- end-to-end lifecycle


def test_full_crud_lifecycle(api):
    identifier, _ = _create(api, name="Ada Lovelace", age=36)
    assert _find(api, identifier)["age"] == 36

    assert api.put(f"/person/{identifier}").text() == "1"
    assert _find(api, identifier)["age"] == 37

    assert api.delete(f"/person/{identifier}").text() == "1"
    assert _find(api, identifier) is None

    response = api.delete(f"/person/{identifier}")
    assert response.status == 200
    assert response.text() == "0"
