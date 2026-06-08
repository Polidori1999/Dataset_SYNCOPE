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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe principale del progetto.
 * Coordina le fasi iniziali della costruzione del dataset.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    private static final String PROJECT_NAME = "SYNCOPE";
    private static final String RELEASES_FILE = PROJECT_NAME + "VersionInfo.csv";
    private static final String TICKETS_FILE = PROJECT_NAME + "Tickets.csv";
    private static final double RELEASES_TO_KEEP = 0.34;


    private static final String FINAL_CLASS_RELEASE_LABELS_FILE =
            PROJECT_NAME + "_FinalClassReleaseLabels.csv";


    private static final String PROJECT_REPO_PATH =
            System.getProperty("project.repo.path", "/home/leonardo/uni/isw2/syncope");

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
        LOGGER.log(Level.INFO, "Avvio costruzione dataset del progetto {0}.", PROJECT_NAME);



        try {

            
            /*

             * STEP 1:
             * Lettura di tutte le release del progetto.
             */
            List<Release> allReleases = ReleaseCsvReader.loadReleases(RELEASES_FILE);
            LOGGER.log(Level.INFO, "Release lette: {0}", allReleases.size());

            /*
             * STEP 1.1:
             * Selezione del primo 34% delle release
             * (equivalente a ignorare l'ultimo 66%).
             */
            List<Release> selectedReleases =
                    ReleaseSelector.selectInitialReleases(allReleases, RELEASES_TO_KEEP);
            LOGGER.log(Level.INFO, "Release selezionate per il dataset finale: {0}", selectedReleases.size());


            /*
             * STEP 2:
             * Lettura dei ticket validi.
             */
            List<Ticket> tickets = TicketCsvReader.loadTickets(TICKETS_FILE);
            LOGGER.log(Level.INFO, "Ticket letti: {0}", tickets.size());

            /*
             * STEP 3:
             * Arricchimento dei ticket con OV e FV.
             * QUI continuiamo a usare tutte le release, non solo quelle selezionate.
             */
            List<EnhancedTicket> enhancedTickets =
                    TicketVersionEnricher.enrichTickets(tickets, allReleases);
            LOGGER.info("ticket arricchiti creati");

            /*
             * STEP 4:
             * Per i ticket con AV, si assegna una IV iniziale.
             */
            List<EnhancedTicket> avBasedTickets =
                    AffectedVersionIVResolver.assignInitialIVFromAV(enhancedTickets, allReleases);
            LOGGER.info("ticket con IV iniziale da AV creati");

            /*
             * STEP 5:
             * Calcolo della proportion media.
             */
            double proportion =
                    ProportionService.calculateAverageProportion(avBasedTickets, allReleases);
            LOGGER.log(Level.INFO, "Proportion media calcolata: {0}", proportion);

            /*
             * STEP 6:
             * Stima della IV per i ticket che ancora non la possiedono.
             */
            List<EnhancedTicket> estimatedTickets =
                    ProportionService.estimateMissingInjectedVersions(
                            avBasedTickets, allReleases, proportion
                    );
            LOGGER.info("ticket con IV stimata creato");

            LOGGER.info("Selezione release + fase AV -> IV -> P completate con successo.");

            /*
             * STEP 7:
             * Costruzione in memoria della ComputedAV per ogni ticket stimato.
             * Nessun passaggio intermedio su CSV.
             */
            Map<String, String> computedAvByTicketId =
                    ProportionService.buildComputedAvMap(estimatedTickets, allReleases);
            LOGGER.log(Level.INFO, "Ticket con ComputedAV costruiti in memoria: {0}", computedAvByTicketId.size());

            List<ReleaseJavaClass> releaseJavaClasses = null;
            if (!csvExists(FINAL_DATASET_WITH_SMELLS_FILE)) {
                LOGGER.info("File non esiste Faccio gli step");
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
                LOGGER.log(Level.INFO, "File ticket-fix commit creato: {0}", TICKET_FIX_COMMITS_FILE);

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
                LOGGER.log(Level.INFO, "File ticket-buggy classes creato: {0}", TICKET_BUGGY_CLASSES_FILE);


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
                LOGGER.log(Level.INFO, "Coppie classe-release generate: {0}", releaseJavaClasses.size());

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
                LOGGER.log(Level.INFO, "File finale classe-release yes/no creato: {0}", FINAL_CLASS_RELEASE_LABELS_FILE);


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

                LOGGER.log(Level.INFO, "File smell classe-release creato: {0}", CLASS_RELEASE_SMELLS_FILE);

                LOGGER.log(Level.INFO, "File errori PMD creato: {0}", PMD_ANALYSIS_ERRORS_FILE);


                /*
                 * STEP 15:
                 * Merge tra dataset classe-release yes/no e smell PMD.
                 */
                FinalDatasetWithSmellsCsvWriter.writeFinalDatasetWithSmells(
                        FINAL_CLASS_RELEASE_LABELS_FILE,
                        CLASS_RELEASE_SMELLS_FILE,
                        FINAL_DATASET_WITH_SMELLS_FILE
                );

                LOGGER.log(Level.INFO, "Dataset finale con NSmells creato: {0}", FINAL_DATASET_WITH_SMELLS_FILE);


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
            LOGGER.log(Level.INFO, "Coppie classe-release generate: {0}", releaseJavaClasses.size());

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

            LOGGER.log(Level.INFO, "Dataset finale metriche + label creato: {0}", FINAL_METRIC_DATASET_FILE);


            LOGGER.log(Level.INFO, "File metriche classe-release creato: {0}", CLASS_RELEASE_METRICS_FILE);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Errore durante l'esecuzione del flusso principale.", e);
        }
    }



    private static boolean csvExists(String filePath) throws IOException {
        Path path = Path.of(filePath);
        return Files.isRegularFile(path) && Files.size(path) > 0;
    }
}
