package io.github.devh0407.codetasks.model;

public record ExternalIssue(
        String id,
        String filePath,
        int line,
        IssueType type,
        String message,
        String source
) {
    public ExternalIssue {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath must not be blank");
        }
        if (line < 1) {
            throw new IllegalArgumentException("line must be >= 1");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (message == null) {
            message = "";
        }
        if (source == null) {
            source = "";
        }
    }
}
