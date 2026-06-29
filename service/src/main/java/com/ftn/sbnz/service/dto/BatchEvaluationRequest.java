package com.ftn.sbnz.service.dto;

import java.util.List;

/**
 * Request payload for the interactive single-phase rule evaluation.
 *
 * The frontend sends the phase it wants to demonstrate plus the parameters
 * that are relevant for the rules of that phase. Optional template rules
 * (salt, weight loss, fermentation pH, and free-form salt .drt rows) can be
 * supplied to demonstrate the template-driven rules.
 */
public class BatchEvaluationRequest {

    private String id;
    private String productType;   // KULEN | SAUSAGE | OTHER
    private String phase;         // RECEIVING | CURING | FERMENTATION | SMOKING | DRYING_AGING | FINAL_INSPECTION

    // --- Phase 1: Raw material receiving ---
    private Double receivingPh;
    private Double receivingTemperature;
    private Integer receivingVisualScore;
    private Integer rawMaterialShelfLifeDays; // days from today until expiry

    // --- Phase 2: Curing ---
    private Double saltPercentage;
    private Double brineTemperature;
    private Integer curingDurationHours;

    // --- Phase 3: Fermentation ---
    private List<Double> fermentationPhByDay;
    private Double fermentationChamberTemperature;
    private Double fermentationChamberHumidity;

    // --- Phase 4: Smoking ---
    private Double smokeTemperature;
    private Integer smokingDurationHours;

    // --- Phase 5: Drying / aging ---
    private List<Double> weeklyWeightLossPercentages;
    private Double dryingRoomTemperature;
    private Double dryingRoomHumidity;

    // --- Phase 6: Final inspection ---
    private Double finalPh;
    private Double waterActivity;
    private Integer finalVisualScore;

    // --- Optional template rules ---
    private SaltRuleDto saltRule;
    private WeightLossRuleDto weightLossRule;
    private PhFermentationRuleDto phFermentationRule;
    private List<SaltTemplateRowDto> saltTemplateRows;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public Double getReceivingPh() { return receivingPh; }
    public void setReceivingPh(Double receivingPh) { this.receivingPh = receivingPh; }

    public Double getReceivingTemperature() { return receivingTemperature; }
    public void setReceivingTemperature(Double receivingTemperature) { this.receivingTemperature = receivingTemperature; }

    public Integer getReceivingVisualScore() { return receivingVisualScore; }
    public void setReceivingVisualScore(Integer receivingVisualScore) { this.receivingVisualScore = receivingVisualScore; }

    public Integer getRawMaterialShelfLifeDays() { return rawMaterialShelfLifeDays; }
    public void setRawMaterialShelfLifeDays(Integer rawMaterialShelfLifeDays) { this.rawMaterialShelfLifeDays = rawMaterialShelfLifeDays; }

    public Double getSaltPercentage() { return saltPercentage; }
    public void setSaltPercentage(Double saltPercentage) { this.saltPercentage = saltPercentage; }

    public Double getBrineTemperature() { return brineTemperature; }
    public void setBrineTemperature(Double brineTemperature) { this.brineTemperature = brineTemperature; }

    public Integer getCuringDurationHours() { return curingDurationHours; }
    public void setCuringDurationHours(Integer curingDurationHours) { this.curingDurationHours = curingDurationHours; }

    public List<Double> getFermentationPhByDay() { return fermentationPhByDay; }
    public void setFermentationPhByDay(List<Double> fermentationPhByDay) { this.fermentationPhByDay = fermentationPhByDay; }

    public Double getFermentationChamberTemperature() { return fermentationChamberTemperature; }
    public void setFermentationChamberTemperature(Double fermentationChamberTemperature) { this.fermentationChamberTemperature = fermentationChamberTemperature; }

    public Double getFermentationChamberHumidity() { return fermentationChamberHumidity; }
    public void setFermentationChamberHumidity(Double fermentationChamberHumidity) { this.fermentationChamberHumidity = fermentationChamberHumidity; }

    public Double getSmokeTemperature() { return smokeTemperature; }
    public void setSmokeTemperature(Double smokeTemperature) { this.smokeTemperature = smokeTemperature; }

    public Integer getSmokingDurationHours() { return smokingDurationHours; }
    public void setSmokingDurationHours(Integer smokingDurationHours) { this.smokingDurationHours = smokingDurationHours; }

    public List<Double> getWeeklyWeightLossPercentages() { return weeklyWeightLossPercentages; }
    public void setWeeklyWeightLossPercentages(List<Double> weeklyWeightLossPercentages) { this.weeklyWeightLossPercentages = weeklyWeightLossPercentages; }

    public Double getDryingRoomTemperature() { return dryingRoomTemperature; }
    public void setDryingRoomTemperature(Double dryingRoomTemperature) { this.dryingRoomTemperature = dryingRoomTemperature; }

    public Double getDryingRoomHumidity() { return dryingRoomHumidity; }
    public void setDryingRoomHumidity(Double dryingRoomHumidity) { this.dryingRoomHumidity = dryingRoomHumidity; }

    public Double getFinalPh() { return finalPh; }
    public void setFinalPh(Double finalPh) { this.finalPh = finalPh; }

    public Double getWaterActivity() { return waterActivity; }
    public void setWaterActivity(Double waterActivity) { this.waterActivity = waterActivity; }

    public Integer getFinalVisualScore() { return finalVisualScore; }
    public void setFinalVisualScore(Integer finalVisualScore) { this.finalVisualScore = finalVisualScore; }

    public SaltRuleDto getSaltRule() { return saltRule; }
    public void setSaltRule(SaltRuleDto saltRule) { this.saltRule = saltRule; }

    public WeightLossRuleDto getWeightLossRule() { return weightLossRule; }
    public void setWeightLossRule(WeightLossRuleDto weightLossRule) { this.weightLossRule = weightLossRule; }

    public PhFermentationRuleDto getPhFermentationRule() { return phFermentationRule; }
    public void setPhFermentationRule(PhFermentationRuleDto phFermentationRule) { this.phFermentationRule = phFermentationRule; }

    public List<SaltTemplateRowDto> getSaltTemplateRows() { return saltTemplateRows; }
    public void setSaltTemplateRows(List<SaltTemplateRowDto> saltTemplateRows) { this.saltTemplateRows = saltTemplateRows; }

    // --- nested template-rule DTOs ---

    public static class SaltRuleDto {
        private String productType;
        private Double minSalt;
        private Double maxSalt;

        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }

        public Double getMinSalt() { return minSalt; }
        public void setMinSalt(Double minSalt) { this.minSalt = minSalt; }

        public Double getMaxSalt() { return maxSalt; }
        public void setMaxSalt(Double maxSalt) { this.maxSalt = maxSalt; }
    }

    public static class WeightLossRuleDto {
        private String productType;
        private Double minWeightLossPercent;
        private Integer deadlineWeeks;

        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }

        public Double getMinWeightLossPercent() { return minWeightLossPercent; }
        public void setMinWeightLossPercent(Double minWeightLossPercent) { this.minWeightLossPercent = minWeightLossPercent; }

        public Integer getDeadlineWeeks() { return deadlineWeeks; }
        public void setDeadlineWeeks(Integer deadlineWeeks) { this.deadlineWeeks = deadlineWeeks; }
    }

    public static class PhFermentationRuleDto {
        private String productType;
        private Double phThresholdDay5;

        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }

        public Double getPhThresholdDay5() { return phThresholdDay5; }
        public void setPhThresholdDay5(Double phThresholdDay5) { this.phThresholdDay5 = phThresholdDay5; }
    }
}
