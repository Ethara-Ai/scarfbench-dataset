"""decorators cipher smoke — GET + POST."""
import time, requests, pytest
BASE = "http://localhost:8080"
def shift(s):
    tbl = str.maketrans("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ","bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA")
    return s.translate(tbl)
@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(60):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except: time.sleep(1)
    pytest.fail("unreachable")
def test_get_hello():
    r = requests.get(f"{BASE}/decorators", params={"inputString":"hello"}, timeout=10)
    assert r.text == f"Coded: {shift('hello')}", r.text
def test_get_world():
    r = requests.get(f"{BASE}/decorators", params={"inputString":"World"}, timeout=10)
    assert r.text == f"Coded: {shift('World')}", r.text
def test_post_hello():
    r = requests.post(f"{BASE}/decorators", data={"inputString":"hello"}, timeout=10)
    assert r.text == f"Coded: {shift('hello')}", r.text
def test_post_scarfbench():
    r = requests.post(f"{BASE}/decorators", data={"inputString":"ScarfBench"}, timeout=10)
    assert r.text == f"Coded: {shift('ScarfBench')}", r.text
def test_root_ok():
    assert requests.get(f"{BASE}/", timeout=10).status_code == 200
