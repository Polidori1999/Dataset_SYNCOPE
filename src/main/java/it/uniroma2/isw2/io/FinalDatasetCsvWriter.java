package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.BuggyClassReleaseLabel;
import it.uniroma2.isw2.model.ReleaseJavaClass;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FinalDatasetCsvWriter {

    private FinalDatasetCsvWriter() {
    }

    public static void writeFinalDataset(String filePath,
                                         List<ReleaseJavaClass> releaseJavaClasses,
                                         List<BuggyClassReleaseLabel> positiveLabels) throws IOException {
        Set<String> positiveKeys = buildPositiveKeys(positiveLabels);

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,ClassPath,ReleaseID,ReleaseName,ReleaseIndex,Bugginess\n");

            for (ReleaseJavaClass javaClass : releaseJavaClasses) {
                String key = buildKey(javaClass.getClassPath(), javaClass.getReleaseId());
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

    private static Set<String> buildPositiveKeys(List<BuggyClassReleaseLabel> positiveLabels) {
        Set<String> keys = new HashSet<>();

        for (BuggyClassReleaseLabel label : positiveLabels) {
            keys.add(buildKey(label.getClassPath(), label.getReleaseId()));
        }

        return keys;
    }

    private static String buildKey(String classPath, String releaseId) {
        return normalizePath(classPath) + "|" + releaseId;
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }
}