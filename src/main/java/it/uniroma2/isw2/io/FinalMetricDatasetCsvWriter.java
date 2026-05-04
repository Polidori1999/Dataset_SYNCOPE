package it.uniroma2.isw2.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea il dataset finale unendo:
 * - metriche classe-release;
 * - label buggy yes/no.
 *
 * La chiave di merge è ReleaseID + ClassPath.
 */
public class FinalMetricDatasetCsvWriter {

    private static final int METRIC_RELEASE_ID_INDEX = 1;
    private static final int METRIC_CLASS_PATH_INDEX = 2;

    private static final int LABEL_CLASS_PATH_INDEX = 1;
    private static final int LABEL_RELEASE_ID_INDEX = 2;
    private static final int LABEL_BUGGINESS_INDEX = 5;

    private FinalMetricDatasetCsvWriter() {
    }

    public static void writeFinalMetricDataset(String metricsFilePath,
                                               String labelsFilePath,
                                               String outputFilePath)
            throws IOException {
        Map<String, String> bugginessByClassRelease =
                loadBugginessByClassRelease(labelsFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(metricsFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String metricsHeader = reader.readLine();

            if (metricsHeader == null) {
                throw new IOException("File metriche vuoto: " + metricsFilePath);
            }

            writer.append(metricsHeader)
                    .append(",Bugginess")
                    .append("\n");

            String line;
            int writtenRows = 0;
            int missingLabels = 0;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= METRIC_CLASS_PATH_INDEX) {
                    continue;
                }

                String releaseId = CsvUtils.removeQuotes(fields.get(METRIC_RELEASE_ID_INDEX));
                String classPath = normalizePath(
                        CsvUtils.removeQuotes(fields.get(METRIC_CLASS_PATH_INDEX))
                );

                String key = buildKey(releaseId, classPath);
                String bugginess = bugginessByClassRelease.get(key);

                if (bugginess == null) {
                    missingLabels++;
                    continue;
                }

                writer.append(line)
                        .append(",")
                        .append(CsvUtils.escapeCsv(bugginess))
                        .append("\n");

                writtenRows++;
            }

            if (missingLabels > 0) {
                throw new IOException("Merge metriche-label non coerente. Righe metriche senza label: "
                        + missingLabels);
            }

            System.out.println("Righe scritte nel dataset finale: " + writtenRows);
        }
    }

    private static Map<String, String> loadBugginessByClassRelease(String labelsFilePath)
            throws IOException {
        Map<String, String> result = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(labelsFilePath))) {
            String header = reader.readLine();

            if (header == null) {
                throw new IOException("File label vuoto: " + labelsFilePath);
            }

            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= LABEL_BUGGINESS_INDEX) {
                    continue;
                }

                String classPath = normalizePath(
                        CsvUtils.removeQuotes(fields.get(LABEL_CLASS_PATH_INDEX))
                );
                String releaseId = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_ID_INDEX));
                String bugginess = CsvUtils.removeQuotes(fields.get(LABEL_BUGGINESS_INDEX));

                String key = buildKey(releaseId, classPath);

                if (result.putIfAbsent(key, bugginess) != null) {
                    throw new IOException("Duplicato nel file delle label per la chiave: " + key);
                }
            }
        }

        return result;
    }

    private static String buildKey(String releaseId, String classPath) {
        return releaseId + "||" + normalizePath(classPath);
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        return path.replace("\\", "/").trim();
    }
}