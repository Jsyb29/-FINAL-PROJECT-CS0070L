package graveyard.model;

import java.util.Objects;

/** Read-only database snapshot. Changing this object cannot change the database. */
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
