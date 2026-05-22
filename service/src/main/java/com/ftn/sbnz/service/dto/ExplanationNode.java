package com.ftn.sbnz.service.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExplanationNode {

    private final String query;
    private final boolean proven;
    private final String result;
    private final List<ExplanationNode> children;

    public ExplanationNode(String query, boolean proven, String result) {
        this.query = query;
        this.proven = proven;
        this.result = result;
        this.children = new ArrayList<>();
    }

    public ExplanationNode addChild(ExplanationNode child) {
        this.children.add(child);
        return this;
    }

    public String getQuery() {
        return query;
    }

    public boolean isProven() {
        return proven;
    }

    public String getResult() {
        return result;
    }

    public List<ExplanationNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public String toIndentedString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder, 0);
        return builder.toString();
    }

    private void appendTo(StringBuilder builder, int depth) {
        builder.append("  ".repeat(depth))
            .append(proven ? "[OK] " : "[--] ")
            .append(query)
            .append(" -> ")
            .append(result)
            .append(System.lineSeparator());

        for (ExplanationNode child : children) {
            child.appendTo(builder, depth + 1);
        }
    }
}
