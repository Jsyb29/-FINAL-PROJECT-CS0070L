package graveyard.model;

import graveyard.exception.ValidationException;
import java.time.LocalDate;
import java.util.Objects;

/** A deceased person is a kind of Person. Since everything here is immutable, 
it's safe to pass straight to the UI. */

public final class Deceased extends Person {
    private final LocalDate deathDate;

    public Deceased(String name, LocalDate birthDate, LocalDate deathDate) {
        super(name, birthDate);
        this.deathDate = Objects.requireNonNull(deathDate, "Day of death is required.");
        if (birthDate.isAfter(deathDate)) {
            throw new ValidationException("Birth date cannot be after day of death.");
        }
    }

    public LocalDate getDeathDate() { return deathDate; }

    @Override
    public String getSummary() {
        return getName() + " / " + getBirthDate() + " / " + deathDate;
    }
}
