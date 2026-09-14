package io.github.devh0407.codetasks.source;

import io.github.devh0407.codetasks.model.ExternalIssue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IssueSource {
    List<ExternalIssue> load(Path path) throws IOException;
}
