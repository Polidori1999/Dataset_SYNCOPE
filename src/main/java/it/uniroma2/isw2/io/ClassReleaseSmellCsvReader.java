package it.uniroma2.isw2.io;

import it.uniroma2.isw2.metric.MetricService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legge dal CSV gli smell PMD calcolati per ogni coppia classe-release.
 */
public class ClassReleaseSmellCsvReader {

    private static final int RELEASE_ID_INDEX = 1;
    private static final int CLASS_PATH_INDEX = 2;
    private static final int NSMELLS_INDEX = 3;

    private ClassReleaseSmellCsvReader() {
    }

    public static Map<String, Integer> loadSmellsByClassRelease(String filePath)
            throws IOException {
        Map<String, Integer> smellsByClassRelease = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();

            if (header == null) {
                throw new IOException("Il file degli smell è vuoto: " + filePath);
            }

            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= NSMELLS_INDEX) {
                    continue;
                }

                String releaseId = CsvUtils.removeQuotes(fields.get(RELEASE_ID_INDEX));
                String classPath = normalizePath(CsvUtils.removeQuotes(fields.get(CLASS_PATH_INDEX)));
                String nSmellsValue = CsvUtils.removeQuotes(fields.get(NSMELLS_INDEX));

                int nSmells;

                try {
                    nSmells = Integer.parseInt(nSmellsValue);
                } catch (NumberFormatException e) {
                    throw new IOException("Valore NSmells non valido: " + nSmellsValue, e);
                }

                String key = MetricService.buildMetricKey(releaseId, classPath);
                smellsByClassRelease.put(key, nSmells);
            }
        }

        return smellsByClassRelease;
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}