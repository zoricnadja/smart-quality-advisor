package com.ftn.sbnz.model;

import java.time.LocalDateTime;

public class Aggregated15minSmoke {

    private String batchId;
    private LocalDateTime windowStart;
    private double averageTemperature;
    private double minTemperature;
    private double maxTemperature;
    private long sampleCount;

    public Aggregated15minSmoke() {}

    public Aggregated15minSmoke(String batchId, LocalDateTime windowStart, double averageTemperature,
                                double minTemperature, double maxTemperature, long sampleCount) {
        this.batchId = batchId;
        this.windowStart = windowStart;
        this.averageTemperature = averageTemperature;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.sampleCount = sampleCount;
    }

    public Aggregated15minSmoke(String batchId, LocalDateTime windowStart, Number averageTemperature,
                                Number minTemperature, Number maxTemperature, Number sampleCount) {
        this(batchId, windowStart, averageTemperature.doubleValue(), minTemperature.doubleValue(),
            maxTemperature.doubleValue(), sampleCount.longValue());
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

    public long getSampleCount() { return sampleCount; }
    public void setSampleCount(long sampleCount) { this.sampleCount = sampleCount; }
}
