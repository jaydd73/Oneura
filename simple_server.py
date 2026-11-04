from flask import Flask, request, jsonify
import time

app = Flask(__name__)

# This is a "Mock" server. It pretends to be the AI so you can test the app.

@app.route('/', methods=['GET'])
def home():
    return "SERVER IS WORKING! If you see this on your tablet, we are connected."

@app.route('/think', methods=['POST'])
def think():
    data = request.json
    prompt = data.get('prompt', '')
    return jsonify({
        "reply": f"I received your message: '{prompt}'. This is a test reply from your local server!",
        "innerMonologue": "The user seems to be testing the connection. I should respond helpfully."
    })

@app.route('/reflect', methods=['POST'])
def reflect():
    return jsonify({
        "insight": "You have been testing the app successfully today.",
        "insightType": "observation"
    })

@app.route('/dream', methods=['POST'])
def dream():
    return jsonify({
        "narrative": "A digital landscape where connections form like glowing threads.",
        "insight": "Connectivity is the foundation of understanding."
    })

if __name__ == '__main__':
    # Runs the server on port 8000
    print("--- Oneura Mock Server Starting ---")
    print("Connect your app to: http://10.0.2.2:8000 (if using Emulator)")
    app.run(host='0.0.0.0', port=8000)
