package graveyard.model;

import java.time.LocalDate;
import java.util.Objects;

/**a burial record HAS A deceased person and refers to a plot. */
public final class BurialRecord {
    private final long recordId;
    private final Deceased deceased;
    private final LocalDate burialDate;
    private final String plotNumber;

    public BurialRecord(long recordId, Deceased deceased,
                        LocalDate burialDate, String plotNumber) {
        this.recordId = recordId;
        this.deceased = Objects.requireNonNull(deceased);
        this.burialDate = Objects.requireNonNull(burialDate);
        this.plotNumber = Objects.requireNonNull(plotNumber);
    }

    public long getRecordId() { return recordId; }
    public Deceased getDeceased() { return deceased; }
    public LocalDate getBurialDate() { return burialDate; }
    public String getPlotNumber() { return plotNumber; }
}
