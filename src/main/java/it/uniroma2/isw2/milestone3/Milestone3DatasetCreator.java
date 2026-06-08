package it.uniroma2.isw2.milestone3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Milestone3DatasetCreator {
    private static final Logger LOGGER =
            Logger.getLogger(Milestone3DatasetCreator.class.getName());

    private static final Path INPUT_CSV = Path.of(
            "/home/leonardo/uni/isw2/Dataset_SYNCOPE/dataset_SYNCOPE.csv"
    );

    private static final String NSMELLS_COLUMN = "NSMELLS";
    private static final String SMELL_DENSITY_COLUMN = "SMELL_DENSITY";
    private static final String BUGGY_COLUMN = "Buggy";

    public static void main(String[] args) throws IOException {
        Path inputCsv = args.length > 0 ? Path.of(args[0]) : INPUT_CSV;
        Path outputDir = inputCsv.getParent().resolve("milestone3");
        Files.createDirectories(outputDir);

        CsvTable table = readCsv(inputCsv);

        int nSmellsIndex = findColumnIndex(table.header(), NSMELLS_COLUMN);
        int smellDensityIndex = findColumnIndex(table.header(), SMELL_DENSITY_COLUMN);
        int buggyIndex = findColumnIndex(table.header(), BUGGY_COLUMN);

        List<List<String>> datasetA = new ArrayList<>(table.rows());
        List<List<String>> datasetBPlus = new ArrayList<>();
        List<List<String>> datasetB = new ArrayList<>();
        List<List<String>> datasetC = new ArrayList<>();

        for (int i = 0; i < table.rows().size(); i++) {
            List<String> row = table.rows().get(i);
            double nSmells = parseDouble(row.get(nSmellsIndex), i + 2, NSMELLS_COLUMN);

            if (nSmells > 0) {
                datasetBPlus.add(row);

                List<String> syntheticRow = new ArrayList<>(row);
                syntheticRow.set(nSmellsIndex, "0");
                syntheticRow.set(smellDensityIndex, "0");
                datasetB.add(syntheticRow);
            } else if (nSmells == 0) {
                datasetC.add(row);
            } else {
                throw new IllegalArgumentException("Valore NSMELLS negativo alla riga " + (i + 2));
            }
        }

        writeCsv(outputDir.resolve("A_original.csv"), table.header(), datasetA, table.delimiter());
        writeCsv(outputDir.resolve("B_plus_smelly.csv"), table.header(), datasetBPlus, table.delimiter());
        writeCsv(outputDir.resolve("B_synthetic_zero_smells.csv"), table.header(), datasetB, table.delimiter());
        writeCsv(outputDir.resolve("C_no_smells.csv"), table.header(), datasetC, table.delimiter());

        writeSummary(
                outputDir.resolve("summary_milestone3.csv"),
                datasetA,
                datasetBPlus,
                datasetB,
                datasetC,
                nSmellsIndex,
                buggyIndex
        );

        LOGGER.info("Dataset Milestone 3 creati correttamente.");
        LOGGER.log(Level.INFO, "Input: {0}", inputCsv.toAbsolutePath());
        LOGGER.log(Level.INFO, "Cartella output: {0}", outputDir.toAbsolutePath());

        LOGGER.log(Level.INFO, "A_original.csv: {0} righe", datasetA.size());
        LOGGER.log(Level.INFO, "B_plus_smelly.csv: {0} righe", datasetBPlus.size());
        LOGGER.log(Level.INFO, "B_synthetic_zero_smells.csv: {0} righe", datasetB.size());
        LOGGER.log(Level.INFO, "C_no_smells.csv: {0} righe", datasetC.size());
    }

    private static CsvTable readCsv(Path inputCsv) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(inputCsv, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(removeBom(line));
                }
            }
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Il CSV è vuoto.");
        }

        char delimiter = detectDelimiter(lines.get(0));

        List<String> header = parseCsvLine(lines.get(0), delimiter);
        List<List<String>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            List<String> row = parseCsvLine(lines.get(i), delimiter);

            while (row.size() < header.size()) {
                row.add("");
            }

            if (row.size() > header.size()) {
                throw new IllegalArgumentException("La riga " + (i + 1) + " contiene più colonne dell'header.");
            }

            rows.add(row);
        }

        return new CsvTable(header, rows, delimiter);
    }

    private static String removeBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    private static char detectDelimiter(String headerLine) {
        int commas = countCharOutsideQuotes(headerLine, ',');
        int semicolons = countCharOutsideQuotes(headerLine, ';');
        int tabs = countCharOutsideQuotes(headerLine, '\t');

        if (semicolons > commas && semicolons > tabs) {
            return ';';
        }

        if (tabs > commas && tabs > semicolons) {
            return '\t';
        }

        return ',';
    }

    private static int countCharOutsideQuotes(String line, char target) {
        boolean inQuotes = false;
        int count = 0;
        int i = 0;

        while (i < line.length()) {
            char current = line.charAt(i);

            if (current == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i += 2;
                } else {
                    inQuotes = !inQuotes;
                    i++;
                }
            } else {
                if (current == target && !inQuotes) {
                    count++;
                }
                i++;
            }
        }

        return count;
    }

    private static List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char current = line.charAt(i);

            if (current == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i += 2;
                } else {
                    inQuotes = !inQuotes;
                    i++;
                }
            } else if (current == delimiter && !inQuotes) {
                values.add(currentValue.toString());
                currentValue.setLength(0);
                i++;
            } else {
                currentValue.append(current);
                i++;
            }
        }

        values.add(currentValue.toString());
        return values;
    }

    private static void writeCsv(
            Path outputPath,
            List<String> header,
            List<List<String>> rows,
            char delimiter
    ) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(toCsvLine(header, delimiter));
            writer.newLine();

            for (List<String> row : rows) {
                writer.write(toCsvLine(row, delimiter));
                writer.newLine();
            }
        }
    }

    private static String toCsvLine(List<String> values, char delimiter) {
        List<String> escapedValues = new ArrayList<>();

        for (String value : values) {
            escapedValues.add(escapeCsvValue(value, delimiter));
        }

        return String.join(String.valueOf(delimiter), escapedValues);
    }

    private static String escapeCsvValue(String value, char delimiter) {
        if (value == null) {
            return "";
        }

        boolean mustQuote = value.indexOf(delimiter) >= 0
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        if (mustQuote) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }

    private static int findColumnIndex(List<String> header, String columnName) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "Colonna non trovata: " + columnName + ". Colonne disponibili: " + header
        );
    }

    private static double parseDouble(String value, int rowNumber, String columnName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Valore vuoto nella colonna " + columnName + " alla riga " + rowNumber
            );
        }

        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Valore non numerico nella colonna " + columnName + " alla riga " + rowNumber + ": " + value,
                    exception
            );
        }
    }

    private static void writeSummary(
            Path outputPath,
            List<List<String>> datasetA,
            List<List<String>> datasetBPlus,
            List<List<String>> datasetB,
            List<List<String>> datasetC,
            int nSmellsIndex,
            int buggyIndex
    ) throws IOException {
        List<String> header = List.of(
                "Dataset",
                "Rows",
                "RowsWithNSMELLSGreaterThanZero",
                "RowsWithNSMELLSEqualToZero",
                "BuggyYes",
                "BuggyNo"
        );

        List<List<String>> rows = new ArrayList<>();
        rows.add(summaryRow("A_original", datasetA, nSmellsIndex, buggyIndex));
        rows.add(summaryRow("B_plus_smelly", datasetBPlus, nSmellsIndex, buggyIndex));
        rows.add(summaryRow("B_synthetic_zero_smells", datasetB, nSmellsIndex, buggyIndex));
        rows.add(summaryRow("C_no_smells", datasetC, nSmellsIndex, buggyIndex));

        writeCsv(outputPath, header, rows, ',');
    }

    private static List<String> summaryRow(
            String datasetName,
            List<List<String>> rows,
            int nSmellsIndex,
            int buggyIndex
    ) {
        int greaterThanZero = 0;
        int equalToZero = 0;
        int yes = 0;
        int no = 0;

        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            double nSmells = parseDouble(row.get(nSmellsIndex), i + 2, NSMELLS_COLUMN);

            if (nSmells > 0) {
                greaterThanZero++;
            } else if (nSmells == 0) {
                equalToZero++;
            }

            String buggy = row.get(buggyIndex).trim().toLowerCase();

            if (buggy.equals("yes")) {
                yes++;
            } else if (buggy.equals("no")) {
                no++;
            } else {
                throw new IllegalArgumentException(
                        "Valore non valido nella colonna " + BUGGY_COLUMN + ": " + row.get(buggyIndex)
                );
            }
        }

        return List.of(
                datasetName,
                String.valueOf(rows.size()),
                String.valueOf(greaterThanZero),
                String.valueOf(equalToZero),
                String.valueOf(yes),
                String.valueOf(no)
        );
    }

    private record CsvTable(List<String> header, List<List<String>> rows, char delimiter) {
    }
}