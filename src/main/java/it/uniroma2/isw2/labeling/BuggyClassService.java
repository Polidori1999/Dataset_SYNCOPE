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
                processGitLogLine(line, validTicketIds, unique);
            }

            result.addAll(unique.values());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la ricerca dei fix commit.", e);
        }
    }
    private static void processGitLogLine(String line,
                                          Set<String> validTicketIds,
                                          Map<String, TicketFixCommit> unique) {
        String[] parts = line.split("\t", 3);

        if (isValidGitLogLine(parts)) {
            String commitHash = parts[0].trim();
            long epochSeconds = parseEpochSeconds(parts[1].trim());
            String subject = parts[2];

            if (epochSeconds > 0) {
                addFixCommitsFromSubject(subject, commitHash, epochSeconds, validTicketIds, unique);
            }
        }
    }

    private static boolean isValidGitLogLine(String[] parts) {
        return parts.length >= 3
                && !parts[0].trim().isBlank()
                && !parts[1].trim().isBlank();
    }

    private static long parseEpochSeconds(String epochString) {
        try {
            return Long.parseLong(epochString);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static void addFixCommitsFromSubject(String subject,
                                                 String commitHash,
                                                 long epochSeconds,
                                                 Set<String> validTicketIds,
                                                 Map<String, TicketFixCommit> unique) {
        Matcher matcher = TICKET_PATTERN.matcher(subject);

        while (matcher.find()) {
            String ticketId = matcher.group().toUpperCase(Locale.ROOT);

            if (validTicketIds.contains(ticketId)) {
                String key = ticketId + "|" + commitHash;
                unique.putIfAbsent(
                        key,
                        new TicketFixCommit(ticketId, commitHash, epochSeconds)
                );
            }
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
            addBuggyClassesForFixCommit(fixCommit, repositoryPath, result, seen);
        }

        return result;
    }
    private static void addBuggyClassesForFixCommit(TicketFixCommit fixCommit,
                                                    String repositoryPath,
                                                    List<TicketBuggyClass> result,
                                                    Set<String> seen) throws IOException {
        String parentHash = findParentCommit(fixCommit.getFixCommitHash(), repositoryPath);

        if (!parentHash.isBlank()) {
            List<String> changedFiles = findChangedFiles(
                    parentHash,
                    fixCommit.getFixCommitHash(),
                    repositoryPath
            );

            addBuggyClassesFromChangedFiles(
                    fixCommit,
                    parentHash,
                    changedFiles,
                    repositoryPath,
                    result,
                    seen
            );
        }
    }

    private static void addBuggyClassesFromChangedFiles(TicketFixCommit fixCommit,
                                                        String parentHash,
                                                        List<String> changedFiles,
                                                        String repositoryPath,
                                                        List<TicketBuggyClass> result,
                                                        Set<String> seen) throws IOException {
        for (String filePath : changedFiles) {
            String normalizedPath = normalizePath(filePath);

            if (isProductionJavaClass(normalizedPath)
                    && isBuggyJavaFileByBlame(
                    parentHash,
                    fixCommit.getFixCommitHash(),
                    normalizedPath,
                    repositoryPath
            )) {
                addTicketBuggyClass(fixCommit, normalizedPath, result, seen);
            }
        }
    }

    private static void addTicketBuggyClass(TicketFixCommit fixCommit,
                                            String normalizedPath,
                                            List<TicketBuggyClass> result,
                                            Set<String> seen) {
        String key = fixCommit.getTicketId() + "|" + normalizedPath;

        if (seen.add(key)) {
            result.add(new TicketBuggyClass(
                    fixCommit.getTicketId(),
                    fixCommit.getFixCommitHash(),
                    normalizedPath
            ));
        }
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

                if (matcher.matches()) {
                    addParentChangedRange(matcher, ranges);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante l'analisi del diff.", e);
        }

        return ranges;
    }

    private static void addParentChangedRange(Matcher matcher, List<int[]> ranges) {
        int oldStart = Integer.parseInt(matcher.group(1));
        int oldCount = matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2));

        /*
         * Se oldCount = 0, l'hunk contiene solo aggiunte.
         * In questa versione semplificata di SZZ non possiamo fare blame
         * su righe precedenti che non esistono.
         */
        if (oldCount > 0) {
            int start = oldStart;
            int end = oldStart + oldCount - 1;
            ranges.add(new int[]{start, end});
        }
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