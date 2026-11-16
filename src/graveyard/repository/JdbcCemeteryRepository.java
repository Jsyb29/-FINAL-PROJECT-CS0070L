package graveyard.repository;

import graveyard.db.Database;
import graveyard.exception.CemeteryException;
import graveyard.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** M8 JDBC implementation. All user values use PreparedStatement parameters. */
public final class JdbcCemeteryRepository implements CemeteryRepository {
    private final Database database;
    private static final String PLOTS = """
        SELECT p.plot_number, p.section, b.record_id
        FROM plots p LEFT JOIN burials b ON p.plot_number = b.plot_number
        """;
    private static final String BURIALS = """
        SELECT record_id, deceased_name, birth_date, death_date, burial_date, plot_number
        FROM burials
        """;
    private static final String INSERT_BURIAL = """
        INSERT INTO burials (deceased_name, birth_date, death_date, burial_date, plot_number)
        VALUES (?, ?, ?, ?, ?)
        """;

    public JdbcCemeteryRepository(Database database) { this.database = database; }

    @Override
    public List<Plot> findPlots() {
        List<Plot> results = new ArrayList<>();
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement(PLOTS + " ORDER BY p.plot_number");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(readPlot(rs));
            return results;
        } catch (SQLException ex) { throw failure("load plots", ex); }
    }

    @Override
    public Optional<Plot> findPlot(String number) {
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement(PLOTS + " WHERE p.plot_number = ?")) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readPlot(rs)) : Optional.empty();
            }
        } catch (SQLException ex) { throw failure("find plot", ex); }
    }

    @Override
    public void insertPlot(String number, String section) {
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement("INSERT INTO plots VALUES (?, ?)")) {
            ps.setString(1, number);
            ps.setString(2, section);
            ps.executeUpdate();
        } catch (SQLException ex) { throw failure("add plot", ex); }
    }

    @Override
    public boolean updateSection(String number, String section) {
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement("UPDATE plots SET section = ? WHERE plot_number = ?")) {
            ps.setString(1, section);
            ps.setString(2, number);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("update section", ex); }
    }

    @Override
    public boolean deleteEmptyPlot(String number) {
        String sql = """
            DELETE FROM plots WHERE plot_number = ? AND NOT EXISTS
            (SELECT 1 FROM burials WHERE burials.plot_number = plots.plot_number)
            """;
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, number);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("delete empty plot", ex); }
    }

    @Override
    public List<BurialRecord> findBurials() {
        List<BurialRecord> results = new ArrayList<>();
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement(BURIALS + " ORDER BY record_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) results.add(readBurial(rs));
            return results;
        } catch (SQLException ex) { throw failure("load burials", ex); }
    }

    @Override
    public Optional<BurialRecord> findBurial(long id) {
        try (Connection c = database.connect();
             PreparedStatement ps = c.prepareStatement(BURIALS + " WHERE record_id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readBurial(rs)) : Optional.empty();
            }
        } catch (SQLException ex) { throw failure("find burial", ex); }
    }

    @Override
    public BurialRecord insertBurial(BurialRequest request) {
        try (Connection c = database.connect()) {
            // The UNIQUE constraint, not just a prior availability check, prevents double booking.
            c.setAutoCommit(false);
            try {
                BurialRecord record = insertBurial(c, request);
                c.commit();
                return record;
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        } catch (SQLException ex) { throw failure("register burial", ex); }
    }

    private BurialRecord insertBurial(Connection c, BurialRequest r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(INSERT_BURIAL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.name());
            ps.setDate(2, Date.valueOf(r.birthDate()));
            ps.setDate(3, Date.valueOf(r.deathDate()));
            ps.setDate(4, Date.valueOf(r.burialDate()));
            ps.setString(5, r.plotNumber());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No burial ID was generated.");
                return new BurialRecord(keys.getLong(1), new Deceased(r.name(), r.birthDate(), r.deathDate()),
                        r.burialDate(), r.plotNumber());
            }
        }
    }

    @Override
    public boolean updateBurial(long id, Deceased deceased, LocalDate burialDate) {
        String sql = """
            UPDATE burials SET deceased_name = ?, birth_date = ?, death_date = ?, burial_date = ?
            WHERE record_id = ?
            """;
        try (Connection c = database.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, deceased.getName());
            ps.setDate(2, Date.valueOf(deceased.getBirthDate()));
            ps.setDate(3, Date.valueOf(deceased.getDeathDate()));
            ps.setDate(4, Date.valueOf(burialDate));
            ps.setLong(5, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) { throw failure("correct burial record", ex); }
    }

    @Override
    public void seedIfEmpty(List<Plot> plots, List<BurialRequest> burials) {
        try (Connection c = database.connect()) {
            c.setAutoCommit(false);
            try {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("LOCK TABLE plots IN EXCLUSIVE MODE");
                    try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM plots")) {
                        rs.next();
                        if (rs.getLong(1) != 0) throw new SQLException("Demo seeding requires an empty database.");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO plots VALUES (?, ?)")) {
                    for (Plot plot : plots) {
                        ps.setString(1, plot.getPlotNumber());
                        ps.setString(2, plot.getSection());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                for (BurialRequest request : burials) insertBurial(c, request);
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                try { c.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        } catch (SQLException ex) { throw failure("seed demo database", ex); }
    }

    private Plot readPlot(ResultSet rs) throws SQLException {
        rs.getLong("record_id");
        PlotStatus status = rs.wasNull() ? PlotStatus.AVAILABLE : PlotStatus.OCCUPIED;
        return new Plot(rs.getString("plot_number"), rs.getString("section"), status);
    }

    private BurialRecord readBurial(ResultSet rs) throws SQLException {
        Deceased person = new Deceased(rs.getString("deceased_name"),
                rs.getDate("birth_date").toLocalDate(), rs.getDate("death_date").toLocalDate());
        return new BurialRecord(rs.getLong("record_id"), person,
                rs.getDate("burial_date").toLocalDate(), rs.getString("plot_number"));
    }

    private CemeteryException failure(String action, SQLException ex) {
        String detail = switch (ex.getSQLState() == null ? "" : ex.getSQLState()) {
            case "23505" -> "That plot already exists or already has a burial.";
            case "23503" -> "The plot is missing or is referenced by a burial.";
            case "23513" -> "The values violate a database validation rule.";
            case "40001", "40XL1", "40XL2" -> "Another operation is using these records. Refresh and retry.";
            default -> "Check the database configuration and the original error: " + ex.getMessage();
        };
        return new CemeteryException("Cannot " + action + ". " + detail, ex);
    }
}
