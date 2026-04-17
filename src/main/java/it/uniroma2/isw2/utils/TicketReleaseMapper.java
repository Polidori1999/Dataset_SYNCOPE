package it.uniroma2.isw2.utils;

import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.Ticket;
import it.uniroma2.isw2.model.TicketReleaseMapping;

import java.util.ArrayList;
import java.util.List;

public class TicketReleaseMapper {

    private TicketReleaseMapper() {
    }

    public static List<TicketReleaseMapping> mapTicketsToSelectedReleases(List<Ticket> tickets, List<Release> selectedReleases) {
        List<TicketReleaseMapping> mappings = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isBlank()) {
                continue;
            }

            String[] affected = ticket.getAffectedVersions().split(";");

            for (String affectedVersion : affected) {
                String normalizedAffectedVersion = affectedVersion.trim();

                for (Release release : selectedReleases) {
                    if (release.getVersionName().equalsIgnoreCase(normalizedAffectedVersion)) {
                        mappings.add(new TicketReleaseMapping(
                                ticket.getTicketId(),
                                ticket.getCreationDate(),
                                ticket.getResolutionDate(),
                                release.getIndex(),
                                release.getVersionId(),
                                release.getVersionName(),
                                release.getDate()
                        ));
                    }
                }
            }
        }

        return mappings;
    }
}