package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestisce le operazioni principali della phase proportion:
 * - calcolo della proportion media
 * - stima delle IV mancanti
 * - costruzione della ComputedAV come intervallo [IV, FV)
 */
public class ProportionService {

    private ProportionService() {
    }

    public static double calculateAverageProportion(List<EnhancedTicket> tickets, List<Release> releases) {
        double sum = 0.0;
        int count = 0;

        for (EnhancedTicket ticket : tickets) {
            if (isTicketUsableForProportion(ticket, releases)) {
                int iv = findReleaseIndex(ticket.getInjectedVersion(), releases);
                int ov = findReleaseIndex(ticket.getOpeningVersion(), releases);
                int fv = findReleaseIndex(ticket.getFixedVersion(), releases);

                double proportion = (double) (fv - iv) / (double) (fv - ov);
                sum += proportion;
                count++;
            }
        }

        if (count == 0) {
            return 0.0;
        }

        return sum / count;
    }
    private static boolean isTicketUsableForProportion(EnhancedTicket ticket, List<Release> releases) {
        if (!"AV".equals(ticket.getInjectedVersionSource())) {
            return false;
        }

        int iv = findReleaseIndex(ticket.getInjectedVersion(), releases);
        int ov = findReleaseIndex(ticket.getOpeningVersion(), releases);
        int fv = findReleaseIndex(ticket.getFixedVersion(), releases);

        return isValidLifecycle(iv, ov, fv);
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

            EnhancedTicket updatedTicket = EnhancedTicket.builder()
                    .ticketId(ticket.getTicketId())
                    .creationDate(ticket.getCreationDate())
                    .resolutionDate(ticket.getResolutionDate())
                    .affectedVersions(ticket.getAffectedVersions())
                    .openingVersion(ticket.getOpeningVersion())
                    .fixedVersion(ticket.getFixedVersion())
                    .injectedVersion(estimatedIv)
                    .injectedVersionSource(source)
                    .build();

            result.add(updatedTicket);
        }

        return result;
    }

    public static String buildComputedAv(EnhancedTicket ticket, List<Release> releases) {
        if (ticket.getInjectedVersion() == null || ticket.getInjectedVersion().isBlank()) {
            return "";
        }

        if (ticket.getFixedVersion() == null || ticket.getFixedVersion().isBlank()) {
            return "";
        }

        int ivIndex = findReleaseIndex(ticket.getInjectedVersion(), releases);
        int fvIndex = findReleaseIndex(ticket.getFixedVersion(), releases);

        if (ivIndex == -1 || fvIndex == -1) {
            return "";
        }

        if (ivIndex >= fvIndex) {
            return "";
        }

        List<String> affectedReleaseNames = new ArrayList<>();

        for (Release release : releases) {
            int currentIndex = release.getIndex();

            if (currentIndex >= ivIndex && currentIndex < fvIndex) {
                affectedReleaseNames.add(release.getVersionName());
            }
        }

        return affectedReleaseNames.stream().collect(Collectors.joining(";"));
    }

    public static Map<String, String> buildComputedAvMap(List<EnhancedTicket> tickets,
                                                         List<Release> releases) {
        Map<String, String> result = new HashMap<>();

        for (EnhancedTicket ticket : tickets) {
            result.put(ticket.getTicketId(), buildComputedAv(ticket, releases));
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

    private static boolean isValidLifecycle(int ivIndex, int ovIndex, int fvIndex) {
        return ivIndex != -1
                && ovIndex != -1
                && fvIndex != -1
                && ivIndex <= ovIndex
                && ovIndex < fvIndex;
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

    private static Release findReleaseByIndex(int index, List<Release> releases) {
        for (Release release : releases) {
            if (release.getIndex() == index) {
                return release;
            }
        }

        return null;
    }

    
}