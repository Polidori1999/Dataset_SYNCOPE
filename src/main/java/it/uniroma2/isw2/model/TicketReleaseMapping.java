package it.uniroma2.isw2.model;

public class TicketReleaseMapping {
    private final String ticketId;
    private final String creationDate;
    private final String resolutionDate;
    private final int releaseIndex;
    private final String releaseId;
    private final String releaseName;
    private final String releaseDate;

    public TicketReleaseMapping(String ticketId,
                                String creationDate,
                                String resolutionDate,
                                int releaseIndex,
                                String releaseId,
                                String releaseName,
                                String releaseDate) {
        this.ticketId = ticketId;
        this.creationDate = creationDate;
        this.resolutionDate = resolutionDate;
        this.releaseIndex = releaseIndex;
        this.releaseId = releaseId;
        this.releaseName = releaseName;
        this.releaseDate = releaseDate;
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

    public int getReleaseIndex() {
        return releaseIndex;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public String getReleaseDate() {
        return releaseDate;
    }
}