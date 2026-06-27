package com.ftn.sbnz.model;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class SmokeTemperatureEvent {

    private String batchId;
    private double temperature;
    private LocalDateTime timestamp;

    public SmokeTemperatureEvent() {}

    public SmokeTemperatureEvent(String batchId, double temperature, LocalDateTime timestamp) {
        this.batchId = batchId;
        this.temperature = temperature;
        this.timestamp = timestamp;
    }

    public LocalDateTime getWindowStart15Min() {
        int minute = timestamp.getMinute() - (timestamp.getMinute() % 15);
        return timestamp.withMinute(minute).withSecond(0).withNano(0);
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public long getTimestampMillis() {
        return timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
