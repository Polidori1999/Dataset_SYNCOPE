package it.uniroma2.isw2.metric;

import it.uniroma2.isw2.labeling.GitCommandRunner;
import it.uniroma2.isw2.labeling.GitReleaseSnapshotService;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.utils.DateUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calcola le metriche classe-release del dataset di defect prediction.
 *
 * Metriche strutturali calcolate sullo snapshot della release Ri:
 * - SIZE_LOC
 * - NOM
 * - AVG_METHOD_SIZE
 * - CYCLO_COMPLEXITY
 * - FAN_OUT
 *
 * Metriche storiche calcolate da F0 a Ri:
 * - NR
 * - NFIX
 * - NAUTH
 * - LOC_ADDED
 * - MAX_LOC_ADDED
 * - CHURN
 * - MAX_CHURN
 * - MAX_CHANGE_SET_SIZE
 * - AVG_MODIFIED_DIRS
 * - AGE_SINCE_LAST_CHANGE
 * - OWNERSHIP_RATIO
 * - SAME_DIRECTORY_CHANGE_RATIO
 * - AGE
 *
 * Metriche da smell calcolate sullo snapshot della release Ri:
 * - NSMELLS
 * - SMELL_DENSITY
 */
public class MetricService {
    private static final Logger LOGGER =
            Logger.getLogger(MetricService.class.getName());

    private static final DateTimeFormatter GIT_BEFORE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String projectName;
    private final Path repositoryPath;
    private final GitReleaseSnapshotService snapshotService;

    private final Set<String> fixCommitHashes;
    private final Map<String, List<String>> changedFilesByCommit = new HashMap<>();


    public MetricService(String projectName,
                         Path repositoryPath,
                         Set<String> fixCommitHashes) {
        this.projectName = projectName;
        this.repositoryPath = repositoryPath;
        this.snapshotService = new GitReleaseSnapshotService(repositoryPath);
        this.fixCommitHashes = normalizeCommitHashes(fixCommitHashes);
    }
    public List<ClassReleaseMetric> computeMetrics(List<Release> selectedReleases,
                                                   List<ReleaseJavaClass> releaseJavaClasses)
            throws IOException {
        return computeMetrics(
                selectedReleases,
                releaseJavaClasses,
                Collections.emptyMap()
        );
    }

    /**
     * Calcola le metriche usando anche una mappa opzionale degli smell.
     *
     * La chiave della mappa deve essere:
     * releaseId + "||" + classPath normalizzato
     *
     * Esempio:
     * MetricService.buildMetricKey("1", "src/main/java/org/example/A.java")
     */
    public List<ClassReleaseMetric> computeMetrics(List<Release> selectedReleases,
                                                   List<ReleaseJavaClass> releaseJavaClasses,
                                                   Map<String, Integer> smellsByClassRelease)
            throws IOException {
        List<ClassReleaseMetric> result = new ArrayList<>();

        String originalCommitHash = snapshotService.getCurrentCommitHash();

        try {
            for (Release release : selectedReleases) {
                String releaseId = release.getVersionId();

                String snapshotCommitHash = snapshotService.findLastCommitOfReleaseDay(
                        release.getDate(),
                        originalCommitHash
                );

                if (snapshotCommitHash.isBlank()) {
                    continue;
                }

                snapshotService.checkout(snapshotCommitHash);

                List<ReleaseJavaClass> classesOfRelease =
                        filterClassesByRelease(releaseJavaClasses, releaseId);

                Set<String> internalClassNames =
                        buildInternalClassNames(classesOfRelease);

                for (ReleaseJavaClass javaClass : classesOfRelease) {
                    String normalizedClassPath = normalizePath(javaClass.getClassPath());

                    Path absoluteClassPath = repositoryPath
                            .resolve(normalizedClassPath)
                            .toAbsolutePath()
                            .normalize();

                    SourceMetrics sourceMetrics = computeSourceMetrics(
                            absoluteClassPath,
                            internalClassNames
                    );

                    HistoricalMetrics historicalMetrics = computeHistoricalMetrics(
                            normalizedClassPath,
                            release.getDate()
                    );

                    int nSmells = smellsByClassRelease.getOrDefault(
                            buildMetricKey(releaseId, normalizedClassPath),
                            0
                    );

                    double smellDensity = computeRatio(
                            nSmells,
                            Math.max(1, sourceMetrics.sizeLoc)
                    );

                    result.add(ClassReleaseMetric.builder()
                            .identity(projectName, releaseId, normalizedClassPath)
                            .sourceMetrics(
                                    sourceMetrics.sizeLoc,
                                    sourceMetrics.nom,
                                    sourceMetrics.avgMethodSize,
                                    sourceMetrics.cycloComplexity,
                                    sourceMetrics.fanOut
                            )
                            .historicalCounts(
                                    historicalMetrics.nr,
                                    historicalMetrics.nFix,
                                    historicalMetrics.nAuth
                            )
                            .historicalChanges(
                                    historicalMetrics.locAdded,
                                    historicalMetrics.maxLocAdded,
                                    historicalMetrics.churn,
                                    historicalMetrics.maxChurn,
                                    historicalMetrics.maxChangeSetSize,
                                    historicalMetrics.avgModifiedDirs
                            )
                            .historicalRatios(
                                    historicalMetrics.ageSinceLastChange,
                                    historicalMetrics.ownershipRatio,
                                    historicalMetrics.sameDirectoryChangeRatio,
                                    historicalMetrics.age
                            )
                            .smellMetrics(nSmells, smellDensity)
                            .build());
                }

                LOGGER.log(Level.INFO,
                        "Metriche calcolate per release {0} ({1}).",
                        new Object[]{release.getVersionName(), releaseId});
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante il calcolo delle metriche.", e);
        } finally {
            try {
                checkout(originalCommitHash);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.SEVERE,
                        "Attenzione: impossibile ripristinare il commit originale della repository.",
                        e);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE,
                        "Attenzione: impossibile ripristinare il commit originale della repository.",
                        e);
            }
        }

        return result;
    }

    public static String buildMetricKey(String releaseId, String classPath) {
        return releaseId + "||" + normalizeStaticPath(classPath);
    }

    private static Set<String> normalizeCommitHashes(Set<String> hashes) {
        Set<String> result = new HashSet<>();

        if (hashes == null) {
            return result;
        }

        for (String hash : hashes) {
            if (hash != null && !hash.isBlank()) {
                result.add(hash.trim());
            }
        }

        return result;
    }

    private List<ReleaseJavaClass> filterClassesByRelease(List<ReleaseJavaClass> releaseJavaClasses,
                                                          String releaseId) {
        List<ReleaseJavaClass> result = new ArrayList<>();

        for (ReleaseJavaClass javaClass : releaseJavaClasses) {
            if (releaseId.equals(javaClass.getReleaseId())) {
                result.add(javaClass);
            }
        }

        return result;
    }

    private Set<String> buildInternalClassNames(List<ReleaseJavaClass> classesOfRelease) {
        Set<String> internalClassNames = new HashSet<>();

        for (ReleaseJavaClass javaClass : classesOfRelease) {
            String fullyQualifiedName = toFullyQualifiedName(javaClass.getClassPath());

            if (!fullyQualifiedName.isBlank()) {
                internalClassNames.add(fullyQualifiedName);
            }
        }

        return internalClassNames;
    }

    private SourceMetrics computeSourceMetrics(Path absoluteClassPath,
                                               Set<String> internalClassNames)
            throws IOException {
        if (!Files.exists(absoluteClassPath)) {
            return new SourceMetrics();
        }

        List<String> lines = Files.readAllLines(absoluteClassPath);

        int sizeLoc = lines.size();
        int nom = countMethods(lines);
        double avgMethodSize = computeRatio(sizeLoc, Math.max(1, nom));
        int cycloComplexity = countCyclomaticComplexity(lines);
        int fanOut = countDependencies(absoluteClassPath, internalClassNames);

        return new SourceMetrics(
                sizeLoc,
                nom,
                avgMethodSize,
                cycloComplexity,
                fanOut
        );
    }

    private int countMethods(List<String> lines) {
        int count = 0;
        StringBuilder signatureBuilder = new StringBuilder();

        for (String line : lines) {
            String normalizedLine = removeInlineComment(line).trim();

            if (!normalizedLine.isBlank()) {
                signatureBuilder.append(' ').append(normalizedLine);

                if (isSignatureComplete(normalizedLine)) {
                    String candidate = signatureBuilder.toString().trim();

                    if (looksLikeMethodDeclaration(candidate)) {
                        count++;
                    }

                    signatureBuilder.setLength(0);
                }
            }
        }

        return count;
    }
    private boolean isSignatureComplete(String normalizedLine) {
        return normalizedLine.contains("{")
                || normalizedLine.endsWith(";")
                || normalizedLine.endsWith("}");
    }

    private boolean looksLikeMethodDeclaration(String candidate) {
        String normalized = candidate.replaceAll("\\s+", " ");

        if (!normalized.contains("(")
                || !normalized.contains(")")
                || !normalized.contains("{")) {
            return false;
        }

        if (normalized.contains(" class ")
                || normalized.contains(" interface ")
                || normalized.contains(" enum ")
                || normalized.contains(" record ")) {
            return false;
        }

        if (normalized.contains("->")) {
            return false;
        }

        int openParenIndex = normalized.indexOf('(');
        String beforeParen = normalized.substring(0, openParenIndex).trim();

        if (beforeParen.isBlank()) {
            return false;
        }

        String lastTokenBeforeParen = beforeParen.substring(
                beforeParen.lastIndexOf(' ') + 1
        );

        Set<String> controlKeywords = Set.of(
                "if",
                "for",
                "while",
                "switch",
                "catch",
                "return",
                "new",
                "throw",
                "synchronized"
        );

        return !controlKeywords.contains(lastTokenBeforeParen);
    }

    private int countCyclomaticComplexity(List<String> lines) {
        if (lines.isEmpty()) {
            return 0;
        }

        int complexity = 1;

        for (String line : lines) {
            String normalizedLine = removeInlineComment(line);

            complexity += countKeyword(normalizedLine, "if");
            complexity += countKeyword(normalizedLine, "for");
            complexity += countKeyword(normalizedLine, "while");
            complexity += countKeyword(normalizedLine, "case");
            complexity += countKeyword(normalizedLine, "catch");
            complexity += countKeyword(normalizedLine, "default");

            complexity += countOccurrences(normalizedLine, "&&");
            complexity += countOccurrences(normalizedLine, "||");
            complexity += countOccurrences(normalizedLine, "?");
        }

        return complexity;
    }

    private int countKeyword(String line, String keyword) {
        String[] tokens = line.split("\\W+");

        int count = 0;

        for (String token : tokens) {
            if (keyword.equals(token)) {
                count++;
            }
        }

        return count;
    }

    private int countOccurrences(String line, String token) {
        int count = 0;
        int index = 0;

        while ((index = line.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }

        return count;
    }

    private String removeInlineComment(String line) {
        int commentIndex = line.indexOf("//");

        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }

        return line;
    }

    private int countDependencies(Path absoluteClassPath,
                                  Set<String> internalClassNames) throws IOException {
        if (!Files.exists(absoluteClassPath)) {
            return 0;
        }

        Set<String> dependencies = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(absoluteClassPath)) {
            String line;

            while ((line = reader.readLine()) != null) {
                String normalizedLine = line.trim();

                if (!normalizedLine.startsWith("import ")) {
                    continue;
                }

                String importedName = normalizedLine
                        .replace("import static ", "")
                        .replace("import ", "")
                        .replace(";", "")
                        .trim();

                if (importedName.endsWith(".*")) {
                    String packageName = importedName.substring(0, importedName.length() - 2);
                    addWildcardDependencies(packageName, internalClassNames, dependencies);
                } else {
                    String matchedClass = findMatchingInternalClass(importedName, internalClassNames);

                    if (!matchedClass.isBlank()) {
                        dependencies.add(matchedClass);
                    }
                }
            }
        }

        return dependencies.size();
    }

    private void addWildcardDependencies(String packageName,
                                         Set<String> internalClassNames,
                                         Set<String> dependencies) {
        String packagePrefix = packageName + ".";

        for (String internalClassName : internalClassNames) {
            if (internalClassName.startsWith(packagePrefix)) {
                dependencies.add(internalClassName);
            }
        }
    }

    private String findMatchingInternalClass(String importedName,
                                             Set<String> internalClassNames) {
        if (internalClassNames.contains(importedName)) {
            return importedName;
        }

        /*
         * Gestione conservativa di import verso inner class o membri statici.
         * Esempio:
         * org.example.Outer.Inner -> org.example.Outer
         */
        String candidate = importedName;

        while (candidate.contains(".")) {
            int lastDotIndex = candidate.lastIndexOf('.');
            candidate = candidate.substring(0, lastDotIndex);

            if (internalClassNames.contains(candidate)) {
                return candidate;
            }
        }

        return "";
    }

    private HistoricalMetrics computeHistoricalMetrics(String classPath,
                                                       String releaseDate)
            throws IOException, InterruptedException {
        LocalDateTime currentReleaseEndExclusive =
                DateUtils.parseReleaseDate(releaseDate).plusDays(1);

        /*
         * Il repository è già stato portato allo snapshot della release corrente.
         * Per questo motivo non serve filtrare nuovamente con --before nel git log:
         * la storia raggiungibile da HEAD è già limitata allo snapshot Ri.
         */
        List<String> lines = readGitLogWithFollow(classPath);

        if (lines.isEmpty()) {
            lines = readGitLogWithoutFollow(classPath);
        }

        HistoricalAccumulator accumulator = new HistoricalAccumulator();
        CurrentCommit currentCommit = null;

        for (String line : lines) {
            if (line.startsWith("__COMMIT__")) {
                if (currentCommit != null) {
                    acceptCurrentCommit(accumulator, currentCommit, classPath);
                }

                currentCommit = parseCommitHeader(line);
            } else if (currentCommit != null) {
                updateCurrentCommitStats(currentCommit, line);
            }
        }

        if (currentCommit != null) {
            acceptCurrentCommit(accumulator, currentCommit, classPath);
        }

        return accumulator.toHistoricalMetrics(currentReleaseEndExclusive);
    }

    private void acceptCurrentCommit(
            HistoricalAccumulator accumulator,
            CurrentCommit currentCommit,
            String classPath
    ) throws IOException, InterruptedException {
        CommitContext context = buildCommitContext(
                currentCommit.commitHash,
                classPath
        );

        accumulator.accept(
                currentCommit,
                context,
                fixCommitHashes.contains(currentCommit.commitHash)
        );
    }

    private void updateCurrentCommitStats(CurrentCommit currentCommit, String line) {
        String trimmed = line.trim();

        if (!trimmed.isBlank()) {
            String[] parts = trimmed.split("\\t");

            if (parts.length >= 2) {
                int added = parseNumstatValue(parts[0]);
                int deleted = parseNumstatValue(parts[1]);

                currentCommit.added += added;
                currentCommit.deleted += deleted;
            }
        }
    }

    private CurrentCommit parseCommitHeader(String line) {
        String payload = line.substring("__COMMIT__".length());
        String[] parts = payload.split("\\t", 3);

        if (parts.length < 3) {
            return new CurrentCommit("", "", 0L);
        }

        String commitHash = parts[0].trim();
        String author = parts[1].trim();

        long epochSeconds;

        try {
            epochSeconds = Long.parseLong(parts[2].trim());
        } catch (NumberFormatException e) {
            epochSeconds = 0L;
        }

        return new CurrentCommit(commitHash, author, epochSeconds);
    }

    private int parseNumstatValue(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private CommitContext buildCommitContext(String commitHash,
                                             String classPath)
            throws IOException, InterruptedException {
        if (commitHash == null || commitHash.isBlank()) {
            return new CommitContext();
        }

        List<String> changedFiles = getChangedFiles(commitHash);

        int changeSetSize = 0;
        Set<String> modifiedDirs = new HashSet<>();

        String classDir = directoryOf(classPath);
        boolean sameDirectoryOnly = true;

        for (String file : changedFiles) {
            String normalizedFile = normalizePath(file);

            if (normalizedFile.isBlank()) {
                continue;
            }

            changeSetSize++;

            String fileDir = directoryOf(normalizedFile);
            modifiedDirs.add(fileDir);

            if (!fileDir.equals(classDir)) {
                sameDirectoryOnly = false;
            }
        }

        if (changeSetSize == 0) {
            sameDirectoryOnly = false;
        }

        return new CommitContext(
                changeSetSize,
                modifiedDirs.size(),
                sameDirectoryOnly
        );
    }

    private List<String> getChangedFiles(String commitHash)
            throws IOException, InterruptedException {
        List<String> cachedValue = changedFilesByCommit.get(commitHash);

        if (cachedValue != null) {
            return cachedValue;
        }

        List<String> changedFiles = GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "diff-tree",
                "--no-commit-id",
                "--name-only",
                "-r",
                "--root",
                commitHash
        );

        changedFilesByCommit.put(commitHash, changedFiles);
        return changedFiles;
    }

    private String findLastCommitOfReleaseDay(String releaseDate,
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

    private String getCurrentCommitHash() throws IOException {
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

    private void checkout(String commitHash) throws IOException, InterruptedException {
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

    private String toFullyQualifiedName(String classPath) {
        String normalizedPath = normalizePath(classPath);

        int srcMainJavaIndex = normalizedPath.indexOf("/src/main/java/");
        if (srcMainJavaIndex < 0) {
            return "";
        }

        String javaRelativePath = normalizedPath.substring(
                srcMainJavaIndex + "/src/main/java/".length()
        );

        if (!javaRelativePath.endsWith(".java")) {
            return "";
        }

        return javaRelativePath
                .substring(0, javaRelativePath.length() - ".java".length())
                .replace("/", ".");
    }

    private String directoryOf(String path) {
        String normalizedPath = normalizePath(path);
        int lastSlashIndex = normalizedPath.lastIndexOf('/');

        if (lastSlashIndex < 0) {
            return "";
        }

        return normalizedPath.substring(0, lastSlashIndex);
    }

    private String normalizePath(String path) {
        return normalizeStaticPath(path);
    }

    private static String normalizeStaticPath(String path) {
        if (path == null) {
            return "";
        }

        return path.replace("\\", "/").trim();
    }

    private static double computeRatio(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }





    private static class SourceMetrics {

        private final int sizeLoc;
        private final int nom;
        private final double avgMethodSize;
        private final int cycloComplexity;
        private final int fanOut;

        private SourceMetrics() {
            this(0, 0, 0.0, 0, 0);
        }

        private SourceMetrics(int sizeLoc,
                              int nom,
                              double avgMethodSize,
                              int cycloComplexity,
                              int fanOut) {
            this.sizeLoc = sizeLoc;
            this.nom = nom;
            this.avgMethodSize = avgMethodSize;
            this.cycloComplexity = cycloComplexity;
            this.fanOut = fanOut;
        }
    }

    private static class CurrentCommit {

        private final String commitHash;
        private final String author;
        private final long epochSeconds;

        private int added;
        private int deleted;

        private CurrentCommit(String commitHash,
                              String author,
                              long epochSeconds) {
            this.commitHash = commitHash;
            this.author = author;
            this.epochSeconds = epochSeconds;
        }
    }

    private static class CommitContext {

        private final int changeSetSize;
        private final int modifiedDirCount;
        private final boolean sameDirectoryOnly;

        private CommitContext() {
            this(0, 0, false);
        }

        private CommitContext(int changeSetSize,
                              int modifiedDirCount,
                              boolean sameDirectoryOnly) {
            this.changeSetSize = changeSetSize;
            this.modifiedDirCount = modifiedDirCount;
            this.sameDirectoryOnly = sameDirectoryOnly;
        }
    }

    private static class HistoricalAccumulator {

        private int nr;
        private int nFix;

        private int locAdded;
        private int maxLocAdded;

        private int churn;
        private int maxChurn;

        private int maxChangeSetSize;
        private int totalModifiedDirs;

        private int sameDirectoryCommits;

        private long firstCommitEpochSeconds = Long.MAX_VALUE;
        private long latestCommitEpochSeconds = Long.MIN_VALUE;

        private final Map<String, Integer> commitsByAuthor = new HashMap<>();

        private void accept(CurrentCommit commit,
                            CommitContext context,
                            boolean isFixCommit) {
            if (commit.commitHash == null || commit.commitHash.isBlank()) {
                return;
            }

            nr++;

            if (isFixCommit) {
                nFix++;
            }

            if (commit.author != null && !commit.author.isBlank()) {
                commitsByAuthor.merge(commit.author, 1, Integer::sum);
            }

            firstCommitEpochSeconds = Math.min(
                    firstCommitEpochSeconds,
                    commit.epochSeconds
            );

            latestCommitEpochSeconds = Math.max(
                    latestCommitEpochSeconds,
                    commit.epochSeconds
            );

            int commitChurn = commit.added + commit.deleted;

            locAdded += commit.added;
            maxLocAdded = Math.max(maxLocAdded, commit.added);

            churn += commitChurn;
            maxChurn = Math.max(maxChurn, commitChurn);

            maxChangeSetSize = Math.max(
                    maxChangeSetSize,
                    context.changeSetSize
            );

            totalModifiedDirs += context.modifiedDirCount;

            if (context.sameDirectoryOnly) {
                sameDirectoryCommits++;
            }
        }


        private long daysBetween(long commitEpochSeconds,
                                 LocalDateTime releaseEndExclusive) {
            if (commitEpochSeconds <= 0) {
                return 0;
            }

            Instant commitInstant = Instant.ofEpochSecond(commitEpochSeconds);
            Instant releaseInstant = releaseEndExclusive.toInstant(ZoneOffset.UTC);

            long days = ChronoUnit.DAYS.between(commitInstant, releaseInstant);

            return Math.max(days, 0);
        }

        private HistoricalMetrics toHistoricalMetrics(LocalDateTime releaseEndExclusive) {
            HistoricalMetrics metrics = new HistoricalMetrics();

            if (nr == 0) {
                return metrics;
            }

            metrics.nr = nr;
            metrics.nFix = nFix;
            metrics.nAuth = commitsByAuthor.size();

            metrics.locAdded = locAdded;
            metrics.maxLocAdded = maxLocAdded;

            metrics.churn = churn;
            metrics.maxChurn = maxChurn;

            metrics.maxChangeSetSize = maxChangeSetSize;
            metrics.avgModifiedDirs = computeRatio(
                    totalModifiedDirs,
                    Math.max(1, nr)
            );

            if (latestCommitEpochSeconds != Long.MIN_VALUE) {
                metrics.ageSinceLastChange = daysBetween(
                        latestCommitEpochSeconds,
                        releaseEndExclusive
                );
            }

            if (firstCommitEpochSeconds != Long.MAX_VALUE) {
                metrics.age = daysBetween(
                        firstCommitEpochSeconds,
                        releaseEndExclusive
                );
            }

            metrics.ownershipRatio = computeOwnershipRatio();

            metrics.sameDirectoryChangeRatio = computeRatio(
                    sameDirectoryCommits,
                    Math.max(1, nr)
            );

            return metrics;
        }

        private double computeOwnershipRatio() {
            if (nr == 0 || commitsByAuthor.isEmpty()) {
                return 0.0;
            }

            int maxCommitsBySingleAuthor = 0;

            for (int commits : commitsByAuthor.values()) {
                maxCommitsBySingleAuthor = Math.max(
                        maxCommitsBySingleAuthor,
                        commits
                );
            }

            return (double) maxCommitsBySingleAuthor / nr;
        }
    }

    private static class HistoricalMetrics {

        private int nr;
        private int nFix;
        private int nAuth;

        private int locAdded;
        private int maxLocAdded;

        private int churn;
        private int maxChurn;

        private int maxChangeSetSize;
        private double avgModifiedDirs;

        private long ageSinceLastChange;
        private double ownershipRatio;

        private double sameDirectoryChangeRatio;

        private long age;
    }
    private List<String> readGitLogWithFollow(String classPath)
            throws IOException, InterruptedException {
        return GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "log",
                "--follow",
                "--numstat",
                "--pretty=format:__COMMIT__%H%x09%an%x09%ct",
                "--",
                classPath
        );
    }

    private List<String> readGitLogWithoutFollow(String classPath)
            throws IOException, InterruptedException {
        return GitCommandRunner.runCommand(
                repositoryPath.toString(),
                "git",
                "log",
                "--numstat",
                "--pretty=format:__COMMIT__%H%x09%an%x09%ct",
                "--",
                classPath
        );
    }
}