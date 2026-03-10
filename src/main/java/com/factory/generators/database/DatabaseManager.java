package com.factory.generators.database;

import com.factory.generators.utils.Constants;
import com.factory.generators.utils.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages database connections and queries with optimization techniques from CoreProtect.
 * Implements HikariCP connection pooling, prepared statements, and efficient indexing.
 */
public class DatabaseManager {

    // Cached SQL queries to avoid repeated string creation
    private static final Map<String, String> SQL_QUERIES = new HashMap<>();

    private final DatabaseConfig config;
    private final HikariConnectionPool connectionPool;
    private boolean connected = false;

    static {
        // Pre-cache commonly used queries
        initializeQueries();
    }

    public DatabaseManager(@NotNull DatabaseConfig config) {
        this.config = config;
        try {
            this.connectionPool = new HikariConnectionPool(config);
        } catch (Exception e) {
            Logger.error("Failed to initialize HikariCP connection pool", e);
            throw new RuntimeException("Cannot initialize database connection pool", e);
        }
    }

    /**
     * Initializes all cached SQL queries.
     */
    private static void initializeQueries() {
        // Generator queries
        SQL_QUERIES.put("create_generators_table",
            "CREATE TABLE IF NOT EXISTS `cp_generators` (" +
            "  `id` INT AUTO_INCREMENT PRIMARY KEY," +
            "  `location_key` VARCHAR(100) NOT NULL UNIQUE," +
            "  `world` VARCHAR(50) NOT NULL," +
            "  `x` INT NOT NULL," +
            "  `y` INT NOT NULL," +
            "  `z` INT NOT NULL," +
            "  `owner_uuid` VARCHAR(36) NOT NULL," +
            "  `type_id` VARCHAR(50) NOT NULL," +
            "  `created_time` BIGINT NOT NULL," +
            "  `last_ticked` BIGINT NOT NULL," +
            "  `total_generated` BIGINT DEFAULT 0," +
            "  `is_broken` BOOLEAN DEFAULT FALSE," +
            "  INDEX `idx_owner` (`owner_uuid`)," +
            "  INDEX `idx_world_coords` (`world`, `x`, `z`)," +
            "  INDEX `idx_created_time` (`created_time`)," +
            "  INDEX `idx_type` (`type_id`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        SQL_QUERIES.put("create_statistics_table",
            "CREATE TABLE IF NOT EXISTS `cp_statistics` (" +
            "  `id` INT AUTO_INCREMENT PRIMARY KEY," +
            "  `player_uuid` VARCHAR(36) NOT NULL UNIQUE," +
            "  `generators_placed` BIGINT DEFAULT 0," +
            "  `generators_broken` BIGINT DEFAULT 0," +
            "  `generators_upgraded` BIGINT DEFAULT 0," +
            "  `resources_generated` BIGINT DEFAULT 0," +
            "  `last_update` BIGINT NOT NULL," +
            "  INDEX `idx_uuid` (`player_uuid`)," +
            "  INDEX `idx_last_update` (`last_update`)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        SQL_QUERIES.put("insert_generator",
            "INSERT INTO `cp_generators` " +
            "(location_key, world, x, y, z, owner_uuid, type_id, created_time, last_ticked, total_generated, is_broken) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        SQL_QUERIES.put("update_generator",
            "UPDATE `cp_generators` " +
            "SET `last_ticked`=?, `total_generated`=?, `is_broken`=? " +
            "WHERE `location_key`=?");

        SQL_QUERIES.put("delete_generator",
            "DELETE FROM `cp_generators` WHERE `location_key`=?");

        SQL_QUERIES.put("select_by_owner",
            "SELECT * FROM `cp_generators` WHERE `owner_uuid`=? ORDER BY `created_time` DESC");

        SQL_QUERIES.put("select_by_location",
            "SELECT * FROM `cp_generators` WHERE `location_key`=? LIMIT 1");

        SQL_QUERIES.put("select_all",
            "SELECT * FROM `cp_generators` ORDER BY `created_time` DESC");

        SQL_QUERIES.put("select_by_world",
            "SELECT * FROM `cp_generators` WHERE `world`=? ORDER BY `x`, `z`");

        SQL_QUERIES.put("upsert_statistics",
            "INSERT INTO `cp_statistics` " +
            "(player_uuid, generators_placed, generators_broken, generators_upgraded, resources_generated, last_update) " +
            "VALUES (?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "generators_placed=VALUES(generators_placed), " +
            "generators_broken=VALUES(generators_broken), " +
            "generators_upgraded=VALUES(generators_upgraded), " +
            "resources_generated=VALUES(resources_generated), " +
            "last_update=VALUES(last_update)");

        SQL_QUERIES.put("select_stats",
            "SELECT * FROM `cp_statistics` WHERE `player_uuid`=? LIMIT 1");
    }

    /**
     * Connects to the database and creates tables.
     *
     * @return true if connection successful
     */
    public boolean connect() {
        try {
            // Test connection
            try (Connection conn = connectionPool.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                Logger.info("Connected to " + meta.getDatabaseProductName() +
                           " " + meta.getDatabaseProductVersion());
            }

            // Create tables
            createTables();
            this.connected = true;
            Logger.info("Database initialized successfully");
            return true;
        } catch (SQLException e) {
            Logger.error("Failed to connect to database", e);
            return false;
        }
    }

    /**
     * Creates necessary database tables with optimized schema.
     */
    private void createTables() throws SQLException {
        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {

            // Enable InnoDB for transactions
            stmt.execute("SET SESSION storage_engine='InnoDB'");

            // Create generators table
            stmt.execute(SQL_QUERIES.get("create_generators_table"));

            // Create statistics table
            stmt.execute(SQL_QUERIES.get("create_statistics_table"));

            Logger.info("Database tables created/verified");
        }
    }

    /**
     * Executes a parameterized query with efficient statement caching.
     *
     * @param queryKey Query key from SQL_QUERIES
     * @param params Parameters for prepared statement
     * @return true if execution successful
     */
    public boolean executeUpdate(@NotNull String queryKey, @NotNull Object... params) {
        String query = SQL_QUERIES.get(queryKey);
        if (query == null) {
            Logger.warn("Query not found: " + queryKey);
            return false;
        }

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            setParameters(stmt, params);
            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            Logger.error("Error executing query: " + queryKey, e);
            return false;
        }
    }

    /**
     * Executes a query that returns results with automatic resource management.
     *
     * @param queryKey Query key from SQL_QUERIES
     * @param callback Callback to process ResultSet
     * @param params Parameters for prepared statement
     * @return true if successful
     */
    public boolean executeQueryWithCallback(@NotNull String queryKey,
                                           @NotNull ResultSetCallback callback,
                                           @NotNull Object... params) {
        String query = SQL_QUERIES.get(queryKey);
        if (query == null) {
            Logger.warn("Query not found: " + queryKey);
            return false;
        }

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            setParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return callback.process(rs);
            }
        } catch (SQLException e) {
            Logger.error("Error executing query: " + queryKey, e);
            return false;
        }
    }

    /**
     * Callback interface for processing ResultSet.
     */
    @FunctionalInterface
    public interface ResultSetCallback {
        /**
         * Process ResultSet.
         *
         * @param rs ResultSet to process
         * @return true if successful
         * @throws SQLException if database error occurs
         */
        boolean process(ResultSet rs) throws SQLException;
    }

    /**
     * Sets parameters in prepared statement with type safety and null protection.
     * Защита от неправильных типов параметров.
     */
    private void setParameters(@NotNull PreparedStatement stmt, @NotNull Object... params) throws SQLException {
        if (params == null || params.length == 0) {
            return; // Нет параметров
        }

        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int paramIndex = i + 1;

            try {
                if (param == null) {
                    stmt.setNull(paramIndex, Types.VARCHAR);
                } else if (param instanceof String) {
                    stmt.setString(paramIndex, (String) param);
                } else if (param instanceof Integer) {
                    stmt.setInt(paramIndex, (Integer) param);
                } else if (param instanceof Long) {
                    stmt.setLong(paramIndex, (Long) param);
                } else if (param instanceof Boolean) {
                    stmt.setBoolean(paramIndex, (Boolean) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(paramIndex, (Double) param);
                } else if (param instanceof Float) {
                    stmt.setFloat(paramIndex, (Float) param);
                } else if (param instanceof java.sql.Date) {
                    stmt.setDate(paramIndex, (java.sql.Date) param);
                } else if (param instanceof java.util.Date) {
                    stmt.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) param).getTime()));
                } else if (param instanceof UUID) {
                    stmt.setString(paramIndex, param.toString());
                } else {
                    // Неподдерживаемый тип - логируем предупреждение
                    Logger.warn("Unsupported parameter type at index " + i + ": " +
                               param.getClass().getSimpleName() + ", using toString()");
                    stmt.setString(paramIndex, param.toString());
                }
            } catch (SQLException e) {
                Logger.error("Error setting parameter at index " + i + " with value: " + param, e);
                throw e;
            }
        }
    }

    /**
     * Executes multiple queries in a single transaction for atomic operations.
     * Similar to CoreProtect's transaction handling.
     *
     * @param operations List of operations to execute
     * @return true if all operations successful
     */
    public boolean executeTransaction(@NotNull DatabaseOperation... operations) {
        try (Connection conn = connectionPool.getConnection()) {
            conn.setAutoCommit(false);

            try {
                for (DatabaseOperation op : operations) {
                    op.execute(conn);
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                Logger.error("Transaction failed, rolled back", e);
                return false;
            }
        } catch (SQLException e) {
            Logger.error("Error in transaction", e);
            return false;
        }
    }

    /**
     * Closes all database connections.
     */
    public void close() {
        if (connectionPool != null) {
            connectionPool.close();
            this.connected = false;
            Logger.info("Database connection closed");
        }
    }

    /**
     * Checks if connected to database.
     *
     * @return Connection status
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Interface for database operations in transactions.
     */
    public interface DatabaseOperation {
        void execute(Connection conn) throws SQLException;
    }

    /**
     * Gets the SQL query string for a given key.
     *
     * @param key Query key
     * @return SQL query or null
     */
    @Nullable
    public static String getQuery(@NotNull String key) {
        return SQL_QUERIES.get(key);
    }
}
