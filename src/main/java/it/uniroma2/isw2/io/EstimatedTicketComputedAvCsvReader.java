package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.TicketComputedAv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EstimatedTicketComputedAvCsvReader {

    private EstimatedTicketComputedAvCsvReader() {
    }

    public static List<TicketComputedAv> loadTicketsWithComputedAv(String filePath) throws IOException {
        List<TicketComputedAv> tickets = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return tickets;
            }

            List<String> headers = CsvUtils.parseCsvLine(headerLine);

            int ticketIdIndex = findColumnIndex(headers, "TicketID", "ticketId", "id");
            int computedAvIndex = findColumnIndex(headers, "ComputedAV", "computedAv");

            String line;
            while ((line = br.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                String ticketId = getField(fields, ticketIdIndex);
                String computedAv = getField(fields, computedAvIndex);

                if (ticketId.isBlank()) {
                    continue;
                }

                tickets.add(new TicketComputedAv(ticketId, computedAv));
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