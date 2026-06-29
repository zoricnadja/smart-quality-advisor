"use strict";

// ============================================================
//  Phase field definitions
//  Each field is annotated with the rule thresholds it drives so the
//  demonstrator knows which value triggers which rule.
// ============================================================

const PHASE_FIELDS = {
    RECEIVING: [
        { key: "receivingPh", label: "Receiving pH", type: "number", step: "0.1", value: 5.9,
          hint: "F1-1 block > 6.5 · F1-2 warning 6.2–6.5 · advance ≤ 6.2" },
        { key: "receivingTemperature", label: "Receiving temperature (°C)", type: "number", step: "0.1", value: 4.0,
          hint: "F1-3 warning > 7 °C" },
        { key: "receivingVisualScore", label: "Visual score (1–5)", type: "number", step: "1", value: 4,
          hint: "F1-6 block < 3 · advance ≥ 3" },
        { key: "rawMaterialShelfLifeDays", label: "Days until shelf-life expiry", type: "number", step: "1", value: 20,
          hint: "F1-4 warning < 5 days · F1-5 block when also temp > 7 °C" },
    ],
    CURING: [
        { key: "saltPercentage", label: "Salt (%)", type: "number", step: "0.1", value: 2.8,
          hint: "F2-1 block < 1.8 · F2-2 template range check" },
        { key: "brineTemperature", label: "Brine temperature (°C)", type: "number", step: "0.1", value: 5.0,
          hint: "F2-3 warning > 8 (→ F2-5 block) · F2-4 warning < 2" },
        { key: "curingDurationHours", label: "Curing duration (h)", type: "number", step: "1", value: 48,
          hint: "F2-6 warning outside 24–96 h" },
    ],
    FERMENTATION: [
        { key: "fermentationPhByDay", label: "pH by day (5 values, comma-separated)", type: "list", value: "6.2, 5.9, 5.6, 5.35, 5.1",
          hint: "F3-1 D1 > 6.0 · F3-2 D3 > 5.6 · F3-3 D5 > 5.3 block · F3-5/6 slow-trend Δ < 0.1" },
        { key: "fermentationChamberTemperature", label: "Chamber temperature (°C)", type: "number", step: "0.1", value: 21.0,
          hint: "F3-7 warning > 26 °C (pathogen risk) · advance 18–24 °C" },
        { key: "fermentationChamberHumidity", label: "Chamber humidity (%)", type: "number", step: "0.1", value: 90.0,
          hint: "advance requires 85–95 %" },
    ],
    SMOKING: [
        { key: "smokeTemperature", label: "Smoke temperature (°C)", type: "number", step: "0.1", value: 70.0,
          hint: "F4-2 warning < 55 · F4-3/4 < 60 → F4-5 block · F4-1 warning > 90 · advance 60–85" },
        { key: "smokingDurationHours", label: "Smoking duration (h)", type: "number", step: "1", value: 6,
          hint: "F4-6 warning > 10 h · advance 4–8 h" },
    ],
    DRYING_AGING: [
        { key: "weeklyWeightLossPercentages", label: "Cumulative weight loss by week (%, comma-separated)", type: "list",
          value: "4, 9, 14, 18, 22, 26, 29, 32",
          hint: "F5-1 week2 < 8 % · F5-2 week2 > 20 % · advance total 25–35 %" },
        { key: "dryingRoomTemperature", label: "Drying room temperature (°C)", type: "number", step: "0.1", value: 14.0,
          hint: "F5-3 warning > 18 °C · F5-5 CEP risk > 16 °C" },
        { key: "dryingRoomHumidity", label: "Drying room humidity (%)", type: "number", step: "0.1", value: 80.0,
          hint: "F5-4 warning outside 70–90 %" },
    ],
    FINAL_INSPECTION: [
        { key: "finalPh", label: "Final pH", type: "number", step: "0.1", value: 4.95,
          hint: "F6-1 block > 5.3" },
        { key: "waterActivity", label: "Water activity (aw)", type: "number", step: "0.01", value: 0.88,
          hint: "F6-2 block > 0.92" },
        { key: "finalVisualScore", label: "Final visual score (1–5)", type: "number", step: "1", value: 5,
          hint: "F6-3 warning < 4 · F6-4 block when also bad chemistry · approve ≥ 4" },
    ],
};

// Which template rule(s) are relevant per phase.
const PHASE_TEMPLATES = {
    CURING: ["saltRule", "saltTemplateRows"],
    FERMENTATION: ["phFermentationRule"],
    DRYING_AGING: ["weightLossRule"],
};

const LIST_KEYS = new Set(["fermentationPhByDay", "weeklyWeightLossPercentages"]);

// ============================================================
//  DOM references
// ============================================================
const els = {
    batchId: document.querySelector("#batchId"),
    productType: document.querySelector("#productType"),
    phase: document.querySelector("#phase"),
    phaseFields: document.querySelector("#phase-fields"),
    templateFields: document.querySelector("#template-fields"),
    evaluateBtn: document.querySelector("#evaluate-btn"),
    fillExampleBtn: document.querySelector("#fill-example-btn"),
    serviceStatus: document.querySelector("#service-status"),
    // result
    resultEmpty: document.querySelector("#result-empty"),
    result: document.querySelector("#result"),
    resultOutcome: document.querySelector("#result-outcome"),
    resultMeta: document.querySelector("#result-meta"),
    resultAlerts: document.querySelector("#result-alerts"),
    appliedTemplatesBlock: document.querySelector("#applied-templates-block"),
    resultTemplates: document.querySelector("#result-templates"),
    resultLog: document.querySelector("#result-log"),
    explanationBlock: document.querySelector("#explanation-block"),
    resultExplanation: document.querySelector("#result-explanation"),
    // demos
    demoOutput: document.querySelector("#demo-output"),
};

// ============================================================
//  Form rendering
// ============================================================
function fieldId(key) {
    return "f_" + key;
}

function renderPhaseFields() {
    const phase = els.phase.value;
    const fields = PHASE_FIELDS[phase] || [];
    els.phaseFields.innerHTML = "";

    const wrap = document.createElement("div");
    wrap.className = "field-grid";
    fields.forEach((f) => {
        const label = document.createElement("label");
        label.innerHTML = `${f.label}<span class="hint">${f.hint}</span>`;
        const input = document.createElement("input");
        input.id = fieldId(f.key);
        input.value = f.value;
        if (f.type === "number") {
            input.type = "number";
            input.step = f.step || "any";
        } else {
            input.type = "text";
        }
        label.appendChild(input);
        wrap.appendChild(label);
    });
    els.phaseFields.appendChild(wrap);

    renderTemplateFields(phase);
}

function renderTemplateFields(phase) {
    els.templateFields.innerHTML = "";
    const templates = PHASE_TEMPLATES[phase];
    if (!templates) return;

    const section = document.createElement("div");
    section.className = "template-section";
    section.innerHTML = `<h3>Optional template rules</h3>
        <p class="muted small">Leave blank to use the built-in defaults. These define data-driven rules per product type.</p>`;

    if (templates.includes("saltRule")) {
        section.appendChild(buildSaltRuleFields());
    }
    if (templates.includes("phFermentationRule")) {
        section.appendChild(buildPhFermentationFields());
    }
    if (templates.includes("weightLossRule")) {
        section.appendChild(buildWeightLossFields());
    }
    if (templates.includes("saltTemplateRows")) {
        section.appendChild(buildSaltTemplateRowsBuilder());
    }

    els.templateFields.appendChild(section);
}

function buildSaltRuleFields() {
    const box = document.createElement("div");
    box.className = "template-block";
    box.innerHTML = `
        <strong>SaltRule (F2-2)</strong> – allowed salt range for the product type.
        <div class="field-grid">
            <label>Min salt (%)<input type="number" step="0.1" id="tpl_saltMin" placeholder="e.g. 2.5"></label>
            <label>Max salt (%)<input type="number" step="0.1" id="tpl_saltMax" placeholder="e.g. 3.0"></label>
        </div>`;
    return box;
}

function buildPhFermentationFields() {
    const box = document.createElement("div");
    box.className = "template-block";
    box.innerHTML = `
        <strong>PhFermentationRule (F3-3T)</strong> – custom day-5 pH cut-off.
        <div class="field-grid">
            <label>Day-5 pH threshold<input type="number" step="0.1" id="tpl_phDay5" placeholder="e.g. 5.0"></label>
        </div>`;
    return box;
}

function buildWeightLossFields() {
    const box = document.createElement("div");
    box.className = "template-block";
    box.innerHTML = `
        <strong>WeightLossRule (F5-7)</strong> – minimum weight loss after a deadline.
        <div class="field-grid">
            <label>Min weight loss (%)<input type="number" step="0.1" id="tpl_wlMin" placeholder="e.g. 30"></label>
            <label>Deadline (weeks)<input type="number" step="1" id="tpl_wlWeeks" placeholder="e.g. 8"></label>
        </div>`;
    return box;
}

// Dynamic salt .drt template rows – the user can ADD template rules at runtime.
function buildSaltTemplateRowsBuilder() {
    const box = document.createElement("div");
    box.className = "template-block";
    box.innerHTML = `
        <strong>Salt .drt template rows</strong> – generate Drools template rules on the fly.
        <div id="salt-rows"></div>
        <button type="button" class="ghost small" id="add-salt-row">+ Add template row</button>`;
    return box;
}

function addSaltRow() {
    const container = document.querySelector("#salt-rows");
    if (!container) return;
    const row = document.createElement("div");
    row.className = "salt-row field-grid";
    row.innerHTML = `
        <label>Product type
            <select class="srow-type">
                <option value="KULEN">KULEN</option>
                <option value="SAUSAGE">SAUSAGE</option>
                <option value="OTHER">OTHER</option>
            </select></label>
        <label>Min salt (%)<input type="number" step="0.1" class="srow-min" placeholder="2.5"></label>
        <label>Max salt (%)<input type="number" step="0.1" class="srow-max" placeholder="3.0"></label>
        <button type="button" class="ghost small srow-remove">Remove</button>`;
    row.querySelector(".srow-remove").addEventListener("click", () => row.remove());
    container.appendChild(row);
}

// ============================================================
//  Build the request payload
// ============================================================
function numOrNull(id) {
    const el = document.querySelector("#" + id);
    if (!el || el.value.trim() === "") return null;
    const n = Number(el.value);
    return Number.isNaN(n) ? null : n;
}

function buildRequest() {
    const phase = els.phase.value;
    const productType = els.productType.value;
    const req = {
        id: els.batchId.value,
        productType,
        phase,
    };

    (PHASE_FIELDS[phase] || []).forEach((f) => {
        const el = document.querySelector("#" + fieldId(f.key));
        if (!el) return;
        const raw = el.value.trim();
        if (raw === "") return;
        if (LIST_KEYS.has(f.key)) {
            req[f.key] = raw.split(",").map((s) => Number(s.trim())).filter((n) => !Number.isNaN(n));
        } else if (f.type === "number") {
            const n = Number(raw);
            if (!Number.isNaN(n)) req[f.key] = n;
        } else {
            req[f.key] = raw;
        }
    });

    // Template rules
    const saltMin = numOrNull("tpl_saltMin");
    const saltMax = numOrNull("tpl_saltMax");
    if (saltMin !== null && saltMax !== null) {
        req.saltRule = { productType, minSalt: saltMin, maxSalt: saltMax };
    }
    const phDay5 = numOrNull("tpl_phDay5");
    if (phDay5 !== null) {
        req.phFermentationRule = { productType, phThresholdDay5: phDay5 };
    }
    const wlMin = numOrNull("tpl_wlMin");
    const wlWeeks = numOrNull("tpl_wlWeeks");
    if (wlMin !== null && wlWeeks !== null) {
        req.weightLossRule = { productType, minWeightLossPercent: wlMin, deadlineWeeks: wlWeeks };
    }

    const saltRows = document.querySelectorAll(".salt-row");
    if (saltRows.length) {
        const rows = [];
        saltRows.forEach((r) => {
            const type = r.querySelector(".srow-type").value;
            const min = r.querySelector(".srow-min").value.trim();
            const max = r.querySelector(".srow-max").value.trim();
            if (min !== "" && max !== "") {
                rows.push({ productType: type, minSalt: Number(min), maxSalt: Number(max) });
            }
        });
        if (rows.length) req.saltTemplateRows = rows;
    }

    return req;
}

// ============================================================
//  Result rendering
// ============================================================
const OUTCOME_CLASS = {
    BLOCKED: "badge-block",
    WARNING: "badge-warn",
    APPROVED: "badge-ok",
    ADVANCED: "badge-ok",
    NO_TRIGGER: "badge-neutral",
};

function renderResult(data) {
    els.resultEmpty.hidden = true;
    els.result.hidden = false;

    els.resultOutcome.textContent = data.outcome || data.status;
    els.resultOutcome.className = "badge " + (OUTCOME_CLASS[data.outcome] || "badge-neutral");
    els.resultMeta.textContent =
        `Batch ${data.batchId} · ${data.productType} · phase ${data.phase} · status ${data.status}`;

    // Alerts
    els.resultAlerts.innerHTML = "";
    if (!data.alerts || data.alerts.length === 0) {
        const li = document.createElement("li");
        li.className = "muted";
        li.textContent = "No alerts or warnings raised.";
        els.resultAlerts.appendChild(li);
    } else {
        data.alerts.forEach((a) => {
            const li = document.createElement("li");
            li.textContent = a;
            els.resultAlerts.appendChild(li);
        });
    }

    // Applied template rules
    if (data.appliedTemplateRules && data.appliedTemplateRules.length) {
        els.appliedTemplatesBlock.hidden = false;
        els.resultTemplates.innerHTML = "";
        data.appliedTemplateRules.forEach((t) => {
            const li = document.createElement("li");
            li.textContent = t;
            els.resultTemplates.appendChild(li);
        });
    } else {
        els.appliedTemplatesBlock.hidden = true;
    }

    // Log
    els.resultLog.innerHTML = "";
    (data.log || []).forEach((line) => {
        const li = document.createElement("li");
        li.textContent = line;
        if (line.includes("[BLOCKED]")) li.classList.add("log-block");
        else if (line.includes("[WARNING]")) li.classList.add("log-warn");
        else if (line.includes("[APPROVED]") || line.includes("[ADVANCE]")) li.classList.add("log-ok");
        els.resultLog.appendChild(li);
    });
    if (!data.log || data.log.length === 0) {
        const li = document.createElement("li");
        li.className = "muted";
        li.textContent = "No log entries – no rule changed the batch.";
        els.resultLog.appendChild(li);
    }

    // Explanation tree
    if (data.explanation) {
        els.explanationBlock.hidden = false;
        els.resultExplanation.innerHTML = "";
        els.resultExplanation.appendChild(renderExplanationNode(data.explanation));
    } else {
        els.explanationBlock.hidden = true;
    }
}

function renderExplanationNode(node) {
    const li = document.createElement("li");
    const head = document.createElement("div");
    head.className = "exp-node " + (node.proven ? "exp-proven" : "exp-unproven");
    head.innerHTML = `<span class="exp-mark">${node.proven ? "✔" : "•"}</span>
        <span class="exp-query">${escapeHtml(node.query)}</span>
        <span class="exp-result">${escapeHtml(node.result)}</span>`;
    li.appendChild(head);

    if (node.children && node.children.length) {
        const ul = document.createElement("ul");
        node.children.forEach((c) => ul.appendChild(renderExplanationNode(c)));
        li.appendChild(ul);
    }
    const ul = document.createElement("ul");
    ul.className = "exp-root";
    ul.appendChild(li);
    return ul;
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// ============================================================
//  Actions
// ============================================================
async function evaluate() {
    els.evaluateBtn.disabled = true;
    els.serviceStatus.textContent = "Evaluating";
    try {
        const response = await fetch("/api/quality/evaluate", {
            method: "POST",
            headers: { "Content-Type": "application/json", Accept: "application/json" },
            body: JSON.stringify(buildRequest()),
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `HTTP ${response.status}`);
        }
        const data = await response.json();
        renderResult(data);
        els.serviceStatus.textContent = "Complete";
    } catch (error) {
        els.resultEmpty.hidden = false;
        els.result.hidden = true;
        els.resultEmpty.textContent = "Error: " + error.message;
        els.serviceStatus.textContent = "Error";
    } finally {
        els.evaluateBtn.disabled = false;
    }
}

// "Fill example" loads values that trigger an interesting result per phase.
const EXAMPLES = {
    RECEIVING: { receivingPh: 6.8, receivingTemperature: 5.0, receivingVisualScore: 4, rawMaterialShelfLifeDays: 20 },
    CURING: { saltPercentage: 1.5, brineTemperature: 5.0, curingDurationHours: 48 },
    FERMENTATION: { fermentationPhByDay: "6.2, 6.0, 5.9, 5.8, 5.7", fermentationChamberTemperature: 22.0, fermentationChamberHumidity: 90.0 },
    SMOKING: { smokeTemperature: 55.0, smokingDurationHours: 6 },
    DRYING_AGING: { weeklyWeightLossPercentages: "3, 5, 8, 11, 13, 16, 19, 22", dryingRoomTemperature: 14.0, dryingRoomHumidity: 80.0 },
    FINAL_INSPECTION: { finalPh: 5.5, waterActivity: 0.88, finalVisualScore: 4 },
};

function fillExample() {
    const phase = els.phase.value;
    const ex = EXAMPLES[phase] || {};
    Object.entries(ex).forEach(([key, val]) => {
        const el = document.querySelector("#" + fieldId(key));
        if (el) el.value = val;
    });
}

// ============================================================
//  Prebuilt demos (unchanged behaviour)
// ============================================================
async function runDemo(button) {
    const endpoint = button.dataset.endpoint;
    const demoButtons = document.querySelectorAll("button[data-endpoint]");
    demoButtons.forEach((b) => (b.disabled = true));
    els.serviceStatus.textContent = "Running";
    els.demoOutput.textContent = `Calling ${endpoint} ...`;
    try {
        const response = await fetch(endpoint, { headers: { Accept: "text/plain" } });
        const text = await response.text();
        if (!response.ok) throw new Error(text || `HTTP ${response.status}`);
        els.demoOutput.textContent = text;
        els.serviceStatus.textContent = "Complete";
    } catch (error) {
        els.demoOutput.textContent = error.message;
        els.serviceStatus.textContent = "Error";
    } finally {
        demoButtons.forEach((b) => (b.disabled = false));
    }
}

// ============================================================
//  Wiring
// ============================================================
els.phase.addEventListener("change", renderPhaseFields);
els.evaluateBtn.addEventListener("click", evaluate);
els.fillExampleBtn.addEventListener("click", fillExample);

// Delegated click for the dynamically-rendered "add salt row" button.
els.templateFields.addEventListener("click", (e) => {
    if (e.target && e.target.id === "add-salt-row") addSaltRow();
});

document.querySelectorAll("button[data-endpoint]").forEach((button) => {
    button.addEventListener("click", () => runDemo(button));
});

renderPhaseFields();
