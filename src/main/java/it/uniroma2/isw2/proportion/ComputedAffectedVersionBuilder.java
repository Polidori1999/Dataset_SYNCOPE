package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Costruisce la ComputedAV come intervallo [IV, FV).
 */
public class ComputedAffectedVersionBuilder {

    private ComputedAffectedVersionBuilder() {
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

        /*
         * ComputedAV = [IV, FV)
         * Quindi IV inclusa e FV esclusa.
         */
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

    private static int findReleaseIndex(String versionName, List<Release> releases) {
        for (Release release : releases) {
            if (release.getVersionName().equalsIgnoreCase(versionName)) {
                return release.getIndex();
            }
        }
        return -1;
    }
}