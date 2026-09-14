package io.github.devh0407.codetasks.model;

import java.util.Locale;

public enum IssueType {
    TODO,
    PROBLEM;

    public static IssueType parse(String value) {
        if (value == null || value.isBlank()) {
            return TODO;
        }
        return IssueType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
