package it.uniroma2.isw2.labeling;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GitCommandRunner {

    private GitCommandRunner() {
    }

    public static List<String> runCommand(String repositoryPath, String... command)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(repositoryPath));
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> outputLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Comando Git fallito con exit code " + exitCode);
        }

        return outputLines;
    }
}