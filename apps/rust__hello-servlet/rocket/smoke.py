"""hello-servlet smoke."""
import time, requests, pytest
BASE = "http://localhost:8080"
@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(60):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except: time.sleep(1)
    pytest.fail("unreachable")
def test_greeting_name():
    r = requests.get(f"{BASE}/greeting", params={"name":"Duke"}, timeout=10)
    assert r.status_code == 200; assert "Hello" in r.text and "Duke" in r.text
def test_greeting_default():
    r = requests.get(f"{BASE}/greeting", timeout=10)
    assert r.status_code == 200; assert "Hello" in r.text and "World" in r.text
def test_root_hello():
    assert requests.get(f"{BASE}/", timeout=10).status_code == 200
