package it.uniroma2.isw2.model;

public class TicketBuggyClass {
    private final String ticketId;
    private final String fixCommitHash;
    private final String classPath;

    public TicketBuggyClass(String ticketId, String fixCommitHash, String classPath) {
        this.ticketId = ticketId;
        this.fixCommitHash = fixCommitHash;
        this.classPath = classPath;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getFixCommitHash() {
        return fixCommitHash;
    }

    public String getClassPath() {
        return classPath;
    }
}