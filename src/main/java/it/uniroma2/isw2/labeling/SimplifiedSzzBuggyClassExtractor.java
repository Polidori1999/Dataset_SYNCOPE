package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.TicketBuggyClass;
import it.uniroma2.isw2.model.TicketFixCommit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplifiedSzzBuggyClassExtractor {

    private static final Pattern HUNK_PATTERN =
            Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$");

    private SimplifiedSzzBuggyClassExtractor() {
    }

    public static List<TicketBuggyClass> extractBuggyClasses(List<TicketFixCommit> fixCommits,
                                                             String repositoryPath) throws IOException {
        List<TicketBuggyClass> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (TicketFixCommit fixCommit : fixCommits) {
            String parentHash = findParentCommit(fixCommit.getFixCommitHash(), repositoryPath);
            if (parentHash.isBlank()) {
                continue;
            }

            List<String> changedFiles = findChangedFiles(parentHash, fixCommit.getFixCommitHash(), repositoryPath);

            for (String filePath : changedFiles) {
                String normalizedPath = normalizePath(filePath);

                if (!isProductionJavaClass(normalizedPath)) {
                    continue;
                }

                if (isBuggyJavaFileByBlame(
                        parentHash,
                        fixCommit.getFixCommitHash(),
                        normalizedPath,
                        repositoryPath
                )) {
                    String key = fixCommit.getTicketId() + "|" + normalizedPath;

                    if (seen.add(key)) {
                        result.add(new TicketBuggyClass(
                                fixCommit.getTicketId(),
                                fixCommit.getFixCommitHash(),
                                normalizedPath
                        ));
                    }
                }
            }
        }

        return result;
    }

    private static String findParentCommit(String fixCommitHash, String repositoryPath) throws IOException {
        try {
            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "rev-list",
                    "--parents",
                    "-n",
                    "1",
                    fixCommitHash
            );

            if (lines.isEmpty()) {
                return "";
            }

            String[] parts = lines.get(0).trim().split("\\s+");
            if (parts.length < 2) {
                return "";
            }

            return parts[1].trim();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la ricerca del parent commit.", e);
        }
    }

    private static List<String> findChangedFiles(String parentHash,
                                                 String fixCommitHash,
                                                 String repositoryPath) throws IOException {
        try {
            return GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "diff",
                    "--name-only",
                    parentHash,
                    fixCommitHash,
                    "--"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la lettura dei file modificati.", e);
        }
    }

    private static boolean isBuggyJavaFileByBlame(String parentHash,
                                                  String fixCommitHash,
                                                  String filePath,
                                                  String repositoryPath) throws IOException {
        List<int[]> parentRanges = extractParentChangedRanges(parentHash, fixCommitHash, filePath, repositoryPath);

        for (int[] range : parentRanges) {
            int start = range[0];
            int end = range[1];

            if (end < start) {
                continue;
            }

            List<String> blameLines = blameParentRange(parentHash, filePath, start, end, repositoryPath);
            if (!blameLines.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static List<int[]> extractParentChangedRanges(String parentHash,
                                                          String fixCommitHash,
                                                          String filePath,
                                                          String repositoryPath) throws IOException {
        List<int[]> ranges = new ArrayList<>();

        try {
            List<String> diffLines = GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "diff",
                    "-U0",
                    parentHash,
                    fixCommitHash,
                    "--",
                    filePath
            );

            for (String line : diffLines) {
                Matcher matcher = HUNK_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                int oldStart = Integer.parseInt(matcher.group(1));
                int oldCount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));

                /*
                 * Se oldCount = 0, l'hunk contiene solo aggiunte.
                 * In questa versione semplificata di SZZ non possiamo fare blame
                 * su righe precedenti che non esistono.
                 */
                if (oldCount == 0) {
                    continue;
                }

                int start = oldStart;
                int end = oldStart + oldCount - 1;
                ranges.add(new int[]{start, end});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante l'analisi del diff.", e);
        }

        return ranges;
    }

    private static List<String> blameParentRange(String parentHash,
                                                 String filePath,
                                                 int start,
                                                 int end,
                                                 String repositoryPath) throws IOException {
        try {
            return GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "blame",
                    "-w",
                    "-l",
                    "-L",
                    start + "," + end,
                    parentHash,
                    "--",
                    filePath
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante git blame.", e);
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