const STORAGE_KEY = "oneura-web-v1";
const EVERY_N_TURNS = 4;
const STAGES = ["LIGHT", "DEEP", "REM"];
const STAGE_COLOR = { LIGHT: "#00e5ff", DEEP: "#7c4dff", REM: "#ff4081" };
const METRICS = [
  ["Engagement Coefficient", 0.87],
  ["Motor-Facade Sync", 0.94],
  ["Arboreal Meanders", 0.41],
  ["Spatial Phenomena", 0.76]
];

const screen = document.querySelector("#screen");
const composer = document.querySelector("#composer");
const chatInput = document.querySelector("#chat-input");
const sendButton = document.querySelector("#send-button");
const clearChat = document.querySelector("#clear-chat");
const tabs = [...document.querySelectorAll(".tabbar button")];

const state = loadState();

chatInput.addEventListener("input", () => {
  sendButton.disabled = chatInput.value.trim() === "" || state.busy;
});

composer.addEventListener("submit", (event) => {
  event.preventDefault();
  const text = chatInput.value;
  chatInput.value = "";
  sendButton.disabled = true;
  sendMessage(text);
});

clearChat.addEventListener("click", () => {
  state.messages = [];
  log("SYS", "Chat cleared");
  saveState();
  render();
});

for (const tab of tabs) {
  tab.addEventListener("click", () => {
    state.tab = tab.dataset.tab;
    saveState();
    render();
  });
}

render();
logOnceStarted();

function loadState() {
  const blank = {
    tab: "chat",
    messages: [],
    reflections: [],
    dreams: [],
    logs: [],
    started: false,
    busy: false,
    dreaming: false,
    reflecting: false,
    reflectionError: "",
    scanning: false,
    notice: "",
    settings: {
      provider: "MOCK",
      timeout: 120,
      baseUrl: "http://127.0.0.1:8000/",
      modelName: "tinyllama",
      geminiKey: "",
      xaiKey: "",
      xaiCompanion: "",
      xaiBackground: ""
    }
  };
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || "null");
    if (!saved) return blank;
    return { ...blank, ...saved, busy: false, dreaming: false, reflecting: false, scanning: false, settings: { ...blank.settings, ...(saved.settings || {}) } };
  } catch {
    return blank;
  }
}

function saveState() {
  const { busy, dreaming, reflecting, scanning, ...rest } = state;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(rest));
}

function logOnceStarted() {
  if (state.started) return;
  state.started = true;
  log("SYS", "App Started");
  saveState();
  if (state.tab === "logs") render();
}

function uid() {
  return crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random();
}

function log(tag, message) {
  state.logs.unshift({ id: uid(), tag, message, ts: Date.now() });
  if (state.logs.length > 500) state.logs.length = 500;
}

function render() {
  tabs.forEach((tab) => {
    const on = tab.dataset.tab === state.tab;
    tab.setAttribute("aria-current", on ? "page" : "false");
  });
  const onChat = state.tab === "chat";
  composer.hidden = !onChat;
  clearChat.hidden = !onChat;
  sendButton.disabled = chatInput.value.trim() === "" || state.busy;
  screen.replaceChildren();
  if (state.tab === "chat") renderChat();
  if (state.tab === "reflections") renderReflections();
  if (state.tab === "dreams") renderDreams();
  if (state.tab === "settings") renderSettings();
  if (state.tab === "logs") renderLogs();
}

function renderChat() {
  if (state.messages.length === 0) {
    screen.append(p("empty", "Outer voice. Say something and Oneura answers from the local mock server."));
  }
  const list = div("messages");
  for (const message of state.messages) {
    const row = div("bubble-row " + (message.sender === "USER" ? "user" : "ai"));
    const bubble = div("bubble");
    bubble.textContent = message.content;
    row.append(bubble);
    list.append(row);
  }
  screen.append(list);
  screen.scrollTop = screen.scrollHeight;
}

function renderReflections() {
  const stage = div("center-stage");
  stage.append(div("orb"));
  const button = actionButton(state.reflecting ? "Reflecting..." : "Trigger Reflection", () => reflect("META_MANUAL"));
  button.disabled = state.reflecting;
  stage.append(button);
  screen.append(stage);
  screen.append(p("hint", "Meta reflection runs every " + EVERY_N_TURNS + " of your messages, and when you tap the button. It is saved here and folded into later replies."));
  if (state.reflectionError) screen.append(p("error-line", state.reflectionError));
  const strategy = state.reflections.find((entry) => kindOf(entry.triggerSource) === "STRATEGY_SHIFT");
  if (strategy) {
    const card = div("card");
    const body = div("card-body");
    body.append(el("p", "label primary", "Current strategy"));
    body.append(p("", strategyLine(strategy.content)));
    card.append(body);
    screen.append(card);
  }
  if (state.reflections.length === 0) {
    screen.append(p("empty", "No reflections yet. Inner voice notes appear after Oneura replies. A meta reflection appears every " + EVERY_N_TURNS + " messages, or when you tap Trigger Reflection."));
  }
  const list = div("stack");
  for (const entry of state.reflections) {
    const card = div("card");
    const body = div("card-body");
    body.append(el("p", "label primary", displayLabel(entry.triggerSource)));
    const text = p("", entry.content);
    body.append(text);
    card.append(body);
    list.append(card);
  }
  screen.append(list);
}

function renderDreams() {
  const stage = div("center-stage");
  stage.append(div("dream-glow"));
  const button = actionButton(state.dreaming ? "Dreaming..." : "Consolidate Dream", dream);
  button.id = "dream-button";
  button.disabled = state.dreaming;
  stage.append(button);
  screen.append(stage);
  if (state.notice) screen.append(p("hint", state.notice));
  if (state.dreams.length === 0) {
    const empty = p("empty", "Oneura is awake. No dreams consolidated yet.");
    empty.style.textAlign = "center";
    empty.style.marginTop = "48px";
    screen.append(empty);
    return;
  }
  const list = div("stack");
  for (const dream of state.dreams) list.append(dreamCard(dream));
  screen.append(list);
}

function dreamCard(dream) {
  const color = STAGE_COLOR[dream.sleepStage] || "#00e5ff";
  const card = div("card");
  const art = div("dream-art");
  art.style.backgroundImage = "url('/art/dream_" + dream.sleepStage.toLowerCase() + ".jpg')";
  const pill = span("stage-pill", dream.sleepStage);
  pill.style.background = color;
  const time = span("dream-time", formatTime(dream.ts, true));
  art.append(pill, time);
  const body = div("card-body stack");
  if (dream.sleepStage === "DEEP") {
    const heading = el("p", "label", "Cognitive Vectors");
    heading.style.color = color;
    body.append(heading);
    for (const [label, value] of METRICS) body.append(metric(label, value, color));
  }
  body.append(el("p", "label primary", "Memory Consolidation"));
  body.append(p("", dream.summary));
  body.append(el("p", "label insight", "Personality Insight Evolved"));
  body.append(p("", dream.insight));
  card.append(art, body);
  return card;
}

function metric(label, value, color) {
  const wrap = div("metric");
  const row = div("metric-row");
  row.append(span("", label), span("", Math.round(value * 100) + "%"));
  row.lastChild.style.color = color;
  const bar = div("bar");
  const fill = document.createElement("i");
  fill.style.width = (value * 100) + "%";
  fill.style.background = color;
  bar.append(fill);
  wrap.append(row, bar);
  return wrap;
}

function renderSettings() {
  const settings = state.settings;
  screen.append(el("h2", "headline", "Brain Source"));
  const chips = div("chip-row");
  const options = [
    ["MOCK", "This page"],
    ["GEMINI", "Cloud"],
    ["XAI", "Grok"],
    ["MEDIAPIPE", "Device"],
    ["OLLAMA", "Server"]
  ];
  for (const [value, label] of options) {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.textContent = label;
    chip.setAttribute("aria-pressed", settings.provider === value ? "true" : "false");
    chip.addEventListener("click", () => {
      settings.provider = value;
      saveState();
      render();
    });
    chips.append(chip);
  }
  screen.append(chips);
  screen.append(p("fine", "Chat on this page uses the mock server shipped with Oneura. Cloud, Grok, on-device, and Ollama are the Android app's brains. Their settings stay in this browser."));

  if (settings.provider === "GEMINI") screen.append(secretCard("Cloud Setup", "Paste your key below. Oneura will keep it in this browser only.", "geminiKey", "Paste Gemini API Key"));
  if (settings.provider === "XAI") screen.append(grokCard());
  if (settings.provider === "MEDIAPIPE") screen.append(deviceCard());
  if (settings.provider === "OLLAMA" || settings.provider === "MOCK") screen.append(serverCard());

  screen.append(p("label", "Connection Timeout: " + settings.timeout + "s"));
  const slider = document.createElement("input");
  slider.className = "slider";
  slider.type = "range";
  slider.min = "10";
  slider.max = "300";
  slider.step = "10";
  slider.value = String(settings.timeout);
  slider.addEventListener("input", () => {
    settings.timeout = Number(slider.value);
    slider.previousSibling.textContent = "Connection Timeout: " + settings.timeout + "s";
    saveState();
  });
  screen.append(slider);
  screen.append(el("p", "label", "Quick Presets"));
  screen.append(presetRow([
    ["Termux", () => { settings.baseUrl = "http://127.0.0.1:8000/"; }],
    ["Emulator", () => { settings.baseUrl = "http://10.0.2.2:11434/"; }]
  ], true));
  screen.append(presetRow([
    ["TinyLlama", () => { settings.modelName = "tinyllama"; }],
    ["Llama 3.2", () => { settings.modelName = "llama3.2:1b"; }],
    ["Phi-3", () => { settings.modelName = "phi3:mini"; }]
  ], false));
  screen.append(systemCard());
}

function secretCard(title, copy, keyName, placeholder) {
  const card = div("card");
  const body = div("card-body");
  body.append(el("p", "label", title), p("fine", copy));
  const field = labeledField(state.settings[keyName] ? "Key stored (" + state.settings[keyName].length + " chars)" : placeholder, state.settings[keyName], true, (value) => {
    state.settings[keyName] = value.trim();
    saveState();
  });
  body.append(field);
  card.append(body);
  return card;
}

function grokCard() {
  const card = secretCard(
    "xAI (Grok)",
    "Paste your own key from console.x.ai. On this page it stays in browser storage and is not sent anywhere.",
    "xaiKey",
    "Paste xAI API key"
  );
  const body = card.querySelector(".card-body");
  body.append(labeledField("Companion model", state.settings.xaiCompanion, false, (value) => {
    state.settings.xaiCompanion = value.replace(/[\r\n]/g, "");
    saveState();
  }, "grok-4.6"));
  body.append(labeledField("Background model", state.settings.xaiBackground, false, (value) => {
    state.settings.xaiBackground = value.replace(/[\r\n]/g, "");
    saveState();
  }, "grok-4.3"));
  body.append(p("fine", "Leave a field blank to use its default. Avoid grok-beta, grok-2, grok-3, and grok-latest."));
  return card;
}

function deviceCard() {
  const card = div("card");
  const body = div("card-body");
  body.append(el("p", "label", "On-Device AI"));
  body.append(p("fine", "Private and offline. The MediaPipe model runs inside the Android app, not in this page."));
  card.append(body);
  return card;
}

function serverCard() {
  const card = div("card");
  const body = div("card-body");
  const scan = actionButton(state.scanning ? "Scanning Network..." : "Auto-Detect Ollama", () => {
    state.scanning = true;
    log("NET", "Auto-detect is a phone-side scan. This page is already on the mock server.");
    saveState();
    render();
    setTimeout(() => {
      state.scanning = false;
      state.settings.provider = "MOCK";
      saveState();
      render();
    }, 700);
  });
  scan.classList.add("secondary");
  scan.disabled = state.scanning;
  body.append(scan);
  body.append(labeledField("Base URL", state.settings.baseUrl, false, (value) => {
    state.settings.baseUrl = value.replace(/[\r\n]/g, "");
    saveState();
  }, "http://127.0.0.1:11434"));
  body.append(labeledField("Model Name", state.settings.modelName, false, (value) => {
    state.settings.modelName = value.replace(/[\r\n]/g, "");
    saveState();
  }, "tinyllama"));
  card.append(body);
  return card;
}

function systemCard() {
  const card = div("card");
  const body = div("card-body");
  body.append(el("p", "label", "System Info"));
  const provider = state.settings.provider;
  if (provider === "GEMINI") {
    body.append(p("fine", "Brain: Gemini (Cloud), stored for the Android app"));
    body.append(p("fine", "API key: " + (state.settings.geminiKey ? "stored in this browser" : "missing")));
  } else if (provider === "XAI") {
    body.append(p("fine", "Brain: xAI Grok, stored for the Android app"));
    body.append(p("fine", "Chat model: " + (state.settings.xaiCompanion || "grok-4.6")));
    body.append(p("fine", "Background model: " + (state.settings.xaiBackground || "grok-4.3")));
    body.append(p("fine", "API key: " + (state.settings.xaiKey ? "stored in this browser" : "missing")));
  } else if (provider === "OLLAMA") {
    body.append(p("fine", "Brain: Local server (" + (state.settings.modelName || "unset") + ")"));
    body.append(p("fine", "Address: " + (state.settings.baseUrl || "unset")));
  } else if (provider === "MEDIAPIPE") {
    body.append(p("fine", "Brain: On-device (private), in the Android app"));
  } else {
    body.append(p("fine", "Brain: Mock server on this page"));
    body.append(p("fine", "Address: " + location.origin));
  }
  const status = p("fine", "Status: " + (state.scanning ? "Scanning..." : "Standby"));
  status.classList.add(state.scanning ? "label" : "ok");
  body.append(status);
  card.append(body);
  return card;
}

function renderLogs() {
  const head = div("split");
  const title = el("h2", "headline", "Internal Diagnostics");
  const clear = document.createElement("button");
  clear.type = "button";
  clear.className = "icon-button";
  clear.style.position = "static";
  clear.setAttribute("aria-label", "Clear logs");
  clear.textContent = "Clear";
  clear.addEventListener("click", () => {
    state.logs = [];
    saveState();
    render();
  });
  head.append(title, clear);
  const list = div("log-list");
  for (const entry of state.logs) {
    const item = div("log-item");
    const meta = div("log-meta");
    meta.append(span("", "[" + formatTime(entry.ts, false) + "] "));
    const tag = span("tag " + entry.tag, entry.tag);
    meta.append(tag);
    const message = p("fine", entry.message);
    message.style.color = "rgba(255,255,255,0.9)";
    item.append(meta, message);
    list.append(item);
  }
  screen.append(head, list);
}

async function sendMessage(text) {
  const clean = text.trim();
  if (!clean || state.busy) return;
  state.busy = true;
  state.messages.push({ id: uid(), sender: "USER", content: clean, ts: Date.now() });
  log("SYS", "Processing message: " + clean);
  log("SYS", "Using AI Brain: Mock Server");
  saveState();
  render();
  try {
    const data = await postJson("/think", { prompt: clean });
    const reply = data.reply || "Brain Error: empty reply";
    state.messages.push({ id: uid(), sender: "AI", content: reply, ts: Date.now() });
    log("SYS", "AI Replied: " + reply.slice(0, 20) + "...");
    if (data.innerMonologue) {
      state.reflections.unshift({
        id: uid(),
        content: data.innerMonologue,
        triggerSource: "AUTO_MONOLOGUE",
        ts: Date.now()
      });
    }
    const turns = state.messages.filter((message) => message.sender === "USER").length;
    if (turns >= EVERY_N_TURNS && turns % EVERY_N_TURNS === 0) await reflect("META_AUTO", true);
  } catch (error) {
    const message = error.message || "Failed";
    state.messages.push({ id: uid(), sender: "AI", content: "Brain Error: " + message, ts: Date.now() });
    log("ERR", "AI Brain Failure: " + message);
  } finally {
    state.busy = false;
    saveState();
    render();
  }
}

async function reflect(origin, silent) {
  if (state.reflecting) return;
  if (state.messages.length === 0) {
    state.reflectionError = "Nothing to reflect on yet. Talk with Oneura first.";
    log("ERR", state.reflectionError);
    saveState();
    render();
    return;
  }
  state.reflecting = true;
  state.reflectionError = "";
  if (!silent) {
    saveState();
    render();
  }
  try {
    const data = await postJson("/reflect", { origin: origin === "META_MANUAL" ? "manual" : "auto" });
    const kind = String(data.insightType || "observation").toLowerCase().includes("strategy") ? "STRATEGY_SHIFT" : "OBSERVATION";
    const note = data.insight || "";
    const strategy = data.strategy || "";
    const content = kind === "STRATEGY_SHIFT" && strategy ? (note ? note + "\n\nStrategy: " + strategy : strategy) : note;
    if (!content) throw new Error("The reflection was empty.");
    state.reflections.unshift({ id: uid(), content, triggerSource: origin + ":" + kind, ts: Date.now() });
    log("SYS", "Meta reflection saved: " + kind);
  } catch (error) {
    state.reflectionError = error.message || "Reflection failed.";
    log("ERR", "Meta reflection failed: " + state.reflectionError);
  } finally {
    state.reflecting = false;
    saveState();
    if (state.tab === "reflections" || state.tab === "logs") render();
  }
}

async function dream() {
  if (state.dreaming) return;
  if (state.messages.length === 0 && state.reflections.length === 0) {
    state.notice = "Nothing to consolidate yet.";
    log("SYS", "Nothing to consolidate yet.");
    saveState();
    render();
    return;
  }
  state.dreaming = true;
  state.notice = "";
  log("SYS", "Starting dream consolidation...");
  saveState();
  render();
  const sleepStage = STAGES[state.dreams.length % STAGES.length];
  try {
    const data = await postJson("/dream", { sleepStage });
    const stage = STAGES.includes(data.sleepStage) ? data.sleepStage : sleepStage;
    state.dreams.unshift({
      id: uid(),
      sleepStage: stage,
      summary: data.memoryConsolidationSummary || data.narrative || "",
      insight: data.personalityInsightEvolved || data.insight || "Processed and integrated recent interactions into core memory matrix.",
      ts: Date.now()
    });
    log("SYS", "Dream saved successfully: " + stage);
  } catch (error) {
    log("ERR", "Dream consolidation failed: " + (error.message || "Failed"));
    state.notice = error.message || "Dream consolidation failed.";
  } finally {
    state.dreaming = false;
    saveState();
    render();
  }
}

async function postJson(path, body) {
  if (location.protocol === "file:") return localBrain(path, body);
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!response.ok) throw new Error("Server returned " + response.status);
  return response.json();
}

function localBrain(path, body) {
  if (path === "/think") {
    return {
      reply: "I received your message: '" + body.prompt + "'. This is a test reply from your local server!",
      innerMonologue: "The user seems to be testing the connection. I should respond helpfully."
    };
  }
  if (path === "/reflect") {
    if (body.origin === "manual") {
      return {
        insight: "Replies have been direct. Match the user's pace.",
        insightType: "strategy shift",
        strategy: "Answer in two sentences unless asked for more."
      };
    }
    return { insight: "You have been testing the app successfully today.", insightType: "observation" };
  }
  return {
    sleepStage: body.sleepStage || "REM",
    narrative: "A digital landscape where connections form like glowing threads.",
    memoryConsolidationSummary: "A digital landscape where connections form like glowing threads.",
    insight: "Connectivity is the foundation of understanding.",
    personalityInsightEvolved: "Connectivity is the foundation of understanding."
  };
}

function kindOf(triggerSource) {
  if (!triggerSource || !triggerSource.startsWith("META_")) return null;
  return triggerSource.split(":")[1] || null;
}

function displayLabel(triggerSource) {
  const kind = kindOf(triggerSource);
  if (kind === "OBSERVATION") return "Observation";
  if (kind === "CRITIQUE") return "Critique";
  if (kind === "STRATEGY_SHIFT") return "Strategy shift";
  if (triggerSource === "AUTO_MONOLOGUE") return "Inner voice";
  return "Reflection";
}

function strategyLine(content) {
  const marker = "Strategy:";
  const index = content.lastIndexOf(marker);
  return index >= 0 ? content.slice(index + marker.length).trim() : content.trim();
}

function formatTime(ts, withDate) {
  const date = new Date(ts);
  if (!withDate) return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  return date.toLocaleString([], { month: "short", day: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function actionButton(label, onClick) {
  const button = document.createElement("button");
  button.type = "button";
  button.className = "action";
  button.textContent = label;
  button.addEventListener("click", onClick);
  return button;
}

function presetRow(items, filled) {
  const row = div("preset-row");
  for (const [label, apply] of items) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = filled ? "action" : "text-button";
    button.textContent = label;
    button.addEventListener("click", () => {
      apply();
      saveState();
      render();
    });
    row.append(button);
  }
  return row;
}

function labeledField(label, value, secret, onInput, placeholder) {
  const field = div("field");
  field.append(el("span", "fine", label));
  const input = document.createElement("input");
  input.type = secret ? "password" : "text";
  input.value = value || "";
  if (placeholder) input.placeholder = placeholder;
  input.addEventListener("input", () => onInput(input.value));
  field.append(input);
  return field;
}

function div(className) { return el("div", className); }
function p(className, text) { return el("p", className, text); }
function span(className, text) { return el("span", className, text); }

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}
