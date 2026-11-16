package graveyard.util;

import graveyard.exception.ValidationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class Inputs {
    private Inputs() { }

    public static String text(String raw, String label, int maxLength) {
        if (raw == null) throw new ValidationException(label + " is required.");
        String value = raw.strip().replaceAll("(?U)\\s+", " ");
        if (value.isEmpty()) throw new ValidationException(label + " is required.");
        if (value.length() > maxLength) {
            throw new ValidationException(label + " must be at most " + maxLength + " characters.");
        }
        return value;
    }

    public static String plotNumber(String raw) {
        if (raw == null) throw new ValidationException("Plot number is required.");
        String value = raw.replaceAll("(?U)\\s+", "").toUpperCase(Locale.ROOT);
        return text(value, "Plot number", 20);
    }

    /** Strict calendar parsing: 2025-02-29 is rejected, not silently adjusted. */
    public static LocalDate date(String raw, String label) {
        if (raw == null || !raw.strip().matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new ValidationException(label + " must use YYYY-MM-DD.");
        }
        try {
            LocalDate date = LocalDate.parse(raw.strip());
            requireDate(date, label);
            return date;
        } catch (DateTimeParseException ex) {
            throw new ValidationException(label + " is not a valid calendar date.");
        }
    }

    public static void requireDate(LocalDate date, String label) {
        if (date == null || date.getYear() < 1 || date.getYear() > 9999) {
            throw new ValidationException(label + " must be a date from year 0001 to 9999.");
        }
    }
}
