package it.uniroma2.isw2.smell;

import it.uniroma2.isw2.io.CsvUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Legge i report prodotti da PMD.
 * Il CSV viene usato per contare le violation per classe.
 * L'XML viene usato per tracciare i file non analizzabili da PMD.
 */
public class PmdReportParser {

    private static final int CSV_FILE_COLUMN_INDEX = 2;

    private PmdReportParser() {
    }

    public static Map<String, Integer> countViolationsByClassPath(Path csvReportPath,
                                                                  Path repositoryPath) throws IOException {
        Map<String, Integer> violationsByClassPath = new HashMap<>();

        if (!Files.exists(csvReportPath)) {
            return violationsByClassPath;
        }

        try (BufferedReader reader = Files.newBufferedReader(csvReportPath)) {
            String line = reader.readLine();

            if (line == null) {
                return violationsByClassPath;
            }

            while ((line = reader.readLine()) != null) {
                List<String> fields = CsvUtils.parseCsvLine(line);

                if (fields.size() <= CSV_FILE_COLUMN_INDEX) {
                    continue;
                }

                String absoluteFilePath = CsvUtils.removeQuotes(fields.get(CSV_FILE_COLUMN_INDEX));
                String classPath = toRepositoryRelativePath(absoluteFilePath, repositoryPath);

                violationsByClassPath.merge(classPath, 1, Integer::sum);
            }
        }

        return violationsByClassPath;
    }

    public static List<PmdAnalysisError> readAnalysisErrors(String projectName,
                                                            String releaseId,
                                                            Path xmlReportPath,
                                                            Path repositoryPath) throws IOException {
        List<PmdAnalysisError> errors = new java.util.ArrayList<>();

        if (!Files.exists(xmlReportPath)) {
            return errors;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            disableExternalEntities(factory);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlReportPath.toFile());

            NodeList errorNodes = document.getElementsByTagName("error");

            for (int i = 0; i < errorNodes.getLength(); i++) {
                Element errorElement = (Element) errorNodes.item(i);

                String absoluteFilePath = errorElement.getAttribute("filename");
                String message = cleanErrorMessage(errorElement.getAttribute("msg"));
                String classPath = toRepositoryRelativePath(absoluteFilePath, repositoryPath);

                errors.add(new PmdAnalysisError(
                        projectName,
                        releaseId,
                        classPath,
                        message
                ));
            }

            return errors;
        } catch (Exception e) {
            throw new IOException("Errore durante la lettura del report XML PMD.", e);
        }
    }

    private static String toRepositoryRelativePath(String filePath, Path repositoryPath) {
        Path normalizedRepositoryPath = repositoryPath.toAbsolutePath().normalize();
        Path normalizedFilePath = Path.of(filePath).toAbsolutePath().normalize();

        if (normalizedFilePath.startsWith(normalizedRepositoryPath)) {
            return normalizePath(normalizedRepositoryPath.relativize(normalizedFilePath).toString());
        }

        return normalizePath(filePath);
    }

    private static String cleanErrorMessage(String message) {
        if (message == null) {
            return "";
        }

        return message
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizePath(String path) {
        return path.replace("\\", "/").trim();
    }

    private static void disableExternalEntities(DocumentBuilderFactory factory) {
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
            // Alcuni parser XML potrebbero non supportare tutte le feature.
        }

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
    }
}