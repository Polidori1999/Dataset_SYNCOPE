package it.uniroma2.isw2.smell;

import java.nio.file.Path;

/**
 * Rappresenta i report prodotti da una singola esecuzione PMD.
 */
public class PmdRunResult {

    private final Path csvReportPath;
    private final Path xmlReportPath;

    public PmdRunResult(Path csvReportPath, Path xmlReportPath) {
        this.csvReportPath = csvReportPath;
        this.xmlReportPath = xmlReportPath;
    }

    public Path getCsvReportPath() {
        return csvReportPath;
    }

    public Path getXmlReportPath() {
        return xmlReportPath;
    }
}