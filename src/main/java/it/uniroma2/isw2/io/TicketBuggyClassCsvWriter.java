package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.TicketBuggyClass;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TicketBuggyClassCsvWriter {

    private TicketBuggyClassCsvWriter() {
    }

    public static void writeTicketBuggyClasses(String filePath,
                                               List<TicketBuggyClass> ticketBuggyClasses) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("TicketID,FixCommitHash,ClassPath\n");

            for (TicketBuggyClass item : ticketBuggyClasses) {
                writer.append(CsvUtils.escapeCsv(item.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(item.getFixCommitHash())).append(",");
                writer.append(CsvUtils.escapeCsv(item.getClassPath())).append("\n");
            }
        }
    }
}