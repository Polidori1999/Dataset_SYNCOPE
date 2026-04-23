package it.uniroma2.isw2.io;

import it.uniroma2.isw2.proportion.ComputedAffectedVersionBuilder;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.proportion.EnhancedTicket;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Scrive un CSV uguale a EstimatedTickets ma con una colonna extra ComputedAV.
 */
public class EnhancedTicketComputedAvCsvWriter {

    private EnhancedTicketComputedAvCsvWriter() {
    }

    public static void writeEnhancedTicketsWithComputedAv(String filePath,
                                                          List<EnhancedTicket> tickets,
                                                          List<Release> releases) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("TicketID,CreationDate,ResolutionDate,AV,OV,FV,IV,IVSource,ComputedAV\n");

            for (EnhancedTicket ticket : tickets) {
                String computedAv = ComputedAffectedVersionBuilder.buildComputedAv(ticket, releases);

                writer.append(CsvUtils.escapeCsv(ticket.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getCreationDate())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getResolutionDate())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getAffectedVersions())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getOpeningVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getFixedVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getInjectedVersion())).append(",");
                writer.append(CsvUtils.escapeCsv(ticket.getInjectedVersionSource())).append(",");
                writer.append(CsvUtils.escapeCsv(computedAv)).append("\n");
            }
        }
    }
}