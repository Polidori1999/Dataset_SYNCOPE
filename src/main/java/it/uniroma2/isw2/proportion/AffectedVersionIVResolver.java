package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;

import java.util.ArrayList;
import java.util.List;

/**
 * Ricava una prima Injected Version dai ticket che possiedono AV.
 * La IV iniziale è assunta come la release affetta più vecchia valida,
 * ma deve essere coerente con OV e FV.
 *
 * Nota:
 * AV appartiene all'intervallo [IV, FV), quindi FV è esclusa.
 */
public class AffectedVersionIVResolver {

    private AffectedVersionIVResolver() {
    }

    public static List<EnhancedTicket> assignInitialIVFromAV(List<EnhancedTicket> tickets, List<Release> releases) {
        List<EnhancedTicket> updatedTickets = new ArrayList<>();

        for (EnhancedTicket ticket : tickets) {
            String initialIV = findInitialIVFromAV(ticket, releases);
            String source = initialIV.isBlank() ? "NONE" : "AV";

            int ivIndex = findReleaseIndex(initialIV, releases);
            int ovIndex = findReleaseIndex(ticket.getOpeningVersion(), releases);
            int fvIndex = findReleaseIndex(ticket.getFixedVersion(), releases);

            /*
             * Vincoli di coerenza:
             * - IV deve esistere
             * - OV deve esistere
             * - FV deve esistere
             * - IV <= OV
             * - IV < FV  (FV esclusa dall'intervallo delle AV)
             */
            if (ivIndex == -1 || ovIndex == -1 || fvIndex == -1 || ivIndex > ovIndex || ivIndex >= fvIndex) {
                initialIV = "";
                source = "NONE";
            }

            EnhancedTicket updatedTicket = new EnhancedTicket(
                    ticket.getTicketId(),
                    ticket.getCreationDate(),
                    ticket.getResolutionDate(),
                    ticket.getAffectedVersions(),
                    ticket.getOpeningVersion(),
                    ticket.getFixedVersion(),
                    initialIV,
                    source
            );

            updatedTickets.add(updatedTicket);
        }

        return updatedTickets;
    }

    private static String findInitialIVFromAV(EnhancedTicket ticket, List<Release> releases) {
        if (ticket.getAffectedVersions() == null || ticket.getAffectedVersions().isBlank()) {
            return "";
        }

        int ovIndex = findReleaseIndex(ticket.getOpeningVersion(), releases);
        int fvIndex = findReleaseIndex(ticket.getFixedVersion(), releases);

        if (ovIndex == -1 || fvIndex == -1) {
            return "";
        }

        String[] affectedVersions = ticket.getAffectedVersions().split(";");
        Release oldestValidAffectedRelease = null;

        for (String av : affectedVersions) {
            String normalized = av.trim();
            Release release = findReleaseByName(normalized, releases);

            if (release == null) {
                continue;
            }

            int releaseIndex = release.getIndex();

            /*
             * Vincoli di coerenza:
             * - la IV non può essere successiva alla OV
             * - la IV deve essere strettamente precedente alla FV
             */
            if (releaseIndex > ovIndex) {
                continue;
            }

            if (releaseIndex >= fvIndex) {
                continue;
            }

            if (oldestValidAffectedRelease == null || releaseIndex < oldestValidAffectedRelease.getIndex()) {
                oldestValidAffectedRelease = release;
            }
        }

        if (oldestValidAffectedRelease == null) {
            return "";
        }

        return oldestValidAffectedRelease.getVersionName();
    }

    private static Release findReleaseByName(String versionName, List<Release> releases) {
        for (Release release : releases) {
            if (release.getVersionName().equalsIgnoreCase(versionName)) {
                return release;
            }
        }
        return null;
    }

    private static int findReleaseIndex(String versionName, List<Release> releases) {
        if (versionName == null || versionName.isBlank()) {
            return -1;
        }

        for (Release release : releases) {
            if (release.getVersionName().equalsIgnoreCase(versionName)) {
                return release.getIndex();
            }
        }
        return -1;
    }
}