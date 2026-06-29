package com.ftn.sbnz.service.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured result of an interactive batch evaluation.
 *
 * Carries the final decision (status), the phase the batch ended in, every
 * alert/warning that was raised, the full reasoning log, and - when the batch
 * was blocked - the backward-chaining explanation tree describing HOW the
 * decision was reached.
 */
public class EvaluationResult {

    private String batchId;
    private String productType;
    private String phase;
    private String status;          // ACTIVE | BLOCKED | APPROVED
    private String outcome;         // human-readable summary: BLOCKED / WARNING / APPROVED / ADVANCED / NO_TRIGGER
    private List<String> alerts = new ArrayList<>();
    private List<String> log = new ArrayList<>();
    private ExplanationNode explanation; // backward-chaining tree (null when not blocked)
    private List<String> appliedTemplateRules = new ArrayList<>();

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public List<String> getAlerts() { return alerts; }
    public void setAlerts(List<String> alerts) { this.alerts = alerts; }

    public List<String> getLog() { return log; }
    public void setLog(List<String> log) { this.log = log; }

    public ExplanationNode getExplanation() { return explanation; }
    public void setExplanation(ExplanationNode explanation) { this.explanation = explanation; }

    public List<String> getAppliedTemplateRules() { return appliedTemplateRules; }
    public void setAppliedTemplateRules(List<String> appliedTemplateRules) { this.appliedTemplateRules = appliedTemplateRules; }
}
