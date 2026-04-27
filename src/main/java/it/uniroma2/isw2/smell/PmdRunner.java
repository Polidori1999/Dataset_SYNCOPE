package it.uniroma2.isw2.smell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Esegue PMD da Java e produce due report:
 * - CSV, usato per contare le violation;
 * - XML, usato per tracciare eventuali file non analizzabili.
 */
public class PmdRunner {

    private final String pmdExecutable;
    private final String ruleset;
    private final String javaVersion;
    private final Path reportsDir;

    public PmdRunner(String pmdExecutable,
                     String ruleset,
                     String javaVersion,
                     Path reportsDir) {
        this.pmdExecutable = pmdExecutable;
        this.ruleset = ruleset;
        this.javaVersion = javaVersion;
        this.reportsDir = reportsDir;
    }

    public PmdRunResult run(String releaseId, Path fileListPath)
            throws IOException, InterruptedException {
        Files.createDirectories(reportsDir);

        String safeReleaseId = sanitizeFileName(releaseId);

        Path csvReportPath = reportsDir.resolve("release-" + safeReleaseId + "-pmd.csv");
        Path xmlReportPath = reportsDir.resolve("release-" + safeReleaseId + "-pmd.xml");

        runPmd(fileListPath, csvReportPath, "csv");
        runPmd(fileListPath, xmlReportPath, "xml");

        return new PmdRunResult(csvReportPath, xmlReportPath);
    }

    private void runPmd(Path fileListPath, Path reportPath, String format)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();

        command.add(pmdExecutable);
        command.add("check");
        command.add("--file-list");
        command.add(fileListPath.toString());
        command.add("-R");
        command.add(ruleset);
        command.add("-f");
        command.add(format);
        command.add("-r");
        command.add(reportPath.toString());
        command.add("--use-version");
        command.add(javaVersion);
        command.add("--no-cache");
        command.add("--no-fail-on-violation");
        command.add("--no-fail-on-error");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> outputLines = readProcessOutput(process);

        int exitCode = process.waitFor();

        /*
         * PMD può segnalare errori di analisi nel report XML pur restituendo exit code 0,
         * grazie a --no-fail-on-error. Se l'exit code è diverso da 0, invece,
         * è un errore di esecuzione del comando.
         */
        if (exitCode != 0) {
            throw new IOException("Esecuzione PMD fallita con exit code "
                    + exitCode + ". Output: " + String.join("\n", outputLines));
        }
    }

    private List<String> readProcessOutput(Process process) throws IOException {
        List<String> outputLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;

            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        return outputLines;
    }

    private String sanitizeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}