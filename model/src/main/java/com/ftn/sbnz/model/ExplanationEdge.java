package com.ftn.sbnz.model;

/**
 * Directed edge used by Drools backward queries to build an explanation tree.
 */
public class ExplanationEdge {

    private String batchId;
    private String parent;
    private String child;
    private String text;

    public ExplanationEdge() {}

    public ExplanationEdge(String batchId, String parent, String child, String text) {
        this.batchId = batchId;
        this.parent = parent;
        this.child = child;
        this.text = text;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getParent() { return parent; }
    public void setParent(String parent) { this.parent = parent; }

    public String getChild() { return child; }
    public void setChild(String child) { this.child = child; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
