package graveyard.ui;

import graveyard.async.BackgroundTasks;
import graveyard.model.BurialRecord;
import graveyard.model.BurialRequest;
import graveyard.model.CemeterySummary;
import graveyard.model.Plot;
import graveyard.service.CemeteryService;
import graveyard.util.ErrorMessages;
import graveyard.util.Inputs;
import graveyard.exception.CemeteryException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * Main window of the integrated Simple Graveyard System.
 *
 * The frontend owns: windows, tables, dialogs, listeners, input checks and messages.
 * The backend (CemeteryService) owns: business rules and the database.
 *
 * Flow: button click -> validation (EDT) -> BackgroundTasks worker -> CemeteryService
 *       -> success/error callback on the EDT -> table + status update.
 * Swing fields are only read on the EDT, and table models are only changed in callbacks.
 */
@SuppressWarnings("serial") // Swing components are not used as a persistence format.
public final class MainFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(MainFrame.class.getName());
    private static final String APP_TITLE = "Simple Graveyard System";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD
    private static final String ALL_SECTIONS = "All sections";
    private static final String ALL_STATUSES = "All statuses";
    private static final String AVAILABLE = "Available";
    private static final String OCCUPIED = "Occupied";
    private static final Color ERROR_COLOR = new Color(0xB3261E);
    private static final Color OK_COLOR = new Color(0x1B7F3B);
    private static final String SEARCH_HINT = "Type a name or plot number and press Search (empty = all records).";

    // Callbacks run on the Swing event dispatch thread.
    private final BackgroundTasks jobs = new BackgroundTasks(SwingUtilities::invokeLater);
    private volatile CemeteryService service;
    private final JMenuItem retryConnection = new JMenuItem("Retry Database Connection");

    // EDT-only state
    private boolean ready;    // database opened
    private boolean closing;  // window is shutting down: schedule no new jobs
    private int pending;      // number of running background jobs
    private Snapshot latest = Snapshot.empty();

    private final List<AbstractButton> guardedButtons = new ArrayList<>();
    private final List<RefreshablePanel> panels = new ArrayList<>();
    private final JLabel summaryLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ");
    private final Color normalStatusColor;

    public MainFrame() {
        super(APP_TITLE);
        normalStatusColor = statusLabel.getForeground();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // we close safely ourselves
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                beginShutdown();
            }
        });

        panels.add(new PlotsPanel());
        panels.add(new BurialsPanel());
        panels.add(new SearchPanel());

        JTabbedPane tabs = new JTabbedPane();
        for (RefreshablePanel p : panels) {
            tabs.addTab(p.title(), p);
        }

        summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        setLayout(new BorderLayout());
        add(summaryLabel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        setJMenuBar(buildMenuBar());

        setSize(940, 560);
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(null);

        // Open the database off the EDT; keep the buttons disabled until it is ready.
        openDatabase();
    }

    private void openDatabase() {
        if (closing || pending > 0 || ready) return;
        refreshControls();
        begin("Opening database...");
        submitTask(CemeteryService::openDefault,
                readyService -> {
                    service = readyService;
                    ready = true;
                    done();
                    refreshAll("Ready");
                },
                error -> fail(error, ErrorMessages.forUser(error)));
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------
    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu("File");
        JMenuItem refresh = guarded(new JMenuItem("Refresh"));
        JMenuItem exit = new JMenuItem("Exit");
        refresh.addActionListener(e -> refreshAll("Records refreshed."));
        exit.addActionListener(e -> beginShutdown());
        file.add(refresh);
        retryConnection.addActionListener(e -> openDatabase());
        file.add(retryConnection);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this,
                APP_TITLE + "\nRecord cemetery plots and burials.\nDates use YYYY-MM-DD.",
                "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(help);
        return bar;
    }

    // ------------------------------------------------------------------
    // Busy / ready state, messages
    // ------------------------------------------------------------------
    private <T extends AbstractButton> T guarded(T button) {
        guardedButtons.add(button);
        return button;
    }

    private void refreshControls() {
        boolean enabled = ready && pending == 0 && !closing;
        retryConnection.setEnabled(!ready && pending == 0 && !closing);
        for (AbstractButton b : guardedButtons) {
            b.setEnabled(enabled);
        }
    }

    private void begin(String message) {
        pending++;
        refreshControls();
        if (message != null) {
            setStatus(message, false);
        }
    }

    private void done() {
        if (pending > 0) {
            pending--;
        }
        refreshControls();
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? ERROR_COLOR : normalStatusColor);
    }

    private void showWarning(String message) {
        setStatus(message, true);
        JOptionPane.showMessageDialog(this, message, APP_TITLE, JOptionPane.WARNING_MESSAGE);
    }

    /** Error callback: show the backend's user message, log the original exception. */
    private void fail(Throwable error, Object userMessage) {
        done();
        LOG.log(Level.WARNING, "Operation failed", error);
        if (closing) return;
        String message = String.valueOf(userMessage);
        setStatus(message, true);
        JOptionPane.showMessageDialog(this, message, APP_TITLE, JOptionPane.WARNING_MESSAGE);
    }

    private boolean notReady() {
        if (closing) return true;
        if (pending > 0) {
            setStatus("Please wait for the current operation to finish.", false);
            return true;
        }
        if (!ready || service == null) {
            showWarning("The database is not connected. Use File > Retry Database Connection.");
            return true;
        }
        return false;
    }

    /** Every task callback runs on the EDT and stops touching a closing window. */
    private <T> void submitTask(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        if (closing) {
            done();
            return;
        }
        jobs.submit(task, value -> {
            if (closing) { done(); return; }
            success.accept(value);
        }, error -> {
            if (closing) {
                done();
                LOG.log(Level.WARNING, "Operation failed while closing", error);
                return;
            }
            failure.accept(error);
        });
    }

    // ------------------------------------------------------------------
    // Loading data
    // ------------------------------------------------------------------

    /** Reloads everything. doneMessage == null keeps the current status text. */
    private void refreshAll(String doneMessage) {
        if (closing || service == null || pending > 0) {
            return;
        }
        final CemeteryService svc = service;
        begin(doneMessage == null ? null : "Loading records...");
        submitTask(() -> new Snapshot(svc.getPlots(), svc.searchBurials(""),
                        svc.getSections(), svc.getSummary()),
                snap -> {
                    done();
                    applySnapshot(snap);
                    if (doneMessage != null) {
                        setStatus(doneMessage, false);
                    }
                },
                error -> fail(error, ErrorMessages.forUser(error)));
    }

    private void applySnapshot(Snapshot snap) {
        latest = snap;
        for (RefreshablePanel p : panels) {
            p.refresh(snap);
        }
        CemeterySummary s = snap.summary;
        summaryLabel.setText(String.format(
                "Plots: %d     Available: %d     Occupied: %d     Occupancy: %.1f%%     Burial records: %d",
                s.totalPlots(), s.availablePlots(), s.occupiedPlots(), s.occupancyPercent(),
                snap.burials.size()));
    }

    // ------------------------------------------------------------------
    // Burial actions (shared by several tabs)
    // ------------------------------------------------------------------
    private void startRegister() {
        if (notReady()) {
            return;
        }
        final CemeteryService svc = service;
        begin("Checking available plots...");
        // Ask the backend again so the dropdown is never stale.
        submitTask(() -> svc.getAvailablePlots(),
                available -> {
                    done();
                    if (closing) {
                        return;
                    }
                    if (available.isEmpty()) {
                        showWarning("There are no available plots. Add a plot first on the Plots tab.");
                        return;
                    }
                    List<Plot> choices = new ArrayList<>(available);
                    Collections.sort(choices, new Comparator<Plot>() {
                        @Override
                        public int compare(Plot a, Plot b) {
                            return a.getPlotNumber().compareToIgnoreCase(b.getPlotNumber());
                        }
                    });
                    submitRegister(BurialDialog.forRegister(this, choices));
                },
                error -> fail(error, ErrorMessages.forUser(error)));
    }

    private void submitRegister(BurialDialog dialog) {
        dialog.setSaveAction(form -> {
            if (notReady()) return;
            final CemeteryService svc = service;
            dialog.setSaving(true);
            begin("Saving burial record...");
            submitTask(() -> svc.registerBurial(new BurialRequest(form.getName(), form.getBirthDate(),
                            form.getDeathDate(), form.getBurialDate(), form.getPlotNumber())),
                    saved -> {
                        done();
                        dialog.dispose();
                        refreshAll("Burial saved: " + saved.getDeceased().getName()
                                + " on plot " + saved.getPlotNumber() + " (record #" + saved.getRecordId() + ").");
                    },
                    error -> {
                        done();
                        LOG.log(Level.WARNING, "Burial save failed", error);
                        dialog.setSaving(false);
                        dialog.showError(ErrorMessages.forUser(error));
                        setStatus("Burial was not saved. Your inputs have been kept.", true);
                        // Keep all typed fields and reload plot choices after a competing booking.
                        begin(null);
                        submitTask(svc::getAvailablePlots, available -> {
                            done();
                            dialog.updateAvailablePlots(available);
                            refreshAll(null);
                        }, refreshError -> {
                            done();
                            LOG.log(Level.WARNING, "Could not refresh available plots", refreshError);
                        });
                    });
        });
        dialog.setVisible(true);
    }

    private void startEdit(BurialRecord selected) {
        if (notReady()) {
            return;
        }
        if (selected == null) {
            showWarning("Select a burial record first.");
            return;
        }
        final long id = selected.getRecordId(); // identify by record ID, never by row or name
        final CemeteryService svc = service;
        begin("Opening record #" + id + "...");
        submitTask(() -> svc.getBurial(id),
                fresh -> {
                    done();
                    if (closing) {
                        return;
                    }
                    if (fresh == null) {
                        showWarning("That record could not be found. The lists will be reloaded.");
                        refreshAll(null);
                        return;
                    }
                    submitEdit(fresh);
                },
                error -> fail(error, ErrorMessages.forUser(error)));
    }

    private void submitEdit(BurialRecord record) {
        Plot plot = latest.plotsByNumber.get(record.getPlotNumber());
        String plotText = record.getPlotNumber() + (plot == null ? "" : "  (" + plot.getSection() + ")");
        BurialDialog dialog = BurialDialog.forEdit(this, record, plotText);
        dialog.setSaveAction(form -> {
            if (notReady()) return;
            final long id = record.getRecordId();
            final CemeteryService svc = service;
            dialog.setSaving(true);
            begin("Saving changes...");
            submitTask(() -> {
                        svc.updateBurial(id, form.getName(), form.getBirthDate(),
                                form.getDeathDate(), form.getBurialDate());
                        return Boolean.TRUE;
                    },
                    ok -> {
                        done();
                        dialog.dispose();
                        refreshAll("Record #" + id + " updated.");
                    },
                    error -> {
                        done();
                        LOG.log(Level.WARNING, "Burial correction failed", error);
                        dialog.setSaving(false);
                        dialog.showError(ErrorMessages.forUser(error));
                        setStatus("Changes were not saved. Your inputs have been kept.", true);
                    });
        });
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Safe shutdown
    // ------------------------------------------------------------------
    private void beginShutdown() {
        if (closing) {
            return;
        }
        closing = true;
        refreshControls();
        setStatus("Closing... finishing any save in progress.", false);
        // Close submissions immediately on the EDT, then wait off the EDT.
        jobs.close();
        for (java.awt.Window window : getOwnedWindows()) window.dispose();
        Thread shutdown = new Thread(() -> {
            boolean interrupted = false;
            try {
                boolean finished = false;
                while (!finished) {
                    try {
                        finished = jobs.awaitTermination(1, TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        interrupted = true; // Preserve accepted saves before disposing.
                    }
                }
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(this::dispose);
            }
        }, "cemetery-shutdown");
        shutdown.start();
    }

    // ------------------------------------------------------------------
    // Data holders
    // ------------------------------------------------------------------

    /** EDT-only token: clearing or restarting a search invalidates older callbacks. */
    static final class SearchVersion {
        private long current;
        long next() { return ++current; }
        void invalidate() { current++; }
        boolean isCurrent(long version) { return version == current; }
    }

    /** Everything the tables need, loaded together on the worker thread. */
    static final class Snapshot {
        final List<Plot> plots;
        final List<BurialRecord> burials;
        final List<String> sections;
        final Map<String, Plot> plotsByNumber;
        final CemeterySummary summary;

        Snapshot(Collection<Plot> plots, Collection<BurialRecord> burials,
                 Collection<String> sections, CemeterySummary summary) {
            List<Plot> sortedPlots = new ArrayList<>(plots);
            Collections.sort(sortedPlots, new Comparator<Plot>() {
                @Override
                public int compare(Plot a, Plot b) {
                    return a.getPlotNumber().compareToIgnoreCase(b.getPlotNumber());
                }
            });
            List<String> sortedSections = new ArrayList<>(sections); // backend order is not guaranteed
            Collections.sort(sortedSections, String.CASE_INSENSITIVE_ORDER);
            Map<String, Plot> byNumber = new HashMap<>();
            for (Plot p : sortedPlots) {
                byNumber.put(p.getPlotNumber(), p);
            }
            this.plots = Collections.unmodifiableList(sortedPlots);
            this.burials = Collections.unmodifiableList(new ArrayList<>(burials));
            this.sections = Collections.unmodifiableList(sortedSections);
            this.plotsByNumber = Collections.unmodifiableMap(byNumber);
            this.summary = summary;
        }

        static Snapshot empty() {
            return new Snapshot(new ArrayList<Plot>(), new ArrayList<BurialRecord>(),
                    new ArrayList<String>(), new CemeterySummary(0, 0, 0));
        }
    }

    /** Checked form values for one burial (dates already parsed). */
    static final class BurialForm {
        private final String name;
        private final LocalDate birthDate;
        private final LocalDate deathDate;
        private final LocalDate burialDate;
        private final String plotNumber;

        BurialForm(String name, LocalDate birthDate, LocalDate deathDate,
                   LocalDate burialDate, String plotNumber) {
            this.name = name;
            this.birthDate = birthDate;
            this.deathDate = deathDate;
            this.burialDate = burialDate;
            this.plotNumber = plotNumber;
        }

        String getName() { return name; }
        LocalDate getBirthDate() { return birthDate; }
        LocalDate getDeathDate() { return deathDate; }
        LocalDate getBurialDate() { return burialDate; }
        String getPlotNumber() { return plotNumber; }
    }

    // ------------------------------------------------------------------
    // Input checks (no Swing, no database). The backend validates again.
    // ------------------------------------------------------------------
    static final class InputValidator {
        private InputValidator() { }

        static String checkPlot(String plotNumber, String section) {
            try {
                Inputs.plotNumber(plotNumber);
                Inputs.text(section, "Section", 60);
                return null;
            } catch (CemeteryException ex) {
                return ex.getMessage();
            }
        }

        static LocalDate parseDate(String text, String label) {
            try {
                return Inputs.date(text, label);
            } catch (CemeteryException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }

        static BurialForm buildBurialForm(String name, String birthText, String deathText,
                                          String burialText, String plotNumber) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Deceased person's name is required.");
            }
            LocalDate birth = parseDate(birthText, "Birth date");
            LocalDate death = parseDate(deathText, "Date of death");
            LocalDate burial = parseDate(burialText, "Burial date");
            if (birth.isAfter(death)) {
                throw new IllegalArgumentException("Birth date cannot be after the date of death.");
            }
            if (death.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Date of death cannot be in the future.");
            }
            if (burial.isBefore(death)) {
                throw new IllegalArgumentException("Burial date cannot be before the date of death.");
            }
            try {
                return new BurialForm(Inputs.text(name, "Name", 120), birth, death, burial,
                        Inputs.plotNumber(plotNumber));
            } catch (CemeteryException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
    }

    // ------------------------------------------------------------------
    // Table models
    // ------------------------------------------------------------------
    abstract static class ListTableModel<T> extends AbstractTableModel {
        private final String[] columns;
        private List<T> rows = new ArrayList<>();

        ListTableModel(String... columns) {
            this.columns = columns;
        }

        void setRows(List<T> newRows) {
            rows = new ArrayList<>(newRows);
            fireTableDataChanged();
        }

        T getRow(int modelRow) {
            return rows.get(modelRow);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) { return valueFor(rows.get(row), column); }

        abstract Object valueFor(T item, int column);
    }

    static final class PlotTableModel extends ListTableModel<Plot> {
        PlotTableModel() {
            super("Plot Number", "Section", "Status");
        }

        @Override
        Object valueFor(Plot p, int column) {
            switch (column) {
                case 0: return p.getPlotNumber();
                case 1: return p.getSection();
                default: return p.getStatus().getLabel();
            }
        }
    }

    static final class BurialTableModel extends ListTableModel<BurialRecord> {
        private Map<String, Plot> plotLookup = Collections.emptyMap();

        BurialTableModel() {
            super("Name", "Birth Date", "Date of Death", "Burial Date", "Plot", "Section", "ID");
        }

        void setPlotLookup(Map<String, Plot> lookup) {
            plotLookup = lookup;
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 6 ? Long.class : String.class;
        }

        @Override
        Object valueFor(BurialRecord r, int column) {
            switch (column) {
                case 0: return r.getDeceased().getName();
                case 1: return DATE_FORMAT.format(r.getDeceased().getBirthDate());
                case 2: return DATE_FORMAT.format(r.getDeceased().getDeathDate());
                case 3: return DATE_FORMAT.format(r.getBurialDate());
                case 4: return r.getPlotNumber();
                case 5:
                    Plot p = plotLookup.get(r.getPlotNumber());
                    return p == null ? "" : p.getSection();
                default: return r.getRecordId();
            }
        }
    }

    /** Status column colours: green = available, red = occupied. */
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setForeground(AVAILABLE.equalsIgnoreCase(String.valueOf(value)) ? OK_COLOR : ERROR_COLOR);
            }
            return c;
        }
    }

    private static JTable createTable(TableModel model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true); // click a header to sort
        table.setRowHeight(24);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    static BurialRecord selectedRecord(JTable table, BurialTableModel model) {
        int viewRow = table.getSelectedRow();
        // The row index changes when the table is sorted or filtered; convert it first.
        return viewRow < 0 ? null : model.getRow(table.convertRowIndexToModel(viewRow));
    }

    // ------------------------------------------------------------------
    // Tab panels: one abstract parent, three children
    // ------------------------------------------------------------------
    private abstract class RefreshablePanel extends JPanel {
        RefreshablePanel() {
            super(new BorderLayout(8, 8));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        abstract String title();

        abstract void refresh(Snapshot data);
    }

    /** Tab 1: add / edit section / delete plots, filter by section and status. */
    private final class PlotsPanel extends RefreshablePanel {
        private final PlotTableModel model = new PlotTableModel();
        private final JTable table = createTable(model);
        private final TableRowSorter<PlotTableModel> sorter = new TableRowSorter<>(model);
        private final JTextField plotField = new JTextField(8);
        private final JComboBox<String> sectionBox = new JComboBox<>();
        private final JComboBox<String> sectionFilter = new JComboBox<>();
        private final JComboBox<String> statusFilter =
                new JComboBox<>(new String[] {ALL_STATUSES, AVAILABLE, OCCUPIED});
        private boolean updating;

        PlotsPanel() {
            sectionBox.setEditable(true); // pick an existing section or type a new one
            sectionBox.setPrototypeDisplayValue("Section XXXXXXXX");
            sectionFilter.setPrototypeDisplayValue("All sections XXXX");
            table.setRowSorter(sorter);
            table.getColumnModel().getColumn(2).setCellRenderer(new StatusRenderer());

            JButton addButton = guarded(new JButton("Add Plot"));
            JButton editButton = guarded(new JButton("Edit Section..."));
            JButton deleteButton = guarded(new JButton("Delete Plot"));

            JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            addRow.add(new JLabel("Plot number:"));
            addRow.add(plotField);
            addRow.add(new JLabel("Section:"));
            addRow.add(sectionBox);
            addRow.add(addButton);

            JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            filterRow.add(new JLabel("Show:"));
            filterRow.add(sectionFilter);
            filterRow.add(statusFilter);
            filterRow.add(editButton);
            filterRow.add(deleteButton);

            JPanel top = new JPanel(new GridLayout(2, 1));
            top.add(addRow);
            top.add(filterRow);
            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            addButton.addActionListener(e -> onAdd());
            plotField.addActionListener(e -> onAdd());
            editButton.addActionListener(e -> onEditSection());
            deleteButton.addActionListener(e -> onDelete());
            sectionFilter.addActionListener(e -> applyFilter());
            statusFilter.addActionListener(e -> applyFilter());
        }

        @Override String title() { return "Plots"; }

        @Override
        void refresh(Snapshot data) {
            updating = true;
            Object keepFilter = sectionFilter.getSelectedItem();
            Object typed = sectionBox.getEditor().getItem();
            model.setRows(data.plots);

            sectionFilter.removeAllItems();
            sectionFilter.addItem(ALL_SECTIONS);
            sectionBox.removeAllItems();
            for (String s : data.sections) {
                sectionFilter.addItem(s);
                sectionBox.addItem(s);
            }
            boolean stillThere = ALL_SECTIONS.equals(keepFilter) || data.sections.contains(keepFilter);
            sectionFilter.setSelectedItem(stillThere && keepFilter != null ? keepFilter : ALL_SECTIONS);
            sectionBox.setSelectedItem(typed == null ? "" : typed);
            updating = false;
            applyFilter();
        }

        private void applyFilter() {
            if (updating) {
                return;
            }
            List<RowFilter<PlotTableModel, Integer>> filters = new ArrayList<>();
            Object section = sectionFilter.getSelectedItem();
            if (section != null && !ALL_SECTIONS.equals(section)) {
                filters.add(RowFilter.regexFilter("^" + Pattern.quote(section.toString()) + "$", 1));
            }
            Object status = statusFilter.getSelectedItem();
            if (status != null && !ALL_STATUSES.equals(status)) {
                filters.add(RowFilter.regexFilter("^" + Pattern.quote(status.toString()) + "$", 2));
            }
            sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        }

        private Plot selectedPlot() {
            int viewRow = table.getSelectedRow();
            return viewRow < 0 ? null : model.getRow(table.convertRowIndexToModel(viewRow));
        }

        private void onAdd() {
            if (notReady()) {
                return;
            }
            final String plot = plotField.getText().trim();
            Object item = sectionBox.getEditor().getItem();
            final String section = item == null ? "" : item.toString().trim();
            String problem = InputValidator.checkPlot(plot, section);
            if (problem != null) {
                showWarning(problem);
                return;
            }
            final CemeteryService svc = service;
            begin("Adding plot...");
            submitTask(() -> {
                        svc.addPlot(plot, section);
                        return Boolean.TRUE;
                    },
                    ok -> {
                        done();
                        if (plotField.getText().trim().equals(plot)) plotField.setText("");
                        plotField.requestFocusInWindow();
                        refreshAll("Plot " + plot + " added.");
                    },
                    error -> fail(error, ErrorMessages.forUser(error)));
        }

        private void onEditSection() {
            if (notReady()) {
                return;
            }
            Plot selected = selectedPlot();
            if (selected == null) {
                showWarning("Select a plot first.");
                return;
            }
            final String number = selected.getPlotNumber();
            Object input = JOptionPane.showInputDialog(MainFrame.this,
                    "New section for plot " + number + ":", "Edit Section",
                    JOptionPane.PLAIN_MESSAGE, null, null, selected.getSection());
            if (input == null) {
                return; // cancelled
            }
            final String section = input.toString().trim();
            if (section.isEmpty()) {
                showWarning("Section is required.");
                return;
            }
            final CemeteryService svc = service;
            begin("Updating plot...");
            submitTask(() -> {
                        svc.updatePlotSection(number, section);
                        return Boolean.TRUE;
                    },
                    ok -> {
                        done();
                        refreshAll("Plot " + number + " is now in section " + section + ".");
                    },
                    error -> fail(error, ErrorMessages.forUser(error)));
        }

        private void onDelete() {
            if (notReady()) {
                return;
            }
            Plot selected = selectedPlot();
            if (selected == null) {
                showWarning("Select a plot first.");
                return;
            }
            if (!selected.isAvailable()) {
                showWarning("Plot " + selected.getPlotNumber()
                        + " has a burial record. Only available plots can be deleted.");
                return;
            }
            final String number = selected.getPlotNumber();
            int answer = JOptionPane.showConfirmDialog(MainFrame.this,
                    "Delete available plot " + number + "?", "Delete Plot",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            final CemeteryService svc = service;
            begin("Deleting plot...");
            submitTask(() -> {
                        svc.deleteAvailablePlot(number);
                        return Boolean.TRUE;
                    },
                    ok -> {
                        done();
                        refreshAll("Plot " + number + " deleted.");
                    },
                    error -> {
                        fail(error, ErrorMessages.forUser(error));
                        refreshAll(null);
                    });
        }
    }

    /** Tab 2: every burial record; register new ones or correct existing ones. */
    private final class BurialsPanel extends RefreshablePanel {
        private final BurialTableModel model = new BurialTableModel();
        private final JTable table = createTable(model);

        BurialsPanel() {
            JButton registerButton = guarded(new JButton("Register Burial..."));
            JButton editButton = guarded(new JButton("Edit Details..."));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            top.add(registerButton);
            top.add(editButton);
            top.add(new JLabel("One burial per plot. Double-click a row to correct it."));

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);

            registerButton.addActionListener(e -> startRegister());
            editButton.addActionListener(e -> startEdit(selectedRecord(table, model)));
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && table.getSelectedRow() >= 0 && ready && pending == 0 && !closing) {
                        startEdit(selectedRecord(table, model));
                    }
                }
            });
        }

        @Override String title() { return "Burials"; }

        @Override
        void refresh(Snapshot data) {
            model.setPlotLookup(data.plotsByNumber);
            model.setRows(data.burials);
        }
    }

    /** Tab 3: search burial records by deceased name or plot number. */
    private final class SearchPanel extends RefreshablePanel {
        private final BurialTableModel model = new BurialTableModel();
        private final JTable table = createTable(model);
        private final JTextField queryField = new JTextField(22);
        private final JLabel resultLabel = new JLabel(SEARCH_HINT);
        private String lastQuery; // null = nothing searched yet
        private final SearchVersion searchVersion = new SearchVersion();

        SearchPanel() {
            JButton searchButton = guarded(new JButton("Search"));
            JButton clearButton = new JButton("Clear");
            JButton editButton = guarded(new JButton("Edit Selected..."));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            top.add(new JLabel("Name or plot number:"));
            top.add(queryField);
            top.add(searchButton);
            top.add(clearButton);
            top.add(editButton);

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(table), BorderLayout.CENTER);
            add(resultLabel, BorderLayout.SOUTH);

            searchButton.addActionListener(e -> onSearch());
            queryField.addActionListener(e -> onSearch());
            editButton.addActionListener(e -> startEdit(selectedRecord(table, model)));
            clearButton.addActionListener(e -> {
                searchVersion.invalidate(); // Ignore any earlier result still on the worker.
                lastQuery = null;
                queryField.setText("");
                model.setRows(new ArrayList<BurialRecord>());
                resultLabel.setText(SEARCH_HINT);
            });
        }

        @Override String title() { return "Search"; }

        @Override
        void refresh(Snapshot data) {
            model.setPlotLookup(data.plotsByNumber);
            if (lastQuery != null) {
                runSearch(lastQuery, false); // keep results current after any change
            }
        }

        private void onSearch() {
            if (notReady()) {
                return;
            }
            lastQuery = queryField.getText().trim();
            runSearch(lastQuery, true);
        }

        private void runSearch(final String query, final boolean announce) {
            if (closing || service == null) {
                return;
            }
            final CemeteryService svc = service;
            final long version = searchVersion.next();
            begin(announce ? "Searching..." : null);
            submitTask(() -> svc.searchBurials(query),
                    results -> {
                        done();
                        if (!searchVersion.isCurrent(version)) return;
                        model.setRows(new ArrayList<>(results));
                        String what = query.isEmpty() ? "all records" : "\"" + query + "\"";
                        resultLabel.setText(results.isEmpty()
                                ? "No burial records found for " + what + "."
                                : results.size() + " record(s) found for " + what + ".");
                        if (announce) {
                            setStatus("Search finished.", false);
                        }
                    },
                    error -> {
                        if (searchVersion.isCurrent(version)) fail(error, ErrorMessages.forUser(error));
                        else done();
                    });
        }
    }

    // ------------------------------------------------------------------
    // Register / Edit burial dialog
    // ------------------------------------------------------------------
    private static final class BurialDialog extends JDialog {
        private final JTextField nameField = new JTextField(20);
        private final JTextField birthField = new JTextField(10);
        private final JTextField deathField = new JTextField(10);
        private final JTextField burialField = new JTextField(10);
        private final JComboBox<Plot> plotBox;   // register mode only
        private final String fixedPlotNumber;    // edit mode only
        private final JLabel errorLabel = new JLabel(" ");
        private Consumer<BurialForm> saveAction;
        private final JButton saveButton = new JButton("Save");
        private final JButton cancelButton = new JButton("Cancel");
        private boolean saving;

        static BurialDialog forRegister(Frame owner, List<Plot> availablePlots) {
            return new BurialDialog(owner, "Register Burial", availablePlots, null, null);
        }

        static BurialDialog forEdit(Frame owner, BurialRecord record, String plotText) {
            return new BurialDialog(owner, "Edit Burial Record #" + record.getRecordId(),
                    null, record, plotText);
        }

        private BurialDialog(Frame owner, String title, List<Plot> availablePlots,
                             BurialRecord existing, String plotText) {
            super(owner, title, true);
            errorLabel.setForeground(ERROR_COLOR);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            Component plotComponent;
            if (existing == null) {
                plotBox = new JComboBox<>(availablePlots.toArray(new Plot[0]));
                plotBox.setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList<?> list, Object value,
                            int index, boolean selected, boolean focus) {
                        super.getListCellRendererComponent(list, value, index, selected, focus);
                        if (value instanceof Plot) {
                            Plot p = (Plot) value;
                            setText(p.getPlotNumber() + "  (" + p.getSection() + ")");
                        }
                        return this;
                    }
                });
                fixedPlotNumber = null;
                burialField.setText(DATE_FORMAT.format(LocalDate.now()));
                plotComponent = plotBox;
            } else {
                plotBox = null;
                fixedPlotNumber = existing.getPlotNumber(); // the plot cannot change when correcting
                nameField.setText(existing.getDeceased().getName());
                birthField.setText(DATE_FORMAT.format(existing.getDeceased().getBirthDate()));
                deathField.setText(DATE_FORMAT.format(existing.getDeceased().getDeathDate()));
                burialField.setText(DATE_FORMAT.format(existing.getBurialDate()));
                plotComponent = new JLabel(plotText);
            }

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
            addRow(form, 0, "Deceased name:", nameField, null);
            addRow(form, 1, "Birth date:", birthField, "YYYY-MM-DD");
            addRow(form, 2, "Date of death:", deathField, "YYYY-MM-DD");
            addRow(form, 3, "Burial date:", burialField, "YYYY-MM-DD");
            addRow(form, 4, "Plot:", plotComponent, existing == null ? null : "(cannot be changed)");

            saveButton.addActionListener(e -> onSave());
            cancelButton.addActionListener(e -> dispose());

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            buttons.add(saveButton);
            buttons.add(cancelButton);

            JPanel south = new JPanel(new BorderLayout());
            south.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            south.add(errorLabel, BorderLayout.NORTH);
            south.add(buttons, BorderLayout.SOUTH);

            setLayout(new BorderLayout());
            add(form, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
            getRootPane().setDefaultButton(saveButton);
            pack();
            setResizable(false);
            setLocationRelativeTo(owner);
        }

        private static void addRow(JPanel form, int row, String label, Component field, String hint) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row;
            c.insets = new Insets(4, 4, 4, 4);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            form.add(new JLabel(label), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            form.add(field, c);
            if (hint != null) {
                c.gridx = 2;
                c.fill = GridBagConstraints.NONE;
                form.add(new JLabel(hint), c);
            }
        }

        private void onSave() {
            if (saving || saveAction == null) return;
            try {
                String plotNumber = fixedPlotNumber;
                if (plotBox != null) {
                    Plot chosen = (Plot) plotBox.getSelectedItem();
                    if (chosen == null) {
                        throw new IllegalArgumentException("Please choose an available plot.");
                    }
                    plotNumber = chosen.getPlotNumber();
                }
                // Death date goes to "Date of death", burial date to "Burial date": do not swap them.
                BurialForm form = InputValidator.buildBurialForm(nameField.getText(), birthField.getText(),
                        deathField.getText(), burialField.getText(), plotNumber);
                saveAction.accept(form);
            } catch (IllegalArgumentException ex) {
                errorLabel.setText(ex.getMessage()); // keep the dialog open so the user can fix it
                pack();
            }
        }

        void setSaveAction(Consumer<BurialForm> action) { saveAction = action; }

        void setSaving(boolean value) {
            saving = value;
            saveButton.setEnabled(!value);
            cancelButton.setEnabled(!value);
            nameField.setEnabled(!value);
            birthField.setEnabled(!value);
            deathField.setEnabled(!value);
            burialField.setEnabled(!value);
            if (plotBox != null) plotBox.setEnabled(!value);
            setDefaultCloseOperation(value ? DO_NOTHING_ON_CLOSE : DISPOSE_ON_CLOSE);
            if (value) errorLabel.setText("Saving...");
        }

        void showError(String message) {
            errorLabel.setText(message);
            pack();
        }

        void updateAvailablePlots(List<Plot> plots) {
            if (plotBox == null || !isDisplayable()) return;
            Plot old = (Plot) plotBox.getSelectedItem();
            String selected = old == null ? null : old.getPlotNumber();
            plotBox.removeAllItems();
            for (Plot plot : plots) plotBox.addItem(plot);
            for (Plot plot : plots) {
                if (plot.getPlotNumber().equals(selected)) plotBox.setSelectedItem(plot);
            }
        }
    }
}
