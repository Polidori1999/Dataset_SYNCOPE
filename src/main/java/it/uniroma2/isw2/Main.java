package it.uniroma2.isw2;

import it.uniroma2.isw2.io.*;
import it.uniroma2.isw2.labeling.*;
import it.uniroma2.isw2.model.*;
import it.uniroma2.isw2.proportion.*;
import it.uniroma2.isw2.utils.ReleaseSelector;
import it.uniroma2.isw2.utils.TicketReleaseMapper;


import java.io.IOException;
import java.util.List;



/**
 * Classe principale del progetto.
 * Coordina le fasi iniziali della costruzione del dataset.
 */
public class Main {

    private static final String PROJECT_NAME = "SYNCOPE";
    private static final String RELEASES_FILE = PROJECT_NAME + "VersionInfo.csv";
    private static final String TICKETS_FILE = PROJECT_NAME + "Tickets.csv";
    private static final String ENHANCED_TICKETS_FILE = PROJECT_NAME + "_EnhancedTickets.csv";
    private static final String AV_BASED_TICKETS_FILE = PROJECT_NAME + "_AVBasedTickets.csv";
    private static final String ESTIMATED_TICKETS_FILE = PROJECT_NAME + "_EstimatedTickets.csv";
    private static final double RELEASES_TO_KEEP = 0.34;
    private static final String SELECTED_RELEASES_TICKET_MAP_FILE = PROJECT_NAME + "_SelectedReleaseTicketMap.csv";
    private static final String ESTIMATED_TICKETS_WITH_COMPUTED_AV_FILE =
            PROJECT_NAME + "_EstimatedTickets_WithComputedAV.csv";



    private static final String FINAL_CLASS_RELEASE_LABELS_FILE =
            PROJECT_NAME + "_FinalClassReleaseLabels.csv";


    private static final String PROJECT_REPO_PATH =
            "/home/leonardo/uni/isw2/syncope";

    private static final String TICKET_FIX_COMMITS_FILE =
            PROJECT_NAME + "_TicketFixCommits.csv";

    private static final String TICKET_BUGGY_CLASSES_FILE =
            PROJECT_NAME + "_TicketBuggyClasses.csv";

    private static final String BUGGY_CLASS_RELEASE_LABELS_FILE =
            PROJECT_NAME + "_BuggyClassReleaseLabels.csv";
    public static void main(String[] args) throws IOException {
        System.out.println("Avvio costruzione dataset del progetto " + PROJECT_NAME + ".");

        List<EnhancedTicket> estimatedTickets = null;
        List<Release> selectedReleases = null;
        List<Release> allReleases = null;
        try {
            /*
             * STEP 1:
             * Lettura di tutte le release del progetto.
             */
            allReleases = ReleaseCsvReader.loadReleases(RELEASES_FILE);
            System.out.println("Release lette: " + allReleases.size());

            /*
             * STEP 1.1:
             * Selezione del primo 34% delle release
             * (equivalente a ignorare l'ultimo 66%).
             */
            selectedReleases = ReleaseSelector.selectInitialReleases(allReleases, RELEASES_TO_KEEP);
            System.out.println("Release selezionate per il dataset finale: " + selectedReleases.size());

            /*
             * STEP 2:
             * Lettura dei ticket validi.
             */
            List<Ticket> tickets = TicketCsvReader.loadTickets(TICKETS_FILE);
            System.out.println("Ticket letti: " + tickets.size());

            /*
             * STEP 2.1:
             * Mapping dei ticket alle sole release selezionate.
             * Questo è utile per iniziare a delimitare il perimetro del dataset.
             */
            List<TicketReleaseMapping> selectedMappings =
                    TicketReleaseMapper.mapTicketsToSelectedReleases(tickets, selectedReleases);
            MappingCsvWriter.writeMappingsToCsv(SELECTED_RELEASES_TICKET_MAP_FILE, selectedMappings);
            System.out.println("File mapping ticket-release selezionate creato: "
                    + SELECTED_RELEASES_TICKET_MAP_FILE);

            /*
             * STEP 3:
             * Arricchimento dei ticket con OV e FV.
             * QUI continuiamo a usare tutte le release, non solo quelle selezionate.
             */
            List<EnhancedTicket> enhancedTickets =
                    TicketVersionEnricher.enrichTickets(tickets, allReleases);
            EnhancedTicketCsvWriter.writeEnhancedTickets(ENHANCED_TICKETS_FILE, enhancedTickets);
            System.out.println("File ticket arricchiti creato: " + ENHANCED_TICKETS_FILE);

            /*
             * STEP 4:
             * Per i ticket con AV, si assegna una IV iniziale.
             */
            List<EnhancedTicket> avBasedTickets =
                    AffectedVersionIVResolver.assignInitialIVFromAV(enhancedTickets, allReleases);
            EnhancedTicketCsvWriter.writeEnhancedTickets(AV_BASED_TICKETS_FILE, avBasedTickets);
            System.out.println("File ticket con IV iniziale da AV creato: " + AV_BASED_TICKETS_FILE);

            /*
             * STEP 5:
             * Calcolo della proportion media.
             */
            double proportion =
                    ProportionCalculator.calculateAverageProportion(avBasedTickets, allReleases);
            System.out.println("Proportion media calcolata: " + proportion);

            /*
             * STEP 6:
             * Stima della IV per i ticket che ancora non la possiedono.
             */
            estimatedTickets = InjectedVersionEstimator.estimateMissingInjectedVersions(
                    avBasedTickets, allReleases, proportion);
            EnhancedTicketCsvWriter.writeEnhancedTickets(ESTIMATED_TICKETS_FILE, estimatedTickets);
            System.out.println("File ticket con IV stimata creato: " + ESTIMATED_TICKETS_FILE);

            System.out.println("Selezione release + fase AV -> IV -> P completate con successo.");

            /*
             * STEP 7:
             * Scrittura di un CSV aggiuntivo con la colonna ComputedAV.
             * ComputedAV è sempre calcolata come intervallo [IV, FV).
             */
            EnhancedTicketComputedAvCsvWriter.writeEnhancedTicketsWithComputedAv(
                    ESTIMATED_TICKETS_WITH_COMPUTED_AV_FILE,
                    estimatedTickets,
                    allReleases
            );
            System.out.println("File ticket con ComputedAV creato: "
                    + ESTIMATED_TICKETS_WITH_COMPUTED_AV_FILE);







            /*
             * STEP 7.1:
             * Lettura del CSV con ComputedAV appena scritto.
             * Da questo punto in poi il labeling usa il valore di ComputedAV presente nel file,
             * senza ricalcolarlo in memoria.
             */
            List<TicketComputedAv> ticketsWithComputedAv =
                    EstimatedTicketComputedAvCsvReader.loadTicketsWithComputedAv(
                            ESTIMATED_TICKETS_WITH_COMPUTED_AV_FILE
                    );
            System.out.println("Ticket con ComputedAV letti dal CSV: " + ticketsWithComputedAv.size());

            /*
             * STEP 8:
             * Ricerca dei fix commit associati ai ticket nel repository Git.
             */
            List<TicketFixCommit> ticketFixCommits =
                    FixCommitFinder.findFixCommits(estimatedTickets, PROJECT_REPO_PATH);
            TicketFixCommitCsvWriter.writeTicketFixCommits(TICKET_FIX_COMMITS_FILE, ticketFixCommits);
            System.out.println("File ticket-fix commit creato: " + TICKET_FIX_COMMITS_FILE);

            /*
             * STEP 9:
             * Estrazione semplificata delle buggy classes con SZZ.
             * Sono considerate solo classi Java di produzione.
             */
            List<TicketBuggyClass> ticketBuggyClasses =
                    SimplifiedSzzBuggyClassExtractor.extractBuggyClasses(
                            ticketFixCommits,
                            PROJECT_REPO_PATH
                    );
            TicketBuggyClassCsvWriter.writeTicketBuggyClasses(
                    TICKET_BUGGY_CLASSES_FILE,
                    ticketBuggyClasses
            );
            System.out.println("File ticket-buggy classes creato: " + TICKET_BUGGY_CLASSES_FILE);

            /*
             * STEP 10:
             * Per ogni release selezionata si trova il commit snapshot,
             * cioè l'ultimo commit disponibile prima o alla data della release.
             */
            List<ReleaseSnapshot> releaseSnapshots =
                    GitReleaseSnapshotFinder.findSnapshots(
                            selectedReleases,
                            PROJECT_REPO_PATH
                    );
            System.out.println("Snapshot di release trovati: " + releaseSnapshots.size());

            /*
             * STEP 11:
             * Per ogni snapshot si estraggono tutte le classi Java di produzione presenti.
             */
            List<ReleaseJavaClass> releaseJavaClasses =
                    GitJavaClassInventoryBuilder.buildInventory(
                            PROJECT_NAME,
                            releaseSnapshots,
                            PROJECT_REPO_PATH
                    );
            System.out.println("Coppie classe-release generate: " + releaseJavaClasses.size());

            /*
             * STEP 12:
             * Costruzione dei positivi, filtrati sulle classi che esistono davvero
             * nello snapshot della release.
             */
            List<BuggyClassReleaseLabel> buggyClassReleaseLabels =
                    BuggyClassReleaseLabeler.buildPositiveLabels(
                            PROJECT_NAME,
                            ticketsWithComputedAv,
                            selectedReleases,
                            ticketBuggyClasses,
                            releaseJavaClasses
                    );
            BuggyClassReleaseLabelCsvWriter.writeLabels(
                    BUGGY_CLASS_RELEASE_LABELS_FILE,
                    buggyClassReleaseLabels
            );
            System.out.println("File labeling positivo classe-release creato: "
                    + BUGGY_CLASS_RELEASE_LABELS_FILE);

            /*
             * STEP 13:
             * Merge finale yes/no.
             */
            FinalDatasetCsvWriter.writeFinalDataset(
                    FINAL_CLASS_RELEASE_LABELS_FILE,
                    releaseJavaClasses,
                    buggyClassReleaseLabels
            );
            System.out.println("File finale classe-release yes/no creato: "
                    + FINAL_CLASS_RELEASE_LABELS_FILE);

        } catch (IOException e) {
            System.out.println("Errore durante l'esecuzione del flusso principale.");
            e.printStackTrace();
        }

    }
}