package com.ftn.sbnz.service.service;

import com.ftn.sbnz.model.*;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * Service layer that creates demonstration scenarios and runs Drools rules for all phases.
 */
@Service
public class QualityService {

    @Autowired
    private KieContainer kieContainer;

    /**
     * Runs the complete demonstration for all scenarios.
     */
    public void runDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  SMART QUALITY ADVISOR - Forward Chaining Rule Demonstration");
        System.out.println("=".repeat(70));

        demoPhase1();
        demoPhase2();
        demoPhase3();
        demoPhase4();
        demoPhase5();
        demoPhase6();
        demoCompletePass();

        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Demonstration finished.");
        System.out.println("=".repeat(70));
    }

    private void demoPhase1() {
        System.out.println("\n--- SCENARIO 1: Phase 1 - Raw Material Receiving ---");

        System.out.println("\n[1a] pH > 6.5 -> expected block");
        Batch b1a = new Batch("BATCH-001", ProductType.KULEN);
        b1a.setReceivingPh(6.8);
        b1a.setReceivingTemperature(5.0);
        b1a.setReceivingVisualScore(4);
        b1a.setRawMaterialShelfLife(LocalDate.now().plusDays(20));
        runRules(b1a, null, null);
        printResult(b1a);

        System.out.println("\n[1b] Visual score = 2 -> expected block");
        Batch b1b = new Batch("BATCH-002", ProductType.KULEN);
        b1b.setReceivingPh(5.8);
        b1b.setReceivingTemperature(4.0);
        b1b.setReceivingVisualScore(2);
        b1b.setRawMaterialShelfLife(LocalDate.now().plusDays(20));
        runRules(b1b, null, null);
        printResult(b1b);

        System.out.println("\n[1c] All values OK -> expected advance to Phase 2");
        Batch b1c = new Batch("BATCH-003", ProductType.SAUSAGE);
        b1c.setReceivingPh(5.9);
        b1c.setReceivingTemperature(3.5);
        b1c.setReceivingVisualScore(5);
        b1c.setRawMaterialShelfLife(LocalDate.now().plusDays(30));
        runRules(b1c, null, null);
        printResult(b1c);
    }

    private void demoPhase2() {
        System.out.println("\n--- SCENARIO 2: Phase 2 - Curing ---");

        SaltRule kulenSaltRule = new SaltRule(ProductType.KULEN, 2.5, 3.0);
        SaltRule sausageSaltRule = new SaltRule(ProductType.SAUSAGE, 1.8, 2.2);

        System.out.println("\n[2a] Salt = 1.5% < 1.8% -> block");
        Batch b2a = new Batch("BATCH-010", ProductType.KULEN);
        b2a.setCurrentPhase(ProductionPhase.CURING);
        b2a.setSaltPercentage(1.5);
        b2a.setBrineTemperature(5.0);
        b2a.setCuringDurationHours(48);
        runRules(b2a, kulenSaltRule, null);
        printResult(b2a);

        System.out.println("\n[2b] Salt = 2.0% - outside kulen range (2.5-3.0%) -> template warning");
        Batch b2b = new Batch("BATCH-011", ProductType.KULEN);
        b2b.setCurrentPhase(ProductionPhase.CURING);
        b2b.setSaltPercentage(2.0);
        b2b.setBrineTemperature(5.0);
        b2b.setCuringDurationHours(48);
        runRules(b2b, kulenSaltRule, null);
        printResult(b2b);

        System.out.println("\n[2c] Salt = 2.0%, all OK for sausage -> advance to Phase 3");
        Batch b2c = new Batch("BATCH-012", ProductType.SAUSAGE);
        b2c.setCurrentPhase(ProductionPhase.CURING);
        b2c.setSaltPercentage(2.0);
        b2c.setBrineTemperature(5.0);
        b2c.setCuringDurationHours(48);
        runRules(b2c, sausageSaltRule, null);
        printResult(b2c);
    }

    private void demoPhase3() {
        System.out.println("\n--- SCENARIO 3: Phase 3 - Fermentation (pH trend CEP) ---");

        System.out.println("\n[3a] Day 5 pH = 5.7 > 5.3 -> block");
        Batch b3a = new Batch("BATCH-020", ProductType.KULEN);
        b3a.setCurrentPhase(ProductionPhase.FERMENTATION);
        b3a.setFermentationPhByDay(Arrays.asList(6.2, 6.0, 5.9, 5.8, 5.7));
        b3a.setFermentationChamberTemperature(22.0);
        b3a.setFermentationChamberHumidity(90.0);
        runRules(b3a, null, null);
        printResult(b3a);

        System.out.println("\n[3b] Slow pH trend + chamber temperature > 26 C -> block");
        Batch b3b = new Batch("BATCH-021", ProductType.KULEN);
        b3b.setCurrentPhase(ProductionPhase.FERMENTATION);
        b3b.setFermentationPhByDay(Arrays.asList(6.2, 6.13, 6.06, 5.99, 5.92));
        b3b.setFermentationChamberTemperature(27.0);
        b3b.setFermentationChamberHumidity(90.0);
        runRules(b3b, null, null);
        printResult(b3b);

        System.out.println("\n[3c] Day 5 pH = 5.1, trend OK -> advance to Phase 4");
        Batch b3c = new Batch("BATCH-022", ProductType.KULEN);
        b3c.setCurrentPhase(ProductionPhase.FERMENTATION);
        b3c.setFermentationPhByDay(Arrays.asList(6.2, 5.9, 5.6, 5.4, 5.1));
        b3c.setFermentationChamberTemperature(21.0);
        b3c.setFermentationChamberHumidity(90.0);
        runRules(b3c, null, null);
        printResult(b3c);
    }

    private void demoPhase4() {
        System.out.println("\n--- SCENARIO 4: Phase 4 - Smoking (temperature aggregation CEP) ---");

        System.out.println("\n[4a] Smoke temperature = 55 C < 60 C -> block (insufficient heat treatment)");
        Batch b4a = new Batch("BATCH-030", ProductType.KULEN);
        b4a.setCurrentPhase(ProductionPhase.SMOKING);
        b4a.setSmokeTemperature(55.0);
        b4a.setSmokingDurationHours(6);
        runRules(b4a, null, null);
        printResult(b4a);

        System.out.println("\n[4b] Duration = 12h > 10h -> excessive processing warning");
        Batch b4b = new Batch("BATCH-031", ProductType.KULEN);
        b4b.setCurrentPhase(ProductionPhase.SMOKING);
        b4b.setSmokeTemperature(72.0);
        b4b.setSmokingDurationHours(12);
        runRules(b4b, null, null);
        printResult(b4b);

        System.out.println("\n[4c] Temperature = 72 C, duration = 6h -> advance to Phase 5");
        Batch b4c = new Batch("BATCH-032", ProductType.KULEN);
        b4c.setCurrentPhase(ProductionPhase.SMOKING);
        b4c.setSmokeTemperature(72.0);
        b4c.setSmokingDurationHours(6);
        runRules(b4c, null, null);
        printResult(b4c);
    }

    private void demoPhase5() {
        System.out.println("\n--- SCENARIO 5: Phase 5 - Drying/Aging (weight-loss template) ---");

        WeightLossRule kulenWeightLossRule = new WeightLossRule(ProductType.KULEN, 30.0, 8);

        System.out.println("\n[5a] Weight loss = 22% < 30% for kulen -> progress blocked (template)");
        Batch b5a = new Batch("BATCH-040", ProductType.KULEN);
        b5a.setCurrentPhase(ProductionPhase.DRYING_AGING);
        b5a.setWeeklyWeightLossPercentages(Arrays.asList(3.0, 5.0, 8.0, 11.0, 13.0, 16.0, 19.0, 22.0));
        b5a.setDryingRoomTemperature(14.0);
        b5a.setDryingRoomHumidity(80.0);
        runRules(b5a, null, kulenWeightLossRule);
        printResult(b5a);

        System.out.println("\n[5b] Weight loss = 32% >= 30%, all OK -> advance to Phase 6");
        Batch b5b = new Batch("BATCH-041", ProductType.KULEN);
        b5b.setCurrentPhase(ProductionPhase.DRYING_AGING);
        b5b.setWeeklyWeightLossPercentages(Arrays.asList(4.0, 8.0, 13.0, 18.0, 22.0, 26.0, 29.0, 32.0));
        b5b.setDryingRoomTemperature(14.0);
        b5b.setDryingRoomHumidity(80.0);
        runRules(b5b, null, kulenWeightLossRule);
        printResult(b5b);
    }

    private void demoPhase6() {
        System.out.println("\n--- SCENARIO 6: Phase 6 - Final Inspection ---");

        System.out.println("\n[6a] Final pH = 5.5 -> block");
        Batch b6a = new Batch("BATCH-050", ProductType.KULEN);
        b6a.setCurrentPhase(ProductionPhase.FINAL_INSPECTION);
        b6a.setFinalPh(5.5);
        b6a.setWaterActivity(0.88);
        b6a.setFinalVisualScore(4);
        runRules(b6a, null, null);
        printResult(b6a);

        System.out.println("\n[6b] aw = 0.95 -> block");
        Batch b6b = new Batch("BATCH-051", ProductType.KULEN);
        b6b.setCurrentPhase(ProductionPhase.FINAL_INSPECTION);
        b6b.setFinalPh(5.1);
        b6b.setWaterActivity(0.95);
        b6b.setFinalVisualScore(4);
        runRules(b6b, null, null);
        printResult(b6b);

        System.out.println("\n[6c] All OK -> batch approved");
        Batch b6c = new Batch("BATCH-052", ProductType.KULEN);
        b6c.setCurrentPhase(ProductionPhase.FINAL_INSPECTION);
        b6c.setFinalPh(4.9);
        b6c.setWaterActivity(0.87);
        b6c.setFinalVisualScore(5);
        runRules(b6c, null, null);
        printResult(b6c);
    }

    private void demoCompletePass() {
        System.out.println("\n--- SCENARIO 7: Complete pass for batch BATCH-2025-042 through all phases ---");
        System.out.println("    (This is the batch used in the project's backward chaining example.)");

        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(new SaltRule(ProductType.KULEN, 2.5, 3.0));
        ks.insert(new WeightLossRule(ProductType.KULEN, 30.0, 8));

        Batch batch = new Batch("BATCH-2025-042", ProductType.KULEN);

        System.out.println("\n[Phase 1] Raw material receiving...");
        batch.setReceivingPh(5.9);
        batch.setReceivingTemperature(4.0);
        batch.setReceivingVisualScore(4);
        batch.setRawMaterialShelfLife(LocalDate.now().plusDays(25));
        ks.insert(batch);
        ks.fireAllRules();
        System.out.println("    Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());

        System.out.println("\n[Phase 2] Curing...");
        batch.setSaltPercentage(2.8);
        batch.setBrineTemperature(5.0);
        batch.setCuringDurationHours(48);
        ks.update(ks.getFactHandle(batch), batch);
        ks.fireAllRules();
        System.out.println("    Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());

        System.out.println("\n[Phase 3] Fermentation...");
        batch.setFermentationPhByDay(Arrays.asList(6.2, 5.9, 5.6, 5.35, 5.1));
        batch.setFermentationChamberTemperature(21.0);
        batch.setFermentationChamberHumidity(90.0);
        ks.update(ks.getFactHandle(batch), batch);
        ks.fireAllRules();
        System.out.println("    Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());

        System.out.println("\n[Phase 4] Smoking...");
        batch.setSmokeTemperature(70.0);
        batch.setSmokingDurationHours(5);
        ks.update(ks.getFactHandle(batch), batch);
        ks.fireAllRules();
        System.out.println("    Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());

        System.out.println("\n[Phase 5] Drying/Aging...");
        batch.setWeeklyWeightLossPercentages(Arrays.asList(4.0, 9.0, 14.0, 18.0, 22.0, 26.0, 29.0, 32.0));
        batch.setDryingRoomTemperature(14.0);
        batch.setDryingRoomHumidity(80.0);
        ks.update(ks.getFactHandle(batch), batch);
        ks.fireAllRules();
        System.out.println("    Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());

        System.out.println("\n[Phase 6] Final inspection...");
        batch.setFinalPh(4.95);
        batch.setWaterActivity(0.88);
        batch.setFinalVisualScore(5);
        ks.update(ks.getFactHandle(batch), batch);
        ks.fireAllRules();
        ks.dispose();

        System.out.println("\n[RESULT] Status: " + batch.getStatus() + " | Phase: " + batch.getCurrentPhase());
        System.out.println("[LOG]:");
        batch.getLog().forEach(line -> System.out.println("    " + line));
    }

    private void runRules(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(batch);
        if (saltRule != null) ks.insert(saltRule);
        if (weightLossRule != null) ks.insert(weightLossRule);
        ks.fireAllRules();
        ks.dispose();
    }

    private void printResult(Batch batch) {
        System.out.println("    STATUS: " + batch.getStatus() + " | PHASE: " + batch.getCurrentPhase());
        if (!batch.getActiveAlerts().isEmpty()) {
            System.out.println("    Alerts:");
            batch.getActiveAlerts().forEach(alert -> System.out.println("      - " + alert));
        }
        System.out.println("    Log: " + batch.getLog());
    }
}