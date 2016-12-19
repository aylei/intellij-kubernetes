package com.github.tinselspoon.intellij.kubernetes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;

/**
 * File type descriptor representing a Kubernetes YAML file.
 */
public class KubernetesYamlFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {

    /** Number of bytes to read when guessing for the file type based on content. */
    private static final int BYTES_TO_READ = 4096;

    /** Singleton instance. */
    public static KubernetesYamlFileType INSTANCE = new KubernetesYamlFileType();

    /** Identifier to use for the recursion guard. */
    private static final String GUARD_ID = "KubernetesYamlFileType";

    /** Recursion guard for preventing cycles. */
    private final RecursionGuard recursionGuard = RecursionManager.createGuard(GUARD_ID);

    /** Singleton default constructor. */
    private KubernetesYamlFileType() {
        super(YAMLLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "yaml";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Kubernetes Resource Definition YAML";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return SimpleIcons.FILE;
    }

    @NotNull
    @Override
    public String getName() {
        return "Kubernetes YAML";
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean isMyFileType(@NotNull final VirtualFile file) {
        final String extension = file.getExtension();
        if ("yml".equalsIgnoreCase(extension) || "yaml".equalsIgnoreCase(extension)) {
            return recursionGuard.doPreventingRecursion(GUARD_ID, false, () -> {
                if (file instanceof StubVirtualFile) {
                    return true; // Helps New -> File get correct file type
                }

                try (InputStream inputStream = file.getInputStream()) {
                    final byte[] bytes = new byte[BYTES_TO_READ];
                    final int n = inputStream.read(bytes, 0, BYTES_TO_READ);
                    return n > 0 && isKubernetesYaml(bytes);
                } catch (final IOException e) {
                    return false; // todo log
                }
            });
        }
        return false;
    }

    /**
     * Guess whether the file is a Kubernetes YAML file from a subset of the file content.
     *
     * @param bytes the bytes to check.
     * @return true if the file is a Kubernetes YAML file, otherwise, false.
     */
    private boolean isKubernetesYaml(final byte[] bytes) {
        try (Scanner scanner = new Scanner(new String(bytes, StandardCharsets.UTF_8))) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();
                if (line.startsWith("kind: ") || line.startsWith("apiVersion: ")) {
                    return true;
                }
            }
        }
        return false;
    }
}
