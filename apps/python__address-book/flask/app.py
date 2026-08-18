from flask import Flask, jsonify, request, Response

app = Flask(__name__)

@app.route("/contacts", methods=["GET"])
def handler():
    return jsonify([])


@app.route("/", methods=["GET"])
def root():
    return "OK"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
