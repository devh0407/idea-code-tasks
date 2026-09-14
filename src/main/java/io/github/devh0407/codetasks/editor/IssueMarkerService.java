package io.github.devh0407.codetasks.editor;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.model.IssueType;
import io.github.devh0407.codetasks.service.ExternalIssueService;
import io.github.devh0407.codetasks.service.FileResolver;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Service(Service.Level.PROJECT)
public final class IssueMarkerService {
    private final Project project;
    private final FileResolver fileResolver;
    private final Map<Editor, List<RangeHighlighter>> editorMarkers = new IdentityHashMap<>();

    public IssueMarkerService(Project project) {
        this.project = project;
        this.fileResolver = new FileResolver(project);
    }

    public static IssueMarkerService getInstance(Project project) {
        return project.getService(IssueMarkerService.class);
    }

    public void refreshAllOpenEditors() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            if (project.equals(editor.getProject())) {
                refreshEditor(editor);
            }
        }
    }

    public void refreshFile(VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) {
            return;
        }
        for (Editor editor : EditorFactory.getInstance().getEditors(document, project)) {
            refreshEditor(editor);
        }
    }

    private void refreshEditor(Editor editor) {
        clearMarkers(editor);

        Document document = editor.getDocument();
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
        if (currentFile == null) {
            return;
        }

        List<RangeHighlighter> created = new ArrayList<>();
        for (ExternalIssue issue : ExternalIssueService.getInstance(project).getIssues()) {
            VirtualFile resolved = ReadAction.compute(() -> fileResolver.resolve(issue.filePath()).orElse(null));
            if (resolved == null || !resolved.equals(currentFile)) {
                continue;
            }

            int zeroBasedLine = issue.line() - 1;
            if (zeroBasedLine < 0 || zeroBasedLine >= document.getLineCount()) {
                continue;
            }

            TextAttributes attributes = EditorColorsManager.getInstance()
                    .getGlobalScheme()
                    .getAttributes(issue.type() == IssueType.PROBLEM
                            ? CodeInsightColors.WARNINGS_ATTRIBUTES
                            : CodeInsightColors.WEAK_WARNING_ATTRIBUTES);

            RangeHighlighter highlighter = editor.getMarkupModel().addLineHighlighter(
                    zeroBasedLine,
                    HighlighterLayer.WARNING,
                    attributes
            );
            highlighter.setGutterIconRenderer(new IssueGutterRenderer(issue));
            created.add(highlighter);
        }

        if (!created.isEmpty()) {
            editorMarkers.put(editor, created);
        }
    }

    private void clearMarkers(Editor editor) {
        List<RangeHighlighter> markers = editorMarkers.remove(editor);
        if (markers == null) {
            return;
        }
        for (RangeHighlighter marker : markers) {
            marker.dispose();
        }
    }
}
