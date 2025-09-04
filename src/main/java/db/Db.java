package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static costants_values.Costants.*;

public final class Db {
    private Db() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
