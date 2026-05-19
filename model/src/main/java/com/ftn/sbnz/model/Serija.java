package com.ftn.sbnz.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Predstavlja jednu seriju prehrambenog proizvoda koja prolazi kroz 6 faza procesa.
 */
public class Serija {

    private String id;
    private TipProizvoda tipProizvoda;
    private StatusSerije status;
    private FazaProizvodnje trenutnaFaza;
    private List<String> aktivnaUpozorenja;
    private List<String> log;

    // --- Faza 1: Prijem sirovine ---
    private double phPrijema;
    private double temperaturaPrijema;
    private LocalDate rokTrajanjaSirovine;
    private int vizuelnaOcenaPrijema; // 1-5
    private LocalDate datumPrijema;

    // --- Faza 2: Salamurenje ---
    private double procenatSoli;
    private double temperaturaSalamure;
    private int trajanjeSalamurenjaH; // u satima

    // --- Faza 3: Fermentacija ---
    // pH po danima (index 0 = dan 1)
    private List<Double> phFermentacijePoFazama;
    private double temperaturaFermentacijskeKomore;
    private double vlaznostFermentacijskeKomore;

    // --- Faza 4: Dimljenje ---
    private double temperaturaDima;
    private int trajanjeDimljenjaH;

    // --- Faza 5: Sušenje/zrenje ---
    // Gubitak težine po nedeljama u % (kumulativno)
    private List<Double> gubitakTezinaPoNedeljama;
    private double temperaturaSusare;
    private double vlaznostSusare;

    // --- Faza 6: Finalna kontrola ---
    private double phFinalnog;
    private double awVrednost;
    private int vizuelnaOcenaFinalnog;

    public Serija() {
        this.status = StatusSerije.AKTIVNA;
        this.trenutnaFaza = FazaProizvodnje.PRIJEM;
        this.aktivnaUpozorenja = new ArrayList<>();
        this.log = new ArrayList<>();
        this.phFermentacijePoFazama = new ArrayList<>();
        this.gubitakTezinaPoNedeljama = new ArrayList<>();
        this.datumPrijema = LocalDate.now();
    }

    public Serija(String id, TipProizvoda tipProizvoda) {
        this();
        this.id = id;
        this.tipProizvoda = tipProizvoda;
    }

    public void dodajUpozorenje(String upozorenje) {
        if (!aktivnaUpozorenja.contains(upozorenje)) {
            aktivnaUpozorenja.add(upozorenje);
            log.add("[UPOZORENJE] " + upozorenje);
            System.out.println("  >> UPOZORENJE [" + id + "]: " + upozorenje);
            // NOVO: zamijeni listu novim objektom da Drools detektuje promjenu reference
            this.aktivnaUpozorenja = new ArrayList<>(this.aktivnaUpozorenja);
        }
    }

    public void blokiraj(String razlog) {
        this.status = StatusSerije.BLOKIRANA;
        log.add("[BLOKADA] " + razlog);
        System.out.println("  !! BLOKADA [" + id + "]: " + razlog);
    }

    public void odobri() {
        this.status = StatusSerije.ODOBRENA;
        log.add("[ODOBRENA] Serija prošla finalnu kontrolu.");
        System.out.println("  ** ODOBRENA [" + id + "]");
    }

    public boolean imaAktivnoUpozorenje(String upozorenje) {
        return aktivnaUpozorenja.contains(upozorenje);
    }

    public boolean jeBlokirana() {
        return status == StatusSerije.BLOKIRANA;
    }

    public double getPhNaDan(int dan) {
        int idx = dan - 1;
        if (idx >= 0 && idx < phFermentacijePoFazama.size()) {
            return phFermentacijePoFazama.get(idx);
        }
        return -1;
    }

    public double getDeltaPhIzmedjuDana(int danN) {
        // delta između dana danN-1 i danN
        if (danN < 2 || danN > phFermentacijePoFazama.size()) return 0;
        return phFermentacijePoFazama.get(danN - 2) - phFermentacijePoFazama.get(danN - 1);
    }

    public double getUkupniGubitakTezine() {
        if (gubitakTezinaPoNedeljama.isEmpty()) return 0;
        return gubitakTezinaPoNedeljama.get(gubitakTezinaPoNedeljama.size() - 1);
    }

    public double getGubitakPosleNedelje(int nedelja) {
        int idx = nedelja - 1;
        if (idx >= 0 && idx < gubitakTezinaPoNedeljama.size()) {
            return gubitakTezinaPoNedeljama.get(idx);
        }
        return -1;
    }

    public long getDaneDoPistekaPodataka() {
        return datumPrijema.until(rokTrajanjaSirovine, java.time.temporal.ChronoUnit.DAYS);
    }

    // ============ Getters & Setters ============

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public TipProizvoda getTipProizvoda() { return tipProizvoda; }
    public void setTipProizvoda(TipProizvoda tipProizvoda) { this.tipProizvoda = tipProizvoda; }

    public StatusSerije getStatus() { return status; }
    public void setStatus(StatusSerije status) { this.status = status; }

    public FazaProizvodnje getTrenutnaFaza() { return trenutnaFaza; }
    public void setTrenutnaFaza(FazaProizvodnje faza) { this.trenutnaFaza = faza; }

    public List<String> getAktivnaUpozorenja() { return aktivnaUpozorenja; }
    public List<String> getLog() { return log; }

    public double getPhPrijema() { return phPrijema; }
    public void setPhPrijema(double phPrijema) { this.phPrijema = phPrijema; }

    public double getTemperaturaPrijema() { return temperaturaPrijema; }
    public void setTemperaturaPrijema(double temp) { this.temperaturaPrijema = temp; }

    public LocalDate getRokTrajanjaSirovine() { return rokTrajanjaSirovine; }
    public void setRokTrajanjaSirovine(LocalDate rok) { this.rokTrajanjaSirovine = rok; }

    public int getVizuelnaOcenaPrijema() { return vizuelnaOcenaPrijema; }
    public void setVizuelnaOcenaPrijema(int ocena) { this.vizuelnaOcenaPrijema = ocena; }

    public LocalDate getDatumPrijema() { return datumPrijema; }
    public void setDatumPrijema(LocalDate datum) { this.datumPrijema = datum; }

    public double getProcenatSoli() { return procenatSoli; }
    public void setProcenatSoli(double sol) { this.procenatSoli = sol; }

    public double getTemperaturaSalamure() { return temperaturaSalamure; }
    public void setTemperaturaSalamure(double temp) { this.temperaturaSalamure = temp; }

    public int getTrajanjeSalamurenjaH() { return trajanjeSalamurenjaH; }
    public void setTrajanjeSalamurenjaH(int h) { this.trajanjeSalamurenjaH = h; }

    public List<Double> getPhFermentacijePoFazama() { return phFermentacijePoFazama; }
    public void setPhFermentacijePoFazama(List<Double> lista) { this.phFermentacijePoFazama = lista; }

    public double getTemperaturaFermentacijskeKomore() { return temperaturaFermentacijskeKomore; }
    public void setTemperaturaFermentacijskeKomore(double t) { this.temperaturaFermentacijskeKomore = t; }

    public double getVlaznostFermentacijskeKomore() { return vlaznostFermentacijskeKomore; }
    public void setVlaznostFermentacijskeKomore(double v) { this.vlaznostFermentacijskeKomore = v; }

    public double getTemperaturaDima() { return temperaturaDima; }
    public void setTemperaturaDima(double temp) { this.temperaturaDima = temp; }

    public int getTrajanjeDimljenjaH() { return trajanjeDimljenjaH; }
    public void setTrajanjeDimljenjaH(int h) { this.trajanjeDimljenjaH = h; }

    public List<Double> getGubitakTezinaPoNedeljama() { return gubitakTezinaPoNedeljama; }
    public void setGubitakTezinaPoNedeljama(List<Double> lista) { this.gubitakTezinaPoNedeljama = lista; }

    public double getTemperaturaSusare() { return temperaturaSusare; }
    public void setTemperaturaSusare(double t) { this.temperaturaSusare = t; }

    public double getVlaznostSusare() { return vlaznostSusare; }
    public void setVlaznostSusare(double v) { this.vlaznostSusare = v; }

    public double getPhFinalnog() { return phFinalnog; }
    public void setPhFinalnog(double ph) { this.phFinalnog = ph; }

    public double getAwVrednost() { return awVrednost; }
    public void setAwVrednost(double aw) { this.awVrednost = aw; }

    public int getVizuelnaOcenaFinalnog() { return vizuelnaOcenaFinalnog; }
    public void setVizuelnaOcenaFinalnog(int ocena) { this.vizuelnaOcenaFinalnog = ocena; }

    @Override
    public String toString() {
        return "Serija{id='" + id + "', tip=" + tipProizvoda + ", status=" + status + ", faza=" + trenutnaFaza + "}";
    }
}
