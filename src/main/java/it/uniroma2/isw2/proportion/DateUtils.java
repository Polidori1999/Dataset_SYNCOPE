package it.uniroma2.isw2.proportion;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility per la gestione delle date.
 */
public class DateUtils {

    private DateUtils() {
    }

    /**
     * Converte una data del file release, ad esempio:
     * 2006-08-26T00:00
     */
    public static LocalDateTime parseReleaseDate(String date) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    /**
     * Converte una data JIRA del ticket, ad esempio:
     * 2011-03-10T12:34:56.000+0000
     */
    public static LocalDateTime parseTicketDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")).toLocalDateTime();
    }
}