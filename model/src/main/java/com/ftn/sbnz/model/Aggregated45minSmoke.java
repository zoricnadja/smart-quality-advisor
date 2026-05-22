package com.ftn.sbnz.model;

import java.time.LocalDateTime;

public class Aggregated45minSmoke {

    private String batchId;
    private LocalDateTime windowStart;
    private double averageTemperature;
    private double minTemperature;
    private double maxTemperature;

    public Aggregated45minSmoke() {}

    public Aggregated45minSmoke(String batchId, LocalDateTime windowStart, double averageTemperature,
                                double minTemperature, double maxTemperature) {
        this.batchId = batchId;
        this.windowStart = windowStart;
        this.averageTemperature = averageTemperature;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) { this.windowStart = windowStart; }

    public double getAverageTemperature() { return averageTemperature; }
    public void setAverageTemperature(double averageTemperature) { this.averageTemperature = averageTemperature; }

    public double getMinTemperature() { return minTemperature; }
    public void setMinTemperature(double minTemperature) { this.minTemperature = minTemperature; }

    public double getMaxTemperature() { return maxTemperature; }
    public void setMaxTemperature(double maxTemperature) { this.maxTemperature = maxTemperature; }
}
