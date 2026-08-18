from flask import Flask, jsonify, request, Response

app = Flask(__name__)

@app.route("/counter", methods=["GET"])
def handler():
    app.counter = getattr(app, "counter", 0) + 1
    return f"accessed {app.counter} time(s)"


@app.route("/", methods=["GET"])
def root():
    return "OK"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
