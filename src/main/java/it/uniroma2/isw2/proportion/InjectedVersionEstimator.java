package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe che stima la Injected Version usando la proportion media.
 */
public class InjectedVersionEstimator {

    private InjectedVersionEstimator() {
    }

    public static List<EnhancedTicket> estimateMissingInjectedVersions(List<EnhancedTicket> tickets,
                                                                       List<Release> releases,
                                                                       double proportion) {
        List<EnhancedTicket> result = new ArrayList<>();

        for (EnhancedTicket ticket : tickets) {
            if (ticket.getInjectedVersion() != null && !ticket.getInjectedVersion().isBlank()) {
                result.add(ticket);
                continue;
            }

            String estimatedIv = estimateInjectedVersion(ticket, releases, proportion);

            String source = estimatedIv.isBlank() ? "NONE" : "P";

            EnhancedTicket updatedTicket = new EnhancedTicket(
                    ticket.getTicketId(),
                    ticket.getCreationDate(),
                    ticket.getResolutionDate(),
                    ticket.getAffectedVersions(),
                    ticket.getOpeningVersion(),
                    ticket.getFixedVersion(),
                    estimatedIv,
                    source
            );

            result.add(updatedTicket);
        }

        return result;
    }

    private static String estimateInjectedVersion(EnhancedTicket ticket,
                                                  List<Release> releases,
                                                  double proportion) {
        if (ticket.getOpeningVersion() == null || ticket.getOpeningVersion().isBlank()) {
            return "";
        }

        if (ticket.getFixedVersion() == null || ticket.getFixedVersion().isBlank()) {
            return "";
        }

        int ov = findReleaseIndex(ticket.getOpeningVersion(), releases);
        int fv = findReleaseIndex(ticket.getFixedVersion(), releases);

        if (ov == -1 || fv == -1) {
            return "";
        }

        if (fv <= ov) {
            return ticket.getOpeningVersion();
        }

        int estimatedIvIndex = (int) Math.round(fv - proportion * (fv - ov));

        if (estimatedIvIndex < 1) {
            estimatedIvIndex = 1;
        }

        if (estimatedIvIndex > ov) {
            estimatedIvIndex = ov;
        }

        Release estimatedRelease = findReleaseByIndex(estimatedIvIndex, releases);

        if (estimatedRelease == null) {
            return "";
        }

        return estimatedRelease.getVersionName();
    }

    private static int findReleaseIndex(String versionName, List<Release> releases) {
        for (Release release : releases) {
            if (release.getVersionName().equalsIgnoreCase(versionName)) {
                return release.getIndex();
            }
        }
        return -1;
    }

    private static Release findReleaseByIndex(int index, List<Release> releases) {
        for (Release release : releases) {
            if (release.getIndex() == index) {
                return release;
            }
        }
        return null;
    }
}