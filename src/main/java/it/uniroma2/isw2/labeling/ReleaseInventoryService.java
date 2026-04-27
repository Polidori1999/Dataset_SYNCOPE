package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.model.ReleaseSnapshot;
import it.uniroma2.isw2.utils.DateUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Costruisce l'inventario delle classi Java di produzione per le release selezionate.
 *
 * Flusso:
 * 1) per ogni release trova lo snapshot commit
 * 2) legge i file presenti in quello snapshot
 * 3) tiene solo le classi Java di produzione
 */
public class ReleaseInventoryService {

    private static final DateTimeFormatter GIT_BEFORE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ReleaseInventoryService() {
    }

    public static List<ReleaseJavaClass> buildReleaseInventory(String projectName,
                                                               List<Release> selectedReleases,
                                                               String repositoryPath) throws IOException {
        List<ReleaseJavaClass> result = new ArrayList<>();

        for (Release release : selectedReleases) {
            String snapshotCommitHash = findLastCommitOfReleaseDay(
                    release.getDate(),
                    repositoryPath
            );

            if (snapshotCommitHash.isBlank()) {
                continue;
            }

            ReleaseSnapshot snapshot = new ReleaseSnapshot(
                    release.getVersionId(),
                    release.getVersionName(),
                    release.getIndex(),
                    release.getDate(),
                    snapshotCommitHash
            );

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

    private static String findLastCommitOfReleaseDay(String releaseDate,
                                                     String repositoryPath) throws IOException {
        try {
            /*
             * Le release nel CSV hanno orario 00:00 perchÃ© derivano da LocalDate.
             * Per includere tutti i commit della giornata di release,
             * usiamo come limite esclusivo il giorno successivo alle 00:00.
             */
            LocalDateTime endExclusive = DateUtils.parseReleaseDate(releaseDate).plusDays(1);
            String formattedDate = endExclusive.format(GIT_BEFORE_FORMAT);

            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "rev-list",
                    "-n",
                    "1",
                    "--before=" + formattedDate,
                    "--first-parent",
                    "HEAD"
            );

            if (lines.isEmpty()) {
                return "";
            }

            return lines.get(0).trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la ricerca dello snapshot commit.", e);
        }
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
        String normalizedPath = normalizePath(path);

        return normalizedPath.endsWith(".java")
                && normalizedPath.contains("/src/main/java/")
                && !normalizedPath.contains("/src/test/java/")
                && !isExcludedModulePath(normalizedPath);
    }

    private static boolean isExcludedModulePath(String path) {
        return path.startsWith("fit/")
                || path.contains("/fit/")
                || path.startsWith("syncope620/")
                || path.contains("/syncope620/");
    }
    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}