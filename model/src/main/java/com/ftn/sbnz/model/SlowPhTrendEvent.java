package com.ftn.sbnz.model;

public class SlowPhTrendEvent {

    private String batchId;
    private int fromDay;
    private int toDay;
    private double deltaPh;

    public SlowPhTrendEvent() {}

    public SlowPhTrendEvent(String batchId, int fromDay, int toDay, double deltaPh) {
        this.batchId = batchId;
        this.fromDay = fromDay;
        this.toDay = toDay;
        this.deltaPh = deltaPh;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getFromDay() { return fromDay; }
    public void setFromDay(int fromDay) { this.fromDay = fromDay; }

    public int getToDay() { return toDay; }
    public void setToDay(int toDay) { this.toDay = toDay; }

    public double getDeltaPh() { return deltaPh; }
    public void setDeltaPh(double deltaPh) { this.deltaPh = deltaPh; }
}
