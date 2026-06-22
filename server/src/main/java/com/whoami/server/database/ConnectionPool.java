package com.whoami.server.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

public class ConnectionPool {
    private static HikariDataSource dataSource;

    public static void init() {
        if (dataSource != null) return;
        
        // Fix for FATAL: invalid value for parameter "TimeZone": "Europe/Kiev"
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        HikariConfig config = new HikariConfig();
        // Adjust these to match the user's local PostgreSQL setup
        config.setJdbcUrl(System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/whoami_db"));
        config.setUsername(System.getenv().getOrDefault("DB_USER", "postgres"));
        config.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "postgres"));
        
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 seconds

        dataSource = new HikariDataSource(config);
        
        // Run Flyway migrations
        System.out.println("Running Flyway Database Migrations...");
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.migrate();
        System.out.println("Migrations completed successfully!");
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("ConnectionPool is not initialized");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
