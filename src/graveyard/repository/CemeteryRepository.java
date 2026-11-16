package graveyard.repository;

import graveyard.model.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** M4 interface: business logic depends on this contract, not SQL or a window. */
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
