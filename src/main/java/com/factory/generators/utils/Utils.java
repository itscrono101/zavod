package com.factory.generators.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for common operations in the IronFactory plugin.
 * Provides color formatting, time conversion, and validation helpers.
 */
public class Utils {

    private static final Logger LOGGER = Bukkit.getLogger();

    // Prevent instantiation
    private Utils() {
        throw new AssertionError("Cannot instantiate Utils class");
    }

    /**
     * Translates color codes in text (& symbol to §).
     *
     * @param text Text with & color codes
     * @return Formatted text with § color codes, or empty string if text is null
     */
    @NotNull
    public static String colorize(@Nullable String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Converts game ticks to formatted time string.
     *
     * @param ticks Number of ticks (20 ticks = 1 second)
     * @return Formatted string in format "minutes:seconds"
     */
    @NotNull
    public static String formatTime(int ticks) {
        int totalSeconds = ticks / Constants.Timing.TICKS_PER_SECOND;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Converts Material enum name to readable format.
     * Example: IRON_INGOT -> Iron Ingot
     *
     * @param material Material name in enum format
     * @return Formatted material name
     */
    @NotNull
    public static String formatMaterial(@NotNull String material) {
        if (material == null || material.isEmpty()) return "Unknown";

        String name = material.toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();

        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Safely gets player or logs warning if not found.
     *
     * @param uuid Player UUID
     * @return Player object or null if not found
     */
    @Nullable
    public static Player getPlayerSafe(@NotNull java.util.UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            logWarning("Player with UUID " + uuid + " not found");
        }
        return player;
    }

    /**
     * Validates location for null world and coordinates.
     *
     * @param location Location to validate
     * @return true if location is valid, false otherwise
     */
    public static boolean isValidLocation(@Nullable Location location) {
        return location != null && location.getWorld() != null;
    }

    /**
     * Checks if a block location is loaded in memory.
     *
     * @param location Block location
     * @return true if chunk is loaded, false otherwise
     */
    public static boolean isChunkLoaded(@NotNull Location location) {
        if (!isValidLocation(location)) return false;
        int chunkX = location.getBlockX() >> Constants.Chunk.CHUNK_SIZE_BITS;
        int chunkZ = location.getBlockZ() >> Constants.Chunk.CHUNK_SIZE_BITS;
        return location.getWorld().isChunkLoaded(chunkX, chunkZ);
    }

    /**
     * Sends colored message to player if online.
     *
     * @param player Target player (can be null)
     * @param message Message with & color codes
     */
    public static void sendMessage(@Nullable Player player, @NotNull String message) {
        if (player == null) return;
        player.sendMessage(colorize(message));
    }

    /**
     * Sends multiple messages to player.
     *
     * @param player Target player
     * @param messages Messages to send
     */
    public static void sendMessages(@Nullable Player player, @NotNull String... messages) {
        if (player == null) return;
        for (String msg : messages) {
            player.sendMessage(colorize(msg));
        }
    }

    /**
     * Logs info message to console.
     *
     * @param message Message to log
     */
    public static void logInfo(@NotNull String message) {
        LOGGER.log(Level.INFO, "[IronFactory] " + message);
    }

    /**
     * Logs warning message to console.
     *
     * @param message Warning message
     */
    public static void logWarning(@NotNull String message) {
        LOGGER.log(Level.WARNING, "[IronFactory] " + message);
    }

    /**
     * Logs error message with exception to console.
     *
     * @param message Error message
     * @param exception Exception to log
     */
    public static void logError(@NotNull String message, @Nullable Exception exception) {
        LOGGER.log(Level.SEVERE, "[IronFactory] " + message, exception);
    }

    /**
     * Checks if a number is within range.
     *
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return true if value is in range
     */
    public static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Clamps a value between min and max.
     *
     * @param value Value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return Clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
