package it.uniroma2.isw2;

import com.github.javaparser.ast.body.*;
import it.uniroma2.isw2.io.CsvUtils;
import it.uniroma2.isw2.io.ReleaseCsvReader;
import it.uniroma2.isw2.labeling.ReleaseInventoryService;
import it.uniroma2.isw2.metric.ClassReleaseMetric;
import it.uniroma2.isw2.metric.MetricService;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.smell.ClassReleaseSmell;
import it.uniroma2.isw2.smell.PmdFileListWriter;
import it.uniroma2.isw2.smell.PmdRunResult;
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



public class Milestone4ClassSelector {

    private static final String PROJECT_NAME = "SYNCOPE";

    private static final String RELEASES_FILE = PROJECT_NAME + "VersionInfo.csv";

    private static final String PROJECT_REPO_PATH =
            "/home/leonardo/uni/syncope";

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

        System.out.println("Ultima release selezionata:");
        System.out.println("- Index: " + latestRelease.getIndex());
        System.out.println("- ID: " + latestRelease.getVersionId());
        System.out.println("- Name: " + latestRelease.getVersionName());
        System.out.println("- Date: " + latestRelease.getDate());

        List<Release> releasesForMilestone4 = List.of(latestRelease);

        List<ReleaseJavaClass> latestReleaseClasses =
                ReleaseInventoryService.buildReleaseInventory(
                        PROJECT_NAME,
                        releasesForMilestone4,
                        PROJECT_REPO_PATH
                );

        System.out.println("Classi Java di produzione nell'ultima release: "
                + latestReleaseClasses.size());

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
                System.out.println("Metriche leggere calcolate per "
                        + processed + "/" + latestReleaseClasses.size() + " classi...");
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

            result.add(new RankedClass(
                    latestRelease.getVersionId(),
                    latestRelease.getVersionName(),
                    classPath,
                    toQualifiedName(classPath),
                    nSmells,
                    stats.sizeLoc,
                    stats.nom,
                    stats.publicMethods,
                    stats.avgMethodSize,
                    stats.cycloComplexity,
                    stats.fanOut,
                    smellDensity,
                    stats.isAbstract,
                    stats.isInterface,
                    stats.isEnum,
                    stats.isAnnotation
            ));
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
            return new SourceStats(0, 0, 0, 0.0, 0, 0, false, false, false, false);
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

            return new SourceStats(
                    sizeLoc,
                    nom,
                    publicMethods,
                    avgMethodSize,
                    cycloComplexity,
                    fanOut,
                    isAbstract,
                    isInterface,
                    isEnum,
                    isAnnotation
            );

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

            return new SourceStats(
                    sizeLoc,
                    nom,
                    publicMethods,
                    avgMethodSize,
                    cycloComplexity,
                    fanOut,
                    isAbstract,
                    isInterface,
                    isEnum,
                    isAnnotation
            );
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

        boolean inBlockComment = false;
        boolean inLineComment = false;
        boolean inString = false;
        boolean inChar = false;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    result.append(current);
                }
                continue;
            }

            if (inBlockComment) {
                if (current == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }

            if (!inString && !inChar && current == '/' && next == '/') {
                inLineComment = true;
                i++;
                continue;
            }

            if (!inString && !inChar && current == '/' && next == '*') {
                inBlockComment = true;
                i++;
                continue;
            }

            if (!inChar && current == '"' && !isEscaped(source, i)) {
                inString = !inString;
            }

            if (!inString && current == '\'' && !isEscaped(source, i)) {
                inChar = !inChar;
            }

            result.append(current);
        }

        return result.toString();
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

        private SourceStats(
                int sizeLoc,
                int nom,
                int publicMethods,
                double avgMethodSize,
                int cycloComplexity,
                int fanOut,
                boolean isAbstract,
                boolean isInterface,
                boolean isEnum,
                boolean isAnnotation
        ) {
            this.sizeLoc = sizeLoc;
            this.nom = nom;
            this.publicMethods = publicMethods;
            this.avgMethodSize = avgMethodSize;
            this.cycloComplexity = cycloComplexity;
            this.fanOut = fanOut;
            this.isAbstract = isAbstract;
            this.isInterface = isInterface;
            this.isEnum = isEnum;
            this.isAnnotation = isAnnotation;
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
        System.out.println();
        System.out.println("Ranking Milestone 4 completato.");
        System.out.println("Release usata: "
                + latestRelease.getVersionName()
                + " | ID=" + latestRelease.getVersionId()
                + " | Index=" + latestRelease.getIndex());
        System.out.println("Classi accettate: " + accepted.size());
        System.out.println("Classi scartate: " + discarded.size());
        System.out.println("Output: " + outputDir);

        System.out.println();
        System.out.println("Top 10 classi accettate:");

        accepted.stream()
                .limit(10)
                .forEach(c -> System.out.println(
                        "- " + c.qualifiedName
                                + " | NSmells=" + c.nSmells
                                + " | LOC=" + c.sizeLoc
                                + " | NOM=" + c.nom
                                + " | PUBLIC=" + c.publicMethods
                                + " | CYCLO=" + c.cycloComplexity
                ));

        System.out.println();
        System.out.println("Classi selezionate:");

        for (RankedClass c : selected) {
            System.out.println(
                    "- originalRank=" + c.originalRank
                            + " | " + c.qualifiedName
                            + " | NSmells=" + c.nSmells
                            + " | LOC=" + c.sizeLoc
                            + " | NOM=" + c.nom
                            + " | PUBLIC=" + c.publicMethods
            );
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

        private RankedClass(
                String releaseId,
                String releaseName,
                String classPath,
                String qualifiedName,
                int nSmells,
                int sizeLoc,
                int nom,
                int publicMethods,
                double avgMethodSize,
                int cycloComplexity,
                int fanOut,
                double smellDensity,
                boolean isAbstract,
                boolean isInterface,
                boolean isEnum,
                boolean isAnnotation
        ) {
            this.releaseId = releaseId;
            this.releaseName = releaseName;
            this.classPath = classPath;
            this.qualifiedName = qualifiedName;
            this.nSmells = nSmells;
            this.sizeLoc = sizeLoc;
            this.nom = nom;
            this.publicMethods = publicMethods;
            this.avgMethodSize = avgMethodSize;
            this.cycloComplexity = cycloComplexity;
            this.fanOut = fanOut;
            this.smellDensity = smellDensity;
            this.isAbstract = isAbstract;
            this.isInterface = isInterface;
            this.isEnum = isEnum;
            this.isAnnotation = isAnnotation;
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