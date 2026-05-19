package com.ftn.sbnz.model;

/**
 * Template rule for user-defined salt boundaries by product type.
 */
public class SaltRule {

    private ProductType productType;
    private double minSalt;
    private double maxSalt;

    public SaltRule() {}

    public SaltRule(ProductType productType, double minSalt, double maxSalt) {
        this.productType = productType;
        this.minSalt = minSalt;
        this.maxSalt = maxSalt;
    }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public double getMinSalt() { return minSalt; }
    public void setMinSalt(double minSalt) { this.minSalt = minSalt; }

    public double getMaxSalt() { return maxSalt; }
    public void setMaxSalt(double maxSalt) { this.maxSalt = maxSalt; }
}