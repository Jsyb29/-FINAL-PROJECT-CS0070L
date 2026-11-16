package graveyard.model;

/** Counts come from one consistent plot-list query. */
public record CemeterySummary(int totalPlots, int availablePlots, int occupiedPlots) {
    public double occupancyPercent() {
        return totalPlots == 0 ? 0.0 : (double) occupiedPlots / totalPlots * 100;
    }
}
