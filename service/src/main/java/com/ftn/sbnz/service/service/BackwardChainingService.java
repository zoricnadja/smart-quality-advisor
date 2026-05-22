package com.ftn.sbnz.service.service;

import com.ftn.sbnz.model.Batch;
import com.ftn.sbnz.model.ProductionPhase;
import com.ftn.sbnz.model.SaltRule;
import com.ftn.sbnz.model.WeightLossRule;
import com.ftn.sbnz.service.dto.ExplanationNode;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class BackwardChainingService {

    private static final double MIN_SAFE_SALT = 1.8;
    private static final double MAX_FINAL_PH = 5.3;
    private static final double MAX_WATER_ACTIVITY = 0.92;

    public ExplanationNode explainWhyBlocked(Batch batch) {
        return explainWhyBlocked(batch, null, null);
    }

    public ExplanationNode explainWhyBlocked(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        return prove(Query.ZASTO_JE_SERIJA_BLOKIRANA, new Context(batch, saltRule, weightLossRule));
    }

    public String explainWhyBlockedText(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        return explainWhyBlocked(batch, saltRule, weightLossRule).toIndentedString();
    }

    private ExplanationNode prove(Query query, Context context) {
        Batch batch = context.batch;

        switch (query) {
            case ZASTO_JE_SERIJA_BLOKIRANA:
                ExplanationNode root = node(query, batch.isBlocked(),
                    batch.isBlocked()
                        ? "Serija " + batch.getId() + " ima status BLOKIRANA."
                        : "Serija " + batch.getId() + " nije blokirana.");
                if (batch.isBlocked()) {
                    root.addChild(prove(Query.UZROK_BLOKIRANJA, context));
                }
                return root;

            case UZROK_BLOKIRANJA:
                ExplanationNode cause = node(query, false, "Rekurzivno se proveravaju faze od 1 do 6.");
                for (Query phaseQuery : Query.phaseQueries()) {
                    ExplanationNode phaseResult = prove(phaseQuery, context);
                    cause.addChild(phaseResult);
                    if (phaseResult.isProven()) {
                        return node(query, true, "Uzrok je pronadjen u " + phaseName(phaseQuery) + ".")
                            .addChild(phaseResult);
                    }
                }
                return cause;

            case ZASTO_FAZA_1_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.RECEIVING) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 1.");
                }
                ExplanationNode phase1 = node(query, false, "Faza 1 nije uzrok blokiranja.");
                return firstProven(phase1,
                    leaf(Query.PROVERA_PH_PRIJEMA, batch.getReceivingPh() > 6.5,
                        "pH prijema = " + batch.getReceivingPh() + " (prag: <= 6.5)"),
                    leaf(Query.PROVERA_VIZUELNOG_PRIJEMA, batch.getReceivingVisualScore() < 3,
                        "vizuelna ocena = " + batch.getReceivingVisualScore() + " (prag: >= 3)"),
                    node(Query.PROVERA_TEMPERATURE_I_ROKA, batch.getReceivingTemperature() > 7.0 && batch.getDaysUntilRawMaterialExpiry() < 5,
                        "temperatura = " + batch.getReceivingTemperature() + " C, dana do isteka = "
                            + batch.getDaysUntilRawMaterialExpiry() + " (blokira kombinacija > 7 C i < 5 dana)"));

            case ZASTO_FAZA_2_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.CURING) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 2.");
                }
                ExplanationNode phase2 = node(query, false, "Faza 2 nije uzrok blokiranja.");
                phase2.addChild(leaf(Query.PROVERA_SOLI, batch.getSaltPercentage() < MIN_SAFE_SALT,
                    "so = " + batch.getSaltPercentage() + "% (bezbednosni minimum: " + MIN_SAFE_SALT + "%)"));
                phase2.addChild(leaf(Query.PROVERA_TEMPERATURE_SALAMURE, batch.getBrineTemperature() > 8.0,
                    "temperatura salamure = " + batch.getBrineTemperature() + " C (prag: <= 8 C)"));
                phase2.addChild(leaf(Query.PROVERA_TRAJANJA_SALAMURENJA,
                    batch.getCuringDurationHours() < 24 || batch.getCuringDurationHours() > 96,
                    "trajanje salamurenja = " + batch.getCuringDurationHours() + "h (opseg: 24-96h)"));
                if (context.saltRule != null) {
                    phase2.addChild(leaf(Query.PROVERA_SOLI_PO_TIPU,
                        batch.getSaltPercentage() < context.saltRule.getMinSalt()
                            || batch.getSaltPercentage() > context.saltRule.getMaxSalt(),
                        "so = " + batch.getSaltPercentage() + "%, opseg za " + batch.getProductType()
                            + ": " + context.saltRule.getMinSalt() + "-" + context.saltRule.getMaxSalt() + "%"));
                }
                return withPhaseResult(phase2);

            case ZASTO_FAZA_3_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.FERMENTATION) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 3.");
                }
                ExplanationNode phase3 = node(query, false, "Faza 3 nije uzrok blokiranja.");
                phase3.addChild(prove(Query.ZASTO_PH_FERMENTACIJE_FAILED, context));
                phase3.addChild(prove(Query.ZASTO_FERMENTACIJA_UGROZENA, context));
                phase3.addChild(leaf(Query.PROVERA_TEMPERATURE_FERMENTACIJE,
                    batch.getFermentationChamberTemperature() > 26.0,
                    "temperatura komore = " + batch.getFermentationChamberTemperature() + " C (prag: <= 26 C)"));
                return withPhaseResult(phase3);

            case ZASTO_PH_FERMENTACIJE_FAILED:
                return leaf(query, batch.getPhOnDay(5) > MAX_FINAL_PH && batch.getPhOnDay(5) > 0,
                    "pH na dan 5 = " + batch.getPhOnDay(5) + " (prag: <= " + MAX_FINAL_PH + ")");

            case ZASTO_FERMENTACIJA_UGROZENA:
                ExplanationNode trend = node(query, false, "Trend opadanja pH nije aktivirao blokadu.");
                for (int day = 2; day <= Math.min(5, batch.getFermentationPhByDay().size()); day++) {
                    trend.addChild(leaf(Query.TREND_PH_OPADA,
                        batch.getPhDeltaBetweenDays(day) >= 0 && batch.getPhDeltaBetweenDays(day) < 0.1,
                        "dan " + (day - 1) + "-" + day + ": delta = "
                            + String.format("%.2f", batch.getPhDeltaBetweenDays(day)) + " (prag: >= 0.10)"));
                }
                int consecutiveSlowDays = maxConsecutiveSlowPhDrops(batch);
                ExplanationNode consecutive = node(Query.PROVERA_UZASTOPNOG_TRENDA, consecutiveSlowDays >= 2,
                    "najduzi niz sporog pada pH = " + consecutiveSlowDays + " dana (prag: >= 2)");
                trend.addChild(consecutive);
                return withPhaseResult(trend);

            case ZASTO_FAZA_4_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.SMOKING) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 4.");
                }
                ExplanationNode phase4 = node(query, false, "Faza 4 nije uzrok blokiranja.");
                phase4.addChild(prove(Query.ZASTO_NEDOV_TERM_OBRADA, context));
                return withPhaseResult(phase4);

            case ZASTO_NEDOV_TERM_OBRADA:
                return leaf(Query.TEMP_DIMA_PO_PROZORIMA_CEP, batch.getSmokeTemperature() < 60.0 && batch.getSmokeTemperature() > 0,
                    "prosecna temperatura dima = " + batch.getSmokeTemperature()
                        + " C kroz CEP prozore (prag: >= 60 C)");

            case ZASTO_FAZA_5_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.DRYING_AGING) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 5.");
                }
                ExplanationNode phase5 = node(query, false, "Faza 5 nije uzrok blokiranja.");
                double minimumLoss = context.weightLossRule != null ? context.weightLossRule.getMinWeightLossPercent() : 25.0;
                int deadlineWeeks = context.weightLossRule != null ? context.weightLossRule.getDeadlineWeeks()
                    : batch.getWeeklyWeightLossPercentages().size();
                phase5.addChild(leaf(Query.PROVERA_GUB_TEZINE,
                    batch.getWeeklyWeightLossPercentages().size() >= deadlineWeeks && batch.getTotalWeightLoss() < minimumLoss,
                    "ukupan gubitak = " + String.format("%.1f", batch.getTotalWeightLoss())
                        + "% (minimum: " + String.format("%.1f", minimumLoss) + "% posle " + deadlineWeeks + " nedelja)"));
                return withPhaseResult(phase5);

            case ZASTO_FAZA_6_BLOKIRANA:
                if (batch.getCurrentPhase() != ProductionPhase.FINAL_INSPECTION) {
                    return node(query, false, "Serija nije zaustavljena u Fazi 6.");
                }
                ExplanationNode phase6 = node(query, false, "Faza 6 nije uzrok blokiranja.");
                phase6.addChild(leaf(Query.PROVERA_PH_FINALNOG, batch.getFinalPh() > MAX_FINAL_PH,
                    "pH finalnog proizvoda = " + batch.getFinalPh() + " (prag: <= " + MAX_FINAL_PH + ")"));
                phase6.addChild(leaf(Query.PROVERA_AW, batch.getWaterActivity() > MAX_WATER_ACTIVITY,
                    "aw = " + batch.getWaterActivity() + " (prag: <= " + MAX_WATER_ACTIVITY + ")"));
                return withPhaseResult(phase6);

            default:
                return node(query, false, "Upit nema implementiranu proveru.");
        }
    }

    private ExplanationNode firstProven(ExplanationNode parent, ExplanationNode... children) {
        Arrays.stream(children).forEach(parent::addChild);

        Optional<ExplanationNode> provenChild = parent.getChildren().stream()
            .filter(ExplanationNode::isProven)
            .findFirst();

        if (!provenChild.isPresent()) {
            return parent;
        }

        ExplanationNode result = cloneWith(parent, true,
            "Blokada je dokazana preko podupita: " + provenChild.get().getQuery());
        parent.getChildren().forEach(result::addChild);
        return result;
    }

    private ExplanationNode withPhaseResult(ExplanationNode phase) {
        boolean proven = phase.getChildren().stream().anyMatch(ExplanationNode::isProven);
        if (!proven) {
            return phase;
        }

        ExplanationNode result = cloneWith(phase, true, "Blokada je dokazana jednim ili vise podupita.");
        phase.getChildren().forEach(result::addChild);
        return result;
    }

    private ExplanationNode cloneWith(ExplanationNode source, boolean proven, String result) {
        return new ExplanationNode(source.getQuery(), proven, result);
    }

    private ExplanationNode leaf(Query query, boolean proven, String result) {
        return node(query, proven, result);
    }

    private ExplanationNode node(Query query, boolean proven, String result) {
        return new ExplanationNode(query.getLabel(), proven, result);
    }

    private int maxConsecutiveSlowPhDrops(Batch batch) {
        int max = 0;
        int current = 0;
        for (int day = 2; day <= Math.min(5, batch.getFermentationPhByDay().size()); day++) {
            double delta = batch.getPhDeltaBetweenDays(day);
            if (delta >= 0 && delta < 0.1) {
                current++;
                max = Math.max(max, current);
            } else {
                current = 0;
            }
        }
        return max;
    }

    private String phaseName(Query phaseQuery) {
        switch (phaseQuery) {
            case ZASTO_FAZA_1_BLOKIRANA:
                return "Fazi 1 (Prijem sirovine)";
            case ZASTO_FAZA_2_BLOKIRANA:
                return "Fazi 2 (Salamurenje)";
            case ZASTO_FAZA_3_BLOKIRANA:
                return "Fazi 3 (Fermentacija)";
            case ZASTO_FAZA_4_BLOKIRANA:
                return "Fazi 4 (Dimljenje)";
            case ZASTO_FAZA_5_BLOKIRANA:
                return "Fazi 5 (Susenje/Zrenje)";
            case ZASTO_FAZA_6_BLOKIRANA:
                return "Fazi 6 (Finalna kontrola)";
            default:
                return "nepoznatoj fazi";
        }
    }

    private static class Context {
        private final Batch batch;
        private final SaltRule saltRule;
        private final WeightLossRule weightLossRule;

        private Context(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
            this.batch = batch;
            this.saltRule = saltRule;
            this.weightLossRule = weightLossRule;
        }
    }

    private enum Query {
        ZASTO_JE_SERIJA_BLOKIRANA("zastoJeSerijaBlokirana($s)"),
        UZROK_BLOKIRANJA("uzrokBlokiranja($s)"),
        ZASTO_FAZA_1_BLOKIRANA("zastoFaza1Blokirana($s)"),
        ZASTO_FAZA_2_BLOKIRANA("zastoFaza2Blokirana($s)"),
        ZASTO_FAZA_3_BLOKIRANA("zastoFaza3Blokirana($s)"),
        ZASTO_FAZA_4_BLOKIRANA("zastoFaza4Blokirana($s)"),
        ZASTO_FAZA_5_BLOKIRANA("zastoFaza5Blokirana($s)"),
        ZASTO_FAZA_6_BLOKIRANA("zastoFaza6Blokirana($s)"),
        PROVERA_PH_PRIJEMA("proveraPhPrijema($s, $ph)"),
        PROVERA_VIZUELNOG_PRIJEMA("proveraVizuelnogPrijema($s, $ocena)"),
        PROVERA_TEMPERATURE_I_ROKA("proveraTemperatureIRoka($s, $temp, $dani)"),
        PROVERA_SOLI("proveraSoli($s, $sol)"),
        PROVERA_SOLI_PO_TIPU("proveraSoliPoTipu($s, $sol, $opseg)"),
        PROVERA_TEMPERATURE_SALAMURE("proveraTemperatureSalamure($s, $temp)"),
        PROVERA_TRAJANJA_SALAMURENJA("proveraTrajanjaSalamurenja($s, $sati)"),
        ZASTO_PH_FERMENTACIJE_FAILED("zastoPhFermentacijeFailed($s)"),
        ZASTO_FERMENTACIJA_UGROZENA("zastoFermentacijaUgrozena($s)"),
        PROVERA_TEMPERATURE_FERMENTACIJE("proveraTemperatureFermentacije($s, $temp)"),
        TREND_PH_OPADA("trendPhOpada($s, $dan, $delta)"),
        PROVERA_UZASTOPNOG_TRENDA("proveraUzastopnogTrenda($s, $brojDana)"),
        ZASTO_NEDOV_TERM_OBRADA("zastoNedovTermObrada($s)"),
        TEMP_DIMA_PO_PROZORIMA_CEP("tempDimaPoProzorimaCEP($s, $avg)"),
        PROVERA_GUB_TEZINE("proveraGubTezine($s, $ned, $pct)"),
        PROVERA_PH_FINALNOG("proveraPhFinalnog($s, $ph)"),
        PROVERA_AW("proveraAw($s, $aw)");

        private final String label;

        Query(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static List<Query> phaseQueries() {
            return Arrays.asList(
                ZASTO_FAZA_1_BLOKIRANA,
                ZASTO_FAZA_2_BLOKIRANA,
                ZASTO_FAZA_3_BLOKIRANA,
                ZASTO_FAZA_4_BLOKIRANA,
                ZASTO_FAZA_5_BLOKIRANA,
                ZASTO_FAZA_6_BLOKIRANA
            );
        }
    }
}
