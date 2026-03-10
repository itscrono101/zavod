package com.factory.generators.database;

import com.factory.generators.utils.Logger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP-based connection pool implementation (как в CoreProtect).
 * Производственный-качества connection pooling с минимальными утечками.
 *
 * Преимущества HikariCP:
 * - Быстрая инициализация
 * - Низкие накладные расходы на соединение
 * - Встроенная валидация соединений
 * - Автоматическое управление жизненным циклом
 * - Thread-safe без явных блокировок
 * - Мониторинг и метрики
 */
public class HikariConnectionPool {

    private final HikariDataSource dataSource;
    private final DatabaseConfig config;

    public HikariConnectionPool(@NotNull DatabaseConfig config) {
        this.config = config;
        this.dataSource = initializeHikariPool();
    }

    /**
     * Инициализирует HikariCP пул с оптимальными параметрами.
     */
    private HikariDataSource initializeHikariPool() {
        HikariConfig hConfig = new HikariConfig();

        // Основные параметры подключения
        hConfig.setJdbcUrl(config.getJdbcUrl());
        hConfig.setUsername(config.getUsername());
        hConfig.setPassword(config.getPassword());
        hConfig.setDriverClassName(config.getJdbcDriver());

        // Оптимизация размера пула
        // Для Minecraft плагина: обычно 5-10 соединений достаточно
        int maxPoolSize = Math.min(config.getMaxPoolSize(), 20);
        hConfig.setMaximumPoolSize(maxPoolSize);
        hConfig.setMinimumIdle(Math.max(2, maxPoolSize / 3));

        // Таймауты (в миллисекундах)
        hConfig.setConnectionTimeout(10000);      // 10 сек для получения соединения
        hConfig.setIdleTimeout(600000);           // 10 мин перед закрытием неиспользуемого
        hConfig.setMaxLifetime(1800000);          // 30 мин максимальный век соединения
        hConfig.setLeakDetectionThreshold(60000); // Детект утечек через 60 сек

        // Валидация соединений
        hConfig.setConnectionTestQuery("SELECT 1");  // Проверка перед использованием
        hConfig.setAutoCommit(true);

        // Имя пула для логирования
        hConfig.setPoolName("IronFactory-Pool");

        // Отключить детальное логирование в логгерах HikariCP (используем свой Logger)
        hConfig.setInitializationFailTimeout(1000);

        try {
            HikariDataSource ds = new HikariDataSource(hConfig);
            Logger.info("HikariCP pool initialized: max=" + maxPoolSize + ", min=" + hConfig.getMinimumIdle());
            return ds;
        } catch (Exception e) {
            Logger.error("Failed to initialize HikariCP pool", e);
            throw new RuntimeException("Cannot initialize database connection pool", e);
        }
    }

    /**
     * Получить соединение из пула (thread-safe, автоматически управляется HikariCP).
     *
     * @return Database connection
     * @throws SQLException if unable to get connection
     */
    @NotNull
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariCP datasource is closed");
        }
        return dataSource.getConnection();
    }

    /**
     * Проверить статус пула.
     *
     * @return true если пул рабочий
     */
    public boolean isHealthy() {
        try {
            if (dataSource == null || dataSource.isClosed()) {
                return false;
            }
            try (Connection conn = dataSource.getConnection()) {
                return !conn.isClosed();
            }
        } catch (SQLException e) {
            Logger.warn("Pool health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Получить статистику пула.
     *
     * @return статистика в строке
     */
    @NotNull
    public String getStats() {
        if (dataSource == null) {
            return "Pool not initialized";
        }
        try {
            return String.format(
                "Active: %d, Idle: %d, Max: %d, Waiting: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getMaximumPoolSize(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
            );
        } catch (Exception e) {
            return "Stats unavailable: " + e.getMessage();
        }
    }

    /**
     * Закрыть пул и все соединения.
     * HikariCP сам управляет закрытием соединений - просто вызов close().
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Logger.info("HikariCP pool closed");
        }
    }

    /**
     * Проверить закрыт ли пул.
     *
     * @return true если закрыт
     */
    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    /**
     * Получить DataSource напрямую для расширенных операций.
     *
     * @return HikariDataSource
     */
    @NotNull
    public HikariDataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource not initialized");
        }
        return dataSource;
    }
}
