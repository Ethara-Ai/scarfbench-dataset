"""ScarfBench Python standalone smoke (FastAPI → Django variant).
Same behavior expected: JSON {"message":"Greetings!"} on /standalone and /.
"""
import time, requests, pytest

BASE = "http://localhost:8080"

@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(30):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except Exception:
            time.sleep(1)
    pytest.fail(f"App unreachable at {BASE}")

def test_standalone_returns_greetings():
    r = requests.get(f"{BASE}/standalone", timeout=10)
    assert r.status_code == 200, r.text
    body = r.json()
    assert body.get("message") == "Greetings!", f"Unexpected: {body}"

def test_root_returns_greetings():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200, r.text
    body = r.json()
    assert body.get("message") == "Greetings!", f"Unexpected: {body}"

def test_standalone_returns_json():
    r = requests.get(f"{BASE}/standalone", timeout=10)
    ct = (r.headers.get("content-type") or "").lower()
    assert "application/json" in ct, f"Expected JSON, got: {ct}"
