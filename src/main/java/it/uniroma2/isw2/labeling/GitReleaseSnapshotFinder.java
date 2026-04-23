package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseSnapshot;
import it.uniroma2.isw2.proportion.DateUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GitReleaseSnapshotFinder {

    private static final DateTimeFormatter GIT_BEFORE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private GitReleaseSnapshotFinder() {
    }

    public static List<ReleaseSnapshot> findSnapshots(List<Release> selectedReleases,
                                                      String repositoryPath) throws IOException {
        List<ReleaseSnapshot> snapshots = new ArrayList<>();

        for (Release release : selectedReleases) {
            String snapshotCommitHash =
                    findLastCommitOfReleaseDay(release.getDate(), repositoryPath);

            if (snapshotCommitHash.isBlank()) {
                continue;
            }

            snapshots.add(new ReleaseSnapshot(
                    release.getVersionId(),
                    release.getVersionName(),
                    release.getIndex(),
                    release.getDate(),
                    snapshotCommitHash
            ));
        }

        return snapshots;
    }

    private static String findLastCommitOfReleaseDay(String releaseDate,
                                                     String repositoryPath) throws IOException {
        try {
            /*
             * Le release nel CSV hanno orario 00:00 perché derivano da LocalDate.
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
}