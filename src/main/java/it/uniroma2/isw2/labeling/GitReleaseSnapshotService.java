package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.utils.DateUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GitReleaseSnapshotService {

    private static final DateTimeFormatter GIT_BEFORE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path repositoryPath;

    public GitReleaseSnapshotService(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getCurrentCommitHash() throws IOException {
        try {
            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath.toString(),
                    "git",
                    "rev-parse",
                    "HEAD"
            );

            if (lines.isEmpty()) {
                return "";
            }

            return lines.get(0).trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la lettura del commit corrente.", e);
        }
    }

    public String findLastCommitOfReleaseDay(String releaseDate,
                                             String referenceCommitHash)
            throws IOException, InterruptedException {
        LocalDateTime endExclusive = DateUtils.parseReleaseDate(releaseDate).plusDays(1);
        String formattedDate = endExclusive.format(GIT_BEFORE_FORMAT);

        List<String> lines = GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "rev-list",
                "-n",
                "1",
                "--before=" + formattedDate,
                "--first-parent",
                referenceCommitHash
        );

        if (lines.isEmpty()) {
            return "";
        }

        return lines.get(0).trim();
    }

    public void checkout(String commitHash) throws IOException, InterruptedException {
        if (commitHash == null || commitHash.isBlank()) {
            return;
        }

        GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "checkout",
                "-f",
                commitHash
        );
    }
}