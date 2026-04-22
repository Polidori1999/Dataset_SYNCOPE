package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.Ticket;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TicketCsvReader {

    private TicketCsvReader() {
    }

    public static List<Ticket> loadTickets(String filePath) throws IOException {
        List<Ticket> tickets = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return tickets;
            }

            List<String> headers = CsvUtils.parseCsvLine(headerLine);

            int ticketIdIndex = findColumnIndex(headers, "TicketID", "ticketId", "id");
            int creationDateIndex = findColumnIndex(headers, "CreationDate", "creationDate");
            int resolutionDateIndex = findColumnIndex(headers, "ResolutionDate", "resolutionDate");
            int affectedVersionsIndex = findColumnIndex(headers, "AV", "AffectedVersions", "affectedVersions");

            String line;
            while ((line = br.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                String ticketId = getField(fields, ticketIdIndex);
                String creationDate = getField(fields, creationDateIndex);
                String resolutionDate = getField(fields, resolutionDateIndex);
                String affectedVersions = getField(fields, affectedVersionsIndex);

                if (ticketId.isBlank() || creationDate.isBlank()) {
                    continue;
                }

                tickets.add(new Ticket(
                        ticketId,
                        creationDate,
                        resolutionDate,
                        affectedVersions
                ));
            }
        }

        return tickets;
    }

    private static int findColumnIndex(List<String> headers, String... candidates) {
        for (int i = 0; i < headers.size(); i++) {
            String normalizedHeader = CsvUtils.removeQuotes(headers.get(i)).trim();

            for (String candidate : candidates) {
                if (normalizedHeader.equalsIgnoreCase(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String getField(List<String> fields, int index) {
        if (index < 0 || index >= fields.size()) {
            return "";
        }
        return CsvUtils.removeQuotes(fields.get(index)).trim();
    }
}