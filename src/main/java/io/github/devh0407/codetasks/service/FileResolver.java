package io.github.devh0407.codetasks.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public final class FileResolver {
    private final Project project;

    public FileResolver(Project project) {
        this.project = project;
    }

    public Optional<VirtualFile> resolve(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return Optional.empty();
        }

        String normalized = FileUtil.toSystemIndependentName(rawPath.trim());
        LocalFileSystem fileSystem = LocalFileSystem.getInstance();

        VirtualFile absolute = fileSystem.findFileByPath(normalized);
        if (absolute != null && !absolute.isDirectory()) {
            return Optional.of(absolute);
        }

        String basePath = project.getBasePath();
        if (basePath != null) {
            Path candidate = Path.of(basePath).resolve(rawPath).normalize();
            VirtualFile relative = fileSystem.findFileByPath(FileUtil.toSystemIndependentName(candidate.toString()));
            if (relative != null && !relative.isDirectory()) {
                return Optional.of(relative);
            }
        }

        String fileName = Path.of(rawPath).getFileName().toString();
        Collection<VirtualFile> matches = FilenameIndex.getVirtualFilesByName(
                fileName,
                GlobalSearchScope.projectScope(project)
        );

        if (matches.size() == 1) {
            return matches.stream().findFirst();
        }

        String suffix = "/" + normalized;
        return matches.stream()
                .filter(file -> file.getPath().endsWith(suffix) || file.getPath().endsWith(normalized))
                .findFirst();
    }
}
