"""counter smoke — state increment."""
import time, requests, pytest, re
BASE = "http://localhost:8080"
@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(60):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except: time.sleep(1)
    pytest.fail("unreachable")
def test_counter_first():
    r = requests.get(f"{BASE}/counter", timeout=10); assert r.status_code == 200
    assert re.search(r"accessed \d+ time", r.text)
def test_counter_increments():
    def hit():
        m = re.search(r"accessed (\d+) time", requests.get(f"{BASE}/counter", timeout=10).text); assert m; return int(m.group(1))
    a = hit(); b = hit(); assert b > a
def test_root_ok():
    assert requests.get(f"{BASE}/", timeout=10).status_code == 200
