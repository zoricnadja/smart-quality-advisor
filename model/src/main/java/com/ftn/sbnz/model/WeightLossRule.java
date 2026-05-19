package com.ftn.sbnz.model;

/**
 * Template rule for user-defined minimum weight loss by product type.
 */
public class WeightLossRule {

    private ProductType productType;
    private double minWeightLossPercent;
    private int deadlineWeeks;

    public WeightLossRule() {}

    public WeightLossRule(ProductType productType, double minWeightLossPercent, int deadlineWeeks) {
        this.productType = productType;
        this.minWeightLossPercent = minWeightLossPercent;
        this.deadlineWeeks = deadlineWeeks;
    }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public double getMinWeightLossPercent() { return minWeightLossPercent; }
    public void setMinWeightLossPercent(double minWeightLossPercent) { this.minWeightLossPercent = minWeightLossPercent; }

    public int getDeadlineWeeks() { return deadlineWeeks; }
    public void setDeadlineWeeks(int deadlineWeeks) { this.deadlineWeeks = deadlineWeeks; }
}