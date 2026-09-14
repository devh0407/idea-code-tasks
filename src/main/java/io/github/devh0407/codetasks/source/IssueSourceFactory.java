package io.github.devh0407.codetasks.source;

import java.nio.file.Path;
import java.util.Locale;

public final class IssueSourceFactory {
    private IssueSourceFactory() {
    }

    public static IssueSource forPath(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".csv")) {
            return new CsvIssueSource();
        }
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return new ExcelIssueSource();
        }
        throw new IllegalArgumentException("Unsupported issue source: " + fileName);
    }
}
