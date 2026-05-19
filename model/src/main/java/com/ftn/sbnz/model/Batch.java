package com.ftn.sbnz.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one food product batch as it moves through the six process phases.
 */
public class Batch {

    private String id;
    private ProductType productType;
    private BatchStatus status;
    private ProductionPhase currentPhase;
    private List<String> activeAlerts;
    private List<String> log;

    // --- Phase 1: Raw material receiving ---
    private double receivingPh;
    private double receivingTemperature;
    private LocalDate rawMaterialShelfLife;
    private int receivingVisualScore; // 1-5
    private LocalDate receivingDate;

    // --- Phase 2: Curing ---
    private double saltPercentage;
    private double brineTemperature;
    private int curingDurationHours;

    // --- Phase 3: Fermentation ---
    // pH by day (index 0 = day 1)
    private List<Double> fermentationPhByDay;
    private double fermentationChamberTemperature;
    private double fermentationChamberHumidity;

    // --- Phase 4: Smoking ---
    private double smokeTemperature;
    private int smokingDurationHours;

    // --- Phase 5: Drying/aging ---
    // Cumulative weight loss by week, in percent.
    private List<Double> weeklyWeightLossPercentages;
    private double dryingRoomTemperature;
    private double dryingRoomHumidity;

    // --- Phase 6: Final inspection ---
    private double finalPh;
    private double waterActivity;
    private int finalVisualScore;

    public Batch() {
        this.status = BatchStatus.ACTIVE;
        this.currentPhase = ProductionPhase.RECEIVING;
        this.activeAlerts = new ArrayList<>();
        this.log = new ArrayList<>();
        this.fermentationPhByDay = new ArrayList<>();
        this.weeklyWeightLossPercentages = new ArrayList<>();
        this.receivingDate = LocalDate.now();
    }

    public Batch(String id, ProductType productType) {
        this();
        this.id = id;
        this.productType = productType;
    }

    public void addAlert(String alert) {
        if (!activeAlerts.contains(alert)) {
            activeAlerts.add(alert);
            log.add("[WARNING] " + alert);
            System.out.println("  >> WARNING [" + id + "]: " + alert);
            // Replace the list instance so Drools can detect the changed reference.
            this.activeAlerts = new ArrayList<>(this.activeAlerts);
        }
    }

    public void block(String reason) {
        this.status = BatchStatus.BLOCKED;
        log.add("[BLOCKED] " + reason);
        System.out.println("  !! BLOCKED [" + id + "]: " + reason);
    }

    public void approve() {
        this.status = BatchStatus.APPROVED;
        log.add("[APPROVED] Batch passed final inspection.");
        System.out.println("  ** APPROVED [" + id + "]");
    }

    public boolean hasActiveAlert(String alert) {
        return activeAlerts.contains(alert);
    }

    public boolean isBlocked() {
        return status == BatchStatus.BLOCKED;
    }

    public double getPhOnDay(int day) {
        int idx = day - 1;
        if (idx >= 0 && idx < fermentationPhByDay.size()) {
            return fermentationPhByDay.get(idx);
        }
        return -1;
    }

    public double getPhDeltaBetweenDays(int dayN) {
        if (dayN < 2 || dayN > fermentationPhByDay.size()) return 0;
        return fermentationPhByDay.get(dayN - 2) - fermentationPhByDay.get(dayN - 1);
    }

    public double getTotalWeightLoss() {
        if (weeklyWeightLossPercentages.isEmpty()) return 0;
        return weeklyWeightLossPercentages.get(weeklyWeightLossPercentages.size() - 1);
    }

    public double getWeightLossAfterWeek(int week) {
        int idx = week - 1;
        if (idx >= 0 && idx < weeklyWeightLossPercentages.size()) {
            return weeklyWeightLossPercentages.get(idx);
        }
        return -1;
    }

    public long getDaysUntilRawMaterialExpiry() {
        return receivingDate.until(rawMaterialShelfLife, java.time.temporal.ChronoUnit.DAYS);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public ProductionPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(ProductionPhase currentPhase) { this.currentPhase = currentPhase; }

    public List<String> getActiveAlerts() { return activeAlerts; }
    public List<String> getLog() { return log; }

    public double getReceivingPh() { return receivingPh; }
    public void setReceivingPh(double receivingPh) { this.receivingPh = receivingPh; }

    public double getReceivingTemperature() { return receivingTemperature; }
    public void setReceivingTemperature(double receivingTemperature) { this.receivingTemperature = receivingTemperature; }

    public LocalDate getRawMaterialShelfLife() { return rawMaterialShelfLife; }
    public void setRawMaterialShelfLife(LocalDate rawMaterialShelfLife) { this.rawMaterialShelfLife = rawMaterialShelfLife; }

    public int getReceivingVisualScore() { return receivingVisualScore; }
    public void setReceivingVisualScore(int receivingVisualScore) { this.receivingVisualScore = receivingVisualScore; }

    public LocalDate getReceivingDate() { return receivingDate; }
    public void setReceivingDate(LocalDate receivingDate) { this.receivingDate = receivingDate; }

    public double getSaltPercentage() { return saltPercentage; }
    public void setSaltPercentage(double saltPercentage) { this.saltPercentage = saltPercentage; }

    public double getBrineTemperature() { return brineTemperature; }
    public void setBrineTemperature(double brineTemperature) { this.brineTemperature = brineTemperature; }

    public int getCuringDurationHours() { return curingDurationHours; }
    public void setCuringDurationHours(int curingDurationHours) { this.curingDurationHours = curingDurationHours; }

    public List<Double> getFermentationPhByDay() { return fermentationPhByDay; }
    public void setFermentationPhByDay(List<Double> fermentationPhByDay) { this.fermentationPhByDay = fermentationPhByDay; }

    public double getFermentationChamberTemperature() { return fermentationChamberTemperature; }
    public void setFermentationChamberTemperature(double fermentationChamberTemperature) { this.fermentationChamberTemperature = fermentationChamberTemperature; }

    public double getFermentationChamberHumidity() { return fermentationChamberHumidity; }
    public void setFermentationChamberHumidity(double fermentationChamberHumidity) { this.fermentationChamberHumidity = fermentationChamberHumidity; }

    public double getSmokeTemperature() { return smokeTemperature; }
    public void setSmokeTemperature(double smokeTemperature) { this.smokeTemperature = smokeTemperature; }

    public int getSmokingDurationHours() { return smokingDurationHours; }
    public void setSmokingDurationHours(int smokingDurationHours) { this.smokingDurationHours = smokingDurationHours; }

    public List<Double> getWeeklyWeightLossPercentages() { return weeklyWeightLossPercentages; }
    public void setWeeklyWeightLossPercentages(List<Double> weeklyWeightLossPercentages) { this.weeklyWeightLossPercentages = weeklyWeightLossPercentages; }

    public double getDryingRoomTemperature() { return dryingRoomTemperature; }
    public void setDryingRoomTemperature(double dryingRoomTemperature) { this.dryingRoomTemperature = dryingRoomTemperature; }

    public double getDryingRoomHumidity() { return dryingRoomHumidity; }
    public void setDryingRoomHumidity(double dryingRoomHumidity) { this.dryingRoomHumidity = dryingRoomHumidity; }

    public double getFinalPh() { return finalPh; }
    public void setFinalPh(double finalPh) { this.finalPh = finalPh; }

    public double getWaterActivity() { return waterActivity; }
    public void setWaterActivity(double waterActivity) { this.waterActivity = waterActivity; }

    public int getFinalVisualScore() { return finalVisualScore; }
    public void setFinalVisualScore(int finalVisualScore) { this.finalVisualScore = finalVisualScore; }

    @Override
    public String toString() {
        return "Batch{id='" + id + "', productType=" + productType + ", status=" + status + ", phase=" + currentPhase + "}";
    }
}