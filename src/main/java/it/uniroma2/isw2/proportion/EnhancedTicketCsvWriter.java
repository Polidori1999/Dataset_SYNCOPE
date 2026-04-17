package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.io.CsvUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Classe per la scrittura dei ticket arricchiti in formato CSV.
 */
public class EnhancedTicketCsvWriter {

    private EnhancedTicketCsvWriter() {
    }

    public static void writeEnhancedTickets(String filePath, List<EnhancedTicket> tickets) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("TicketID,CreationDate,ResolutionDate,AV,OV,FV,IV,IVSource\n");

            for (EnhancedTicket ticket : tickets) {
                writer.append(CsvUtils.escapeCsv(ticket.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getCreationDate())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getResolutionDate())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getAffectedVersions())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getOpeningVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getFixedVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getInjectedVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getInjectedVersionSource())).append("\n");
            }
        }
    }
}