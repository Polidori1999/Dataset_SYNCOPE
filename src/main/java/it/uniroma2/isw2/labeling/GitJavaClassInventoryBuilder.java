package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.model.ReleaseSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitJavaClassInventoryBuilder {

    private GitJavaClassInventoryBuilder() {
    }

    public static List<ReleaseJavaClass> buildInventory(String projectName,
                                                        List<ReleaseSnapshot> releaseSnapshots,
                                                        String repositoryPath) throws IOException {
        List<ReleaseJavaClass> result = new ArrayList<>();

        for (ReleaseSnapshot snapshot : releaseSnapshots) {
            List<String> trackedFiles = listTrackedFilesAtCommit(
                    snapshot.getSnapshotCommitHash(),
                    repositoryPath
            );

            for (String filePath : trackedFiles) {
                String normalizedPath = normalizePath(filePath);

                if (!isProductionJavaClass(normalizedPath)) {
                    continue;
                }

                result.add(new ReleaseJavaClass(
                        projectName,
                        normalizedPath,
                        snapshot.getReleaseId(),
                        snapshot.getReleaseName(),
                        snapshot.getReleaseIndex()
                ));
            }
        }

        return result;
    }

    private static List<String> listTrackedFilesAtCommit(String commitHash,
                                                         String repositoryPath) throws IOException {
        try {
            return GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "ls-tree",
                    "-r",
                    "--name-only",
                    commitHash
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la lettura dei file della release.", e);
        }
    }

    private static boolean isProductionJavaClass(String path) {
        return path.endsWith(".java")
                && path.contains("/src/main/java/")
                && !path.contains("/src/test/java/");
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}