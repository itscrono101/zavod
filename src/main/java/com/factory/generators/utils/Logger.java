package com.factory.generators.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Centralized logging utility for the IronFactory plugin.
 * Logs to files in plugins/IronFactory/logs/ directory.
 */
public class Logger {

    private static final String LOG_DIR = "plugins/IronFactory/logs";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    // Prevent instantiation
    private Logger() {
        throw new AssertionError("Cannot instantiate Logger class");
    }

    /**
     * Logs an informational message to file.
     *
     * @param message Message to log
     */
    public static void info(@NotNull String message) {
        logToFile("info", message, null);
    }

    /**
     * Logs a debug message to file.
     *
     * @param message Message to log
     */
    public static void debug(@NotNull String message) {
        logToFile("debug", message, null);
    }

    /**
     * Logs a warning message to file.
     *
     * @param message Warning message
     */
    public static void warn(@NotNull String message) {
        logToFile("warning", message, null);
    }

    /**
     * Logs a warning message with exception to file.
     *
     * @param message Warning message
     * @param exception Exception to log
     */
    public static void warn(@NotNull String message, @NotNull Exception exception) {
        logToFile("warning", message, exception);
    }

    /**
     * Logs an error message to file.
     *
     * @param message Error message
     */
    public static void error(@NotNull String message) {
        logToFile("error", message, null);
    }

    /**
     * Logs an error message with exception to file.
     *
     * @param message Error message
     * @param exception Exception to log
     */
    public static void error(@NotNull String message, @Nullable Exception exception) {
        logToFile("error", message, exception);
    }

    /**
     * Logs a critical error to file.
     *
     * @param message Error message
     * @param exception Exception details
     */
    public static void critical(@NotNull String message, @Nullable Exception exception) {
        logToFile("critical", message, exception);
    }

    /**
     * Logs security-related events to a separate file.
     *
     * @param message Security event message
     */
    public static void security(@NotNull String message) {
        logToSecurityFile(message);
    }

    /**
     * Internal method to log to file.
     */
    private static void logToFile(@NotNull String level, @NotNull String message, @Nullable Exception exception) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String dateStr = FILE_DATE_FORMAT.format(new Date());
            File logFile = new File(logDir, "ironfactory-" + dateStr + ".log");

            try (FileWriter writer = new FileWriter(logFile, true)) {
                String timestamp = DATE_FORMAT.format(new Date());
                String logLevel = level.toUpperCase();
                writer.append(String.format("[%s] [%s] %s\n", timestamp, logLevel, message));

                if (exception != null) {
                    writer.append(String.format("[%s] [%s] Exception: %s\n", timestamp, logLevel, exception.getMessage()));
                    for (StackTraceElement element : exception.getStackTrace()) {
                        writer.append(String.format("[%s] [%s]   at %s\n", timestamp, logLevel, element));
                    }
                }
                writer.flush();
            }
        } catch (IOException e) {
            // Silent fail - don't spam console
        }
    }

    /**
     * Logs security events to a separate file.
     */
    private static void logToSecurityFile(@NotNull String message) {
        try {
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String dateStr = FILE_DATE_FORMAT.format(new Date());
            File securityFile = new File(logDir, "security-" + dateStr + ".log");

            try (FileWriter writer = new FileWriter(securityFile, true)) {
                String timestamp = DATE_FORMAT.format(new Date());
                writer.append(String.format("[%s] %s\n", timestamp, message));
                writer.flush();
            }
        } catch (IOException e) {
            // Silent fail
        }
    }
}
