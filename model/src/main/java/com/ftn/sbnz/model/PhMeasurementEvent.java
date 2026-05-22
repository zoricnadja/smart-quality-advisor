package com.ftn.sbnz.model;

import java.time.LocalDateTime;

public class PhMeasurementEvent {

    private String batchId;
    private int day;
    private double ph;
    private LocalDateTime timestamp;

    public PhMeasurementEvent() {}

    public PhMeasurementEvent(String batchId, int day, double ph, LocalDateTime timestamp) {
        this.batchId = batchId;
        this.day = day;
        this.ph = ph;
        this.timestamp = timestamp;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public double getPh() { return ph; }
    public void setPh(double ph) { this.ph = ph; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
