package graveyard.model;

/** These counts all come from the same plot-list query, so they stay consistent with each other. */

public record CemeterySummary(int totalPlots, int availablePlots, int occupiedPlots) {
    public double occupancyPercent() {
        return totalPlots == 0 ? 0.0 : (double) occupiedPlots / totalPlots * 100;
    }
}
