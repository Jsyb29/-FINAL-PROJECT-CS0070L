package graveyard.service;

import graveyard.db.Database;
import graveyard.exception.ValidationException;
import graveyard.model.*;
import graveyard.repository.CemeteryRepository;
import graveyard.repository.JdbcCemeteryRepository;
import graveyard.util.Inputs;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/** Frontend entry point: validation, business rules, collections, and database calls. */
public final class CemeteryService {
    private final CemeteryRepository repository;
    private final Clock clock;

    public CemeteryService(CemeteryRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public CemeteryService(CemeteryRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
    }

    public static CemeteryService openDefault() {
        Database database = Database.defaultDatabase();
        database.initialize();
        // Interface variable -> JDBC implementation: runtime polymorphism.
        CemeteryRepository repository = new JdbcCemeteryRepository(database);
        return new CemeteryService(repository);
    }

    public void addPlot(String number, String section) {
        repository.insertPlot(Inputs.plotNumber(number), Inputs.text(section, "Section", 60));
    }

    public void updatePlotSection(String number, String section) {
        if (!repository.updateSection(Inputs.plotNumber(number), Inputs.text(section, "Section", 60))) {
            throw new ValidationException("Plot not found.");
        }
    }

    public void deleteAvailablePlot(String number) {
        if (!repository.deleteEmptyPlot(Inputs.plotNumber(number))) {
            throw new ValidationException("Plot is occupied or does not exist. Burial records are retained.");
        }
    }

    public List<Plot> getPlots() { return List.copyOf(repository.findPlots()); }

    public List<Plot> getAvailablePlots() {
        List<Plot> results = new ArrayList<>(repository.findPlots());
        results.removeIf(plot -> !plot.isAvailable());
        return List.copyOf(results);
    }

    public Set<String> getSections() {
        Set<String> sections = new HashSet<>();
        for (Plot plot : repository.findPlots()) sections.add(plot.getSection());
        return Collections.unmodifiableSet(sections);
    }

    public Map<String, Plot> getPlotsByNumber() {
        Map<String, Plot> lookup = new HashMap<>();
        for (Plot plot : repository.findPlots()) lookup.put(plot.getPlotNumber(), plot);
        return Collections.unmodifiableMap(lookup);
    }

    public CemeterySummary getSummary() {
        List<Plot> plots = repository.findPlots();
        int available = 0;
        for (Plot plot : plots) if (plot.isAvailable()) available++;
        return new CemeterySummary(plots.size(), available, plots.size() - available);
    }

    public BurialRecord registerBurial(BurialRequest request) {
        if (request == null) throw new ValidationException("Burial details are required.");
        Deceased person = validatePerson(request.name(), request.birthDate(), request.deathDate(), request.burialDate());
        String number = Inputs.plotNumber(request.plotNumber());
        Plot plot = repository.findPlot(number).orElseThrow(() -> new ValidationException("Plot not found."));
        if (!plot.isAvailable()) throw new ValidationException("This plot is already occupied.");
        return repository.insertBurial(new BurialRequest(person.getName(), person.getBirthDate(),
                person.getDeathDate(), request.burialDate(), number));
    }

    /** Overload for text fields. Date of death and burial date are separate inputs. */
    public BurialRecord registerBurial(String name, String birthDate, String deathDate,
                                       String burialDate, String plotNumber) {
        return registerBurial(new BurialRequest(name, Inputs.date(birthDate, "Birth date"),
                Inputs.date(deathDate, "Day of death"), Inputs.date(burialDate, "Burial date"), plotNumber));
    }

    /** Corrects details without moving a burial to another plot. */
    public void updateBurial(long id, String name, LocalDate birth, LocalDate death, LocalDate burial) {
        requireId(id);
        Deceased person = validatePerson(name, birth, death, burial);
        if (!repository.updateBurial(id, person, burial)) throw new ValidationException("Burial record not found.");
    }

    public BurialRecord getBurial(long id) {
        requireId(id);
        return repository.findBurial(id).orElseThrow(() -> new ValidationException("Burial record not found."));
    }

    public List<BurialRecord> searchBurials(String query) { return searchBurials(query, SortOrder.NAME); }

    /** Empty search returns all records. Matching is a literal, case-insensitive substring. */
    public List<BurialRecord> searchBurials(String query, SortOrder order) {
        if (query == null || order == null) throw new ValidationException("Search and sort order are required.");
        String nameQuery = query.strip().replaceAll("(?U)\\s+", " ").toLowerCase(Locale.ROOT);
        String plotQuery = query.replaceAll("(?U)\\s+", "").toUpperCase(Locale.ROOT);
        List<BurialRecord> matches = new ArrayList<>(repository.findBurials());
        // M6 Iterator.remove is safe while traversing; list.remove inside for-each is not.
        Iterator<BurialRecord> iterator = matches.iterator();
        while (iterator.hasNext()) {
            BurialRecord record = iterator.next();
            if (!record.getDeceased().getName().toLowerCase(Locale.ROOT).contains(nameQuery)
                    && !record.getPlotNumber().contains(plotQuery)) iterator.remove();
        }
        Comparator<BurialRecord> comparator = switch (order) {
            case NAME -> Comparator.comparing(r -> r.getDeceased().getName(), String.CASE_INSENSITIVE_ORDER);
            case DEATH_DATE -> Comparator.comparing(r -> r.getDeceased().getDeathDate());
            case PLOT_NUMBER -> Comparator.comparing(BurialRecord::getPlotNumber);
        };
        return sortedCopy(matches, comparator.thenComparingLong(BurialRecord::getRecordId));
    }

    public void seedDemo(List<Plot> plots, List<BurialRequest> burials) {
        if (plots == null || burials == null) throw new ValidationException("Demo data is required.");
        Set<String> numbers = new HashSet<>();
        List<Plot> cleanedPlots = new ArrayList<>();
        for (Plot p : plots) {
            String number = Inputs.plotNumber(p.getPlotNumber());
            if (!numbers.add(number)) throw new ValidationException("Duplicate demo plot: " + number);
            cleanedPlots.add(new Plot(number, Inputs.text(p.getSection(), "Section", 60), PlotStatus.AVAILABLE));
        }
        Set<String> occupied = new HashSet<>();
        List<BurialRequest> cleanedBurials = new ArrayList<>();
        for (BurialRequest r : burials) {
            Deceased p = validatePerson(r.name(), r.birthDate(), r.deathDate(), r.burialDate());
            String number = Inputs.plotNumber(r.plotNumber());
            if (!numbers.contains(number) || !occupied.add(number)) {
                throw new ValidationException("Demo burial needs a unique existing plot: " + number);
            }
            cleanedBurials.add(new BurialRequest(p.getName(), p.getBirthDate(), p.getDeathDate(), r.burialDate(), number));
        }
        repository.seedIfEmpty(cleanedPlots, cleanedBurials);
    }

    private Deceased validatePerson(String name, LocalDate birth, LocalDate death, LocalDate burial) {
        Inputs.requireDate(birth, "Birth date");
        Inputs.requireDate(death, "Day of death");
        Inputs.requireDate(burial, "Burial date");
        Deceased person = new Deceased(name, birth, death);
        if (death.isAfter(LocalDate.now(clock))) throw new ValidationException("Day of death cannot be in the future.");
        if (burial.isBefore(death)) throw new ValidationException("Burial date cannot be before day of death.");
        return person; // Future burial dates ARE allowed for planned burials.
    }

    private void requireId(long id) {
        if (id <= 0) throw new ValidationException("Record ID must be a positive number.");
    }

    private static <T> List<T> sortedCopy(List<T> values, Comparator<? super T> comparator) {
        List<T> copy = new ArrayList<>(values);
        copy.sort(comparator);
        return List.copyOf(copy);
    }
}
