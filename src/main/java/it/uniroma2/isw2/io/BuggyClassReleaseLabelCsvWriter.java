package it.uniroma2.isw2.io;

import it.uniroma2.isw2.model.BuggyClassReleaseLabel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class BuggyClassReleaseLabelCsvWriter {

    private BuggyClassReleaseLabelCsvWriter() {
    }

    public static void writeLabels(String filePath,
                                   List<BuggyClassReleaseLabel> labels) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,TicketID,FixCommitHash,ClassPath,ReleaseID,ReleaseName,ReleaseIndex,Bugginess\n");

            for (BuggyClassReleaseLabel label : labels) {
                writer.append(CsvUtils.escapeCsv(label.getProjectName())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getTicketId())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getFixCommitHash())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getClassPath())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getReleaseName())).append(",");
                writer.append(String.valueOf(label.getReleaseIndex())).append(",");
                writer.append(CsvUtils.escapeCsv(label.getBugginess())).append("\n");
            }
        }
    }
}