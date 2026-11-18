package graveyard.model;

import java.time.LocalDate;

/** The values the frontend sends over to register a burial. */
public record BurialRequest(String name, LocalDate birthDate, LocalDate deathDate,
                            LocalDate burialDate, String plotNumber) { }
