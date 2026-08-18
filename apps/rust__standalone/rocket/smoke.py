"""standalone smoke."""
import time, requests, pytest
BASE = "http://localhost:8080"
@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(60):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except: time.sleep(1)
    pytest.fail("unreachable")
def test_standalone_greetings():
    r = requests.get(f"{BASE}/standalone", timeout=10); assert r.status_code == 200
    assert r.json().get("message") == "Greetings!"
def test_root_greetings():
    r = requests.get(f"{BASE}/", timeout=10); assert r.status_code == 200
    assert r.json().get("message") == "Greetings!"
def test_content_type_json():
    r = requests.get(f"{BASE}/standalone", timeout=10)
    assert "application/json" in (r.headers.get("content-type") or "").lower()
