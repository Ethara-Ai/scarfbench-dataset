"""
Smoke tests for the Cargo Tracker Spring Boot application.
Validates that the application starts correctly and key REST endpoints are functional.
"""

import json
import os
import sys
import time
import urllib.request
import urllib.error


# Set at import time so the grader works under pytest (test.sh runs
# `pytest smoke.py`, which never executes the __main__ block below).
# Defaults to the Spring Boot context path; override via env if needed.
BASE_URL = os.getenv("CARGOTRACKER_BASE_URL", "http://localhost:8080/cargo-tracker").rstrip("/")


def get_base_url():
    """Get the base URL (initialized at import; overridable via set_base_url)."""
    return BASE_URL


def set_base_url(url):
    global BASE_URL
    BASE_URL = url


def http_get(path, timeout=10):
    """Make an HTTP GET request and return (status_code, body)."""
    url = get_base_url() + path
    try:
        req = urllib.request.Request(url)
        resp = urllib.request.urlopen(req, timeout=timeout)
        body = resp.read().decode("utf-8")
        return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8") if e.fp else ""
        return e.code, body
    except Exception as e:
        return 0, str(e)


def http_post(path, data=None, content_type="application/x-www-form-urlencoded", timeout=10):
    """Make an HTTP POST request and return (status_code, body)."""
    url = get_base_url() + path
    try:
        if data is not None:
            if isinstance(data, dict) and content_type == "application/x-www-form-urlencoded":
                encoded = urllib.parse.urlencode(data).encode("utf-8")
            elif isinstance(data, str):
                encoded = data.encode("utf-8")
            else:
                encoded = json.dumps(data).encode("utf-8")
        else:
            encoded = b""
        req = urllib.request.Request(url, data=encoded, method="POST")
        req.add_header("Content-Type", content_type)
        resp = urllib.request.urlopen(req, timeout=timeout)
        body = resp.read().decode("utf-8")
        return resp.status, body
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8") if e.fp else ""
        return e.code, body
    except Exception as e:
        return 0, str(e)


import urllib.parse


def wait_for_app(max_wait=120):
    """Wait for the application to be ready."""
    print(f"Waiting for application at {get_base_url()} ...")
    start = time.time()
    while time.time() - start < max_wait:
        try:
            status, _ = http_get("/rest/booking/locations", timeout=5)
            if status == 200:
                print(f"Application is ready (took {int(time.time() - start)}s)")
                return True
        except Exception:
            pass
        time.sleep(2)
    print(f"Application did not become ready within {max_wait}s")
    return False


def test_list_locations():
    """Test that we can list shipping locations."""
    status, body = http_get("/rest/booking/locations")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    assert len(data) > 0, "Expected at least one location"
    print(f"  PASS: list locations returned {len(data)} locations")
    return True


def test_list_cargos():
    """Test that we can list all cargos."""
    status, body = http_get("/rest/booking/cargos")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, dict), f"Expected dict, got {type(data)}"
    print(f"  PASS: list cargos returned categories")
    return True


def test_list_tracking_ids():
    """Test that we can list tracking IDs."""
    status, body = http_get("/rest/booking/tracking/ids")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    print(f"  PASS: list tracking IDs returned {len(data)} IDs")
    return True


def test_graph_traversal():
    """Test the graph traversal shortest-path endpoint."""
    status, body = http_get("/rest/graph-traversal/shortest-path?origin=CNHKG&destination=FIHEL")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    # The endpoint returns a TransitPaths wrapper object with a "transitPaths" list
    if isinstance(data, dict):
        assert "transitPaths" in data, f"Expected 'transitPaths' key in response: {data.keys()}"
        routes = data["transitPaths"]
    else:
        routes = data
    assert isinstance(routes, list), f"Expected list of routes, got {type(routes)}"
    print(f"  PASS: graph traversal returned {len(routes)} routes")
    return True


def test_event_logger_tracking_ids():
    """Test the event logger tracking IDs endpoint."""
    status, body = http_get("/rest/event-logger/tracking-ids")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    print(f"  PASS: event logger tracking IDs returned {len(data)} IDs")
    return True


def test_event_logger_locations():
    """Test the event logger locations endpoint."""
    status, body = http_get("/rest/event-logger/locations")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    print(f"  PASS: event logger locations returned {len(data)} locations")
    return True


def test_event_logger_voyages():
    """Test the event logger voyages endpoint."""
    status, body = http_get("/rest/event-logger/voyages")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    print(f"  PASS: event logger voyages returned {len(data)} voyages")
    return True


def test_event_types():
    """Test the event types endpoint."""
    status, body = http_get("/rest/event-logger/event-types")
    assert status == 200, f"Expected 200, got {status}: {body}"
    data = json.loads(body)
    assert isinstance(data, list), f"Expected list, got {type(data)}"
    assert len(data) > 0, "Expected at least one event type"
    print(f"  PASS: event types returned {len(data)} types")
    return True


def run_tests():
    """Run all smoke tests."""
    tests = [
        ("List Locations", test_list_locations),
        ("List Cargos", test_list_cargos),
        ("List Tracking IDs", test_list_tracking_ids),
        ("Graph Traversal", test_graph_traversal),
        ("Event Logger Tracking IDs", test_event_logger_tracking_ids),
        ("Event Logger Locations", test_event_logger_locations),
        ("Event Logger Voyages", test_event_logger_voyages),
        ("Event Types", test_event_types),
    ]

    passed = 0
    failed = 0
    errors = []

    for name, test_fn in tests:
        try:
            print(f"Running: {name}")
            test_fn()
            passed += 1
        except Exception as e:
            failed += 1
            errors.append((name, str(e)))
            print(f"  FAIL: {name}: {e}")

    print(f"\n{'='*50}")
    print(f"Results: {passed} passed, {failed} failed out of {len(tests)} tests")
    if errors:
        print("\nFailed tests:")
        for name, error in errors:
            print(f"  - {name}: {error}")

    return failed == 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python smoke.py <base_url>")
        print("Example: python smoke.py http://localhost:8080/cargo-tracker")
        sys.exit(1)

    set_base_url(sys.argv[1].rstrip("/"))

    if not wait_for_app():
        sys.exit(1)

    success = run_tests()
    sys.exit(0 if success else 1)
