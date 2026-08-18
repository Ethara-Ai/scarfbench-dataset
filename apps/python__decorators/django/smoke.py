"""ScarfBench Python decorators smoke tests.

Preserves Flask's dual input pattern:
  request.form.get("inputString", request.args.get("inputString", ""))

Cipher is Caesar+1 shift on ASCII letters (a→b ... z→a, A→B ... Z→A).
"""
import time, requests, pytest

BASE = "http://localhost:8080"

def shift(s):
    tbl = str.maketrans("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                        "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA")
    return s.translate(tbl)

@pytest.fixture(scope="session", autouse=True)
def wait_ready():
    for _ in range(30):
        try:
            requests.get(f"{BASE}/", timeout=2); return
        except Exception:
            time.sleep(1)
    pytest.fail(f"App unreachable at {BASE}")


# ---------- GET (query-string) ----------
def test_get_hello():
    r = requests.get(f"{BASE}/decorators", params={"inputString": "hello"}, timeout=10)
    assert r.status_code == 200, r.text
    assert r.text == f"Coded: {shift('hello')}", f"Expected 'Coded: ifmmp', got {r.text!r}"

def test_get_world():
    r = requests.get(f"{BASE}/decorators", params={"inputString": "World"}, timeout=10)
    assert r.status_code == 200, r.text
    assert r.text == f"Coded: {shift('World')}", f"Expected 'Coded: Xpsme', got {r.text!r}"

def test_get_empty():
    r = requests.get(f"{BASE}/decorators", timeout=10)
    assert r.status_code == 200, r.text
    assert r.text == "Coded: ", f"Expected 'Coded: ', got {r.text!r}"


# ---------- POST (form-body) ----------
def test_post_hello():
    r = requests.post(f"{BASE}/decorators", data={"inputString": "hello"}, timeout=10)
    assert r.status_code == 200, f"HTTP {r.status_code}: {r.text[:200]}"
    assert r.text == f"Coded: {shift('hello')}", f"Expected 'Coded: ifmmp', got {r.text!r}"

def test_post_scarfbench():
    r = requests.post(f"{BASE}/decorators", data={"inputString": "ScarfBench"}, timeout=10)
    assert r.status_code == 200, f"HTTP {r.status_code}: {r.text[:200]}"
    assert r.text == f"Coded: {shift('ScarfBench')}", f"Expected 'Coded: TdbsgCfodi', got {r.text!r}"


# ---------- Content type ----------
def test_get_plain_text():
    r = requests.get(f"{BASE}/decorators", params={"inputString": "x"}, timeout=10)
    ct = (r.headers.get("content-type") or "").lower()
    assert "text/plain" in ct or "text/html" in ct, f"Unexpected content-type: {ct!r}"
    # must not be JSON (would break Flask parity)
    assert "json" not in ct, f"Response should not be JSON, got {ct!r}"


# ---------- Root ----------
def test_root_ok():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200, r.text
    assert r.text == "OK", f"Expected 'OK', got {r.text!r}"
