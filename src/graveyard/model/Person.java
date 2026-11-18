package graveyard.model;

import graveyard.util.Inputs;
import java.time.LocalDate;
import java.util.Objects;

/** Abstract base class for a person — 
keeps its fields private and only exposes them through getters. */

public abstract class Person {
    private final String name;
    private final LocalDate birthDate;

    protected Person(String name, LocalDate birthDate) {
        this.name = Inputs.text(name, "Name", 120);
        this.birthDate = Objects.requireNonNull(birthDate, "Birth date is required.");
    }

    public String getName() { return name; }
    public LocalDate getBirthDate() { return birthDate; }
    public abstract String getSummary();
}
