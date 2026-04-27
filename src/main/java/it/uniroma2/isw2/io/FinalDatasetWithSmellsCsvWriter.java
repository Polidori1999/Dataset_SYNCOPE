package it.uniroma2.isw2.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unisce il dataset classe-release con il CSV degli smell PMD.
 * La chiave di merge è ClassPath + ReleaseID.
 */
public class FinalDatasetWithSmellsCsvWriter {

    private static final int LABEL_PROJECT_INDEX = 0;
    private static final int LABEL_CLASS_PATH_INDEX = 1;
    private static final int LABEL_RELEASE_ID_INDEX = 2;
    private static final int LABEL_RELEASE_NAME_INDEX = 3;
    private static final int LABEL_RELEASE_INDEX_INDEX = 4;
    private static final int LABEL_BUGGINESS_INDEX = 5;

    private static final int SMELL_RELEASE_ID_INDEX = 1;
    private static final int SMELL_CLASS_PATH_INDEX = 2;
    private static final int SMELL_NSMELLS_INDEX = 3;

    private FinalDatasetWithSmellsCsvWriter() {
    }

    public static void writeFinalDatasetWithSmells(String labelFilePath,
                                                   String smellsFilePath,
                                                   String outputFilePath) throws IOException {
        Map<String, Integer> smellsByClassRelease = loadSmells(smellsFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(labelFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Il file delle label è vuoto: " + labelFilePath);
            }

            writer.append("Project,ClassPath,ReleaseID,ReleaseName,ReleaseIndex,NSmells,Bugginess\n");

            String line;
            int rows = 0;
            int missingSmells = 0;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= LABEL_BUGGINESS_INDEX) {
                    continue;
                }

                String project = CsvUtils.removeQuotes(fields.get(LABEL_PROJECT_INDEX));
                String classPath = normalizePath(CsvUtils.removeQuotes(fields.get(LABEL_CLASS_PATH_INDEX)));
                String releaseId = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_ID_INDEX));
                String releaseName = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_NAME_INDEX));
                String releaseIndex = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_INDEX_INDEX));
                String bugginess = CsvUtils.removeQuotes(fields.get(LABEL_BUGGINESS_INDEX));

                String key = buildKey(classPath, releaseId);
                Integer nSmells = smellsByClassRelease.get(key);

                if (nSmells == null) {
                    missingSmells++;
                    nSmells = 0;
                }

                writer.append(CsvUtils.escapeCsv(project)).append(",");
                writer.append(CsvUtils.escapeCsv(classPath)).append(",");
                writer.append(CsvUtils.escapeCsv(releaseId)).append(",");
                writer.append(CsvUtils.escapeCsv(releaseName)).append(",");
                writer.append(CsvUtils.escapeCsv(releaseIndex)).append(",");
                writer.append(String.valueOf(nSmells)).append(",");
                writer.append(CsvUtils.escapeCsv(bugginess)).append("\n");

                rows++;
            }

            if (missingSmells > 0) {
                throw new IOException("Merge non coerente: " + missingSmells
                        + " righe del dataset finale non hanno un valore NSmells.");
            }

            System.out.println("Righe scritte nel dataset con smell: " + rows);
        }
    }

    private static Map<String, Integer> loadSmells(String smellsFilePath) throws IOException {
        Map<String, Integer> smellsByClassRelease = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(smellsFilePath))) {
            String header = reader.readLine();
            if (header == null) {
                throw new IOException("Il file degli smell è vuoto: " + smellsFilePath);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= SMELL_NSMELLS_INDEX) {
                    continue;
                }

                String releaseId = CsvUtils.removeQuotes(fields.get(SMELL_RELEASE_ID_INDEX));
                String classPath = normalizePath(CsvUtils.removeQuotes(fields.get(SMELL_CLASS_PATH_INDEX)));
                String nSmellsValue = CsvUtils.removeQuotes(fields.get(SMELL_NSMELLS_INDEX));

                int nSmells;
                try {
                    nSmells = Integer.parseInt(nSmellsValue);
                } catch (NumberFormatException e) {
                    throw new IOException("Valore NSmells non valido: " + nSmellsValue, e);
                }

                String key = buildKey(classPath, releaseId);

                if (smellsByClassRelease.putIfAbsent(key, nSmells) != null) {
                    throw new IOException("Duplicato nel file degli smell per la chiave: " + key);
                }
            }
        }

        return smellsByClassRelease;
    }

    private static String buildKey(String classPath, String releaseId) {
        return normalizePath(classPath) + "|" + releaseId;
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}