package com.ecematerial;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseConfig {
    private static final Path DATA_DIR = Path.of("data");
    private static final Path DATABASE_FILE = DATA_DIR.resolve("ece_material.db");
    private static final String JDBC_URL = "jdbc:sqlite:" + DATABASE_FILE.toAbsolutePath();

    private DatabaseConfig() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(DATA_DIR);

            try (Connection connection = DriverManager.getConnection(JDBC_URL);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        google_email TEXT NOT NULL UNIQUE,
                        ddu_id TEXT NOT NULL UNIQUE,
                        ddu_id_hash TEXT,
                        password_hash TEXT,
                        points INTEGER NOT NULL DEFAULT 0,
                        rank TEXT NOT NULL DEFAULT 'Student'
                    )
                    """);
                ensureColumnExists(statement, "Users", "ddu_id_hash", "TEXT");
                ensureColumnExists(statement, "Users", "password_hash", "TEXT");
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Materials (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        author_name TEXT NOT NULL DEFAULT '',
                        course_code TEXT NOT NULL,
                        file_path TEXT NOT NULL
                    )
                    """);
                ensureColumnExists(statement, "Materials", "author_name", "TEXT NOT NULL DEFAULT ''");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to initialize SQLite database", exception);
        }
    }

    private static void ensureColumnExists(Statement statement, String tableName, String columnName, String columnType)
        throws SQLException {
        try {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        } catch (SQLException exception) {
            String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
            if (!message.contains("duplicate column")) {
                throw exception;
            }
        }
    }

    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
}
