package com.ftn.sbnz.service.dto;

/**
 * One row for the salt .drt template (productType + allowed salt range).
 * Allows the user to add template rules from the frontend.
 */
public class SaltTemplateRowDto {

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
