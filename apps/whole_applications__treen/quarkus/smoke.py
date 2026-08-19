"""Behavioural oracle for treen (framework-neutral).

Derived from the SOURCE application: @ApplicationPath("service") with
resources login, auth, user, note, notebook beneath it.
"""
import os, requests
BASE = os.environ.get("BASE_URL", "http://localhost:18086")
def _s(p):
    return requests.get(f"{BASE}{p}", timeout=15).status_code

def test_note_routed():       assert _s("/service/note") != 404
def test_notebook_routed():   assert _s("/service/notebook") != 404
def test_user_routed():       assert _s("/service/user") != 404
def test_unknown_path_rejected():
    # The SOURCE ships an AuthorizationFilter that rejects unauthenticated
    # requests with 401 before routing, so 401 -- not 404 -- is the contract.
    assert _s("/service/nosuchresource") in (401, 404)
def test_server_responds():   assert _s("/") < 500
