"""ScarfBench Python mood smoke tests."""
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

def test_mood_report():
    r = requests.get(f"{BASE}/report", timeout=10)
    assert r.status_code == 200, r.text
    assert "mood" in r.text.lower(), f"missing 'mood' marker: {r.text!r}"
    assert "awake" in r.text.lower(), f"missing 'awake' state: {r.text!r}"

def test_root_live():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200
