package it.uniroma2.isw2.io;

import it.uniroma2.isw2.smell.PmdAnalysisError;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Scrive i file Java che PMD non è riuscito ad analizzare.
 */
public class PmdAnalysisErrorCsvWriter {

    private PmdAnalysisErrorCsvWriter() {
    }

    public static void writePmdAnalysisErrors(String filePath,
                                              List<PmdAnalysisError> errors) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,ReleaseID,ClassPath,ErrorMessage\n");

            for (PmdAnalysisError error : errors) {
                writer.append(CsvUtils.escapeCsv(error.getProject())).append(",");
                writer.append(CsvUtils.escapeCsv(error.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(error.getClassPath())).append(",");
                writer.append(CsvUtils.escapeCsv(error.getErrorMessage())).append("\n");
            }
        }
    }
}