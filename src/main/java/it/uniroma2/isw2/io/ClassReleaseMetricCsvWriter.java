package it.uniroma2.isw2.io;

import it.uniroma2.isw2.metric.ClassReleaseMetric;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Scrive le metriche calcolate per ogni coppia classe-release.
 */
public class ClassReleaseMetricCsvWriter {

    private ClassReleaseMetricCsvWriter() {
    }

    public static void writeClassReleaseMetrics(String filePath,
                                                List<ClassReleaseMetric> metrics)
            throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,ReleaseID,ClassPath,")
                    .append("SIZE_LOC,")
                    .append("NOM,")
                    .append("AVG_METHOD_SIZE,")
                    .append("CYCLO_COMPLEXITY,")
                    .append("FAN_OUT,")
                    .append("NR,")
                    .append("NFIX,")
                    .append("NAUTH,")
                    .append("LOC_ADDED,")
                    .append("MAX_LOC_ADDED,")
                    .append("CHURN,")
                    .append("MAX_CHURN,")
                    .append("MAX_CHANGE_SET_SIZE,")
                    .append("AVG_MODIFIED_DIRS,")
                    .append("AGE_SINCE_LAST_CHANGE,")
                    .append("OWNERSHIP_RATIO,")
                    .append("NSMELLS,")
                    .append("SMELL_DENSITY,")
                    .append("SAME_DIRECTORY_CHANGE_RATIO,")
                    .append("AGE\n");

            for (ClassReleaseMetric metric : metrics) {
                writer.append(CsvUtils.escapeCsv(metric.getProject())).append(",");
                writer.append(CsvUtils.escapeCsv(metric.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(metric.getClassPath())).append(",");

                writer.append(String.valueOf(metric.getSizeLoc())).append(",");
                writer.append(String.valueOf(metric.getNom())).append(",");
                writer.append(formatDouble(metric.getAvgMethodSize())).append(",");
                writer.append(String.valueOf(metric.getCycloComplexity())).append(",");
                writer.append(String.valueOf(metric.getFanOut())).append(",");

                writer.append(String.valueOf(metric.getNr())).append(",");
                writer.append(String.valueOf(metric.getNFix())).append(",");
                writer.append(String.valueOf(metric.getNAuth())).append(",");

                writer.append(String.valueOf(metric.getLocAdded())).append(",");
                writer.append(String.valueOf(metric.getMaxLocAdded())).append(",");

                writer.append(String.valueOf(metric.getChurn())).append(",");
                writer.append(String.valueOf(metric.getMaxChurn())).append(",");

                writer.append(String.valueOf(metric.getMaxChangeSetSize())).append(",");
                writer.append(formatDouble(metric.getAvgModifiedDirs())).append(",");

                writer.append(String.valueOf(metric.getAgeSinceLastChange())).append(",");
                writer.append(formatDouble(metric.getOwnershipRatio())).append(",");

                writer.append(String.valueOf(metric.getNSmells())).append(",");
                writer.append(formatDouble(metric.getSmellDensity())).append(",");

                writer.append(formatDouble(metric.getSameDirectoryChangeRatio())).append(",");
                writer.append(String.valueOf(metric.getAge())).append("\n");
            }
        }
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.6f", value);
    }
}