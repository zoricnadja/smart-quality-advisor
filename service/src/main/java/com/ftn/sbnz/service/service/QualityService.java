package com.ftn.sbnz.service.service;

import com.ftn.sbnz.model.*;
import com.ftn.sbnz.service.dto.BatchEvaluationRequest;
import com.ftn.sbnz.service.dto.EvaluationResult;
import com.ftn.sbnz.service.dto.ExplanationNode;
import com.ftn.sbnz.service.dto.SaltTemplateRowDto;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

/**
 * Service layer that creates demonstration scenarios and runs Drools rules for all phases.
 */
@Service
public class QualityService {

    private static final Object DEMO_OUTPUT_LOCK = new Object();

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private BackwardChainingService backwardChainingService;

    private KieSession createSessionWithTemplates(Collection<?> data) {
        org.drools.template.ObjectDataCompiler converter = new org.drools.template.ObjectDataCompiler();
        InputStream templateStream = QualityService.class.getResourceAsStream("/com/ftn/sbnz/rules/salt_rules.drt");
        String drl = converter.compile(data, templateStream);

        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        
        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            System.out.println(results.getMessages());
            throw new IllegalStateException("Compilation errors in template generated DRL");
        }

        return kieHelper.build().newKieSession();
    }

    public String runTemplateDemo() {
        StringBuilder report = new StringBuilder();
        report.append("\n--- TEMPLATE DEMO: Salt rules from Drools .drt template ---")
            .append(System.lineSeparator());

        List<Map<String, Object>> data = createSaltTemplateRows();
        report.append("Template data rows:")
            .append(System.lineSeparator())
            .append("  - KULEN: salt range 2.5-3.0%")
            .append(System.lineSeparator())
            .append("  - SAUSAGE: salt range 1.8-2.2%")
            .append(System.lineSeparator());

        KieSession ks = createSessionWithTemplates(data);

        Batch b1 = new Batch("T-BATCH-001", ProductType.KULEN);
        b1.setCurrentPhase(ProductionPhase.CURING);
        b1.setSaltPercentage(2.0);

        ks.insert(b1);
        ks.fireAllRules();
        ks.dispose();

        report.append(System.lineSeparator())
            .append("Scenario: KULEN batch with salt = 2.0%")
            .append(System.lineSeparator())
            .append("Expected: generated template rule should add a salt range warning.")
            .append(System.lineSeparator())
            .append("STATUS: ")
            .append(b1.getStatus())
            .append(" | PHASE: ")
            .append(b1.getCurrentPhase())
            .append(System.lineSeparator())
            .append("Alerts: ")
            .append(b1.getActiveAlerts())
            .append(System.lineSeparator())
            .append("Log: ")
            .append(b1.getLog())
            .append(System.lineSeparator());

        return report.toString();
    }

    public void demoTemplates() {
        System.out.println(runTemplateDemo());
    }

    private List<Map<String, Object>> createSaltTemplateRows() {
        List<Map<String, Object>> data = new ArrayList<>();

        Map<String, Object> kulenRow = new HashMap<>();
        kulenRow.put("productType", "KULEN");
        kulenRow.put("minSalt", 2.5);
        kulenRow.put("maxSalt", 3.0);
        data.add(kulenRow);

        Map<String, Object> sausageRow = new HashMap<>();
        sausageRow.put("productType", "SAUSAGE");
        sausageRow.put("minSalt", 1.8);
        sausageRow.put("maxSalt", 2.2);
        data.add(sausageRow);
        return data;
    }

    /**
     * Interactive evaluation: builds a batch from user-supplied parameters, runs the
     * Drools rules for the chosen phase, and returns the decision, the triggered
     * alerts/warnings, the full reasoning log and (when blocked) the backward-chaining
     * explanation tree describing HOW the decision was reached.
     */
    public EvaluationResult evaluateBatch(BatchEvaluationRequest request) {
        ProductType productType = parseProductType(request.getProductType());
        String id = (request.getId() == null || request.getId().isBlank())
            ? "UI-BATCH" : request.getId().trim();

        Batch batch = new Batch(id, productType);
        ProductionPhase phase = parsePhase(request.getPhase());
        batch.setCurrentPhase(phase);

        applyPhaseParameters(batch, phase, request);

        SaltRule saltRule = toSaltRule(request.getSaltRule());
        WeightLossRule weightLossRule = toWeightLossRule(request.getWeightLossRule());
        PhFermentationRule phFermentationRule = toPhFermentationRule(request.getPhFermentationRule());

        List<String> appliedTemplateRules = new ArrayList<>();

        // 1) Optional free-form salt .drt template rows: compiled and run as a
        //    dedicated template session so the generated rules can fire too.
        runSaltTemplateRows(batch, request.getSaltTemplateRows(), appliedTemplateRules);

        // 2) Main phase evaluation with the typed template-rule facts.
        runRules(batch, saltRule, weightLossRule, phFermentationRule);

        if (saltRule != null) {
            appliedTemplateRules.add("SaltRule(" + saltRule.getProductType() + ", "
                + saltRule.getMinSalt() + "-" + saltRule.getMaxSalt() + "%)");
        }
        if (weightLossRule != null) {
            appliedTemplateRules.add("WeightLossRule(" + weightLossRule.getProductType() + ", min "
                + weightLossRule.getMinWeightLossPercent() + "% after " + weightLossRule.getDeadlineWeeks() + " weeks)");
        }
        if (phFermentationRule != null) {
            appliedTemplateRules.add("PhFermentationRule(" + phFermentationRule.getProductType()
                + ", day5 pH <= " + phFermentationRule.getPhThresholdDay5() + ")");
        }

        EvaluationResult result = new EvaluationResult();
        result.setBatchId(batch.getId());
        result.setProductType(String.valueOf(batch.getProductType()));
        result.setPhase(String.valueOf(batch.getCurrentPhase()));
        result.setStatus(String.valueOf(batch.getStatus()));
        result.setAlerts(new ArrayList<>(batch.getActiveAlerts()));
        result.setLog(new ArrayList<>(batch.getLog()));
        result.setAppliedTemplateRules(appliedTemplateRules);
        result.setOutcome(deriveOutcome(batch));

        // Backward-chaining explanation only makes sense for a blocked batch.
        if (batch.isBlocked()) {
            ExplanationNode explanation =
                backwardChainingService.explainWhyBlocked(batch, saltRule, weightLossRule);
            result.setExplanation(explanation);
        }

        return result;
    }

    private String deriveOutcome(Batch batch) {
        if (batch.getStatus() == BatchStatus.BLOCKED) return "BLOCKED";
        if (batch.getStatus() == BatchStatus.APPROVED) return "APPROVED";
        boolean advanced = batch.getLog().stream().anyMatch(l -> l.contains("[ADVANCE]"));
        if (!batch.getActiveAlerts().isEmpty()) return "WARNING";
        if (advanced) return "ADVANCED";
        return "NO_TRIGGER";
    }

    private ProductType parseProductType(String value) {
        if (value == null || value.isBlank()) return ProductType.KULEN;
        try {
            return ProductType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ProductType.OTHER;
        }
    }

    private ProductionPhase parsePhase(String value) {
        if (value == null || value.isBlank()) return ProductionPhase.RECEIVING;
        return ProductionPhase.valueOf(value.trim().toUpperCase());
    }

    private void applyPhaseParameters(Batch batch, ProductionPhase phase, BatchEvaluationRequest r) {
        switch (phase) {
            case RECEIVING:
                if (r.getReceivingPh() != null) batch.setReceivingPh(r.getReceivingPh());
                if (r.getReceivingTemperature() != null) batch.setReceivingTemperature(r.getReceivingTemperature());
                if (r.getReceivingVisualScore() != null) batch.setReceivingVisualScore(r.getReceivingVisualScore());
                if (r.getRawMaterialShelfLifeDays() != null) {
                    batch.setRawMaterialShelfLife(LocalDate.now().plusDays(r.getRawMaterialShelfLifeDays()));
                }
                break;
            case CURING:
                if (r.getSaltPercentage() != null) batch.setSaltPercentage(r.getSaltPercentage());
                if (r.getBrineTemperature() != null) batch.setBrineTemperature(r.getBrineTemperature());
                if (r.getCuringDurationHours() != null) batch.setCuringDurationHours(r.getCuringDurationHours());
                break;
            case FERMENTATION:
                if (r.getFermentationPhByDay() != null) batch.setFermentationPhByDay(new ArrayList<>(r.getFermentationPhByDay()));
                if (r.getFermentationChamberTemperature() != null) batch.setFermentationChamberTemperature(r.getFermentationChamberTemperature());
                if (r.getFermentationChamberHumidity() != null) batch.setFermentationChamberHumidity(r.getFermentationChamberHumidity());
                break;
            case SMOKING:
                if (r.getSmokeTemperature() != null) batch.setSmokeTemperature(r.getSmokeTemperature());
                if (r.getSmokingDurationHours() != null) batch.setSmokingDurationHours(r.getSmokingDurationHours());
                break;
            case DRYING_AGING:
                if (r.getWeeklyWeightLossPercentages() != null) batch.setWeeklyWeightLossPercentages(new ArrayList<>(r.getWeeklyWeightLossPercentages()));
                if (r.getDryingRoomTemperature() != null) batch.setDryingRoomTemperature(r.getDryingRoomTemperature());
                if (r.getDryingRoomHumidity() != null) batch.setDryingRoomHumidity(r.getDryingRoomHumidity());
                break;
            case FINAL_INSPECTION:
                if (r.getFinalPh() != null) batch.setFinalPh(r.getFinalPh());
                if (r.getWaterActivity() != null) batch.setWaterActivity(r.getWaterActivity());
                if (r.getFinalVisualScore() != null) batch.setFinalVisualScore(r.getFinalVisualScore());
                break;
            default:
                break;
        }
    }

    private SaltRule toSaltRule(BatchEvaluationRequest.SaltRuleDto dto) {
        if (dto == null || dto.getMinSalt() == null || dto.getMaxSalt() == null) return null;
        return new SaltRule(parseProductType(dto.getProductType()), dto.getMinSalt(), dto.getMaxSalt());
    }

    private WeightLossRule toWeightLossRule(BatchEvaluationRequest.WeightLossRuleDto dto) {
        if (dto == null || dto.getMinWeightLossPercent() == null || dto.getDeadlineWeeks() == null) return null;
        return new WeightLossRule(parseProductType(dto.getProductType()), dto.getMinWeightLossPercent(), dto.getDeadlineWeeks());
    }

    private PhFermentationRule toPhFermentationRule(BatchEvaluationRequest.PhFermentationRuleDto dto) {
        if (dto == null || dto.getPhThresholdDay5() == null) return null;
        return new PhFermentationRule(parseProductType(dto.getProductType()), dto.getPhThresholdDay5());
    }

    /**
     * Compiles user-supplied salt .drt template rows and runs them against the batch
     * in a dedicated template session, merging any alerts/log into the batch.
     */
    private void runSaltTemplateRows(Batch batch, List<SaltTemplateRowDto> rows, List<String> appliedTemplateRules) {
        if (rows == null || rows.isEmpty()) return;

        List<Map<String, Object>> data = new ArrayList<>();
        for (SaltTemplateRowDto row : rows) {
            if (row.getProductType() == null || row.getMinSalt() == null || row.getMaxSalt() == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("productType", row.getProductType().trim().toUpperCase());
            map.put("minSalt", row.getMinSalt());
            map.put("maxSalt", row.getMaxSalt());
            data.add(map);
            appliedTemplateRules.add("Salt .drt row: " + map.get("productType")
                + " range " + row.getMinSalt() + "-" + row.getMaxSalt() + "%");
        }
        if (data.isEmpty()) return;

        KieSession ks = createSessionWithTemplates(data);
        try {
            ks.insert(batch);
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }
    }

    /**
     * Runs the complete demonstration for all scenarios.
     */
    public String runDemo() {
        synchronized (DEMO_OUTPUT_LOCK) {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            try (PrintStream demoOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
                System.setOut(demoOut);
                runDemoToConsole();
            } finally {
                System.setOut(originalOut);
            }

            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private void runDemoToConsole() {
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

    public String runCepDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  SMART QUALITY ADVISOR - CEP Demonstration");
        System.out.println("=".repeat(70));

        StringBuilder report = new StringBuilder();
        demoCepSmokeAggregation(report);
        demoCepPhTrend(report);
        demoCepDryerAggregation(report);

        System.out.println("\n[CEP SUMMARY]");
        System.out.println(report);
        return report.toString();
    }

    public String runCepPseudoClockDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  SMART QUALITY ADVISOR - CEP Pseudo Clock Demonstration");
        System.out.println("=".repeat(70));

        Batch batch = new Batch("CEP-CLOCK-001", ProductType.KULEN);
        batch.setCurrentPhase(ProductionPhase.SMOKING);
        batch.setSmokeTemperature(58.0);
        batch.setSmokingDurationHours(1);

        StringBuilder report = new StringBuilder();
        LocalDateTime start = LocalDateTime.of(2026, 6, 27, 9, 0);
        double[] temperatures = {58.0, 57.5, 58.2, 57.8, 58.4, 57.9, 58.1, 57.7, 58.3};
        List<Double> windowAverages = new ArrayList<>();
        double currentWindowSum = 0.0;
        int currentWindowCount = 0;

        for (int i = 0; i < temperatures.length; i++) {
            LocalDateTime eventTime = start.plusMinutes(i * 5L);
            currentWindowSum += temperatures[i];
            currentWindowCount++;

            report.append("T+")
                .append(i * 5)
                .append(" min | pseudo clock inserted smoke event ")
                .append(String.format("%.1f", temperatures[i]))
                .append(" C at ")
                .append(eventTime)
                .append(System.lineSeparator());

            if (currentWindowCount == 3) {
                double average = currentWindowSum / currentWindowCount;
                windowAverages.add(average);
                report.append("       CEP C-1 simulated 15-minute smoke average: ")
                    .append(String.format("%.2f", average))
                    .append(" C")
                    .append(System.lineSeparator());
                currentWindowSum = 0.0;
                currentWindowCount = 0;
            }

            if (i < temperatures.length - 1) {
                report.append("       pseudo clock advanced by 5 minutes")
                    .append(System.lineSeparator());
            }
        }

        if (windowAverages.size() >= 3) {
            double aggregate45Min = windowAverages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

            report.append(System.lineSeparator())
                .append("CEP C-2 simulated 45-minute smoke average: ")
                .append(String.format("%.2f", aggregate45Min))
                .append(" C")
                .append(System.lineSeparator());

            if (aggregate45Min < 60.0) {
                batch.addAlert("F4-4: InsufficientHeatTreatment - 45-minute smoke aggregate average = "
                    + String.format("%.2f", aggregate45Min) + " C (< 60 C). CRITICAL!");
                batch.block("F4-5: SmokingFailed - insufficient heat treatment. Repeat smoking or discard the batch.");
            }
        }

        report.append(System.lineSeparator())
            .append("Final status: ")
            .append(batch.getStatus())
            .append(" | phase: ")
            .append(batch.getCurrentPhase())
            .append(System.lineSeparator())
            .append("Alerts: ")
            .append(batch.getActiveAlerts())
            .append(System.lineSeparator())
            .append("Log: ")
            .append(batch.getLog())
            .append(System.lineSeparator());

        System.out.println("\n[CEP PSEUDO CLOCK SUMMARY]");
        System.out.println(report);
        return report.toString();
    }

    private void demoCepSmokeAggregation(StringBuilder report) {
        System.out.println("\n--- CEP C-1/C-2: Smoke temperature aggregation and chaining ---");

        Batch batch = new Batch("CEP-SMOKE-001", ProductType.KULEN);
        batch.setCurrentPhase(ProductionPhase.SMOKING);

        LocalDateTime start = LocalDateTime.of(2026, 5, 22, 8, 0);
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(batch);
        insertSmokeWindow(ks, batch.getId(), start, 58.0, 57.0, 59.0);
        insertSmokeWindow(ks, batch.getId(), start.plusMinutes(15), 56.0, 58.0, 57.0);
        insertSmokeWindow(ks, batch.getId(), start.plusMinutes(30), 59.0, 58.0, 57.0);
        ks.fireAllRules();
        ks.dispose();

        printResult(batch);
        report.append("C-1/C-2 smoke CEP -> ")
            .append(batch.getStatus())
            .append(" | alerts: ")
            .append(batch.getActiveAlerts())
            .append(System.lineSeparator());
    }

    private void demoCepPhTrend(StringBuilder report) {
        System.out.println("\n--- CEP C-3/C-4: pH trend detection and chaining ---");

        Batch batch = new Batch("CEP-PH-001", ProductType.KULEN);
        batch.setCurrentPhase(ProductionPhase.FERMENTATION);
        batch.setFermentationPhByDay(Arrays.asList(6.2, 6.12, 6.05, 5.95, 5.8));
        batch.setFermentationChamberTemperature(22.0);
        batch.setFermentationChamberHumidity(90.0);

        LocalDateTime start = LocalDateTime.of(2026, 5, 22, 8, 0);
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(batch);
        ks.insert(new PhMeasurementEvent(batch.getId(), 1, 6.20, start));
        ks.insert(new PhMeasurementEvent(batch.getId(), 2, 6.12, start.plusDays(1)));
        ks.insert(new PhMeasurementEvent(batch.getId(), 3, 6.05, start.plusDays(2)));
        ks.fireAllRules();
        ks.dispose();

        printResult(batch);
        report.append("C-3/C-4 pH CEP -> ")
            .append(batch.getStatus())
            .append(" | alerts: ")
            .append(batch.getActiveAlerts())
            .append(System.lineSeparator());
    }

    private void demoCepDryerAggregation(StringBuilder report) {
        System.out.println("\n--- CEP C-5: Dryer 4h temperature aggregation ---");

        Batch batch = new Batch("CEP-DRY-001", ProductType.KULEN);
        batch.setCurrentPhase(ProductionPhase.DRYING_AGING);

        LocalDateTime start = LocalDateTime.of(2026, 5, 22, 12, 0);
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(batch);
        ks.insert(new DryerTemperatureEvent(batch.getId(), 17.0, start));
        ks.insert(new DryerTemperatureEvent(batch.getId(), 16.8, start.plusHours(1)));
        ks.insert(new DryerTemperatureEvent(batch.getId(), 17.3, start.plusHours(2)));
        ks.insert(new DryerTemperatureEvent(batch.getId(), 16.9, start.plusHours(3)));
        ks.fireAllRules();
        ks.dispose();

        printResult(batch);
        report.append("C-5 dryer CEP -> ")
            .append(batch.getStatus())
            .append(" | alerts: ")
            .append(batch.getActiveAlerts())
            .append(System.lineSeparator());
    }

    private void insertSmokeWindow(KieSession ks, String batchId, LocalDateTime windowStart, double... temperatures) {
        for (int i = 0; i < temperatures.length; i++) {
            ks.insert(new SmokeTemperatureEvent(batchId, temperatures[i], windowStart.plusMinutes(i * 5L)));
        }
    }

    public String runBackwardChainingDemo() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  SMART QUALITY ADVISOR - Recursive Backward Chaining Demos");
        System.out.println("=".repeat(70));

        SaltRule kulenSaltRule = new SaltRule(ProductType.KULEN, 2.5, 3.0);
        WeightLossRule kulenWeightLossRule = new WeightLossRule(ProductType.KULEN, 30.0, 8);

        StringBuilder report = new StringBuilder();

        Batch phase1 = new Batch("SER-BC-001", ProductType.KULEN);
        phase1.setReceivingPh(6.8);
        phase1.setReceivingTemperature(4.0);
        phase1.setReceivingVisualScore(4);
        phase1.setRawMaterialShelfLife(LocalDate.now().plusDays(20));
        appendBackwardScenario(report, "Phase 1 - Receiving pH critical", phase1, null, null);

        Batch phase2 = new Batch("SER-BC-002", ProductType.KULEN);
        phase2.setCurrentPhase(ProductionPhase.CURING);
        phase2.setSaltPercentage(1.5);
        phase2.setBrineTemperature(5.0);
        phase2.setCuringDurationHours(48);
        appendBackwardScenario(report, "Phase 2 - Salt below safe minimum", phase2, kulenSaltRule, null);

        Batch phase3 = new Batch("SER-2025-042", ProductType.KULEN);
        phase3.setCurrentPhase(ProductionPhase.FERMENTATION);
        phase3.setFermentationPhByDay(Arrays.asList(6.2, 6.08, 5.99, 5.91, 5.84));
        phase3.setFermentationChamberTemperature(27.0);
        phase3.setFermentationChamberHumidity(90.0);
        appendBackwardScenario(report, "Phase 3 - Fermentation pH and trend failure", phase3, null, null);

        Batch phase4 = new Batch("SER-BC-004", ProductType.KULEN);
        phase4.setCurrentPhase(ProductionPhase.SMOKING);
        phase4.setSmokeTemperature(55.0);
        phase4.setSmokingDurationHours(6);
        appendBackwardScenario(report, "Phase 4 - Insufficient thermal treatment", phase4, null, null);

        Batch phase5 = new Batch("SER-BC-005", ProductType.KULEN);
        phase5.setCurrentPhase(ProductionPhase.DRYING_AGING);
        phase5.setWeeklyWeightLossPercentages(Arrays.asList(3.0, 5.0, 8.0, 11.0, 13.0, 16.0, 19.0, 22.0));
        phase5.setDryingRoomTemperature(14.0);
        phase5.setDryingRoomHumidity(80.0);
        appendBackwardScenario(report, "Phase 5 - Weight loss below product minimum", phase5, null, kulenWeightLossRule);

        Batch phase6 = new Batch("SER-BC-006", ProductType.KULEN);
        phase6.setCurrentPhase(ProductionPhase.FINAL_INSPECTION);
        phase6.setFinalPh(5.5);
        phase6.setWaterActivity(0.88);
        phase6.setFinalVisualScore(4);
        appendBackwardScenario(report, "Phase 6 - Final pH unacceptable", phase6, null, null);

        System.out.println("\n[BACKWARD CHAINING EXPLANATION]");
        System.out.println(report);
        return report.toString();
    }

    private void appendBackwardScenario(StringBuilder report, String title, Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        System.out.println("\n--- BACKWARD SCENARIO: " + title + " ---");
        runRules(batch, saltRule, weightLossRule);

        report.append("=== ")
            .append(title)
            .append(" ===")
            .append(System.lineSeparator())
            .append(backwardChainingService.explainWhyBlockedText(batch, saltRule, weightLossRule))
            .append(System.lineSeparator());
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

        System.out.println("\n[2c] Salt = 2.5% - outside sausage range (1.8-2.2%) -> template warning");
        Batch b2c = new Batch("BATCH-012", ProductType.SAUSAGE);
        b2c.setCurrentPhase(ProductionPhase.CURING);
        b2c.setSaltPercentage(2.5);
        b2c.setBrineTemperature(5.0);
        b2c.setCuringDurationHours(48);
        runRules(b2c, sausageSaltRule, null);
        printResult(b2c);

        System.out.println("\n[2d] Salt = 2.0%, all OK for sausage -> advance to Phase 3");
        Batch b2d = new Batch("BATCH-013", ProductType.SAUSAGE);
        b2d.setCurrentPhase(ProductionPhase.CURING);
        b2d.setSaltPercentage(2.0);
        b2d.setBrineTemperature(5.0);
        b2d.setCuringDurationHours(48);
        runRules(b2d, sausageSaltRule, null);
        printResult(b2d);
    }

    private void demoPhase3() {
        System.out.println("\n--- SCENARIO 3: Phase 3 - Fermentation (pH trend CEP + template) ---");

        PhFermentationRule kulenPhRule = new PhFermentationRule(ProductType.KULEN, 5.0);

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

        System.out.println("\n[3c] Template threshold: kulen day 5 pH = 5.1 > custom 5.0 -> block");
        Batch b3c = new Batch("BATCH-022", ProductType.KULEN);
        b3c.setCurrentPhase(ProductionPhase.FERMENTATION);
        b3c.setFermentationPhByDay(Arrays.asList(6.2, 5.9, 5.6, 5.3, 5.1));
        b3c.setFermentationChamberTemperature(21.0);
        b3c.setFermentationChamberHumidity(90.0);
        runRules(b3c, null, null, kulenPhRule);
        printResult(b3c);

        System.out.println("\n[3d] Day 5 pH = 5.1, no template, trend OK -> advance to Phase 4");
        Batch b3d = new Batch("BATCH-023", ProductType.KULEN);
        b3d.setCurrentPhase(ProductionPhase.FERMENTATION);
        b3d.setFermentationPhByDay(Arrays.asList(6.2, 5.9, 5.6, 5.4, 5.1));
        b3d.setFermentationChamberTemperature(21.0);
        b3d.setFermentationChamberHumidity(90.0);
        runRules(b3d, null, null);
        printResult(b3d);
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
        WeightLossRule sausageWeightLossRule = new WeightLossRule(ProductType.SAUSAGE, 25.0, 6);

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

        System.out.println("\n[5c] Weight loss = 20% < 25% after 6 weeks for sausage -> progress blocked (template)");
        Batch b5c = new Batch("BATCH-042", ProductType.SAUSAGE);
        b5c.setCurrentPhase(ProductionPhase.DRYING_AGING);
        b5c.setWeeklyWeightLossPercentages(Arrays.asList(4.0, 7.0, 10.0, 13.0, 17.0, 20.0));
        b5c.setDryingRoomTemperature(14.0);
        b5c.setDryingRoomHumidity(80.0);
        runRules(b5c, null, sausageWeightLossRule);
        printResult(b5c);
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
        runRules(batch, saltRule, weightLossRule, null);
    }

    private void runRules(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule, PhFermentationRule phFermentationRule) {
        KieSession ks = kieContainer.newKieSession("ksession-rules");
        ks.insert(batch);
        if (saltRule != null) ks.insert(saltRule);
        if (weightLossRule != null) ks.insert(weightLossRule);
        if (phFermentationRule != null) ks.insert(phFermentationRule);
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
