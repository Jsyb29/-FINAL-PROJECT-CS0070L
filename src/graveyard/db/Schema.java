package graveyard.db;

import graveyard.exception.CemeteryException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Initializes a NEW database. Existing incompatible databases are never overwritten. */
final class Schema {
    private Schema() { }

    // M5 text blocks keep the SQL readable. M8 PK, FK, UNIQUE and CHECK constraints.
    private static final String[] DDL = {
        "CREATE TABLE schema_version (version_number INT PRIMARY KEY)",
        """
        CREATE TABLE plots (
            plot_number VARCHAR(20) PRIMARY KEY,
            section VARCHAR(60) NOT NULL,
            CONSTRAINT plot_nonempty CHECK (LENGTH(TRIM(plot_number)) > 0),
            CONSTRAINT section_nonempty CHECK (LENGTH(TRIM(section)) > 0)
        )
        """,
        """
        CREATE TABLE burials (
            record_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            deceased_name VARCHAR(120) NOT NULL,
            birth_date DATE NOT NULL,
            death_date DATE NOT NULL,
            burial_date DATE NOT NULL,
            plot_number VARCHAR(20) NOT NULL UNIQUE,
            CONSTRAINT burial_plot FOREIGN KEY (plot_number) REFERENCES plots(plot_number),
            CONSTRAINT name_nonempty CHECK (LENGTH(TRIM(deceased_name)) > 0),
            CONSTRAINT birth_before_death CHECK (birth_date <= death_date),
            CONSTRAINT death_before_burial CHECK (death_date <= burial_date)
        )
        """,
        "INSERT INTO schema_version VALUES (1)"
    };

    static synchronized void initialize(Database database) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                if (tableExists(connection, "SCHEMA_VERSION")) {
                    try (Statement st = connection.createStatement();
                         ResultSet rs = st.executeQuery("SELECT version_number FROM schema_version")) {
                        if (!rs.next() || rs.getInt(1) != 1 || rs.next()
                                || !tableExists(connection, "PLOTS") || !tableExists(connection, "BURIALS")) {
                            throw new SQLException("Unsupported or incomplete database schema.");
                        }
                    }
                } else {
                    if (tableExists(connection, "PLOTS") || tableExists(connection, "BURIALS")) {
                        throw new SQLException("Existing tables need a migration; select a new database folder.");
                    }
                    try (Statement st = connection.createStatement()) {
                        for (String sql : DDL) st.executeUpdate(sql);
                    }
                }
                connection.commit();
            } catch (SQLException ex) {
                try { connection.rollback(); } catch (SQLException rollback) { ex.addSuppressed(rollback); }
                throw ex;
            }
        } catch (SQLException ex) {
            throw new CemeteryException("Cannot initialize the database. Check Derby jars, Java 17+, "
                    + "the database folder, and the original error: " + ex.getMessage(), ex);
        }
    }

    private static boolean tableExists(Connection connection, String name) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getTables(null, "APP", name, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
