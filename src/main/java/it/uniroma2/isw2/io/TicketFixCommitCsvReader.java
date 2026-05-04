package it.uniroma2.isw2.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Legge dal CSV gli hash dei fix commit già trovati nella pipeline.
 */
public class TicketFixCommitCsvReader {

    private static final int FIX_COMMIT_HASH_INDEX = 1;

    private TicketFixCommitCsvReader() {
    }

    public static Set<String> loadFixCommitHashes(String filePath) throws IOException {
        Set<String> fixCommitHashes = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String header = reader.readLine();

            if (header == null) {
                throw new IOException("Il file dei fix commit è vuoto: " + filePath);
            }

            String line;

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= FIX_COMMIT_HASH_INDEX) {
                    continue;
                }

                String fixCommitHash = CsvUtils.removeQuotes(fields.get(FIX_COMMIT_HASH_INDEX));

                if (!fixCommitHash.isBlank()) {
                    fixCommitHashes.add(fixCommitHash.trim());
                }
            }
        }

        return fixCommitHashes;
    }
}