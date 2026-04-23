package it.uniroma2.isw2.labeling;

import it.uniroma2.isw2.model.BuggyClassReleaseLabel;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.model.TicketBuggyClass;
import it.uniroma2.isw2.model.TicketComputedAv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuggyClassReleaseLabeler {

    private BuggyClassReleaseLabeler() {
    }

    public static List<BuggyClassReleaseLabel> buildPositiveLabels(String projectName,
                                                                   List<TicketComputedAv> ticketsWithComputedAv,
                                                                   List<Release> selectedReleases,
                                                                   List<TicketBuggyClass> ticketBuggyClasses,
                                                                   List<ReleaseJavaClass> releaseJavaClasses) {
        List<BuggyClassReleaseLabel> result = new ArrayList<>();

        Map<String, String> computedAvByTicketId = buildComputedAvMap(ticketsWithComputedAv);
        Set<String> existingClassReleaseKeys = buildExistingClassReleaseKeys(releaseJavaClasses);
        Set<String> seen = new HashSet<>();

        for (TicketBuggyClass ticketBuggyClass : ticketBuggyClasses) {
            String computedAv = computedAvByTicketId.get(ticketBuggyClass.getTicketId());

            if (computedAv == null || computedAv.isBlank()) {
                continue;
            }

            Set<String> affectedReleaseNames = parseComputedAv(computedAv);
            String normalizedClassPath = normalizePath(ticketBuggyClass.getClassPath());

            for (Release release : selectedReleases) {
                if (!affectedReleaseNames.contains(release.getVersionName())) {
                    continue;
                }

                /*
                 * Mantieni il positivo solo se la classe esiste davvero
                 * nello snapshot della release.
                 */
                String existingKey = buildClassReleaseKey(normalizedClassPath, release.getVersionId());
                if (!existingClassReleaseKeys.contains(existingKey)) {
                    continue;
                }

                String dedupeKey = ticketBuggyClass.getTicketId()
                        + "|"
                        + normalizedClassPath
                        + "|"
                        + release.getVersionId();

                if (seen.add(dedupeKey)) {
                    result.add(new BuggyClassReleaseLabel(
                            projectName,
                            ticketBuggyClass.getTicketId(),
                            ticketBuggyClass.getFixCommitHash(),
                            normalizedClassPath,
                            release.getVersionId(),
                            release.getVersionName(),
                            release.getIndex(),
                            "yes"
                    ));
                }
            }
        }

        return result;
    }

    private static Map<String, String> buildComputedAvMap(List<TicketComputedAv> ticketsWithComputedAv) {
        Map<String, String> map = new HashMap<>();

        for (TicketComputedAv ticket : ticketsWithComputedAv) {
            map.put(ticket.getTicketId(), ticket.getComputedAv());
        }

        return map;
    }

    private static Set<String> buildExistingClassReleaseKeys(List<ReleaseJavaClass> releaseJavaClasses) {
        Set<String> keys = new HashSet<>();

        for (ReleaseJavaClass releaseJavaClass : releaseJavaClasses) {
            keys.add(buildClassReleaseKey(
                    normalizePath(releaseJavaClass.getClassPath()),
                    releaseJavaClass.getReleaseId()
            ));
        }

        return keys;
    }

    private static Set<String> parseComputedAv(String computedAv) {
        Set<String> releaseNames = new HashSet<>();

        String[] parts = computedAv.split(";");
        for (String part : parts) {
            String normalized = part.trim();
            if (!normalized.isBlank()) {
                releaseNames.add(normalized);
            }
        }

        return releaseNames;
    }

    private static String buildClassReleaseKey(String classPath, String releaseId) {
        return classPath + "|" + releaseId;
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}