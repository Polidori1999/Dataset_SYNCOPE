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
 *
 * Assunzione di progetto:
 * - il CSV dei ticket fornito da RetrieveTicketsID contiene solo CreationDate,
 *   ResolutionDate e AffectedVersions;
 * - non essendo disponibile la vera fix commit date, si assume
 *   ResolutionDate == fix commit date.
 *
 * Quindi:
 * - OV = release più recente già disponibile alla data di apertura del ticket
 * - FV = prima release successiva alla ResolutionDate,
 *        usata come proxy della fix commit date
 */
public class TicketVersionEnricher {

    private TicketVersionEnricher() {
    }

    public static List<EnhancedTicket> enrichTickets(List<Ticket> tickets, List<Release> releases) {
        List<EnhancedTicket> enhancedTickets = new ArrayList<>();

        for (Ticket ticket : tickets) {
            String openingVersion = findOpeningVersion(ticket, releases);
            String fixedVersion = findFixedVersionFromResolutionDate(ticket, releases);

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

        Release bestRelease = null;
        LocalDateTime bestDate = null;

        for (Release release : releases) {
            LocalDateTime releaseDate = DateUtils.parseReleaseDate(release.getDate());

            /*
             * OV = release più recente con data <= creationDate.
             * È la versione già disponibile quando il ticket viene aperto.
             */
            if (!releaseDate.isAfter(creationDate)) {
                if (bestRelease == null || releaseDate.isAfter(bestDate)) {
                    bestRelease = release;
                    bestDate = releaseDate;
                }
            }
        }

        return bestRelease == null ? "" : bestRelease.getVersionName();
    }

    private static String findFixedVersionFromResolutionDate(Ticket ticket, List<Release> releases) {
        LocalDateTime resolutionDate = DateUtils.parseTicketDate(ticket.getResolutionDate());

        if (resolutionDate == null) {
            return "";
        }

        Release bestRelease = null;
        LocalDateTime bestDate = null;

        for (Release release : releases) {
            LocalDateTime releaseDate = DateUtils.parseReleaseDate(release.getDate());

            /*
             * FV = prima release con data > resolutionDate.
             * In questo progetto, resolutionDate è usata come proxy
             * della fix commit date.
             */
            if (releaseDate.isAfter(resolutionDate)) {
                if (bestRelease == null || releaseDate.isBefore(bestDate)) {
                    bestRelease = release;
                    bestDate = releaseDate;
                }
            }
        }

        return bestRelease == null ? "" : bestRelease.getVersionName();
    }
}