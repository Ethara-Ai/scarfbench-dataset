"""Behavioural oracle for squirrel-sql-web (framework-neutral).

Derived from the SOURCE application: a JAX-RS API mounted at @ApplicationPath("/ws")
whose resources require an authenticated session. Assertions cover only the
observable HTTP boundary -- never internal class layout.
"""
import os, requests
BASE = os.environ.get("BASE_URL", "http://localhost:18080")

def test_drivers_requires_auth():
    assert requests.get(f"{BASE}/ws/Drivers", timeout=15).status_code == 401

def test_aliases_requires_auth():
    assert requests.get(f"{BASE}/ws/Aliases", timeout=15).status_code == 401

def test_current_user_requires_auth():
    assert requests.get(f"{BASE}/ws/CurrentUser", timeout=15).status_code == 401

def test_unknown_resource_is_404():
    assert requests.get(f"{BASE}/ws/NoSuchResource", timeout=15).status_code in (401, 404)

def test_server_is_serving():
    assert requests.get(f"{BASE}/", timeout=15).status_code < 500
