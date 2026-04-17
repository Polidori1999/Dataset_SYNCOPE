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
            String line = br.readLine();

            if (line == null) {
                return tickets;
            }

            while ((line = br.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() < 4) {
                    continue;
                }

                String ticketId = CsvUtils.removeQuotes(fields.get(0));
                String creationDate = CsvUtils.removeQuotes(fields.get(1));
                String resolutionDate = CsvUtils.removeQuotes(fields.get(2));
                String affectedVersions = CsvUtils.removeQuotes(fields.get(3));

                tickets.add(new Ticket(ticketId, creationDate, resolutionDate, affectedVersions));
            }
        }

        return tickets;
    }
}