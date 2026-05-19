package com.ftn.sbnz.model;

/**
 * Template pravilo za korisnički definisane granice soli po tipu proizvoda.
 */
public class PraviloSoli {

    private TipProizvoda tipProizvoda;
    private double minSol;
    private double maxSol;

    public PraviloSoli() {}

    public PraviloSoli(TipProizvoda tip, double minSol, double maxSol) {
        this.tipProizvoda = tip;
        this.minSol = minSol;
        this.maxSol = maxSol;
    }

    public TipProizvoda getTipProizvoda() { return tipProizvoda; }
    public void setTipProizvoda(TipProizvoda t) { this.tipProizvoda = t; }

    public double getMinSol() { return minSol; }
    public void setMinSol(double v) { this.minSol = v; }

    public double getMaxSol() { return maxSol; }
    public void setMaxSol(double v) { this.maxSol = v; }
}
