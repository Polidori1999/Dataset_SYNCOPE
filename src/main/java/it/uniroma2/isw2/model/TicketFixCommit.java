package it.uniroma2.isw2.model;

public class TicketFixCommit {
    private final String ticketId;
    private final String fixCommitHash;
    private final long commitEpochSeconds;

    public TicketFixCommit(String ticketId, String fixCommitHash, long commitEpochSeconds) {
        this.ticketId = ticketId;
        this.fixCommitHash = fixCommitHash;
        this.commitEpochSeconds = commitEpochSeconds;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getFixCommitHash() {
        return fixCommitHash;
    }

    public long getCommitEpochSeconds() {
        return commitEpochSeconds;
    }
}