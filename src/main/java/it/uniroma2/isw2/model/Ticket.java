package it.uniroma2.isw2.model;

public class Ticket {
    private final String ticketId;
    private final String creationDate;
    private final String resolutionDate;
    private final String affectedVersions;

    public Ticket(String ticketId, String creationDate, String resolutionDate, String affectedVersions) {
        this.ticketId = ticketId;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.affectedVersions = affectedVersions;
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
}