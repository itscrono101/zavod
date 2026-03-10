package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages casino blocks and game logic.
 * Handles creation, deletion, and validation of casino locations.
 */
public class CasinoManager {

    private final IronFactory plugin;
    private final ConfigManager configManager;

    // Cache of casino block locations (world:x:y:z -> Location)
    private final Map<String, Location> casinoBlocks;

    // Cooldown tracking for players (UUID -> last play time in millis)
    private final Map<UUID, Long> playCooldowns;

    private static final long COOLDOWN_MILLIS = 1000; // 1 second between games
    private static final String OIL_NAME = "Нефть";

    public CasinoManager(@NotNull IronFactory plugin, @NotNull ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.casinoBlocks = new HashMap<>();
        this.playCooldowns = new HashMap<>();

        Logger.info("CasinoManager initialized");
    }

    /**
     * Register a casino block at the given location.
     */
    public void registerCasinoBlock(@NotNull Location location) {
        if (location.getWorld() == null) {
            Logger.warn("Cannot register casino block: world is null");
            return;
        }

        String key = locationToKey(location);
        casinoBlocks.put(key, location);
        Logger.info("Casino block registered at " + key);

        // Save to database
        plugin.getDataManager().saveCasinoBlock(location);
    }

    /**
     * Remove a casino block from the given location.
     */
    public void removeCasinoBlock(@NotNull Location location) {
        if (location.getWorld() == null) {
            Logger.warn("Cannot remove casino block: world is null");
            return;
        }

        String key = locationToKey(location);
        casinoBlocks.remove(key);
        Logger.info("Casino block removed from " + key);

        // Remove from database
        plugin.getDataManager().removeCasinoBlock(location);
    }

    /**
     * Check if the given location has a casino block.
     */
    public boolean isCasinoBlock(@NotNull Location location) {
        if (location.getWorld() == null) return false;
        return casinoBlocks.containsKey(locationToKey(location));
    }

    /**
     * Get casino block at location.
     */
    @Nullable
    public Location getCasinoBlock(@NotNull Location location) {
        if (location.getWorld() == null) return null;
        return casinoBlocks.get(locationToKey(location));
    }

    /**
     * Get all casino blocks.
     */
    @NotNull
    public Map<String, Location> getAllCasinoBlocks() {
        return new HashMap<>(casinoBlocks);
    }

    /**
     * Load all casino blocks from database (called on plugin startup).
     */
    public void loadCasinoBlocks() {
        plugin.getDataManager().loadCasinoBlocks(casinoBlocks);
        Logger.info("Loaded " + casinoBlocks.size() + " casino blocks");
    }

    /**
     * Check if player has enough oil to make a bet.
     */
    public boolean hasOil(@NotNull Player player, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.INK_SAC) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(OIL_NAME)) {
                    count += item.getAmount();
                    if (count >= amount) return true;
                }
            }
        }
        return count >= amount;
    }

    /**
     * Remove oil from player inventory.
     */
    public void removeOil(@NotNull Player player, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.INK_SAC && remaining > 0) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().contains(OIL_NAME)) {
                    int take = Math.min(item.getAmount(), remaining);
                    item.setAmount(item.getAmount() - take);
                    remaining -= take;
                    if (remaining <= 0) break;
                }
            }
        }
        player.updateInventory();
    }

    /**
     * Give oil to player.
     */
    public void giveOil(@NotNull Player player, int amount) {
        ItemStack oil = createOilItem(amount);
        player.getInventory().addItem(oil);
        player.updateInventory();
    }

    /**
     * Create an oil item (for rewards).
     */
    @NotNull
    public ItemStack createOilItem(int amount) {
        ItemStack oil = new ItemStack(Material.INK_SAC, Math.min(amount, 64));
        ItemMeta meta = oil.getItemMeta();
        if (meta != null) {
            // Тот же стиль что и в MultiBlockManager для единообразия
            meta.setDisplayName(colorize("&8⛽ &fНефть"));

            // colorize() — метод в CasinoManager, аналог color() в других классах
            meta.setLore(new java.util.ArrayList<>(java.util.Arrays.asList(
                    colorize(""),
                    colorize("&7Сырая нефть"),
                    colorize("&8Из буровой вышки")
            )));

            oil.setItemMeta(meta);
        }
        return oil;
    }

    /**
     * Check if player is on cooldown for casino plays.
     */
    public boolean isOnCooldown(@NotNull Player player) {
        Long lastPlay = playCooldowns.get(player.getUniqueId());
        if (lastPlay == null) return false;

        long elapsed = System.currentTimeMillis() - lastPlay;
        return elapsed < COOLDOWN_MILLIS;
    }

    /**
     * Set player cooldown.
     */
    public void setCooldown(@NotNull Player player) {
        playCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Get casino block material from config.
     */
    @NotNull
    public Material getCasinoBlockMaterial() {
        String materialName = configManager.getString("casino.casino-block", "EMERALD_BLOCK");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            Logger.warn("Invalid casino block material: " + materialName + ", using EMERALD_BLOCK");
            return Material.EMERALD_BLOCK;
        }
    }

    /**
     * Get bet amounts from config.
     */
    @NotNull
    public Set<Integer> getAllowedBetAmounts() {
        Set<Integer> amounts = new HashSet<>();
        java.util.List<?> list = configManager.getList("casino.bet-amounts", null);
        if (list != null) {
            for (Object item : list) {
                if (item instanceof Integer) {
                    amounts.add((Integer) item);
                } else if (item instanceof String) {
                    try {
                        amounts.add(Integer.parseInt((String) item));
                    } catch (NumberFormatException e) {
                        Logger.warn("Invalid bet amount in config: " + item);
                    }
                }
            }
        }

        // Default if config is empty
        if (amounts.isEmpty()) {
            amounts.add(1);
            amounts.add(5);
            amounts.add(10);
            amounts.add(50);
        }
        return amounts;
    }

    /**
     * Get game configuration values.
     */
    public double getSlotMachineWinChance() {
        return configManager.getDouble("casino.slot-machine-win-chance", 0.25);
    }

    public double getRouletteWinChance() {
        return configManager.getDouble("casino.roulette-win-chance", 0.47);
    }

    public double getHighLowWinChance() {
        return configManager.getDouble("casino.high-low-win-chance", 0.48);
    }

    public double getSlotMachineMultiplier() {
        return configManager.getDouble("casino.slot-machine-multiplier", 3.0);
    }

    public double getRouletteMultiplier() {
        return configManager.getDouble("casino.roulette-multiplier", 1.8);
    }

    public double getHighLowMultiplier() {
        return configManager.getDouble("casino.high-low-multiplier", 1.9);
    }

    /**
     * Check if casino is enabled in config.
     */
    public boolean isCasinoEnabled() {
        return configManager.getBoolean("casino.enabled", true);
    }

    /**
     * Convert location to string key.
     */
    @NotNull
    private String locationToKey(@NotNull Location location) {
        String world = location.getWorld() != null ? location.getWorld().getName() : "null";
        return world + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    /**
     * Colorize text (& to §).
     */
    @NotNull
    private String colorize(@NotNull String text) {
        return text.replace("&", "§");
    }
}
