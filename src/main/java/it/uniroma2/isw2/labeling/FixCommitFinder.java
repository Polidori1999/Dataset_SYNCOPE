package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.TicketFixCommit;
import it.uniroma2.isw2.proportion.EnhancedTicket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FixCommitFinder {

    private FixCommitFinder() {
    }

    public static List<TicketFixCommit> findFixCommits(List<EnhancedTicket> tickets,
                                                       String repositoryPath) throws IOException {
        List<TicketFixCommit> result = new ArrayList<>();

        if (tickets == null || tickets.isEmpty()) {
            return result;
        }

        Set<String> validTicketIds = new HashSet<>();
        for (EnhancedTicket ticket : tickets) {
            if (ticket.getTicketId() != null && !ticket.getTicketId().isBlank()) {
                validTicketIds.add(ticket.getTicketId().trim().toUpperCase(Locale.ROOT));
            }
        }

        /*
         * Pattern generico per ticket tipo SYNCOPE-123, OPENJPA-456, ecc.
         * Poi filtriamo solo quelli presenti davvero nei ticket del dataset.
         */
        Pattern ticketPattern = Pattern.compile("\\b[A-Z][A-Z0-9]+-\\d+\\b", Pattern.CASE_INSENSITIVE);

        Map<String, TicketFixCommit> unique = new LinkedHashMap<>();

        try {
            /*
             * Un solo passaggio su tutto il log Git.
             * Molto più veloce di un git log separato per ogni ticket.
             */
            List<String> lines = GitCommandRunner.runCommand(
                    repositoryPath,
                    "git",
                    "log",
                    "--all",
                    "--regexp-ignore-case",
                    "--format=%H\t%ct\t%s"
            );

            for (String line : lines) {
                String[] parts = line.split("\t", 3);
                if (parts.length < 3) {
                    continue;
                }

                String commitHash = parts[0].trim();
                String epochString = parts[1].trim();
                String subject = parts[2];

                if (commitHash.isBlank() || epochString.isBlank()) {
                    continue;
                }

                long epochSeconds;
                try {
                    epochSeconds = Long.parseLong(epochString);
                } catch (NumberFormatException e) {
                    continue;
                }

                Matcher matcher = ticketPattern.matcher(subject);
                while (matcher.find()) {
                    String ticketId = matcher.group().toUpperCase(Locale.ROOT);

                    if (!validTicketIds.contains(ticketId)) {
                        continue;
                    }

                    String key = ticketId + "|" + commitHash;
                    unique.putIfAbsent(
                            key,
                            new TicketFixCommit(ticketId, commitHash, epochSeconds)
                    );
                }
            }

            result.addAll(unique.values());
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interruzione durante la ricerca dei fix commit.", e);
        }
    }
}