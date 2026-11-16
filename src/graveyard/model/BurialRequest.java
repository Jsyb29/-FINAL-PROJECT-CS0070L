package graveyard.model;

import java.time.LocalDate;

/** M5 record: the values the frontend sends to register a burial. */
public record BurialRequest(String name, LocalDate birthDate, LocalDate deathDate,
                            LocalDate burialDate, String plotNumber) { }
