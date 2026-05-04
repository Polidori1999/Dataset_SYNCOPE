package it.uniroma2.isw2.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Crea il dataset finale nel formato dell'esempio:
 *
 * Version,File Name,Method Name,<metriche>,Buggy
 *
 * Le metriche vengono lette dal file delle metriche classe-release.
 * Version e Buggy vengono recuperati dal file delle label classe-release.
 *
 * La chiave di merge è ReleaseID + ClassPath.
 */
public class FinalMetricDatasetCsvWriter {

    private static final int LABEL_CLASS_PATH_INDEX = 1;
    private static final int LABEL_RELEASE_ID_INDEX = 2;
    private static final int LABEL_RELEASE_INDEX_INDEX = 4;
    private static final int LABEL_BUGGINESS_INDEX = 5;

    private FinalMetricDatasetCsvWriter() {
    }

    public static void writeFinalMetricDataset(String metricsFilePath,
                                               String labelsFilePath,
                                               String outputFilePath)
            throws IOException {
        Map<String, LabelInfo> labelInfoByClassRelease =
                loadLabelInfoByClassRelease(labelsFilePath);

        try (BufferedReader reader = new BufferedReader(new FileReader(metricsFilePath));
             FileWriter writer = new FileWriter(outputFilePath)) {

            String metricsHeaderLine = reader.readLine();

            if (metricsHeaderLine == null) {
                throw new IOException("File metriche vuoto: " + metricsFilePath);
            }

            List<String> metricsHeader = CsvUtils.parseCsvLine(metricsHeaderLine);

            int releaseIdIndex = findColumnIndex(metricsHeader, "ReleaseID");
            int classPathIndex = findColumnIndex(metricsHeader, "ClassPath");

            if (releaseIdIndex < 0 || classPathIndex < 0) {
                throw new IOException("Il file metriche deve contenere ReleaseID e ClassPath.");
            }

            List<Integer> metricColumnIndexes = new ArrayList<>();
            List<String> metricColumnNames = new ArrayList<>();

            for (int i = 0; i < metricsHeader.size(); i++) {
                String columnName = CsvUtils.removeQuotes(metricsHeader.get(i)).trim();

                if (isTechnicalColumn(columnName)) {
                    continue;
                }

                metricColumnIndexes.add(i);
                metricColumnNames.add(columnName);
            }

            writeHeader(writer, metricColumnNames);

            String line;
            int writtenRows = 0;
            int missingLabels = 0;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= Math.max(releaseIdIndex, classPathIndex)) {
                    continue;
                }

                String releaseId = CsvUtils.removeQuotes(fields.get(releaseIdIndex)).trim();
                String classPath = normalizePath(
                        CsvUtils.removeQuotes(fields.get(classPathIndex))
                );

                String key = buildKey(releaseId, classPath);
                LabelInfo labelInfo = labelInfoByClassRelease.get(key);

                if (labelInfo == null) {
                    missingLabels++;
                    continue;
                }

                writer.append(CsvUtils.escapeCsv(labelInfo.releaseIndex)).append(",");
                writer.append(CsvUtils.escapeCsv(classPath)).append(",");

                for (int i = 0; i < metricColumnIndexes.size(); i++) {
                    int columnIndex = metricColumnIndexes.get(i);

                    String value = "";
                    if (columnIndex < fields.size()) {
                        value = CsvUtils.removeQuotes(fields.get(columnIndex));
                    }

                    writer.append(CsvUtils.escapeCsv(value)).append(",");
                }

                writer.append(CsvUtils.escapeCsv(labelInfo.bugginess)).append("\n");
                writtenRows++;
            }

            if (missingLabels > 0) {
                throw new IOException("Merge metriche-label non coerente. Righe metriche senza label: "
                        + missingLabels);
            }

            System.out.println("Righe scritte nel dataset finale formato esempio: " + writtenRows);
        }
    }

    private static void writeHeader(FileWriter writer,
                                    List<String> metricColumnNames)
            throws IOException {
        writer.append("Version,");
        writer.append("File Name,");

        for (String columnName : metricColumnNames) {
            writer.append(CsvUtils.escapeCsv(columnName)).append(",");
        }

        writer.append("Buggy\n");
    }

    private static Map<String, LabelInfo> loadLabelInfoByClassRelease(String labelsFilePath)
            throws IOException {
        Map<String, LabelInfo> result = new HashMap<>();

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
                String releaseId = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_ID_INDEX)).trim();
                String releaseIndex = CsvUtils.removeQuotes(fields.get(LABEL_RELEASE_INDEX_INDEX)).trim();
                String bugginess = CsvUtils.removeQuotes(fields.get(LABEL_BUGGINESS_INDEX)).trim();

                String key = buildKey(releaseId, classPath);
                LabelInfo labelInfo = new LabelInfo(releaseIndex, bugginess);

                if (result.putIfAbsent(key, labelInfo) != null) {
                    throw new IOException("Duplicato nel file label per la chiave: " + key);
                }
            }
        }

        return result;
    }

    private static boolean isTechnicalColumn(String columnName) {
        return "Project".equalsIgnoreCase(columnName)
                || "ReleaseID".equalsIgnoreCase(columnName)
                || "ClassPath".equalsIgnoreCase(columnName)
                || "Bugginess".equalsIgnoreCase(columnName)
                || "Buggy".equalsIgnoreCase(columnName);
    }

    private static int findColumnIndex(List<String> header, String targetColumn) {
        for (int i = 0; i < header.size(); i++) {
            String columnName = CsvUtils.removeQuotes(header.get(i)).trim();

            if (targetColumn.equalsIgnoreCase(columnName)) {
                return i;
            }
        }

        return -1;
    }

    private static String buildKey(String releaseId, String classPath) {
        return releaseId.trim() + "||" + normalizePath(classPath);
    }

    private static String normalizePath(String path) {
        if (path == null) {
            return "";
        }

        return path.replace("\\", "/").trim();
    }

    private static class LabelInfo {

        private final String releaseIndex;
        private final String bugginess;

        private LabelInfo(String releaseIndex,
                          String bugginess) {
            this.releaseIndex = releaseIndex;
            this.bugginess = bugginess;
        }
    }
}