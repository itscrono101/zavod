package com.factory.generators.statistics;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks statistics for individual players regarding their generators.
 */
public class GeneratorStatistics {

    private final Map<UUID, PlayerStats> playerStats = new HashMap<>();

    /**
     * Records a generator being placed.
     *
     * @param playerUUID Player UUID
     */
    public void recordGeneratorPlaced(@NotNull UUID playerUUID) {
        playerStats.computeIfAbsent(playerUUID, k -> new PlayerStats())
            .incrementGeneratorsPlaced();
    }

    /**
     * Records a generator being broken.
     *
     * @param playerUUID Player UUID
     */
    public void recordGeneratorBroken(@NotNull UUID playerUUID) {
        playerStats.computeIfAbsent(playerUUID, k -> new PlayerStats())
            .incrementGeneratorsBroken();
    }

    /**
     * Records a generator being upgraded.
     *
     * @param playerUUID Player UUID
     */
    public void recordGeneratorUpgraded(@NotNull UUID playerUUID) {
        playerStats.computeIfAbsent(playerUUID, k -> new PlayerStats())
            .incrementGeneratorsUpgraded();
    }

    /**
     * Records resources generated.
     *
     * @param playerUUID Player UUID
     * @param amount Amount generated
     */
    public void recordResourcesGenerated(@NotNull UUID playerUUID, long amount) {
        playerStats.computeIfAbsent(playerUUID, k -> new PlayerStats())
            .addResourcesGenerated(amount);
    }

    /**
     * Gets statistics for a player.
     *
     * @param playerUUID Player UUID
     * @return PlayerStats or null
     */
    public PlayerStats getPlayerStats(@NotNull UUID playerUUID) {
        return playerStats.get(playerUUID);
    }

    /**
     * Gets all player statistics.
     *
     * @return Unmodifiable map of statistics
     */
    @NotNull
    public Map<UUID, PlayerStats> getAllStats() {
        return Map.copyOf(playerStats);
    }

    /**
     * Player statistics holder class.
     */
    public static class PlayerStats {
        private long generatorsPlaced;
        private long generatorsBroken;
        private long generatorsUpgraded;
        private long resourcesGenerated;

        public long getGeneratorsPlaced() {
            return generatorsPlaced;
        }

        public void incrementGeneratorsPlaced() {
            this.generatorsPlaced++;
        }

        public long getGeneratorsBroken() {
            return generatorsBroken;
        }

        public void incrementGeneratorsBroken() {
            this.generatorsBroken++;
        }

        public long getGeneratorsUpgraded() {
            return generatorsUpgraded;
        }

        public void incrementGeneratorsUpgraded() {
            this.generatorsUpgraded++;
        }

        public long getResourcesGenerated() {
            return resourcesGenerated;
        }

        public void addResourcesGenerated(long amount) {
            this.resourcesGenerated += Math.max(0, amount);
        }
    }
}
