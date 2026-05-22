package com.ftn.sbnz.model;

public class FermentationAtRiskEvent {

    private String batchId;
    private int firstSlowDay;
    private int lastSlowDay;

    public FermentationAtRiskEvent() {}

    public FermentationAtRiskEvent(String batchId, int firstSlowDay, int lastSlowDay) {
        this.batchId = batchId;
        this.firstSlowDay = firstSlowDay;
        this.lastSlowDay = lastSlowDay;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getFirstSlowDay() { return firstSlowDay; }
    public void setFirstSlowDay(int firstSlowDay) { this.firstSlowDay = firstSlowDay; }

    public int getLastSlowDay() { return lastSlowDay; }
    public void setLastSlowDay(int lastSlowDay) { this.lastSlowDay = lastSlowDay; }
}
