"""Behavioral smoke test for Flask→FastAPI kubernetes-hello-flask migration.

Deterministic behaviors of the original Flask app:
  GET /               → 200 "Hello World! I can make change at route: /change"
  GET /change/1/25    → 200 [{"5": "quarters"}]              ($1.25 = 5×25c)
  GET /change/1/30    → 200 [{"5":"quarters"},{"1":"nickels"}]   ($1.30 = 5q + 1n)
  GET /change/0/87    → 200 [{"3":"quarters"},{"1":"dimes"},{"2":"pennies"}]  ($0.87)
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


def _flatten(body):
    flat = {}
    for item in body:
        for k, v in item.items():
            flat[str(v)] = int(k)
    return flat


def test_root_greeting():
    r = requests.get(f"{BASE}/", timeout=10)
    assert r.status_code == 200, r.text
    assert "Hello World" in r.text
    assert "/change" in r.text


def test_change_1_25_five_quarters():
    """$1.25 = 125c = 5 quarters exactly, no remainder."""
    r = requests.get(f"{BASE}/change/1/25", timeout=10)
    assert r.status_code == 200, r.text
    flat = _flatten(r.json())
    assert flat.get("quarters") == 5, flat


def test_change_1_30_quarters_and_nickel():
    """$1.30 = 130c = 5 quarters + 1 nickel."""
    r = requests.get(f"{BASE}/change/1/30", timeout=10)
    assert r.status_code == 200, r.text
    flat = _flatten(r.json())
    assert flat.get("quarters") == 5, flat
    assert flat.get("nickels") == 1, flat


def test_change_0_87_mixed():
    """$0.87 = 3 quarters + 1 dime + 2 pennies."""
    r = requests.get(f"{BASE}/change/0/87", timeout=10)
    assert r.status_code == 200, r.text
    flat = _flatten(r.json())
    assert flat.get("quarters") == 3, flat
    assert flat.get("dimes") == 1, flat
    assert flat.get("pennies") == 2, flat


def test_change_returns_json():
    r = requests.get(f"{BASE}/change/1/25", timeout=10)
    ct = (r.headers.get("content-type") or "").lower()
    assert "json" in ct, ct
