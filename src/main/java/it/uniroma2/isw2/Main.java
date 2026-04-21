package it.uniroma2.isw2;

import it.uniroma2.isw2.io.MappingCsvWriter;
import it.uniroma2.isw2.io.ReleaseCsvReader;
import it.uniroma2.isw2.io.TicketCsvReader;
import it.uniroma2.isw2.model.Release;
import it.uniroma2.isw2.model.Ticket;
import it.uniroma2.isw2.model.TicketReleaseMapping;
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

    public static void main(String[] args) {
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
            List<EnhancedTicket> estimatedTickets =
                    InjectedVersionEstimator.estimateMissingInjectedVersions(
                            avBasedTickets, allReleases, proportion);
            EnhancedTicketCsvWriter.writeEnhancedTickets(ESTIMATED_TICKETS_FILE, estimatedTickets);
            System.out.println("File ticket con IV stimata creato: " + ESTIMATED_TICKETS_FILE);

            System.out.println("Selezione release + fase AV -> IV -> P completate con successo.");

        } catch (IOException e) {
            System.out.println("Errore durante l'esecuzione del flusso principale.");
            e.printStackTrace();
        }
    }
    }