package com.factory.generators.database;

import com.factory.generators.utils.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple connection pool implementation inspired by HikariCP.
 * Reuses connections to reduce overhead and improve performance.
 *
 * Key features from CoreProtect implementation:
 * - Connection pooling to avoid repeated connection creation
 * - Configurable pool size
 * - Automatic connection validation
 * - Thread-safe operations
 */
public class ConnectionPool {

    private final DatabaseConfig config;
    private final Queue<Connection> availableConnections;
    private final Queue<Connection> usedConnections;
    private final int maxPoolSize;
    private volatile boolean closed = false;

    public ConnectionPool(@NotNull DatabaseConfig config) {
        this.config = config;
        this.maxPoolSize = config.getMaxPoolSize();
        this.availableConnections = new ConcurrentLinkedQueue<>();
        this.usedConnections = new ConcurrentLinkedQueue<>();

        // Initialize pool with connections
        initializePool();
    }

    /**
     * Initializes the connection pool with initial connections.
     */
    private void initializePool() {
        try {
            // Load JDBC driver
            loadDriver();

            // Create initial connections
            int initialSize = Math.min(5, maxPoolSize);
            for (int i = 0; i < initialSize; i++) {
                Connection conn = createConnection();
                if (conn != null) {
                    availableConnections.offer(conn);
                }
            }
            Logger.info("Connection pool initialized with " + availableConnections.size() + " connections");
        } catch (SQLException e) {
            Logger.error("Failed to initialize connection pool", e);
        }
    }

    /**
     * Loads the appropriate JDBC driver based on database type.
     */
    private void loadDriver() throws SQLException {
        String driver = config.getJdbcDriver();
        try {
            Class.forName(driver);
            Logger.info("JDBC driver loaded: " + driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found: " + driver, e);
        }
    }

    /**
     * Creates a new database connection.
     *
     * @return Connection or null if failed
     */
    private Connection createConnection() {
        try {
            String url = config.getJdbcUrl();
            String user = config.getUsername();
            String password = config.getPassword();

            // Set login timeout to prevent hanging on unreachable database
            DriverManager.setLoginTimeout(10);

            Connection conn = DriverManager.getConnection(url, user, password);

            // Set connection properties for better performance
            conn.setAutoCommit(true);
            conn.setNetworkTimeout(null, 30000); // 30 second timeout

            return conn;
        } catch (SQLException e) {
            Logger.warn("Failed to create database connection: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a connection from the pool (thread-safe).
     * Similar to HikariCP's behavior - reuses existing connections.
     *
     * @return Database connection
     * @throws SQLException if no connections available
     */
    @NotNull
    public synchronized Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }

        int attempts = 0;
        final int maxAttempts = 10;

        while (attempts < maxAttempts) {
            // Try to get available connection
            Connection conn = availableConnections.poll();

            if (conn != null) {
                // Validate connection before returning
                if (isValid(conn)) {
                    usedConnections.offer(conn);
                    return conn;
                } else {
                    // Connection is stale, create new one
                    conn = createConnection();
                    if (conn != null) {
                        usedConnections.offer(conn);
                        return conn;
                    }
                }
            }

            // No available connections, create new if under max
            if (usedConnections.size() < maxPoolSize) {
                conn = createConnection();
                if (conn != null) {
                    usedConnections.offer(conn);
                    return conn;
                }
            }

            // Pool exhausted, wait and retry (without holding lock for too long)
            if (attempts < maxAttempts - 1) {
                try {
                    Thread.sleep(100);
                    attempts++;
                } catch (InterruptedException e) {
                    throw new SQLException("Interrupted while waiting for connection", e);
                }
            } else {
                attempts++;
            }
        }

        throw new SQLException("Unable to acquire database connection after " + maxAttempts + " attempts. Pool size: " + maxPoolSize + ", Used: " + usedConnections.size());
    }

    /**
     * Returns a connection to the pool.
     *
     * @param conn Connection to return
     */
    public synchronized void returnConnection(@NotNull Connection conn) {
        if (closed) {
            try {
                conn.close();
            } catch (SQLException e) {
                Logger.warn("Error closing connection: " + e.getMessage());
            }
            return;
        }

        usedConnections.remove(conn);

        if (isValid(conn)) {
            availableConnections.offer(conn);
        } else {
            try {
                conn.close();
            } catch (SQLException e) {
                Logger.warn("Error closing stale connection: " + e.getMessage());
            }
        }
    }

    /**
     * Validates if a connection is still usable.
     *
     * @param conn Connection to validate
     * @return true if valid
     */
    private boolean isValid(@NotNull Connection conn) {
        try {
            // Check if connection is closed
            if (conn.isClosed()) {
                return false;
            }

            // Test with simple query (lightweight) - use try-with-resources to avoid leak
            try (var stmt = conn.createStatement()) {
                stmt.execute("SELECT 1");
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Closes all connections in the pool.
     */
    public synchronized void close() {
        if (closed) {
            return;
        }

        closed = true;

        // Close available connections
        while (!availableConnections.isEmpty()) {
            Connection conn = availableConnections.poll();
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                Logger.warn("Error closing connection: " + e.getMessage());
            }
        }

        // Close used connections
        while (!usedConnections.isEmpty()) {
            Connection conn = usedConnections.poll();
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                Logger.warn("Error closing connection: " + e.getMessage());
            }
        }

        Logger.info("Connection pool closed");
    }

    /**
     * Gets current pool statistics.
     *
     * @return Statistics string
     */
    public synchronized String getStats() {
        return "Available: " + availableConnections.size() +
               ", Used: " + usedConnections.size() +
               ", Max: " + maxPoolSize;
    }

    /**
     * Checks if pool is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }
}
