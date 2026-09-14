package io.github.devh0407.codetasks.source;

import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.model.IssueType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ExcelIssueSource implements IssueSource {

    @Override
    public List<ExternalIssue> load(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                return List.of();
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> indexes = headerIndexes(headerRow, formatter);
            requireColumns(indexes, "file", "line", "type", "message");

            List<ExternalIssue> issues = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlank(row, formatter)) {
                    continue;
                }

                try {
                    String id = value(row, indexes, "id", formatter);
                    if (id.isBlank()) {
                        id = "ROW-" + (rowIndex + 1);
                    }
                    String file = value(row, indexes, "file", formatter);
                    int line = parseLineNumber(value(row, indexes, "line", formatter));
                    IssueType type = IssueType.parse(value(row, indexes, "type", formatter));
                    String message = value(row, indexes, "message", formatter);
                    String source = value(row, indexes, "source", formatter);
                    if (source.isBlank()) {
                        source = path.getFileName().toString();
                    }
                    issues.add(new ExternalIssue(id, file, line, type, message, source));
                } catch (RuntimeException ex) {
                    throw new IOException("Invalid spreadsheet row " + (rowIndex + 1) + ": " + ex.getMessage(), ex);
                }
            }
            return List.copyOf(issues);
        }
    }

    private static int parseLineNumber(String value) {
        String normalized = value.trim();
        if (normalized.endsWith(".0")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        return Integer.parseInt(normalized);
    }

    private static Map<String, Integer> headerIndexes(Row row, DataFormatter formatter) {
        Map<String, Integer> result = new HashMap<>();
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            String name = formatter.formatCellValue(row.getCell(i)).trim().toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                result.put(name, i);
            }
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

    private static String value(Row row, Map<String, Integer> indexes, String name, DataFormatter formatter) {
        Integer index = indexes.get(name);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private static boolean isBlank(Row row, DataFormatter formatter) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            if (!formatter.formatCellValue(row.getCell(i)).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
