"""ScarfBench Python address-book smoke tests."""
import time, requests, pytest, json
BASE = "http://localhost:8080"

@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(30):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except Exception:
            time.sleep(1)
    pytest.fail(f"App unreachable at {BASE}")

def test_contacts_returns_json_array():
    r = requests.get(f"{BASE}/contacts", timeout=10)
    assert r.status_code == 200, r.text
    body = r.json()
    assert isinstance(body, list), f"Expected JSON array, got {type(body).__name__}: {body!r}"

def test_contacts_content_type():
    r = requests.get(f"{BASE}/contacts", timeout=10)
    ct = (r.headers.get("content-type") or "")
    assert "json" in ct.lower(), f"Expected JSON content-type, got {ct!r}"

def test_root_live():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200
