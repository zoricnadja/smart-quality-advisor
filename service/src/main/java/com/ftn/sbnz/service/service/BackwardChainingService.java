package com.ftn.sbnz.service.service;

import com.ftn.sbnz.model.Batch;
import com.ftn.sbnz.model.SaltRule;
import com.ftn.sbnz.model.WeightLossRule;
import com.ftn.sbnz.service.dto.ExplanationNode;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.kie.api.runtime.rule.Variable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class BackwardChainingService {

    private static final String ROOT_QUERY = "whyBatchIsBlocked(batch)";

    private final KieContainer kieContainer;

    @Autowired
    public BackwardChainingService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public ExplanationNode explainWhyBlocked(Batch batch) {
        return explainWhyBlocked(batch, null, null);
    }

    public ExplanationNode explainWhyBlocked(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        ExplanationNode root = new ExplanationNode(ROOT_QUERY, batch.isBlocked(),
            batch.isBlocked() ? "The batch is blocked." : "The batch is not blocked.");

        if (!batch.isBlocked()) {
            root.addChild(new ExplanationNode("blockingCause", false,
                "Batch " + batch.getId() + " is not blocked."));
            return root;
        }

        KieSession session = kieContainer.newKieSession("ksession-rules");
        try {
            session.insert(batch);
            if (saltRule != null) session.insert(saltRule);
            if (weightLossRule != null) session.insert(weightLossRule);
            session.fireAllRules();

            Set<String> provenNodes = collectRecursiveProofs(session, batch.getId());
            appendDirectExplanations(session, root, batch.getId(), ROOT_QUERY, provenNodes, new HashSet<>());

            if (root.getChildren().isEmpty()) {
                root.addChild(new ExplanationNode("isExplainedBy", false,
                    "Drools recursive query found that the batch is blocked, but no phase-specific explanation edge matched the current data."));
            }
        } finally {
            session.dispose();
        }

        return root;
    }

    public String explainWhyBlockedText(Batch batch, SaltRule saltRule, WeightLossRule weightLossRule) {
        return explainWhyBlocked(batch, saltRule, weightLossRule).toIndentedString();
    }

    private Set<String> collectRecursiveProofs(KieSession session, String batchId) {
        Set<String> provenNodes = new LinkedHashSet<>();
        QueryResults results = session.getQueryResults(
            "isExplainedBy",
            batchId,
            ROOT_QUERY,
            Variable.v,
            Variable.v
        );

        for (QueryResultsRow row : results) {
            Object descendant = value(row, "$descendant", "descendant");
            if (descendant != null) {
                provenNodes.add(descendant.toString());
            }
        }

        return provenNodes;
    }

    private void appendDirectExplanations(KieSession session, ExplanationNode parentNode, String batchId,
                                          String parentQuery, Set<String> provenNodes, Set<String> visited) {
        if (!visited.add(parentQuery)) {
            return;
        }

        QueryResults results = session.getQueryResults(
            "directExplanationEdge",
            batchId,
            parentQuery,
            Variable.v,
            Variable.v
        );

        for (QueryResultsRow row : results) {
            String child = String.valueOf(value(row, "$child", "child"));
            String text = String.valueOf(value(row, "$text", "text"));
            if (!provenNodes.contains(child)) {
                continue;
            }

            ExplanationNode childNode = new ExplanationNode(child, true, text);
            parentNode.addChild(childNode);
            appendDirectExplanations(session, childNode, batchId, child, provenNodes, visited);
        }
    }

    private Object value(QueryResultsRow row, String droolsName, String plainName) {
        Object value = row.get(droolsName);
        return value != null ? value : row.get(plainName);
    }
}
