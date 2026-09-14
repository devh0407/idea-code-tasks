# Code Tasks

An IntelliJ IDEA plugin that imports external TODO / PROBLEM records from CSV or Excel files, highlights the referenced source lines, and exposes them in a searchable **Code Tasks** tool window.

## Features

- Import `.csv`, `.xls`, and `.xlsx` files.
- Required columns: `file`, `line`, `type`, `message`.
- Optional columns: `id`, `source`.
- Supported issue types: `TODO`, `PROBLEM`.
- Project-relative or absolute source file paths.
- Gutter icons and line highlighting without modifying source code.
- Dedicated Code Tasks tool window.
- Search by id, file, message, or source.
- Filter by TODO / PROBLEM.
- Double-click a row to navigate to the source line.
- Reload the source file without restarting IDEA.

## Spreadsheet format

| id | file | line | type | message | source |
| --- | --- | ---: | --- | --- | --- |
| TASK-001 | src/main/java/com/example/OrderService.java | 123 | TODO | Add parameter validation | review |
| TASK-002 | src/main/java/com/example/PayService.java | 57 | PROBLEM | Check possible null value | review |

`line` is **1-based** in the external file. Internally the plugin converts it to IntelliJ's 0-based editor line index.

## Run locally

Requirements:

- JDK 21
- Gradle 9+

Run a sandbox IDE:

```bash
gradle runIde
```

Build the plugin:

```bash
gradle buildPlugin
```

The project targets IntelliJ IDEA 2024.3+.

## Usage

1. Start IDEA with the plugin installed.
2. Open **Code Tasks** from the bottom tool window bar.
3. Enter a CSV/XLS/XLSX path or click **Choose File**.
4. Click **Reload**.
5. Search or filter imported issues.
6. Double-click an issue to jump to the referenced source line.

When a referenced file is opened, the plugin creates an IntelliJ `RangeHighlighter` with a gutter icon. It does **not** insert comments into the source file, so importing tasks does not create Git diffs.

## Architecture

```text
CSV / XLSX
    |
    v
IssueSource
    |
    v
ExternalIssueService
    |----------------------|
    v                      v
IssueMarkerService     Code Tasks Tool Window
    |                      |
    v                      v
Editor gutter/highlight  Search/filter/navigation
```

The spreadsheet is intentionally treated as an input adapter rather than the plugin core. Additional sources such as HTTP APIs, Jira, Sonar, internal scanners, or AI review results can be implemented later by adding another `IssueSource` adapter.

## Current limitations

- The first version uses `file + line` as the imported anchor.
- Range highlighters follow edits during the current IDE session, but reloading the spreadsheet still starts from the external line number.
- Ambiguous same-name files use the best project-path suffix match.
- Git-based line remapping, PSI symbol anchors, code fingerprints, resolve/ignore state, and native IntelliJ Inspection integration are not implemented yet.

## Roadmap

- Show explicit `Resolved / Missing / Ambiguous / Invalid line` states.
- Group the tool window by file or issue type.
- Persist resolved / ignored issue state.
- Add code fingerprints and PSI symbol anchors to survive line drift.
- Optional Git commit + diff line remapping.
- HTTP / internal platform issue source adapters.
- Optional `ExternalAnnotator` / Inspection integration for richer PROBLEM diagnostics.
