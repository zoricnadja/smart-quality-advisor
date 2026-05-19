package com.ftn.sbnz.model;

/**
 * Template pravilo za korisnički definisani minimalni gubitak težine po tipu proizvoda.
 */
public class PraviloGubitakTezine {

    private TipProizvoda tipProizvoda;
    private double minGubitakPct;
    private int rokNedelje;

    public PraviloGubitakTezine() {}

    public PraviloGubitakTezine(TipProizvoda tip, double minGubitakPct, int rokNedelje) {
        this.tipProizvoda = tip;
        this.minGubitakPct = minGubitakPct;
        this.rokNedelje = rokNedelje;
    }

    public TipProizvoda getTipProizvoda() { return tipProizvoda; }
    public void setTipProizvoda(TipProizvoda t) { this.tipProizvoda = t; }

    public double getMinGubitakPct() { return minGubitakPct; }
    public void setMinGubitakPct(double v) { this.minGubitakPct = v; }

    public int getRokNedelje() { return rokNedelje; }
    public void setRokNedelje(int v) { this.rokNedelje = v; }
}
