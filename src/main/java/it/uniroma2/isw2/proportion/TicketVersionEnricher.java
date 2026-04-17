package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.Ticket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe che arricchisce i ticket con OV e FV.
 * AV viene mantenuta dal CSV originale.
 * IV per ora viene lasciata vuota.
 */
public class TicketVersionEnricher {

    private TicketVersionEnricher() {
    }

    public static List<EnhancedTicket> enrichTickets(List<Ticket> tickets, List<Release> releases) {
        List<EnhancedTicket> enhancedTickets = new ArrayList<>();

        for (Ticket ticket : tickets) {
            String openingVersion = findOpeningVersion(ticket, releases);
            String fixedVersion = findFixedVersion(ticket, releases);

            EnhancedTicket enhancedTicket = new EnhancedTicket(
                    ticket.getTicketId(),
                    ticket.getCreationDate(),
                    ticket.getResolutionDate(),
                    ticket.getAffectedVersions(),
                    openingVersion,
                    fixedVersion,
                    "",
                    "NONE"
            );

            enhancedTickets.add(enhancedTicket);
        }

        return enhancedTickets;
    }

    private static String findOpeningVersion(Ticket ticket, List<Release> releases) {
        LocalDateTime creationDate = DateUtils.parseTicketDate(ticket.getCreationDate());

        if (creationDate == null) {
            return "";
        }

        for (Release release : releases) {
            LocalDateTime releaseDate = DateUtils.parseReleaseDate(release.getDate());
            if (!releaseDate.isBefore(creationDate)) {
                return release.getVersionName();
            }
        }

        return "";
    }

    private static String findFixedVersion(Ticket ticket, List<Release> releases) {
        LocalDateTime resolutionDate = DateUtils.parseTicketDate(ticket.getResolutionDate());

        if (resolutionDate == null) {
            return "";
        }

        for (Release release : releases) {
            LocalDateTime releaseDate = DateUtils.parseReleaseDate(release.getDate());
            if (!releaseDate.isBefore(resolutionDate)) {
                return release.getVersionName();
            }
        }

        return "";
    }
}