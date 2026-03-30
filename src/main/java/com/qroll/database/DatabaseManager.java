package com.qroll.database;
import org.sqlite.SQLiteConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:qroll.db";
    private static volatile DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setBusyTimeout(5000);
            config.setJournalMode(SQLiteConfig.JournalMode.WAL);
            config.enforceForeignKeys(true);

            connection = DriverManager.getConnection(DB_URL, config.toProperties());
            createTables();
            System.out.println("DatabaseManager: connected to qroll.db");
        } catch (SQLException e) {
            throw new RuntimeException("failed to connect to SQLlite db", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    full_name  TEXT NOT NULL,
                    group_name TEXT,
                    email      TEXT
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS modules (
                    module_code TEXT PRIMARY KEY,
                    module_name TEXT NOT NULL,
                    semester    INTEGER
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    session_id   TEXT PRIMARY KEY,
                    module_code  TEXT REFERENCES modules(module_code),
                    session_type TEXT CHECK(session_type IN ('LECTURE','LAB','TUTORIAL')),
                    start_time   TEXT,
                    end_time     TEXT,
                    status       TEXT CHECK(status IN ('ACTIVE','CLOSED')),
                    totp_seed    TEXT NOT NULL
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS attendance_records (
                    record_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT REFERENCES students(student_id),
                    session_id TEXT REFERENCES sessions(session_id),
                    scan_time  TEXT NOT NULL,
                    valid      INTEGER DEFAULT 1,
                    UNIQUE(student_id, session_id)
                )
            """);

            System.out.println("DatabaseManager: all tables ready");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("DatabaseManager: connection closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}