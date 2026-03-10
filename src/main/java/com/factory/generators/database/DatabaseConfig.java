package com.factory.generators.database;

import org.jetbrains.annotations.NotNull;

/**
 * Configuration for database connection.
 * Supports both SQLite and MySQL with optimizations.
 */
public class DatabaseConfig {

    public enum DatabaseType {
        SQLITE("org.sqlite.JDBC", "jdbc:sqlite:%s"),
        MYSQL("com.mysql.cj.jdbc.Driver", "jdbc:mysql://%s:%d/%s?useSSL=%s&characterEncoding=utf8mb4&serverTimezone=UTC");

        private final String driver;
        private final String urlFormat;

        DatabaseType(String driver, String urlFormat) {
            this.driver = driver;
            this.urlFormat = urlFormat;
        }

        public String getDriver() {
            return driver;
        }

        public String getUrlFormat() {
            return urlFormat;
        }
    }

    private final DatabaseType type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String filePath; // For SQLite
    private final int maxPoolSize;
    private final boolean useSSL;

    private DatabaseConfig(@NotNull Builder builder) {
        this.type = builder.type;
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.filePath = builder.filePath;
        this.maxPoolSize = builder.maxPoolSize;
        this.useSSL = builder.useSSL;
    }

    /**
     * Gets the JDBC driver class name.
     *
     * @return Driver class name
     */
    public String getJdbcDriver() {
        return type.getDriver();
    }

    /**
     * Gets the JDBC connection URL.
     *
     * @return JDBC URL
     */
    public String getJdbcUrl() {
        if (type == DatabaseType.SQLITE) {
            return String.format(type.getUrlFormat(), filePath);
        } else {
            return String.format(type.getUrlFormat(), host, port, database, useSSL);
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFilePath() {
        return filePath;
    }

    public DatabaseType getType() {
        return type;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    /**
     * Builder for DatabaseConfig.
     */
    public static class Builder {
        private DatabaseType type = DatabaseType.SQLITE;
        private String host = "localhost";
        private int port = 3306;
        private String database = "ironfactory";
        private String username = "root";
        private String password = "";
        private String filePath = "plugins/IronFactory/data.db";
        private int maxPoolSize = 10;
        private boolean useSSL = false;

        public Builder type(@NotNull DatabaseType type) {
            this.type = type;
            return this;
        }

        public Builder host(@NotNull String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder database(@NotNull String database) {
            this.database = database;
            return this;
        }

        public Builder username(@NotNull String username) {
            this.username = username;
            return this;
        }

        public Builder password(@NotNull String password) {
            this.password = password;
            return this;
        }

        public Builder filePath(@NotNull String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder maxPoolSize(int size) {
            this.maxPoolSize = Math.max(5, Math.min(size, 50));
            return this;
        }

        public Builder useSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        /**
         * Builds DatabaseConfig from YAML configuration.
         *
         * @param dbType Database type (sqlite or mysql)
         * @param host MySQL host
         * @param port MySQL port
         * @param database Database name
         * @param username MySQL username
         * @param password MySQL password
         * @return DatabaseConfig instance
         */
        public static DatabaseConfig fromConfig(@NotNull String dbType, String host, int port,
                                                String database, String username, String password) {
            Builder builder = new Builder();

            if ("mysql".equalsIgnoreCase(dbType)) {
                builder.type(DatabaseType.MYSQL)
                       .host(host)
                       .port(port)
                       .database(database)
                       .username(username)
                       .password(password);
            } else {
                builder.type(DatabaseType.SQLITE);
            }

            return builder.build();
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
