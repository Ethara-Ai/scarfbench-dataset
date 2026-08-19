"""Smoke tests for the "otel-example-quarkus" app family (ScarfBench behavioral oracle).

Framework-neutral: probes only the HTTP boundary of a deployed container.
One pytest test per scenario in otel-example-quarkus.feature.

Run:
    BASE_URL=http://localhost:8080 pytest -q smoke.py
"""

from __future__ import annotations

import os
import uuid

import pytest
from playwright.sync_api import APIRequestContext, sync_playwright

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")


class _Api:
    def __init__(self, request_context: APIRequestContext) -> None:
        self._ctx = request_context

    def get(self, path: str):
        return self._ctx.get(f"{BASE_URL}{path}")

    def post(self, path: str, body=None):
        return self._ctx.post(f"{BASE_URL}{path}", data=body if body is not None else {})

    def put(self, path: str, body=None):
        return self._ctx.put(f"{BASE_URL}{path}", data=body if body is not None else {})

    def delete(self, path: str):
        return self._ctx.delete(f"{BASE_URL}{path}")


@pytest.fixture(scope="session")
def api():
    with sync_playwright() as p:
        ctx = p.request.new_context(timeout=30_000)
        yield _Api(ctx)
        ctx.dispose()


def _unique_email(tag: str = "oracle") -> str:
    return f"{tag}-{uuid.uuid4().hex[:10]}@example.com"


@pytest.fixture()
def created_user(api: _Api):
    email = _unique_email("crud")
    res = api.post("/api/users", {"name": "Oracle User", "email": email, "bio": "created by oracle"})
    assert res.status == 201, f"user create failed: {res.status} {res.text()}"
    user = res.json()
    yield user
    api.delete(f"/api/users/{user['id']}")


def test_list_users_returns_seeded_data(api: _Api):
    res = api.get("/api/users")
    assert res.status == 200
    users = res.json()
    assert isinstance(users, list) and len(users) >= 5
    for field in ("id", "name", "email", "createdAt", "updatedAt"):
        assert field in users[0]
    john = [u for u in users if u["email"] == "john.doe@example.com"]
    assert john and john[0]["name"] == "John Doe"


def test_get_user_by_id(api: _Api):
    res = api.get("/api/users/1")
    assert res.status == 200
    body = res.json()
    assert body["id"] == 1 and body["email"]


def test_get_unknown_id_is_structured_404(api: _Api):
    res = api.get("/api/users/99999")
    assert res.status == 404
    body = res.json()
    assert body["error"] == "User not found with id: 99999"
    assert "timestamp" in body


def test_get_user_by_email(api: _Api):
    res = api.get("/api/users/email/john.doe@example.com")
    assert res.status == 200
    assert res.json()["email"] == "john.doe@example.com"


def test_get_unknown_email_is_structured_404(api: _Api):
    missing = _unique_email("missing")
    res = api.get(f"/api/users/email/{missing}")
    assert res.status == 404
    assert res.json()["error"] == f"User not found with email: {missing}"


def test_create_user_persists_with_positive_id(api: _Api):
    email = _unique_email("create")
    res = api.post("/api/users", {"name": "Test User", "email": email, "bio": "smoke"})
    assert res.status == 201
    body = res.json()
    assert body["name"] == "Test User" and body["email"] == email
    assert isinstance(body["id"], int) and body["id"] > 0
    assert api.get(f"/api/users/{body['id']}").status == 200
    api.delete(f"/api/users/{body['id']}")


def test_create_duplicate_email_rejected(api: _Api, created_user):
    res = api.post("/api/users", {"name": "Someone Else", "email": created_user["email"]})
    assert res.status == 400
    assert f"Email already exists: {created_user['email']}" in res.json()["error"]


def test_create_invalid_email_rejected(api: _Api):
    res = api.post("/api/users", {"name": "Valid Name", "email": "not-an-email"})
    assert res.status == 400
    body = res.json()
    assert "error" in body and "timestamp" in body


def test_create_blank_name_rejected(api: _Api):
    res = api.post("/api/users", {"name": "", "email": _unique_email("blank")})
    assert res.status == 400
    body = res.json()
    assert "error" in body and "timestamp" in body


def test_update_user_changes_fields(api: _Api, created_user):
    new_email = _unique_email("updated")
    res = api.put(
        f"/api/users/{created_user['id']}",
        {"name": "Updated Name", "email": new_email, "bio": "updated"},
    )
    assert res.status == 200
    body = res.json()
    assert body["name"] == "Updated Name" and body["email"] == new_email


def test_update_unknown_user_is_structured_404(api: _Api):
    res = api.put(
        "/api/users/99999",
        {"name": "Ghost User", "email": _unique_email("ghost")},
    )
    assert res.status == 404
    assert res.json()["error"] == "User not found with id: 99999"


def test_update_to_taken_email_rejected(api: _Api, created_user):
    other_email = _unique_email("other")
    other = api.post("/api/users", {"name": "Other User", "email": other_email}).json()
    try:
        res = api.put(
            f"/api/users/{created_user['id']}",
            {"name": "Oracle User", "email": other_email},
        )
        assert res.status == 400
        assert "Email already exists:" in res.json()["error"]
    finally:
        api.delete(f"/api/users/{other['id']}")


def test_delete_user_removes_it(api: _Api):
    created = api.post(
        "/api/users", {"name": "Delete Me", "email": _unique_email("delete")}
    ).json()
    res = api.delete(f"/api/users/{created['id']}")
    assert res.status == 204
    assert api.get(f"/api/users/{created['id']}").status == 404


def test_delete_unknown_user_is_structured_404(api: _Api):
    res = api.delete("/api/users/99999")
    assert res.status == 404
    assert res.json()["error"] == "User not found with id: 99999"


def test_search_case_insensitive_partial(api: _Api):
    res = api.get("/api/users/search?name=jane")
    assert res.status == 200
    names = [u["name"] for u in res.json()]
    assert "Jane Smith" in names


def test_search_without_query_rejected(api: _Api):
    res = api.get("/api/users/search")
    assert res.status == 400
    assert res.json()["error"] == "Search query 'name' is required"


def test_recent_users_within_window(api: _Api):
    res = api.get("/api/users/recent?days=30")
    assert res.status == 200
    body = res.json()
    assert isinstance(body, list) and len(body) >= 5


def test_recent_users_non_positive_window_rejected(api: _Api):
    res = api.get("/api/users/recent?days=-1")
    assert res.status == 400
    assert res.json()["error"] == "Days must be a positive number"


def test_count_reports_persisted_users(api: _Api):
    res = api.get("/api/users/count")
    assert res.status == 200
    assert res.json()["count"] >= 5


def test_app_level_health_up(api: _Api):
    res = api.get("/api/users/health")
    assert res.status == 200
    body = res.json()
    assert body["status"] == "UP" and body["service"] == "UserService"
    assert "timestamp" in body


def test_platform_health_up(api: _Api):
    res = api.get("/q/health")
    assert res.status == 200
    assert res.json()["status"] == "UP"
