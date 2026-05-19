package com.ftn.sbnz.service.service;

import com.ftn.sbnz.model.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Servisni sloj koji kreira scenarija i pokreće Drools pravila za sve faze.
 */
@Service
public class QualityService {

    @Autowired
    private KieContainer kieContainer;

    /**
     * Pokreće kompletnu demonstraciju za sve scenarije.
     */
    public void pokreniDemonstraciju() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  SMART QUALITY ADVISOR – Demonstracija forward chaining pravila");
        System.out.println("=".repeat(70));

        demonstracijaFaza1();
        demonstracijaFaza2();
        demonstracijaFaza3();
        demonstracijaFaza4();
        demonstracijaFaza5();
        demonstracijaFaza6();
        demonstracijaKompletnaProlaz();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Demonstracija završena.");
        System.out.println("=".repeat(70));
    }

    // ============================================================
    //  SCENARIJ 1 – Faza 1: Prijem sirovine
    // ============================================================
    private void demonstracijaFaza1() {
        System.out.println("\n--- SCENARIJ 1: Faza 1 – Prijem sirovine ---");

        // 1a: pH kritičan > 6.5 → blokada
        System.out.println("\n[1a] pH > 6.5 → očekivana blokada");
        Serija s1a = new Serija("SER-001", TipProizvoda.KULEN);
        s1a.setPhPrijema(6.8);
        s1a.setTemperaturaPrijema(5.0);
        s1a.setVizuelnaOcenaPrijema(4);
        s1a.setRokTrajanjaSirovine(LocalDate.now().plusDays(20));
        pokreniPravila(s1a, null, null);
        ispisiRezultat(s1a);

        // 1b: Vizuelna ocena < 3 → blokada
        System.out.println("\n[1b] Vizuelna ocena = 2 → očekivana blokada");
        Serija s1b = new Serija("SER-002", TipProizvoda.KULEN);
        s1b.setPhPrijema(5.8);
        s1b.setTemperaturaPrijema(4.0);
        s1b.setVizuelnaOcenaPrijema(2);
        s1b.setRokTrajanjaSirovine(LocalDate.now().plusDays(20));
        pokreniPravila(s1b, null, null);
        ispisiRezultat(s1b);

        // 1c: Sve OK → napredak u Fazu 2
        System.out.println("\n[1c] Sve vrednosti OK → očekivani napredak u Fazu 2");
        Serija s1c = new Serija("SER-003", TipProizvoda.KOBASICA);
        s1c.setPhPrijema(5.9);
        s1c.setTemperaturaPrijema(3.5);
        s1c.setVizuelnaOcenaPrijema(5);
        s1c.setRokTrajanjaSirovine(LocalDate.now().plusDays(30));
        pokreniPravila(s1c, null, null);
        ispisiRezultat(s1c);
    }

    // ============================================================
    //  SCENARIJ 2 – Faza 2: Salamurenje
    // ============================================================
    private void demonstracijaFaza2() {
        System.out.println("\n--- SCENARIJ 2: Faza 2 – Salamurenje ---");

        PraviloSoli praviloKulen = new PraviloSoli(TipProizvoda.KULEN, 2.5, 3.0);
        PraviloSoli praviloKobasica = new PraviloSoli(TipProizvoda.KOBASICA, 1.8, 2.2);

        // 2a: Sol < 1.8% → blokada
        System.out.println("\n[2a] Sol = 1.5% < 1.8% → blokada");
        Serija s2a = new Serija("SER-010", TipProizvoda.KULEN);
        s2a.setTrenutnaFaza(FazaProizvodnje.SALAMURENJE);
        s2a.setProcenatSoli(1.5);
        s2a.setTemperaturaSalamure(5.0);
        s2a.setTrajanjeSalamurenjaH(48);
        pokreniPravila(s2a, praviloKulen, null);
        ispisiRezultat(s2a);

        // 2b: Sol van opsega za kulen (template)
        System.out.println("\n[2b] Sol = 2.0% – van opsega za kulen (2.5–3.0%) → upozorenje template");
        Serija s2b = new Serija("SER-011", TipProizvoda.KULEN);
        s2b.setTrenutnaFaza(FazaProizvodnje.SALAMURENJE);
        s2b.setProcenatSoli(2.0);
        s2b.setTemperaturaSalamure(5.0);
        s2b.setTrajanjeSalamurenjaH(48);
        pokreniPravila(s2b, praviloKulen, null);
        ispisiRezultat(s2b);

        // 2c: Sve OK za kobasicu
        System.out.println("\n[2c] Sol = 2.0%, sve u redu za kobasicu → napredak u Fazu 3");
        Serija s2c = new Serija("SER-012", TipProizvoda.KOBASICA);
        s2c.setTrenutnaFaza(FazaProizvodnje.SALAMURENJE);
        s2c.setProcenatSoli(2.0);
        s2c.setTemperaturaSalamure(5.0);
        s2c.setTrajanjeSalamurenjaH(48);
        pokreniPravila(s2c, praviloKobasica, null);
        ispisiRezultat(s2c);
    }

    // ============================================================
    //  SCENARIJ 3 – Faza 3: Fermentacija (CEP trend)
    // ============================================================
    private void demonstracijaFaza3() {
        System.out.println("\n--- SCENARIJ 3: Faza 3 – Fermentacija (CEP trend pH) ---");

        // 3a: pH dan 5 > 5.3 → fermentacija failed
        System.out.println("\n[3a] pH dan 5 = 5.7 > 5.3 → blokada");
        Serija s3a = new Serija("SER-020", TipProizvoda.KULEN);
        s3a.setTrenutnaFaza(FazaProizvodnje.FERMENTACIJA);
        s3a.setPhFermentacijePoFazama(Arrays.asList(6.2, 6.0, 5.9, 5.8, 5.7)); // sporo opada
        s3a.setTemperaturaFermentacijskeKomore(22.0);
        s3a.setVlaznostFermentacijskeKomore(90.0);
        pokreniPravila(s3a, null, null);
        ispisiRezultat(s3a);

        // 3b: Spor trend (delta < 0.1 dva uzastopna dana) + visoka temperatura → blokada
        System.out.println("\n[3b] Spor trend pH + temperatura komore > 26°C → blokada");
        Serija s3b = new Serija("SER-021", TipProizvoda.KULEN);
        s3b.setTrenutnaFaza(FazaProizvodnje.FERMENTACIJA);
        s3b.setPhFermentacijePoFazama(Arrays.asList(6.2, 6.13, 6.06, 5.99, 5.92)); // delta ~0.07 svugdje
        s3b.setTemperaturaFermentacijskeKomore(27.0); // > 26°C
        s3b.setVlaznostFermentacijskeKomore(90.0);
        pokreniPravila(s3b, null, null);
        ispisiRezultat(s3b);

        // 3c: Sve OK → napredak u Fazu 4
        System.out.println("\n[3c] pH dan 5 = 5.1, trend OK → napredak u Fazu 4");
        Serija s3c = new Serija("SER-022", TipProizvoda.KULEN);
        s3c.setTrenutnaFaza(FazaProizvodnje.FERMENTACIJA);
        s3c.setPhFermentacijePoFazama(Arrays.asList(6.2, 5.9, 5.6, 5.4, 5.1)); // delta > 0.2 svaki dan
        s3c.setTemperaturaFermentacijskeKomore(21.0);
        s3c.setVlaznostFermentacijskeKomore(90.0);
        pokreniPravila(s3c, null, null);
        ispisiRezultat(s3c);
    }

    // ============================================================
    //  SCENARIJ 4 – Faza 4: Dimljenje (CEP agregacija)
    // ============================================================
    private void demonstracijaFaza4() {
        System.out.println("\n--- SCENARIJ 4: Faza 4 – Dimljenje (CEP agregacija temperature) ---");

        // 4a: Temperatura < 60°C → nedovoljna termička obrada → blokada
        System.out.println("\n[4a] Temp dima = 55°C < 60°C → blokada (nedovoljna termička obrada)");
        Serija s4a = new Serija("SER-030", TipProizvoda.KULEN);
        s4a.setTrenutnaFaza(FazaProizvodnje.DIMLJENJE);
        s4a.setTemperaturaDima(55.0);
        s4a.setTrajanjeDimljenjaH(6);
        pokreniPravila(s4a, null, null);
        ispisiRezultat(s4a);

        // 4b: Trajanje > 10h → upozorenje
        System.out.println("\n[4b] Trajanje = 12h > 10h → upozorenje prekomerne obrade");
        Serija s4b = new Serija("SER-031", TipProizvoda.KULEN);
        s4b.setTrenutnaFaza(FazaProizvodnje.DIMLJENJE);
        s4b.setTemperaturaDima(72.0);
        s4b.setTrajanjeDimljenjaH(12);
        pokreniPravila(s4b, null, null);
        ispisiRezultat(s4b);

        // 4c: Sve OK → napredak u Fazu 5
        System.out.println("\n[4c] Temp = 72°C, trajanje = 6h → napredak u Fazu 5");
        Serija s4c = new Serija("SER-032", TipProizvoda.KULEN);
        s4c.setTrenutnaFaza(FazaProizvodnje.DIMLJENJE);
        s4c.setTemperaturaDima(72.0);
        s4c.setTrajanjeDimljenjaH(6);
        pokreniPravila(s4c, null, null);
        ispisiRezultat(s4c);
    }

    // ============================================================
    //  SCENARIJ 5 – Faza 5: Sušenje/Zrenje (Template gubitak)
    // ============================================================
    private void demonstracijaFaza5() {
        System.out.println("\n--- SCENARIJ 5: Faza 5 – Sušenje/Zrenje (Template gubitak težine) ---");

        PraviloGubitakTezine praviloKulen = new PraviloGubitakTezine(TipProizvoda.KULEN, 30.0, 8);

        // 5a: Ukupni gubitak < 30% posle 8 nedelja → blokada (template)
        System.out.println("\n[5a] Gubitak = 22% < 30% za kulen → blokada napretka (template)");
        Serija s5a = new Serija("SER-040", TipProizvoda.KULEN);
        s5a.setTrenutnaFaza(FazaProizvodnje.SUSENJE_ZRENJE);
        s5a.setGubitakTezinaPoNedeljama(Arrays.asList(3.0, 5.0, 8.0, 11.0, 13.0, 16.0, 19.0, 22.0));
        s5a.setTemperaturaSusare(14.0);
        s5a.setVlaznostSusare(80.0);
        pokreniPravila(s5a, null, praviloKulen);
        ispisiRezultat(s5a);

        // 5b: Sve OK (gubitak = 32%) → napredak u Fazu 6
        System.out.println("\n[5b] Gubitak = 32% ≥ 30%, sve OK → napredak u Fazu 6");
        Serija s5b = new Serija("SER-041", TipProizvoda.KULEN);
        s5b.setTrenutnaFaza(FazaProizvodnje.SUSENJE_ZRENJE);
        s5b.setGubitakTezinaPoNedeljama(Arrays.asList(4.0, 8.0, 13.0, 18.0, 22.0, 26.0, 29.0, 32.0));
        s5b.setTemperaturaSusare(14.0);
        s5b.setVlaznostSusare(80.0);
        pokreniPravila(s5b, null, praviloKulen);
        ispisiRezultat(s5b);
    }

    // ============================================================
    //  SCENARIJ 6 – Faza 6: Finalna kontrola
    // ============================================================
    private void demonstracijaFaza6() {
        System.out.println("\n--- SCENARIJ 6: Faza 6 – Finalna kontrola ---");

        // 6a: pH > 5.3 → blokada
        System.out.println("\n[6a] pH finalnog = 5.5 → blokada");
        Serija s6a = new Serija("SER-050", TipProizvoda.KULEN);
        s6a.setTrenutnaFaza(FazaProizvodnje.FINALNA_KONTROLA);
        s6a.setPhFinalnog(5.5);
        s6a.setAwVrednost(0.88);
        s6a.setVizuelnaOcenaFinalnog(4);
        pokreniPravila(s6a, null, null);
        ispisiRezultat(s6a);

        // 6b: aw > 0.92 → blokada
        System.out.println("\n[6b] aw = 0.95 → blokada");
        Serija s6b = new Serija("SER-051", TipProizvoda.KULEN);
        s6b.setTrenutnaFaza(FazaProizvodnje.FINALNA_KONTROLA);
        s6b.setPhFinalnog(5.1);
        s6b.setAwVrednost(0.95);
        s6b.setVizuelnaOcenaFinalnog(4);
        pokreniPravila(s6b, null, null);
        ispisiRezultat(s6b);

        // 6c: Sve OK → odobrena
        System.out.println("\n[6c] Sve OK → serija odobrena");
        Serija s6c = new Serija("SER-052", TipProizvoda.KULEN);
        s6c.setTrenutnaFaza(FazaProizvodnje.FINALNA_KONTROLA);
        s6c.setPhFinalnog(4.9);
        s6c.setAwVrednost(0.87);
        s6c.setVizuelnaOcenaFinalnog(5);
        pokreniPravila(s6c, null, null);
        ispisiRezultat(s6c);
    }

    // ============================================================
    //  SCENARIJ 7 – Kompletan prolaz kroz sve faze
    // ============================================================
    private void demonstracijaKompletnaProlaz() {
        System.out.println("\n--- SCENARIJ 7: Kompletan prolaz serije SER-2025-042 kroz sve faze ---");
        System.out.println("    (Ovo je serija iz backward chaining primjera u projektu)");

        KieSession ks = kieContainer.newKieSession("ksession-rules");

        // Template pravila
        ks.insert(new PraviloSoli(TipProizvoda.KULEN, 2.5, 3.0));
        ks.insert(new PraviloGubitakTezine(TipProizvoda.KULEN, 30.0, 8));

        Serija serija = new Serija("SER-2025-042", TipProizvoda.KULEN);

        // Faza 1: Prijem – sve OK
        System.out.println("\n[Faza 1] Prijem sirovine...");
        serija.setPhPrijema(5.9);
        serija.setTemperaturaPrijema(4.0);
        serija.setVizuelnaOcenaPrijema(4);
        serija.setRokTrajanjaSirovine(LocalDate.now().plusDays(25));
        ks.insert(serija);
        ks.fireAllRules();
        System.out.println("    Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());

        // Faza 2: Salamurenje – sol OK za kulen
        System.out.println("\n[Faza 2] Salamurenje...");
        serija.setProcenatSoli(2.8);
        serija.setTemperaturaSalamure(5.0);
        serija.setTrajanjeSalamurenjaH(48);
        ks.update(ks.getFactHandle(serija), serija);
        ks.fireAllRules();
        System.out.println("    Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());

        // Faza 3: Fermentacija – pH pada dobro
        System.out.println("\n[Faza 3] Fermentacija...");
        serija.setPhFermentacijePoFazama(Arrays.asList(6.2, 5.9, 5.6, 5.35, 5.1));
        serija.setTemperaturaFermentacijskeKomore(21.0);
        serija.setVlaznostFermentacijskeKomore(90.0);
        ks.update(ks.getFactHandle(serija), serija);
        ks.fireAllRules();
        System.out.println("    Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());

        // Faza 4: Dimljenje – sve u redu
        System.out.println("\n[Faza 4] Dimljenje...");
        serija.setTemperaturaDima(70.0);
        serija.setTrajanjeDimljenjaH(5);
        ks.update(ks.getFactHandle(serija), serija);
        ks.fireAllRules();
        System.out.println("    Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());

        // Faza 5: Sušenje – gubitak 32%
        System.out.println("\n[Faza 5] Sušenje/Zrenje...");
        serija.setGubitakTezinaPoNedeljama(Arrays.asList(4.0, 9.0, 14.0, 18.0, 22.0, 26.0, 29.0, 32.0));
        serija.setTemperaturaSusare(14.0);
        serija.setVlaznostSusare(80.0);
        ks.update(ks.getFactHandle(serija), serija);
        ks.fireAllRules();
        System.out.println("    Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());

        // Faza 6: Finalna kontrola – sve OK
        System.out.println("\n[Faza 6] Finalna kontrola...");
        serija.setPhFinalnog(4.95);
        serija.setAwVrednost(0.88);
        serija.setVizuelnaOcenaFinalnog(5);
        ks.update(ks.getFactHandle(serija), serija);
        ks.fireAllRules();

        ks.dispose();

        System.out.println("\n[REZULTAT] Status: " + serija.getStatus() + " | Faza: " + serija.getTrenutnaFaza());
        System.out.println("[LOG]:");
        serija.getLog().forEach(l -> System.out.println("    " + l));
    }

    // ============================================================
    //  Helper metode
    // ============================================================

    private void pokreniPravila(Serija serija, PraviloSoli praviloSoli, PraviloGubitakTezine praviloGubitak) {
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(serija);
        if (praviloSoli != null) ks.insert(praviloSoli);
        if (praviloGubitak != null) ks.insert(praviloGubitak);
        ks.fireAllRules();
        ks.dispose();
    }

    private void ispisiRezultat(Serija s) {
        System.out.println("    STATUS: " + s.getStatus() + " | FAZA: " + s.getTrenutnaFaza());
        if (!s.getAktivnaUpozorenja().isEmpty()) {
            System.out.println("    Upozorenja:");
            s.getAktivnaUpozorenja().forEach(u -> System.out.println("      - " + u));
        }
        System.out.println("    Log: " + s.getLog());
    }
}
