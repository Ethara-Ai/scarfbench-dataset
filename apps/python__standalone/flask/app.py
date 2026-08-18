"""Flask standalone service — Python equivalent of Java Spring standalone.
Business behavior: GET /standalone returns "Greetings!"
"""
from flask import Flask, jsonify

app = Flask(__name__)


class StandaloneService:
    """Business logic — equivalent of Java's StandaloneService."""

    def return_message(self):
        return "Greetings!"


service = StandaloneService()


@app.route("/standalone", methods=["GET"])
def greet():
    return jsonify({"message": service.return_message()})


@app.route("/", methods=["GET"])
def root():
    return jsonify({"message": service.return_message()})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
