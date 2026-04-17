package it.uniroma2.isw2.proportion;

import it.uniroma2.isw2.model.Release;

import java.util.List;

/**
 * Classe per il calcolo della proportion media.
 */
public class ProportionCalculator {

    private ProportionCalculator() {
    }

    public static double calculateAverageProportion(List<EnhancedTicket> tickets, List<Release> releases) {
        double sum = 0.0;
        int count = 0;

        for (EnhancedTicket ticket : tickets) {
            int iv = findReleaseIndex(ticket.getInjectedVersion(), releases);
            int ov = findReleaseIndex(ticket.getOpeningVersion(), releases);
            int fv = findReleaseIndex(ticket.getFixedVersion(), releases);

            if (iv == -1 || ov == -1 || fv == -1) {
                continue;
            }

            if (!(iv <= ov && ov <= fv)) {
                continue;
            }

            if (fv == ov) {
                continue;
            }

            double proportion = (double) (fv - iv) / (double) (fv - ov);
            sum += proportion;
            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        return sum / count;
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