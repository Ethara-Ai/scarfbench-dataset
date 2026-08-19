"""Smoke tests for the migrated Spring Boot application.

Run against a live instance on http://localhost:8080:

    uv run --with pytest,requests pytest smoke.py -v
"""

import uuid

import pytest
import requests

BASE = "http://localhost:8080"
TIMEOUT = 10


def _new_user_payload():
    return {
        "username": f"user-{uuid.uuid4().hex[:8]}",
        "email": f"{uuid.uuid4().hex[:8]}@example.com",
        "age": 30,
        "isPremium": True,
    }


# ---------------------------------------------------------------- hello

def test_hello_get():
    r = requests.get(f"{BASE}/hello", timeout=TIMEOUT)
    assert r.status_code == 200
    assert r.json() == {"message": "hello"}


def test_hello_post_with_body():
    r = requests.post(f"{BASE}/hello", json={"message": "hi there"}, timeout=TIMEOUT)
    assert r.status_code == 201
    assert r.json() == {"message": "hi there"}


def test_hello_post_without_body():
    r = requests.post(f"{BASE}/hello", timeout=TIMEOUT)
    assert r.status_code == 201
    assert r.json() == {"message": ""}


# ---------------------------------------------------------------- users CRUD

def test_users_crud_lifecycle():
    payload = _new_user_payload()

    # create
    r = requests.post(f"{BASE}/users", json=payload, timeout=TIMEOUT)
    assert r.status_code == 201, r.text
    created = r.json()
    assert created["username"] == payload["username"]
    assert created["email"] == payload["email"]
    assert "id" in created
    user_id = created["id"]

    # read one
    r = requests.get(f"{BASE}/users/{user_id}", timeout=TIMEOUT)
    assert r.status_code == 200
    assert r.json()["id"] == user_id

    # read all
    r = requests.get(f"{BASE}/users", timeout=TIMEOUT)
    assert r.status_code == 200
    users = r.json()["users"]
    assert any(u["id"] == user_id for u in users)

    # update (send back entity incl. id + version for optimistic locking)
    updated_payload = dict(created)
    updated_payload["username"] = "updated-" + payload["username"][:20]
    r = requests.put(f"{BASE}/users/{user_id}", json=updated_payload, timeout=TIMEOUT)
    assert r.status_code == 200, r.text
    assert r.json()["username"] == updated_payload["username"]

    # delete
    r = requests.delete(f"{BASE}/users/{user_id}", timeout=TIMEOUT)
    assert r.status_code == 200

    # gone
    r = requests.get(f"{BASE}/users/{user_id}", timeout=TIMEOUT)
    assert r.status_code == 404


def test_users_get_unknown_returns_404():
    r = requests.get(f"{BASE}/users/999999999", timeout=TIMEOUT)
    assert r.status_code == 404


def test_users_post_invalid_body_returns_400_with_code():
    r = requests.post(
        f"{BASE}/users",
        json={"username": "", "email": "", "age": 1, "isPremium": False},
        timeout=TIMEOUT,
    )
    assert r.status_code == 400
    body = r.json()
    assert body["code"] == "INVALID_REQUEST_BODY"
    assert "blank" in body["message"].lower()


# ---------------------------------------------------------------- boom / errors

def test_boom_returns_500_payload():
    r = requests.get(f"{BASE}/boom", timeout=TIMEOUT)
    assert r.status_code == 500
    body = r.json()
    assert body["code"] == "INTERNAL_SERVER_ERROR"
    assert body["message"] == "BOOM, request exploded"


# ---------------------------------------------------------------- health & metrics

@pytest.mark.parametrize("path", ["/health", "/health/live", "/health/ready"])
def test_health_endpoints_up(path):
    r = requests.get(f"{BASE}{path}", timeout=TIMEOUT)
    assert r.status_code == 200
    assert r.json()["status"] == "UP"


def test_metrics_exposes_custom_counters():
    # generate at least one request first
    requests.get(f"{BASE}/hello", timeout=TIMEOUT)
    r = requests.get(f"{BASE}/metrics", timeout=TIMEOUT)
    assert r.status_code == 200
    assert "http_requests" in r.text
    assert "http_request_duration" in r.text


# ---------------------------------------------------------------- jwt / security

def _get_token():
    r = requests.get(f"{BASE}/secured/token", timeout=TIMEOUT)
    assert r.status_code == 200
    token = r.json()["access_token"]
    assert token
    return token


def test_secured_token_issues_jwt():
    token = _get_token()
    assert token.count(".") == 2  # header.payload.signature


def test_secured_permit_all_anonymous():
    r = requests.get(f"{BASE}/secured/permit-all", timeout=TIMEOUT)
    assert r.status_code == 200
    assert "anonymous" in r.text
    assert "hasJwt: false" in r.text


def test_secured_permit_all_with_token():
    token = _get_token()
    r = requests.get(
        f"{BASE}/secured/permit-all",
        headers={"Authorization": f"Bearer {token}"},
        timeout=TIMEOUT,
    )
    assert r.status_code == 200
    assert "jdoe@quarkus.io" in r.text
    assert "hasJwt: true" in r.text


def test_secured_roles_allowed_without_token_is_401():
    r = requests.get(f"{BASE}/secured/roles-allowed", timeout=TIMEOUT)
    assert r.status_code == 401


def test_secured_roles_allowed_with_token():
    token = _get_token()
    r = requests.get(
        f"{BASE}/secured/roles-allowed",
        headers={"Authorization": f"Bearer {token}"},
        timeout=TIMEOUT,
    )
    assert r.status_code == 200
    assert "jdoe@quarkus.io" in r.text
    assert "hasJwt: true" in r.text
