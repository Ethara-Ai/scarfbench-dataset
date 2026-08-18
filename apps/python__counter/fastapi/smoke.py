"""ScarfBench Python counter smoke tests."""
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

def test_counter_first_hit():
    r = requests.get(f"{BASE}/counter", timeout=10)
    assert r.status_code == 200, r.text
    import re
    assert re.search(r"accessed \d+ time", r.text), f"Unexpected body: {r.text!r}"

def test_counter_increments():
    """Real state: two calls -> strictly increasing count."""
    import re
    def hits():
        m = re.search(r"accessed (\d+) time", requests.get(f"{BASE}/counter", timeout=10).text)
        assert m, "no count in response"
        return int(m.group(1))
    a = hits(); b = hits()
    assert b > a, f"counter did not increment: {a} -> {b}"

def test_root_live():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200
