"""address-book smoke — JSON contacts array."""
import time, requests, pytest
BASE = "http://localhost:8080"
@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(30):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except Exception:
            time.sleep(1)
    pytest.fail("unreachable")
def test_contacts_json_array():
    r = requests.get(f"{BASE}/contacts", timeout=10)
    assert r.status_code == 200
    assert isinstance(r.json(), list)
def test_contacts_content_type():
    r = requests.get(f"{BASE}/contacts", timeout=10)
    assert "json" in (r.headers.get("content-type") or "").lower()
def test_root_ok():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200
