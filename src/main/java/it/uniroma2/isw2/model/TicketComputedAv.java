package it.uniroma2.isw2.model;

public class TicketComputedAv {
    private final String ticketId;
    private final String computedAv;

    public TicketComputedAv(String ticketId, String computedAv) {
        this.ticketId = ticketId;
        this.computedAv = computedAv;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getComputedAv() {
        return computedAv;
    }
}