package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.TicketBuggyClass;
import it.uniroma2.isw2.model.TicketFixCommit;
import it.uniroma2.isw2.proportion.EnhancedTicket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Servizio unico per:
 * - individuare i fix commit associati ai ticket
 * - estrarre le buggy classes con una versione semplificata di SZZ
 */
public class BuggyClassService {

    private static final Pattern TICKET_PATTERN =
            Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern HUNK_PATTERN =
            Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$");

    private BuggyClassService() {
    }

    /**
     * Cerca i fix commit associati ai ticket del dataset.
     * Fa un solo passaggio sull'intero log Git.
     */
    public static List<TicketFixCommit> findFixCommits(List<EnhancedTicket> tickets,
                                                       String repositoryPath) throws IOException {
        List<TicketFixCommit> result = new ArrayList<>();

        if (tickets == null || tickets.isEmpty()) {
            return result;
        }

        Set<String> validTicketIds = extractValidTicketIds(tickets);
        Map<String, TicketFixCommit> unique = new LinkedHashMap<>();

        try {
            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "log",
                    "--all",
                    "--regexp-ignore-case",
                    "--format=%H\t%ct\t%s"
            );

            for (String line : lines) {
                String[] parts = line.split("\t", 3);
                if (parts.length < 3) {
                    continue;
                }

                String commitHash = parts[0].trim();
                String epochString = parts[1].trim();
                String subject = parts[2];

                if (commitHash.isBlank() || epochString.isBlank()) {
                    continue;
                }

                long epochSeconds;
                try {
                    epochSeconds = Long.parseLong(epochString);
                } catch (NumberFormatException e) {
                    continue;
                }

                Matcher matcher = TICKET_PATTERN.matcher(subject);
                while (matcher.find()) {
                    String ticketId = matcher.group().toUpperCase(Locale.ROOT);

                    if (!validTicketIds.contains(ticketId)) {
                        continue;
                    }

                    String key = ticketId + "|" + commitHash;
                    unique.putIfAbsent(
                            key,
                            new TicketFixCommit(ticketId, commitHash, epochSeconds)
                    );
                }
            }

            result.addAll(unique.values());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la ricerca dei fix commit.", e);
        }
    }

    /**
     * Estrae le buggy classes a partire dai fix commit usando SZZ semplificato.
     */
    public static List<TicketBuggyClass> extractBuggyClasses(List<TicketFixCommit> fixCommits,
                                                             String repositoryPath) throws IOException {
        List<TicketBuggyClass> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (fixCommits == null || fixCommits.isEmpty()) {
            return result;
        }

        for (TicketFixCommit fixCommit : fixCommits) {
            String parentHash = findParentCommit(fixCommit.getFixCommitHash(), repositoryPath);
            if (parentHash.isBlank()) {
                continue;
            }

            List<String> changedFiles = findChangedFiles(
                    parentHash,
                    fixCommit.getFixCommitHash(),
                    repositoryPath
            );

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

    private static Set<String> extractValidTicketIds(List<EnhancedTicket> tickets) {
        Set<String> validTicketIds = new HashSet<>();

        for (EnhancedTicket ticket : tickets) {
            if (ticket.getTicketId() != null && !ticket.getTicketId().isBlank()) {
                validTicketIds.add(ticket.getTicketId().trim().toUpperCase(Locale.ROOT));
            }
        }

        return validTicketIds;
    }

    private static String findParentCommit(String fixCommitHash,
                                           String repositoryPath) throws IOException {
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
        List<int[]> parentRanges = extractParentChangedRanges(
                parentHash,
                fixCommitHash,
                filePath,
                repositoryPath
        );

        for (int[] range : parentRanges) {
            int start = range[0];
            int end = range[1];

            if (end < start) {
                continue;
            }

            List<String> blameLines = blameParentRange(
                    parentHash,
                    filePath,
                    start,
                    end,
                    repositoryPath
            );

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