package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.TicketReleaseMapping;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MappingCsvWriter {

    private MappingCsvWriter() {
    }

    public static void writeMappingsToCsv(String filePath, List<TicketReleaseMapping> mappings) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("TicketID,CreationDate,ResolutionDate,ReleaseIndex,ReleaseID,ReleaseName,ReleaseDate\n");

            for (TicketReleaseMapping mapping : mappings) {
                writer.append(CsvUtils.escapeCsv(mapping.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(mapping.getCreationDate())).append(",");
                writer.append(CsvUtils.escapeCsv(mapping.getResolutionDate())).append(",");
                writer.append(String.valueOf(mapping.getReleaseIndex())).append(",");
                writer.append(CsvUtils.escapeCsv(mapping.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(mapping.getReleaseName())).append(",");
                writer.append(CsvUtils.escapeCsv(mapping.getReleaseDate())).append("\n");
            }
        }
    }
}