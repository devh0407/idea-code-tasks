package io.github.devh0407.codetasks.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service(Service.Level.PROJECT)
@State(name = "CodeTasksSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class CodeTasksSettings implements PersistentStateComponent<CodeTasksSettings.StateBean> {

    public static final class StateBean {
        public String sourcePath = "";
    }

    private StateBean state = new StateBean();

    public static CodeTasksSettings getInstance(Project project) {
        return project.getService(CodeTasksSettings.class);
    }

    @Override
    public @Nullable StateBean getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull StateBean state) {
        this.state = state;
    }

    public String getSourcePath() {
        return state.sourcePath == null ? "" : state.sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        state.sourcePath = sourcePath == null ? "" : sourcePath;
    }
}
