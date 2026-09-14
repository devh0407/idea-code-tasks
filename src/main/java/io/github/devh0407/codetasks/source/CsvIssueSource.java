package io.github.devh0407.codetasks.source;

import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.model.IssueType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CsvIssueSource implements IssueSource {

    @Override
    public List<ExternalIssue> load(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }

            List<String> headers = parseLine(stripBom(headerLine));
            Map<String, Integer> indexes = headerIndexes(headers);
            requireColumns(indexes, "file", "line", "type", "message");

            List<ExternalIssue> issues = new ArrayList<>();
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseLine(line);
                try {
                    issues.add(toIssue(values, indexes, row, path.getFileName().toString()));
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid CSV row " + row + ": " + ex.getMessage(), ex);
                }
            }
            return List.copyOf(issues);
        }
    }

    private ExternalIssue toIssue(
            List<String> values,
            Map<String, Integer> indexes,
            int row,
            String defaultSource
    ) {
        String id = value(values, indexes, "id");
        if (id.isBlank()) {
            id = "ROW-" + row;
        }
        String file = value(values, indexes, "file");
        int line = Integer.parseInt(value(values, indexes, "line").trim());
        IssueType type = IssueType.parse(value(values, indexes, "type"));
        String message = value(values, indexes, "message");
        String source = value(values, indexes, "source");
        if (source.isBlank()) {
            source = defaultSource;
        }
        return new ExternalIssue(id, file, line, type, message, source);
    }

    private static String value(List<String> values, Map<String, Integer> indexes, String name) {
        Integer index = indexes.get(name);
        if (index == null || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private static Map<String, Integer> headerIndexes(List<String> headers) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            result.put(headers.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        return result;
    }

    private static void requireColumns(Map<String, Integer> indexes, String... required) throws IOException {
        for (String column : required) {
            if (!indexes.containsKey(column)) {
                throw new IOException("Missing required column: " + column);
            }
        }
    }

    static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
