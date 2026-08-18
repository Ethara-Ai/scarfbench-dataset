from flask import Flask, jsonify, request, Response

app = Flask(__name__)

@app.route("/decorators", methods=["GET", "POST"])
def handler():
    return "Coded: " + request.form.get("inputString", request.args.get("inputString", "")).translate(str.maketrans("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ", "bcdefghijklmnopqrstuvwxyzaBCDEFGHIJKLMNOPQRSTUVWXYZA"))


@app.route("/", methods=["GET"])
def root():
    return "OK"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
