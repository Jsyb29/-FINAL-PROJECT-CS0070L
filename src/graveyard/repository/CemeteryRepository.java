package graveyard.repository;

import graveyard.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * This is the contract the business logic relies on, so it
 * never has to know about SQL or the UI directly.
 */

public interface CemeteryRepository {
    List<Plot> findPlots();
    Optional<Plot> findPlot(String plotNumber);
    void insertPlot(String plotNumber, String section);
    boolean updateSection(String plotNumber, String section);
    boolean deleteEmptyPlot(String plotNumber);
    List<BurialRecord> findBurials();
    Optional<BurialRecord> findBurial(long recordId);
    BurialRecord insertBurial(BurialRequest request);
    boolean updateBurial(long recordId, Deceased deceased, LocalDate burialDate);
    void seedIfEmpty(List<Plot> plots, List<BurialRequest> burials);
}
