package it.uniroma2.isw2;

import it.uniroma2.isw2.io.*;
import it.uniroma2.isw2.labeling.*;
import it.uniroma2.isw2.metric.ClassReleaseMetric;
import it.uniroma2.isw2.metric.MetricService;
import it.uniroma2.isw2.model.*;
import it.uniroma2.isw2.proportion.*;
import it.uniroma2.isw2.smell.PmdFileListWriter;
import it.uniroma2.isw2.smell.PmdRunner;
import it.uniroma2.isw2.smell.SmellComputationResult;
import it.uniroma2.isw2.smell.SmellService;
import it.uniroma2.isw2.utils.ReleaseSelector;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classe principale del progetto.
 * Coordina le fasi iniziali della costruzione del dataset.
 */
public class Main {

    private static final String PROJECT_NAME = "SYNCOPE";
    private static final String RELEASES_FILE = PROJECT_NAME + "VersionInfo.csv";
    private static final String TICKETS_FILE = PROJECT_NAME + "Tickets.csv";
    private static final double RELEASES_TO_KEEP = 0.34;


    private static final String FINAL_CLASS_RELEASE_LABELS_FILE =
            PROJECT_NAME + "_FinalClassReleaseLabels.csv";


    private static final String PROJECT_REPO_PATH =
            "/home/leonardo/uni/isw2/syncope";

    private static final String TICKET_FIX_COMMITS_FILE =
            PROJECT_NAME + "_TicketFixCommits.csv";

    private static final String TICKET_BUGGY_CLASSES_FILE =
            PROJECT_NAME + "_TicketBuggyClasses.csv";

    private static final String CLASS_RELEASE_METRICS_FILE =
            PROJECT_NAME + "_ClassReleaseMetrics.csv";

    private static final String FINAL_DATASET_WITH_SMELLS_FILE =
            PROJECT_NAME + "_FinalDatasetWithSmells.csv";


    private static final String CLASS_RELEASE_SMELLS_FILE =
            PROJECT_NAME + "_ClassReleaseSmells.csv";

    private static final String PMD_ANALYSIS_ERRORS_FILE =
            PROJECT_NAME + "_PmdAnalysisErrors.csv";

    private static final String PMD_EXECUTABLE =
            "/home/leonardo/uni/isw2/pmd/pmd-bin-7.24.0/bin/pmd";

    private static final String PMD_RULESET =
            "rulesets/java/quickstart.xml";

    private static final String PMD_JAVA_VERSION =
            "java-21";

    private static final String PMD_FILE_LISTS_DIR =
            "target/pmd-filelists/" + PROJECT_NAME;

    private static final String PMD_REPORTS_DIR =
            "target/pmd-reports/" + PROJECT_NAME;

    private static final String FINAL_METRIC_DATASET_FILE =
            "dataset_" + PROJECT_NAME + ".csv";


    public static void main(String[] args) throws IOException {
        System.out.println("Avvio costruzione dataset del progetto " + PROJECT_NAME + ".");


        try {

            
            /*

             * STEP 1:
             * Lettura di tutte le release del progetto.
             */
            List<Release> allReleases = ReleaseCsvReader.loadReleases(RELEASES_FILE);
            System.out.println("Release lette: " + allReleases.size());

            /*
             * STEP 1.1:
             * Selezione del primo 34% delle release
             * (equivalente a ignorare l'ultimo 66%).
             */
            List<Release> selectedReleases =
                    ReleaseSelector.selectInitialReleases(allReleases, RELEASES_TO_KEEP);
            System.out.println("Release selezionate per il dataset finale: " + selectedReleases.size());

            /*
             * STEP 2:
             * Lettura dei ticket validi.
             */
            List<Ticket> tickets = TicketCsvReader.loadTickets(TICKETS_FILE);
            System.out.println("Ticket letti: " + tickets.size());

            /*
             * STEP 3:
             * Arricchimento dei ticket con OV e FV.
             * QUI continuiamo a usare tutte le release, non solo quelle selezionate.
             */
            List<EnhancedTicket> enhancedTickets =
                    TicketVersionEnricher.enrichTickets(tickets, allReleases);
            System.out.println("ticket arricchiti creati");

            /*
             * STEP 4:
             * Per i ticket con AV, si assegna una IV iniziale.
             */
            List<EnhancedTicket> avBasedTickets =
                    AffectedVersionIVResolver.assignInitialIVFromAV(enhancedTickets, allReleases);
            System.out.println("ticket con IV iniziale da AV creati");

            /*
             * STEP 5:
             * Calcolo della proportion media.
             */
            double proportion =
                    ProportionService.calculateAverageProportion(avBasedTickets, allReleases);
            System.out.println("Proportion media calcolata: " + proportion);

            /*
             * STEP 6:
             * Stima della IV per i ticket che ancora non la possiedono.
             */
            List<EnhancedTicket> estimatedTickets =
                    ProportionService.estimateMissingInjectedVersions(
                            avBasedTickets, allReleases, proportion
                    );
            System.out.println("ticket con IV stimata creato");

            System.out.println("Selezione release + fase AV -> IV -> P completate con successo.");

            /*
             * STEP 7:
             * Costruzione in memoria della ComputedAV per ogni ticket stimato.
             * Nessun passaggio intermedio su CSV.
             */
            Map<String, String> computedAvByTicketId =
                    ProportionService.buildComputedAvMap(estimatedTickets, allReleases);
            System.out.println("Ticket con ComputedAV costruiti in memoria: " + computedAvByTicketId.size());
            List<ReleaseJavaClass> releaseJavaClasses = null;
            if (!csvExists(FINAL_DATASET_WITH_SMELLS_FILE)) {
                System.out.println("File non esiste Faccio gli step");
                /*
                 * STEP 8:
                 * Ricerca dei fix commit associati ai ticket nel repository Git.
                 */
                List<TicketFixCommit> ticketFixCommits =
                        BuggyClassService.findFixCommits(
                                estimatedTickets,
                                PROJECT_REPO_PATH
                        );
                TicketFixCommitCsvWriter.writeTicketFixCommits(TICKET_FIX_COMMITS_FILE, ticketFixCommits);
                System.out.println("File ticket-fix commit creato: " + TICKET_FIX_COMMITS_FILE);

                /*
                 * STEP 9:
                 * Estrazione semplificata delle buggy classes con SZZ.
                 * Sono considerate solo classi Java di produzione.
                 */
                List<TicketBuggyClass> ticketBuggyClasses =
                        BuggyClassService.extractBuggyClasses(
                                ticketFixCommits,
                                PROJECT_REPO_PATH
                        );
                TicketBuggyClassCsvWriter.writeTicketBuggyClasses(
                        TICKET_BUGGY_CLASSES_FILE,
                        ticketBuggyClasses
                );
                System.out.println("File ticket-buggy classes creato: " + TICKET_BUGGY_CLASSES_FILE);

                /*
                 * STEP 10-11:
                 * Per ogni release selezionata:
                 * - trova il commit snapshot
                 * - estrae tutte le classi Java di produzione presenti
                 */
                releaseJavaClasses = ReleaseInventoryService.buildReleaseInventory(
                        PROJECT_NAME,
                        selectedReleases,
                        PROJECT_REPO_PATH
                );
                System.out.println("Coppie classe-release generate: " + releaseJavaClasses.size());

                /*
                 * STEP 12-13:
                 * Costruzione dei positivi e merge finale yes/no
                 * direttamente dentro FinalDatasetCsvWriter.
                 */
                FinalDatasetCsvWriter.writeFinalDataset(
                        FINAL_CLASS_RELEASE_LABELS_FILE,
                        releaseJavaClasses,
                        selectedReleases,
                        computedAvByTicketId,
                        ticketBuggyClasses
                );
                System.out.println("File finale classe-release yes/no creato: "
                        + FINAL_CLASS_RELEASE_LABELS_FILE);

                /*
                 * STEP 14:
                 * Calcolo degli smell PMD per ogni coppia classe-release.
                 * PMD viene eseguito sullo snapshot reale di ciascuna release selezionata.
                 */
                PmdFileListWriter pmdFileListWriter =
                        new PmdFileListWriter(Path.of(PMD_FILE_LISTS_DIR).toAbsolutePath());

                PmdRunner pmdRunner =
                        new PmdRunner(
                                PMD_EXECUTABLE,
                                PMD_RULESET,
                                PMD_JAVA_VERSION,
                                Path.of(PMD_REPORTS_DIR).toAbsolutePath()
                        );

                SmellService smellService =
                        new SmellService(
                                PROJECT_NAME,
                                Path.of(PROJECT_REPO_PATH),
                                pmdFileListWriter,
                                pmdRunner
                        );

                SmellComputationResult smellResult =
                        smellService.computeSmells(
                                selectedReleases,
                                releaseJavaClasses
                        );

                ClassReleaseSmellCsvWriter.writeClassReleaseSmells(
                        CLASS_RELEASE_SMELLS_FILE,
                        smellResult.getSmells()
                );

                PmdAnalysisErrorCsvWriter.writePmdAnalysisErrors(
                        PMD_ANALYSIS_ERRORS_FILE,
                        smellResult.getErrors()
                );

                System.out.println("File smell classe-release creato: " + CLASS_RELEASE_SMELLS_FILE);
                System.out.println("File errori PMD creato: " + PMD_ANALYSIS_ERRORS_FILE);


                /*
                 * STEP 15:
                 * Merge tra dataset classe-release yes/no e smell PMD.
                 */
                FinalDatasetWithSmellsCsvWriter.writeFinalDatasetWithSmells(
                        FINAL_CLASS_RELEASE_LABELS_FILE,
                        CLASS_RELEASE_SMELLS_FILE,
                        FINAL_DATASET_WITH_SMELLS_FILE
                );

                System.out.println("Dataset finale con NSmells creato: "
                        + FINAL_DATASET_WITH_SMELLS_FILE);

            }
            /*
             * STEP 10-11:
             * Per ogni release selezionata:
             * - trova il commit snapshot
             * - estrae tutte le classi Java di produzione presenti
             */
            releaseJavaClasses = ReleaseInventoryService.buildReleaseInventory(
                    PROJECT_NAME,
                    selectedReleases,
                    PROJECT_REPO_PATH
            );
            System.out.println("Coppie classe-release generate: " + releaseJavaClasses.size());

            /*
             * STEP 16:
             * Calcolo delle metriche classe-release.
             */
            Set<String> fixCommitHashes =
                    TicketFixCommitCsvReader.loadFixCommitHashes(TICKET_FIX_COMMITS_FILE);

            Map<String, Integer> smellsByClassRelease =
                    ClassReleaseSmellCsvReader.loadSmellsByClassRelease(CLASS_RELEASE_SMELLS_FILE);

            MetricService metricService =
                    new MetricService(
                            PROJECT_NAME,
                            Path.of(PROJECT_REPO_PATH),
                            fixCommitHashes
                    );

            List<ClassReleaseMetric> classReleaseMetrics =
                    metricService.computeMetrics(
                            selectedReleases,
                            releaseJavaClasses,
                            smellsByClassRelease
                    );

            ClassReleaseMetricCsvWriter.writeClassReleaseMetrics(
                    CLASS_RELEASE_METRICS_FILE,
                    classReleaseMetrics
            );
            /*
             * STEP 17:
             * Merge finale tra metriche classe-release e label buggy yes/no.
             */
            FinalMetricDatasetCsvWriter.writeFinalMetricDataset(
                    CLASS_RELEASE_METRICS_FILE,
                    FINAL_CLASS_RELEASE_LABELS_FILE,
                    FINAL_METRIC_DATASET_FILE
            );

            System.out.println("Dataset finale metriche + label creato: "
                    + FINAL_METRIC_DATASET_FILE);

            System.out.println("File metriche classe-release creato: "
                    + CLASS_RELEASE_METRICS_FILE);
        } catch (IOException e) {
            System.out.println("Errore durante l'esecuzione del flusso principale.");
            e.printStackTrace();
        }
    }


    private static boolean csvExists(String filePath) throws IOException {
        Path path = Path.of(filePath);
        return Files.isRegularFile(path) && Files.size(path) > 0;
    }
}
