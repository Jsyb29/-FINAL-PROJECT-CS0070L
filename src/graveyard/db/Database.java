package graveyard.db;

import graveyard.exception.CemeteryException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Embedded Derby: one connection per operation; no shared static Connection. */
public final class Database {
    private final String url;

    public Database(Path databaseFolder) {
        Path path = databaseFolder.toAbsolutePath().normalize();
        if (path.toString().contains(";")) {
            throw new CemeteryException("The database path cannot contain a semicolon.");
        }
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ex) {
            throw new CemeteryException("Cannot create the database parent folder.", ex);
        }
        url = "jdbc:derby:" + path + ";create=true";
    }

    public Connection connect() throws SQLException {
        // JDBC 4 discovers Derby automatically when its two jars are on the classpath.
        return DriverManager.getConnection(url);
    }

    public void initialize() { Schema.initialize(this); }

    public static Database defaultDatabase() {
        return new Database(Path.of(System.getProperty("graveyard.db", "data/cemetery")));
    }
}
