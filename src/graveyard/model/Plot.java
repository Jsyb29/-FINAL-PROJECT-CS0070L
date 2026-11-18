package graveyard.model;

import java.util.Objects;

/** A read-only snapshot of a database row. 
Changing this object doesn't touch the actual database. */

public final class Plot {
    private final String plotNumber;
    private final String section;
    private final PlotStatus status;

    public Plot(String plotNumber, String section, PlotStatus status) {
        this.plotNumber = Objects.requireNonNull(plotNumber);
        this.section = Objects.requireNonNull(section);
        this.status = Objects.requireNonNull(status);
    }

    public String getPlotNumber() { return plotNumber; }
    public String getSection() { return section; }
    public PlotStatus getStatus() { return status; }
    public boolean isAvailable() { return status == PlotStatus.AVAILABLE; }
}
