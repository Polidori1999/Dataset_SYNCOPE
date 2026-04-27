package it.uniroma2.isw2.io;

import it.uniroma2.isw2.smell.ClassReleaseSmell;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Scrive il numero di smell PMD per ogni coppia classe-release.
 */
public class ClassReleaseSmellCsvWriter {

    private ClassReleaseSmellCsvWriter() {
    }

    public static void writeClassReleaseSmells(String filePath,
                                               List<ClassReleaseSmell> smells) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Project,ReleaseID,ClassPath,NSmells\n");

            for (ClassReleaseSmell smell : smells) {
                writer.append(CsvUtils.escapeCsv(smell.getProject())).append(",");
                writer.append(CsvUtils.escapeCsv(smell.getReleaseId())).append(",");
                writer.append(CsvUtils.escapeCsv(smell.getClassPath())).append(",");
                writer.append(String.valueOf(smell.getNSmells())).append("\n");
            }
        }
    }
}