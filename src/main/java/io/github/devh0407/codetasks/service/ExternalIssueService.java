package io.github.devh0407.codetasks.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.github.devh0407.codetasks.editor.IssueMarkerService;
import io.github.devh0407.codetasks.model.ExternalIssue;
import io.github.devh0407.codetasks.source.IssueSource;
import io.github.devh0407.codetasks.source.IssueSourceFactory;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service(Service.Level.PROJECT)
public final class ExternalIssueService {
    private final Project project;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong reloadGeneration = new AtomicLong();

    private volatile List<ExternalIssue> issues = List.of();
    private volatile String lastError = "";
    private volatile boolean loading;

    public ExternalIssueService(Project project) {
        this.project = project;
        project.getMessageBus().connect().subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER,
                new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> IssueMarkerService.getInstance(project).refreshFile(file)
                        );
                    }
                }
        );
    }

    public static ExternalIssueService getInstance(Project project) {
        return project.getService(ExternalIssueService.class);
    }

    public List<ExternalIssue> getIssues() {
        return issues;
    }

    public String getLastError() {
        return lastError;
    }

    public boolean isLoading() {
        return loading;
    }

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void setSourcePath(String sourcePath) {
        CodeTasksSettings.getInstance(project).setSourcePath(sourcePath);
        reloadAsync();
    }

    public void reloadAsync() {
        long generation = reloadGeneration.incrementAndGet();
        String configuredPath = CodeTasksSettings.getInstance(project).getSourcePath();

        loading = true;
        lastError = "";
        notifyChangedOnEdt();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<ExternalIssue> loaded = List.of();
            String error = "";
            try {
                if (!configuredPath.isBlank()) {
                    Path path = Path.of(configuredPath);
                    if (!Files.isRegularFile(path)) {
                        throw new IllegalArgumentException("Issue source does not exist: " + configuredPath);
                    }
                    IssueSource source = IssueSourceFactory.forPath(path);
                    loaded = source.load(path);
                }
            } catch (Exception ex) {
                error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            }

            List<ExternalIssue> finalLoaded = List.copyOf(loaded);
            String finalError = error;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (reloadGeneration.get() != generation) {
                    return;
                }

                issues = finalLoaded;
                lastError = finalError;
                loading = false;
                IssueMarkerService.getInstance(project).refreshAllOpenEditors();
                notifyChanged();
            });
        });
    }

    private void notifyChangedOnEdt() {
        ApplicationManager.getApplication().invokeLater(this::notifyChanged);
    }

    private void notifyChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
