package com.ftn.sbnz.model;

/**
 * Template rule for a user-defined fermentation pH cut-off on day 5.
 */
public class PhFermentationRule {

    private ProductType productType;
    private double phThresholdDay5;

    public PhFermentationRule() {}

    public PhFermentationRule(ProductType productType, double phThresholdDay5) {
        this.productType = productType;
        this.phThresholdDay5 = phThresholdDay5;
    }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public double getPhThresholdDay5() { return phThresholdDay5; }
    public void setPhThresholdDay5(double phThresholdDay5) { this.phThresholdDay5 = phThresholdDay5; }
}
