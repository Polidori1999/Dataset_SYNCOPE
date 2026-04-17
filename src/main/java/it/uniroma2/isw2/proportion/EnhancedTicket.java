package it.uniroma2.isw2.proportion;

/**
 * Modello dati esteso per un ticket.
 * Contiene le informazioni necessarie per la fase di proportion.
 */
public class EnhancedTicket {
    private final String ticketId;
    private final String creationDate;
    private final String resolutionDate;
    private final String affectedVersions;
    private final String openingVersion;
    private final String fixedVersion;
    private final String injectedVersion;
    private final String injectedVersionSource;

    public EnhancedTicket(String ticketId,
                          String creationDate,
                          String resolutionDate,
                          String affectedVersions,
                          String openingVersion,
                          String fixedVersion,
                          String injectedVersion,
                          String injectedVersionSource) {
        this.ticketId = ticketId;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.affectedVersions = affectedVersions;
        this.openingVersion = openingVersion;
        this.fixedVersion = fixedVersion;
        this.injectedVersion = injectedVersion;
        this.injectedVersionSource = injectedVersionSource;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getResolutionDate() {
        return resolutionDate;
    }

    public String getAffectedVersions() {
        return affectedVersions;
    }

    public String getOpeningVersion() {
        return openingVersion;
    }

    public String getFixedVersion() {
        return fixedVersion;
    }

    public String getInjectedVersion() {
        return injectedVersion;
    }

    public String getInjectedVersionSource() {
        return injectedVersionSource;
    }
}