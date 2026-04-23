package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.TicketFixCommit;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

public class TicketFixCommitCsvWriter {

    private TicketFixCommitCsvWriter() {
    }

    public static void writeTicketFixCommits(String filePath,
                                             List<TicketFixCommit> ticketFixCommits) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("TicketID,FixCommitHash,CommitEpochSeconds,CommitInstant\n");

            for (TicketFixCommit item : ticketFixCommits) {
                writer.append(CsvUtils.escapeCsv(item.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(item.getFixCommitHash())).append(",");
                writer.append(String.valueOf(item.getCommitEpochSeconds())).append(",");
                writer.append(CsvUtils.escapeCsv(
                        Instant.ofEpochSecond(item.getCommitEpochSeconds()).toString()
                )).append("\n");
            }
        }
    }
}