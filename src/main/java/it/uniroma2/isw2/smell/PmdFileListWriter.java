package it.uniroma2.isw2.smell;

import it.uniroma2.isw2.model.ReleaseJavaClass;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Crea la file-list usata da PMD per analizzare i file Java
 * presenti nello snapshot di una specifica release.
 */
public class PmdFileListWriter {

    private final Path fileListsDir;

    public PmdFileListWriter(Path fileListsDir) {
        this.fileListsDir = fileListsDir;
    }

    public Path writeFileList(String releaseId,
                              Path repositoryPath,
                              List<ReleaseJavaClass> releaseJavaClasses) throws IOException {
        Files.createDirectories(fileListsDir);

        String safeReleaseId = sanitizeFileName(releaseId);
        Path fileListPath = fileListsDir.resolve("release-" + safeReleaseId + "-files.txt");

        try (BufferedWriter writer = Files.newBufferedWriter(fileListPath)) {
            for (ReleaseJavaClass javaClass : releaseJavaClasses) {
                if (!releaseId.equals(javaClass.getReleaseId())) {
                    continue;
                }

                Path absoluteClassPath = repositoryPath
                        .resolve(javaClass.getClassPath())
                        .toAbsolutePath()
                        .normalize();

                if (!Files.exists(absoluteClassPath)) {
                    throw new IOException("File Java non trovato nello snapshot corrente: "
                            + absoluteClassPath);
                }

                writer.write(absoluteClassPath.toString());
                writer.newLine();
            }
        }

        return fileListPath;
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}