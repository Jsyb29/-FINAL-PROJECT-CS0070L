package graveyard.model;

/** Status is calculated from burial records, never written by a user. */
public enum PlotStatus {
    AVAILABLE, OCCUPIED;

    public String getLabel() {
        return switch (this) {
            case AVAILABLE -> "Available";
            case OCCUPIED -> "Occupied";
        };
    }
}
