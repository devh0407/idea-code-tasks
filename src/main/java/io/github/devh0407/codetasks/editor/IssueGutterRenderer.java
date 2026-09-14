package io.github.devh0407.codetasks.editor;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.model.IssueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;

public final class IssueGutterRenderer extends GutterIconRenderer {
    private static final Icon TODO_ICON = IconLoader.getIcon("/icons/todo.svg", IssueGutterRenderer.class);
    private static final Icon PROBLEM_ICON = IconLoader.getIcon("/icons/problem.svg", IssueGutterRenderer.class);

    private final ExternalIssue issue;

    public IssueGutterRenderer(ExternalIssue issue) {
        this.issue = issue;
    }

    @Override
    public @NotNull Icon getIcon() {
        return issue.type() == IssueType.PROBLEM ? PROBLEM_ICON : TODO_ICON;
    }

    @Override
    public @Nullable String getTooltipText() {
        String prefix = issue.id() + " · " + issue.type();
        return issue.message().isBlank() ? prefix : prefix + " · " + issue.message();
    }

    @Override
    public @NotNull Alignment getAlignment() {
        return Alignment.LEFT;
    }

    @Override
    public @Nullable AnAction getClickAction() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IssueGutterRenderer other)) {
            return false;
        }
        return issue.equals(other.issue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issue);
    }
}
