from flask import Flask, jsonify, request, send_from_directory
import os

app = Flask(__name__)

ROOT = os.path.dirname(os.path.abspath(__file__))
WEB_DIR = os.path.join(ROOT, "web")
ART_DIR = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
ALLOWED_ART = {"dream_light.jpg", "dream_deep.jpg", "dream_rem.jpg"}
STAGES = ("LIGHT", "DEEP", "REM")

# Mock server for the Android app and the browser page.
# It pretends to be the AI so the interface can be exercised without a model.


@app.route("/", methods=["GET"])
def home():
    return send_from_directory(WEB_DIR, "index.html")


@app.route("/web/<path:filename>", methods=["GET"])
def web_asset(filename):
    return send_from_directory(WEB_DIR, filename)


@app.route("/art/<path:filename>", methods=["GET"])
def art(filename):
    if filename not in ALLOWED_ART:
        return jsonify({"error": "not found"}), 404
    return send_from_directory(ART_DIR, filename)


@app.route("/health", methods=["GET"])
def health():
    return "SERVER IS WORKING! If you see this on your tablet, we are connected."


@app.route("/think", methods=["POST"])
def think():
    data = request.get_json(silent=True) or {}
    prompt = data.get("prompt", "")
    return jsonify({
        "reply": f"I received your message: '{prompt}'. This is a test reply from your local server!",
        "innerMonologue": "The user seems to be testing the connection. I should respond helpfully."
    })


@app.route("/reflect", methods=["POST"])
def reflect():
    data = request.get_json(silent=True) or {}
    if data.get("origin") == "manual":
        return jsonify({
            "insight": "Replies have been direct. Match the user's pace.",
            "insightType": "strategy shift",
            "strategy": "Answer in two sentences unless asked for more."
        })
    return jsonify({
        "insight": "You have been testing the app successfully today.",
        "insightType": "observation"
    })


@app.route("/dream", methods=["POST"])
def dream():
    data = request.get_json(silent=True) or {}
    stage = str(data.get("sleepStage", "REM")).upper()
    if stage not in STAGES:
        stage = "REM"
    if stage == "LIGHT":
        narrative = "Soft light over the last conversation, details still close to waking."
        insight = "The recent exchange is still warm enough to keep."
    elif stage == "DEEP":
        narrative = "The day's words sink into a quiet archive, sorted by how they felt."
        insight = "Patterns matter more than any single sentence."
    else:
        narrative = "A digital landscape where connections form like glowing threads."
        insight = "Connectivity is the foundation of understanding."
    return jsonify({
        "sleepStage": stage,
        "narrative": narrative,
        "memoryConsolidationSummary": narrative,
        "insight": insight,
        "personalityInsightEvolved": insight
    })


if __name__ == "__main__":
    print("--- Oneura Mock Server Starting ---")
    print("Open the page at: http://127.0.0.1:8000")
    print("Connect the Android app to: http://10.0.2.2:8000 (if using Emulator)")
    app.run(host="0.0.0.0", port=8000)
