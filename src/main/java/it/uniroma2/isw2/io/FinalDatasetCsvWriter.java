package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.ReleaseJavaClass;
import it.uniroma2.isw2.model.TicketBuggyClass;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FinalDatasetCsvWriter {

    private FinalDatasetCsvWriter() {
    }

    /**
     * Scrive il dataset finale partendo direttamente dalle informazioni necessarie
     * per costruire i positivi classe-release.
     */
    public static void writeFinalDataset(String filePath,
                                         List<ReleaseJavaClass> releaseJavaClasses,
                                         List<Release> selectedReleases,
                                         Map<String, String> computedAvByTicketId,
                                         List<TicketBuggyClass> ticketBuggyClasses) throws IOException {

        Set<String> positiveKeys = buildPositiveKeys(
                computedAvByTicketId,
                selectedReleases,
                ticketBuggyClasses,
                releaseJavaClasses
        );

        writeFinalDataset(filePath, releaseJavaClasses, positiveKeys);
    }

    /**
     * Scrive il dataset finale usando un insieme di chiavi positive giÃ  costruite.
     */
    public static void writeFinalDataset(String filePath,
                                         List<ReleaseJavaClass> releaseJavaClasses,
                                         Set<String> positiveKeys) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,ClassPath,ReleaseID,ReleaseName,ReleaseIndex,Bugginess\n");

            for (ReleaseJavaClass javaClass : releaseJavaClasses) {
                String key = buildClassReleaseKey(
                        javaClass.getClassPath(),
                        javaClass.getReleaseId()
                );

                String bugginess = positiveKeys.contains(key) ? "yes" : "no";

                writer.append(CsvUtils.escapeCsv(javaClass.getProjectName())).append(",");
                writer.append(CsvUtils.escapeCsv(normalizePath(javaClass.getClassPath()))).append(",");
                writer.append(CsvUtils.escapeCsv(javaClass.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(javaClass.getReleaseName())).append(",");
                writer.append(String.valueOf(javaClass.getReleaseIndex())).append(",");
                writer.append(CsvUtils.escapeCsv(bugginess)).append("\n");
            }
        }
    }

    /**
     * Costruisce l'insieme delle chiavi positive classe-release.
     * Un positivo viene mantenuto solo se la classe esiste davvero
     * nello snapshot della release.
     */
    public static Set<String> buildPositiveKeys(Map<String, String> computedAvByTicketId,
                                                List<Release> selectedReleases,
                                                List<TicketBuggyClass> ticketBuggyClasses,
                                                List<ReleaseJavaClass> releaseJavaClasses) {
        Set<String> result = new HashSet<>();

        Set<String> existingClassReleaseKeys = buildExistingClassReleaseKeys(releaseJavaClasses);

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

                String positiveKey = buildClassReleaseKey(
                        normalizedClassPath,
                        release.getVersionId()
                );

                /*
                 * Mantieni il positivo solo se la classe esiste davvero
                 * nello snapshot della release.
                 */
                if (!existingClassReleaseKeys.contains(positiveKey)) {
                    continue;
                }

                result.add(positiveKey);
            }
        }

        return result;
    }

    private static Set<String> buildExistingClassReleaseKeys(List<ReleaseJavaClass> releaseJavaClasses) {
        Set<String> keys = new HashSet<>();

        for (ReleaseJavaClass releaseJavaClass : releaseJavaClasses) {
            keys.add(buildClassReleaseKey(
                    releaseJavaClass.getClassPath(),
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
        return normalizePath(classPath) + "|" + releaseId;
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}