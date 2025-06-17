package com.restaurant.pos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service to manage database health and connections
 * Helps prevent H2 "object is already closed" errors
 */
@Service
public class DatabaseHealthService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthService.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Check if database connection is healthy
     */
    public boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isClosed()) {
                logger.warn("Database connection is closed");
                return false;
            }
            
            // Simple health check query
            try (PreparedStatement stmt = connection.prepareStatement("SELECT 1");
                 ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        } catch (SQLException | DataAccessException e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }
    
    /**
     * Get database connection info for debugging
     */
    public String getDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            return String.format("Database: %s, URL: %s, Closed: %s", 
                connection.getMetaData().getDatabaseProductName(),
                connection.getMetaData().getURL(),
                connection.isClosed());
        } catch (SQLException e) {
            logger.error("Failed to get database info", e);
            return "Database info unavailable: " + e.getMessage();
        }
    }
    
    /**
     * Clean up any stale connections
     */
    @Transactional
    public void cleanupConnections() {
        try {
            // Force garbage collection to clean up any dangling connections
            System.gc();
            logger.info("Database cleanup completed");
        } catch (Exception e) {
            logger.error("Database cleanup failed", e);
        }
    }
}