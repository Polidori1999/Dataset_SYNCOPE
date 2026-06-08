package it.uniroma2.isw2;

import com.github.javaparser.ast.body.*;
import it.uniroma2.isw2.io.CsvUtils;
import it.uniroma2.isw2.io.ReleaseCsvReader;
import it.uniroma2.isw2.labeling.ReleaseInventoryService;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.smell.ClassReleaseSmell;
import it.uniroma2.isw2.smell.PmdFileListWriter;
import it.uniroma2.isw2.smell.PmdRunner;
import it.uniroma2.isw2.smell.SmellComputationResult;
import it.uniroma2.isw2.smell.SmellService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.WhileStmt;
import java.util.logging.Level;
import java.util.logging.Logger;



public class Milestone4ClassSelector {

    private static final Logger LOGGER =
            Logger.getLogger(Milestone4ClassSelector.class.getName());

    private static final String PROJECT_NAME = "SYNCOPE";

    private static final String RELEASES_FILE = PROJECT_NAME + "VersionInfo.csv";

    private static final String PROJECT_REPO_PATH =
            System.getProperty("project.repo.path", "/home/leonardo/uni/syncope");

    private static final String OUTPUT_DIR =
            "Classi";

    private static final String FIRST_NAME = "Leonardo";

    private static final String PMD_EXECUTABLE =
            "/home/leonardo/uni/isw2/pmd/pmd-bin-7.24.0/bin/pmd";

    private static final String PMD_RULESET =
            "rulesets/java/quickstart.xml";

    private static final String PMD_JAVA_VERSION =
            "java-21";

    private static final String PMD_FILE_LISTS_DIR =
            "target/pmd-filelists/" + PROJECT_NAME + "-milestone4";

    private static final String PMD_REPORTS_DIR =
            "target/pmd-reports/" + PROJECT_NAME + "-milestone4";

    private static final int MIN_NSMELLS = 4;
    private static final int MIN_LOC = 150;
    private static final int MIN_METHODS = 7;
    private static final int MIN_PUBLIC_METHODS = 2;
    private static final int MIN_COMPLEXITY = 5;

    private static final List<String> GUI_KEYWORDS = List.of(
            "gui", "ui", "view", "window", "panel", "button", "dialog",
            "frame", "screen", "page", "component", "widget", "swing",
            "awt", "javafx", "servlet", "controller", "web", "template",
            "console", "wizard", "wicket", "ajax", "markup", "directory", "selection"
    );

    private static final List<String> SIMPLE_ROLE_KEYWORDS = List.of(
            "dto", "vo", "pojo", "bean", "model", "entity",
            "constant", "constants", "config", "configuration",
            "properties", "exception", "error", "factory", "builder",
            "holder", "wrapper", "adapter", "request", "response"
    );

    private static final List<String> GENERATED_KEYWORDS = List.of(
            "generated", "target/generated", "build/generated",
            "protobuf", "grpc", "thrift", "avro", "openapi", "swagger"
    );

    public static void main(String[] args) throws IOException {
        List<Release> allReleases = ReleaseCsvReader.loadReleases(RELEASES_FILE);
        Release latestRelease = findLatestRelease(allReleases);

        LOGGER.info("Ultima release selezionata:");
        LOGGER.log(Level.INFO, "- Index: {0}", latestRelease.getIndex());
        LOGGER.log(Level.INFO, "- ID: {0}", latestRelease.getVersionId());
        LOGGER.log(Level.INFO, "- Name: {0}", latestRelease.getVersionName());
        LOGGER.log(Level.INFO, "- Date: {0}", latestRelease.getDate());

        List<Release> releasesForMilestone4 = List.of(latestRelease);

        List<ReleaseJavaClass> latestReleaseClasses =
                ReleaseInventoryService.buildReleaseInventory(
                        PROJECT_NAME,
                        releasesForMilestone4,
                        PROJECT_REPO_PATH
                );

        LOGGER.log(Level.INFO,
                "Classi Java di produzione nell''ultima release: {0}",
                latestReleaseClasses.size());

        SmellComputationResult smellResult =
                computeLatestReleaseSmells(releasesForMilestone4, latestReleaseClasses);

        Map<String, Integer> smellsByClassRelease =
                buildSmellMap(smellResult.getSmells());

        List<RankedClass> allRankedClasses =
                computeFastRankedClasses(
                        latestRelease,
                        latestReleaseClasses,
                        smellsByClassRelease,
                        Path.of(PROJECT_REPO_PATH)
                );

        List<RankedClass> accepted = new ArrayList<>();
        List<RankedClass> discarded = new ArrayList<>();

        for (RankedClass rankedClass : allRankedClasses) {
            applyFilters(rankedClass);

            if (rankedClass.discardReasons.isEmpty()) {
                accepted.add(rankedClass);
            } else {
                discarded.add(rankedClass);
            }
        }

        accepted.sort(
                Comparator.comparingInt(RankedClass::nSmells).reversed()
                        .thenComparing(Comparator.comparingInt(RankedClass::sizeLoc).reversed())
                        .thenComparing(Comparator.comparingInt(RankedClass::nom).reversed())
                        .thenComparing(RankedClass::classPath)
        );
        assignOriginalRanks(accepted);

        discarded.sort(
                Comparator.comparingInt(RankedClass::nSmells).reversed()
                        .thenComparing(RankedClass::discardReasonsText)
                        .thenComparing(RankedClass::classPath)
        );

        Path outputDir = Path.of(OUTPUT_DIR).toAbsolutePath().normalize();
        Files.createDirectories(outputDir);

        writeCsv(outputDir.resolve("milestone4_ranked_latest_release.csv"), accepted);
        writeCsv(outputDir.resolve("milestone4_discarded_latest_release.csv"), discarded);

        List<RankedClass> selected =
                selectClassesByNameAlgorithm(accepted, FIRST_NAME);

        writeCsv(outputDir.resolve("milestone4_selected_classes.csv"), selected);
        writeClassesTxt(outputDir.resolve("classes.txt"), selected);

        printSummary(latestRelease, accepted, discarded, selected, outputDir);
    }

    private static void assignOriginalRanks(List<RankedClass> rankedClasses) {
        for (int i = 0; i < rankedClasses.size(); i++) {
            rankedClasses.get(i).originalRank = i + 1;
        }
    }

    private static Release findLatestRelease(List<Release> releases) {
        if (releases.isEmpty()) {
            throw new IllegalArgumentException("Nessuna release trovata.");
        }

        return releases.stream()
                .max(Comparator.comparingInt(Release::getIndex))
                .orElseThrow();
    }

    private static SmellComputationResult computeLatestReleaseSmells(
            List<Release> releases,
            List<ReleaseJavaClass> releaseJavaClasses
    ) throws IOException {
        PmdFileListWriter pmdFileListWriter =
                new PmdFileListWriter(Path.of(PMD_FILE_LISTS_DIR).toAbsolutePath());

        PmdRunner pmdRunner =
                new PmdRunner(
                        PMD_EXECUTABLE,
                        PMD_RULESET,
                        PMD_JAVA_VERSION,
                        Path.of(PMD_REPORTS_DIR).toAbsolutePath()
                );

        SmellService smellService =
                new SmellService(
                        PROJECT_NAME,
                        Path.of(PROJECT_REPO_PATH),
                        pmdFileListWriter,
                        pmdRunner
                );

        return smellService.computeSmells(releases, releaseJavaClasses);
    }

    private static Map<String, Integer> buildSmellMap(List<ClassReleaseSmell> smells) {
        Map<String, Integer> result = new HashMap<>();

        for (ClassReleaseSmell smell : smells) {
            String key = buildMetricKey(
                    smell.getReleaseId(),
                    normalizePath(smell.getClassPath())
            );

            result.put(key, smell.getNSmells());
        }

        return result;
    }

    private static String buildMetricKey(String releaseId, String classPath) {
        return releaseId + "::" + normalizePath(classPath);
    }

    private static List<RankedClass> computeFastRankedClasses(
            Release latestRelease,
            List<ReleaseJavaClass> latestReleaseClasses,
            Map<String, Integer> smellsByClassRelease,
            Path repositoryPath
    ) throws IOException {
        List<RankedClass> result = new ArrayList<>();

        int processed = 0;

        for (ReleaseJavaClass javaClass : latestReleaseClasses) {
            processed++;

            if (processed % 100 == 0) {
                LOGGER.log(Level.INFO,
                        "Metriche leggere calcolate per {0}/{1} classi...",
                        new Object[]{processed, latestReleaseClasses.size()});
            }

            String classPath = normalizePath(javaClass.getClassPath());
            Path absoluteClassPath = resolveClassPath(repositoryPath, classPath);

            SourceStats stats = computeSourceStats(absoluteClassPath);

            int nSmells = smellsByClassRelease.getOrDefault(
                    buildMetricKey(latestRelease.getVersionId(), classPath),
                    0
            );

            double smellDensity = stats.sizeLoc == 0
                    ? 0.0
                    : (double) nSmells / stats.sizeLoc;

            result.add(RankedClass.builder()
                    .releaseId(latestRelease.getVersionId())
                    .releaseName(latestRelease.getVersionName())
                    .classPath(classPath)
                    .qualifiedName(toQualifiedName(classPath))
                    .nSmells(nSmells)
                    .sizeLoc(stats.sizeLoc)
                    .nom(stats.nom)
                    .publicMethods(stats.publicMethods)
                    .avgMethodSize(stats.avgMethodSize)
                    .cycloComplexity(stats.cycloComplexity)
                    .fanOut(stats.fanOut)
                    .smellDensity(smellDensity)
                    .isAbstract(stats.isAbstract)
                    .isInterface(stats.isInterface)
                    .isEnum(stats.isEnum)
                    .isAnnotation(stats.isAnnotation)
                    .build());
        }

        return result;
    }

    private static Path resolveClassPath(Path repositoryPath, String classPath) {
        Path path = Path.of(classPath);

        if (path.isAbsolute()) {
            return path.normalize();
        }

        return repositoryPath.resolve(classPath).toAbsolutePath().normalize();
    }

    private static SourceStats computeSourceStats(Path absoluteClassPath) throws IOException {
        if (!Files.exists(absoluteClassPath)) {
            return SourceStats.builder().build();
        }

        String source = Files.readString(absoluteClassPath, StandardCharsets.UTF_8);
        int sizeLoc = countLoc(source);

        try {
            CompilationUnit cu = StaticJavaParser.parse(source);

            int methods = cu.findAll(MethodDeclaration.class).size();
            int constructors = cu.findAll(ConstructorDeclaration.class).size();
            int publicMethods = (int) cu.findAll(MethodDeclaration.class).stream()
                    .filter(MethodDeclaration::isPublic)
                    .count();
            int nom = methods + constructors;

            int cycloComplexity = computeCyclomaticComplexity(cu);
            int fanOut = cu.getImports().size();



            double avgMethodSize = nom == 0
                    ? 0.0
                    : (double) sizeLoc / nom;

            boolean isAbstract = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .anyMatch(ClassOrInterfaceDeclaration::isAbstract);

            boolean isInterface = cu.findAll(ClassOrInterfaceDeclaration.class).stream()
                    .anyMatch(ClassOrInterfaceDeclaration::isInterface);

            boolean isEnum = !cu.findAll(EnumDeclaration.class).isEmpty();

            boolean isAnnotation = !cu.findAll(AnnotationDeclaration.class).isEmpty();

            return SourceStats.builder()
                    .sizeLoc(sizeLoc)
                    .nom(nom)
                    .publicMethods(publicMethods)
                    .avgMethodSize(avgMethodSize)
                    .cycloComplexity(cycloComplexity)
                    .fanOut(fanOut)
                    .isAbstract(isAbstract)
                    .isInterface(isInterface)
                    .isEnum(isEnum)
                    .isAnnotation(isAnnotation)
                    .build();

        } catch (Exception e) {
            int nom = countMethodsFallback(source);
            int cycloComplexity = countCyclomaticComplexityFallback(source);
            int fanOut = countImportsFallback(source);
            int publicMethods = countPublicMethodsFallback(source);
            double avgMethodSize = nom == 0
                    ? 0.0
                    : (double) sizeLoc / nom;

            boolean isAbstract = source.contains("abstract class ");
            boolean isInterface = source.contains("interface ");
            boolean isEnum = source.contains(" enum ");
            boolean isAnnotation = source.contains("@interface ");

            return SourceStats.builder()
                    .sizeLoc(sizeLoc)
                    .nom(nom)
                    .publicMethods(publicMethods)
                    .avgMethodSize(avgMethodSize)
                    .cycloComplexity(cycloComplexity)
                    .fanOut(fanOut)
                    .isAbstract(isAbstract)
                    .isInterface(isInterface)
                    .isEnum(isEnum)
                    .isAnnotation(isAnnotation)
                    .build();
        }
    }
    private static int countPublicMethodsFallback(String source) {
        int count = 0;

        for (String line : source.split("\\R")) {
            String trimmed = line.trim();

            if (trimmed.startsWith("public ") && looksLikeMethodFallback(trimmed)) {
                count++;
            }
        }

        return count;
    }

    private static int countLoc(String source) {
        String withoutComments = removeComments(source);
        int count = 0;

        for (String line : withoutComments.split("\\R")) {
            if (!line.trim().isBlank()) {
                count++;
            }
        }

        return count;
    }

    private static String removeComments(String source) {
        StringBuilder result = new StringBuilder();
        CommentScanState state = new CommentScanState();

        while (state.index < source.length()) {
            char current = source.charAt(state.index);
            char next = nextChar(source, state.index);

            processCharacter(source, result, state, current, next);
        }

        return result.toString();
    }
    private static void processCharacter(
            String source,
            StringBuilder result,
            CommentScanState state,
            char current,
            char next
    ) {
        if (state.inLineComment) {
            handleLineComment(result, state, current);
        } else if (state.inBlockComment) {
            handleBlockComment(state, current, next);
        } else {
            handleCodeCharacter(source, result, state, current, next);
        }
    }

    private static void handleLineComment(
            StringBuilder result,
            CommentScanState state,
            char current
    ) {
        if (isNewLine(current)) {
            state.inLineComment = false;
            result.append(current);
        }

        state.index++;
    }

    private static void handleBlockComment(
            CommentScanState state,
            char current,
            char next
    ) {
        if (isBlockCommentEnd(current, next)) {
            state.inBlockComment = false;
            state.index += 2;
        } else {
            state.index++;
        }
    }

    private static void handleCodeCharacter(
            String source,
            StringBuilder result,
            CommentScanState state,
            char current,
            char next
    ) {
        if (isLineCommentStart(state, current, next)) {
            state.inLineComment = true;
            state.index += 2;
        } else if (isBlockCommentStart(state, current, next)) {
            state.inBlockComment = true;
            state.index += 2;
        } else {
            updateStringAndCharState(source, state, current);
            result.append(current);
            state.index++;
        }
    }

    private static void updateStringAndCharState(
            String source,
            CommentScanState state,
            char current
    ) {
        if (!state.inChar && current == '"' && !isEscaped(source, state.index)) {
            state.inString = !state.inString;
        }

        if (!state.inString && current == '\'' && !isEscaped(source, state.index)) {
            state.inChar = !state.inChar;
        }
    }

    private static boolean isLineCommentStart(
            CommentScanState state,
            char current,
            char next
    ) {
        return !state.inString && !state.inChar && current == '/' && next == '/';
    }

    private static boolean isBlockCommentStart(
            CommentScanState state,
            char current,
            char next
    ) {
        return !state.inString && !state.inChar && current == '/' && next == '*';
    }

    private static boolean isBlockCommentEnd(char current, char next) {
        return current == '*' && next == '/';
    }

    private static boolean isNewLine(char current) {
        return current == '\n' || current == '\r';
    }

    private static char nextChar(String source, int index) {
        return index + 1 < source.length() ? source.charAt(index + 1) : '\0';
    }

    private static final class CommentScanState {
        private int index;
        private boolean inBlockComment;
        private boolean inLineComment;
        private boolean inString;
        private boolean inChar;
    }

    private static boolean isEscaped(String source, int index) {
        int backslashes = 0;

        for (int i = index - 1; i >= 0 && source.charAt(i) == '\\'; i--) {
            backslashes++;
        }

        return backslashes % 2 == 1;
    }

    private static int computeCyclomaticComplexity(CompilationUnit cu) {
        int complexity = 1;

        complexity += cu.findAll(IfStmt.class).size();
        complexity += cu.findAll(ForStmt.class).size();
        complexity += cu.findAll(ForEachStmt.class).size();
        complexity += cu.findAll(WhileStmt.class).size();
        complexity += cu.findAll(DoStmt.class).size();
        complexity += cu.findAll(CatchClause.class).size();
        complexity += cu.findAll(ConditionalExpr.class).size();

        complexity += cu.findAll(SwitchEntry.class).stream()
                .mapToInt(entry -> entry.getLabels().isEmpty() ? 0 : entry.getLabels().size())
                .sum();

        complexity += cu.findAll(BinaryExpr.class).stream()
                .filter(expr ->
                        expr.getOperator() == BinaryExpr.Operator.AND
                                || expr.getOperator() == BinaryExpr.Operator.OR
                )
                .mapToInt(expr -> 1)
                .sum();

        return complexity;
    }

    private static int countMethodsFallback(String source) {
        int count = 0;

        for (String line : source.split("\\R")) {
            String trimmed = line.trim();

            if (looksLikeMethodFallback(trimmed)) {
                count++;
            }
        }

        return count;
    }

    private static boolean looksLikeMethodFallback(String line) {
        if (!line.contains("(") || !line.contains(")") || !line.contains("{")) {
            return false;
        }

        if (line.startsWith("if ")
                || line.startsWith("for ")
                || line.startsWith("while ")
                || line.startsWith("switch ")
                || line.startsWith("catch ")
                || line.startsWith("return ")) {
            return false;
        }

        return line.contains("public ")
                || line.contains("private ")
                || line.contains("protected ")
                || line.contains("static ")
                || line.contains("final ")
                || line.contains("synchronized ");
    }

    private static int countCyclomaticComplexityFallback(String source) {
        int complexity = 1;

        complexity += countOccurrences(source, "if ");
        complexity += countOccurrences(source, "for ");
        complexity += countOccurrences(source, "while ");
        complexity += countOccurrences(source, "case ");
        complexity += countOccurrences(source, "catch ");
        complexity += countOccurrences(source, "&&");
        complexity += countOccurrences(source, "||");
        complexity += countOccurrences(source, "?");

        return complexity;
    }

    private static int countImportsFallback(String source) {
        int count = 0;

        for (String line : source.split("\\R")) {
            if (line.trim().startsWith("import ")) {
                count++;
            }
        }

        return count;
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }

        return count;
    }

    private static final class SourceStats {
        private final int sizeLoc;
        private final int nom;
        private final int publicMethods;
        private final double avgMethodSize;
        private final int cycloComplexity;
        private final int fanOut;
        private final boolean isAbstract;
        private final boolean isInterface;
        private final boolean isEnum;
        private final boolean isAnnotation;

        private SourceStats(Builder builder) {
            this.sizeLoc = builder.sizeLoc;
            this.nom = builder.nom;
            this.publicMethods = builder.publicMethods;
            this.avgMethodSize = builder.avgMethodSize;
            this.cycloComplexity = builder.cycloComplexity;
            this.fanOut = builder.fanOut;
            this.isAbstract = builder.isAbstract;
            this.isInterface = builder.isInterface;
            this.isEnum = builder.isEnum;
            this.isAnnotation = builder.isAnnotation;
        }

        private static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private int sizeLoc;
            private int nom;
            private int publicMethods;
            private double avgMethodSize;
            private int cycloComplexity;
            private int fanOut;
            private boolean isAbstract;
            private boolean isInterface;
            private boolean isEnum;
            private boolean isAnnotation;

            private Builder sizeLoc(int value) {
                this.sizeLoc = value;
                return this;
            }

            private Builder nom(int value) {
                this.nom = value;
                return this;
            }

            private Builder publicMethods(int value) {
                this.publicMethods = value;
                return this;
            }

            private Builder avgMethodSize(double value) {
                this.avgMethodSize = value;
                return this;
            }

            private Builder cycloComplexity(int value) {
                this.cycloComplexity = value;
                return this;
            }

            private Builder fanOut(int value) {
                this.fanOut = value;
                return this;
            }

            private Builder isAbstract(boolean value) {
                this.isAbstract = value;
                return this;
            }

            private Builder isInterface(boolean value) {
                this.isInterface = value;
                return this;
            }

            private Builder isEnum(boolean value) {
                this.isEnum = value;
                return this;
            }

            private Builder isAnnotation(boolean value) {
                this.isAnnotation = value;
                return this;
            }

            private SourceStats build() {
                return new SourceStats(this);
            }
        }
    }

    private static void applyFilters(RankedClass rankedClass) {
        String text = rankedClass.classPath.toLowerCase(Locale.ROOT);

        if (rankedClass.nSmells < MIN_NSMELLS) {
            rankedClass.discardReasons.add("LOW_NSMELLS<" + MIN_NSMELLS);
        }

        if (rankedClass.sizeLoc < MIN_LOC) {
            rankedClass.discardReasons.add("LOW_LOC<" + MIN_LOC);
        }

        if (rankedClass.nom < MIN_METHODS) {
            rankedClass.discardReasons.add("FEW_METHODS<" + MIN_METHODS);
        }
        if (rankedClass.publicMethods < MIN_PUBLIC_METHODS) {
            rankedClass.discardReasons.add("FEW_PUBLIC_METHODS<" + MIN_PUBLIC_METHODS);
        }

        if (rankedClass.cycloComplexity < MIN_COMPLEXITY) {
            rankedClass.discardReasons.add("LOW_COMPLEXITY<" + MIN_COMPLEXITY);
        }

        if (containsAny(text, GUI_KEYWORDS)) {
            rankedClass.discardReasons.add("LIKELY_GUI_CLASS");
        }

        if (containsAny(text, SIMPLE_ROLE_KEYWORDS)) {
            rankedClass.discardReasons.add("LIKELY_SIMPLE_ROLE");
        }

        if (containsAny(text, GENERATED_KEYWORDS)) {
            rankedClass.discardReasons.add("GENERATED_CODE");
        }

        if (text.contains("/src/test/")) {
            rankedClass.discardReasons.add("TEST_CLASS");
        }
        if (rankedClass.isAbstract) {
            rankedClass.discardReasons.add("ABSTRACT_CLASS");
        }

        if (rankedClass.isInterface) {
            rankedClass.discardReasons.add("INTERFACE");
        }

        if (rankedClass.isEnum) {
            rankedClass.discardReasons.add("ENUM");
        }

        if (rankedClass.isAnnotation) {
            rankedClass.discardReasons.add("ANNOTATION");
        }
    }

    private static boolean containsAny(String text, List<String> keywords) {
        String normalized = text.toLowerCase(Locale.ROOT).replace("\\", "/");

        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private static List<RankedClass> selectClassesByNameAlgorithm(
            List<RankedClass> rankedClasses,
            String firstName
    ) {
        if (firstName == null || firstName.isBlank()) {
            return List.of();
        }

        char firstLetter = Character.toUpperCase(firstName.trim().charAt(0));

        if (firstLetter < 'A' || firstLetter > 'Z') {
            return List.of();
        }

        int letterNumber = firstLetter - 'A' + 1;
        int x = letterNumber % 5;

        if (rankedClasses.size() < (2 * x + 2)) {
            return List.of();
        }

        return List.of(
                rankedClasses.get(x),
                rankedClasses.get(rankedClasses.size() - 1 - x)
        );
    }

    private static void writeCsv(Path outputPath, List<RankedClass> classes)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.join(",",
                    "rank",
                    "originalRank",
                    "releaseId",
                    "releaseName",
                    "classPath",
                    "qualifiedName",
                    "NSmells",
                    "SIZE_LOC",
                    "NOM",
                    "PUBLIC_METHODS",
                    "AVG_METHOD_SIZE",
                    "CYCLO_COMPLEXITY",
                    "FAN_OUT",
                    "SMELL_DENSITY",
                    "isAbstract",
                    "isInterface",
                    "isEnum",
                    "isAnnotation",
                    "discardReasons"
            ));
            writer.newLine();

            int rank = 1;

            for (RankedClass rankedClass : classes) {
                writer.write(String.join(",",
                        csv(rank),
                        csv(rankedClass.originalRank),
                        csv(rankedClass.releaseId),
                        csv(rankedClass.releaseName),
                        csv(rankedClass.classPath),
                        csv(rankedClass.qualifiedName),
                        csv(rankedClass.nSmells),
                        csv(rankedClass.sizeLoc),
                        csv(rankedClass.nom),
                        csv(rankedClass.publicMethods),
                        csv(rankedClass.avgMethodSize),
                        csv(rankedClass.cycloComplexity),
                        csv(rankedClass.fanOut),
                        csv(rankedClass.smellDensity),
                        csv(rankedClass.isAbstract),
                        csv(rankedClass.isInterface),
                        csv(rankedClass.isEnum),
                        csv(rankedClass.isAnnotation),
                        csv(rankedClass.discardReasonsText())
                ));
                writer.newLine();
                rank++;
            }
        }
    }

    private static void writeClassesTxt(Path outputPath, List<RankedClass> selected)
            throws IOException {
        List<String> classNames = selected.stream()
                .map(RankedClass::qualifiedName)
                .sorted()
                .toList();

        Files.write(outputPath, classNames, StandardCharsets.UTF_8);
    }

    private static void printSummary(
            Release latestRelease,
            List<RankedClass> accepted,
            List<RankedClass> discarded,
            List<RankedClass> selected,
            Path outputDir
    ) {
        LOGGER.info("");
        LOGGER.info("Ranking Milestone 4 completato.");
        LOGGER.log(Level.INFO,
                "Release usata: {0} | ID={1} | Index={2}",
                new Object[]{
                        latestRelease.getVersionName(),
                        latestRelease.getVersionId(),
                        latestRelease.getIndex()
                });
        LOGGER.log(Level.INFO, "Classi accettate: {0}", accepted.size());
        LOGGER.log(Level.INFO, "Classi scartate: {0}", discarded.size());
        LOGGER.log(Level.INFO, "Output: {0}", outputDir);

        LOGGER.info("");
        LOGGER.info("Top 10 classi accettate:");

        accepted.stream()
                .limit(10)
                .forEach(c -> LOGGER.log(Level.INFO,
                        "- {0} | NSmells={1} | LOC={2} | NOM={3} | PUBLIC={4} | CYCLO={5}",
                        new Object[]{
                                c.qualifiedName,
                                c.nSmells,
                                c.sizeLoc,
                                c.nom,
                                c.publicMethods,
                                c.cycloComplexity
                        }));

        LOGGER.info("");
        LOGGER.info("Classi selezionate:");

        for (RankedClass c : selected) {
            LOGGER.log(Level.INFO,
                    "- originalRank={0} | {1} | NSmells={2} | LOC={3} | NOM={4} | PUBLIC={5}",
                    new Object[]{
                            c.originalRank,
                            c.qualifiedName,
                            c.nSmells,
                            c.sizeLoc,
                            c.nom,
                            c.publicMethods
                    });
        }
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }

    private static String toQualifiedName(String classPath) {
        String normalized = normalizePath(classPath);

        int srcIndex = normalized.indexOf("src/main/java/");

        if (srcIndex >= 0) {
            normalized = normalized.substring(srcIndex + "src/main/java/".length());
        }

        if (normalized.endsWith(".java")) {
            normalized = normalized.substring(0, normalized.length() - ".java".length());
        }

        return normalized.replace("/", ".");
    }

    private static String csv(Object value) {
        return CsvUtils.escapeCsv(String.valueOf(value));
    }



    private static final class RankedClass {
        private final String releaseId;
        private final String releaseName;
        private final String classPath;
        private final String qualifiedName;
        private final int nSmells;
        private final int sizeLoc;
        private final int nom;
        private final double avgMethodSize;
        private final int cycloComplexity;
        private final int fanOut;
        private final double smellDensity;
        private final List<String> discardReasons = new ArrayList<>();
        private int originalRank;
        private final boolean isAbstract;
        private final boolean isInterface;
        private final boolean isEnum;
        private final boolean isAnnotation;
        private final int publicMethods;

        private RankedClass(Builder builder) {
            this.releaseId = builder.releaseId;
            this.releaseName = builder.releaseName;
            this.classPath = builder.classPath;
            this.qualifiedName = builder.qualifiedName;
            this.nSmells = builder.nSmells;
            this.sizeLoc = builder.sizeLoc;
            this.nom = builder.nom;
            this.publicMethods = builder.publicMethods;
            this.avgMethodSize = builder.avgMethodSize;
            this.cycloComplexity = builder.cycloComplexity;
            this.fanOut = builder.fanOut;
            this.smellDensity = builder.smellDensity;
            this.isAbstract = builder.isAbstract;
            this.isInterface = builder.isInterface;
            this.isEnum = builder.isEnum;
            this.isAnnotation = builder.isAnnotation;
        }

        private static Builder builder() {
            return new Builder();
        }

        private static final class Builder {
            private String releaseId;
            private String releaseName;
            private String classPath;
            private String qualifiedName;
            private int nSmells;
            private int sizeLoc;
            private int nom;
            private int publicMethods;
            private double avgMethodSize;
            private int cycloComplexity;
            private int fanOut;
            private double smellDensity;
            private boolean isAbstract;
            private boolean isInterface;
            private boolean isEnum;
            private boolean isAnnotation;

            private Builder releaseId(String value) {
                this.releaseId = value;
                return this;
            }

            private Builder releaseName(String value) {
                this.releaseName = value;
                return this;
            }

            private Builder classPath(String value) {
                this.classPath = value;
                return this;
            }

            private Builder qualifiedName(String value) {
                this.qualifiedName = value;
                return this;
            }

            private Builder nSmells(int value) {
                this.nSmells = value;
                return this;
            }

            private Builder sizeLoc(int value) {
                this.sizeLoc = value;
                return this;
            }

            private Builder nom(int value) {
                this.nom = value;
                return this;
            }

            private Builder publicMethods(int value) {
                this.publicMethods = value;
                return this;
            }

            private Builder avgMethodSize(double value) {
                this.avgMethodSize = value;
                return this;
            }

            private Builder cycloComplexity(int value) {
                this.cycloComplexity = value;
                return this;
            }

            private Builder fanOut(int value) {
                this.fanOut = value;
                return this;
            }

            private Builder smellDensity(double value) {
                this.smellDensity = value;
                return this;
            }

            private Builder isAbstract(boolean value) {
                this.isAbstract = value;
                return this;
            }

            private Builder isInterface(boolean value) {
                this.isInterface = value;
                return this;
            }

            private Builder isEnum(boolean value) {
                this.isEnum = value;
                return this;
            }

            private Builder isAnnotation(boolean value) {
                this.isAnnotation = value;
                return this;
            }

            private RankedClass build() {
                return new RankedClass(this);
            }
        }

        private int nSmells() {
            return nSmells;
        }

        private int sizeLoc() {
            return sizeLoc;
        }

        private int nom() {
            return nom;
        }

        private String classPath() {
            return classPath;
        }

        private String qualifiedName() {
            return qualifiedName;
        }

        private String discardReasonsText() {
            return String.join("|", discardReasons);
        }
    }
}