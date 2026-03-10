package com.factory.generators.database;

import com.factory.generators.IronFactory;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import com.factory.generators.utils.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Exports IronFactory data to YAML/JSON backups.
 * Supports both generators and multiblock structures.
 */
public class DataExporter {

    private final IronFactory plugin;
    private final File backupDir;

    public DataExporter(@NotNull IronFactory plugin) {
        this.plugin = plugin;
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    /**
     * Exports all generators to YAML backup.
     *
     * @return true if successful
     */
    public boolean exportGenerators() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            Map<String, PlacedGenerator> generators = plugin.getGeneratorManager().getPlacedGenerators();

            for (Map.Entry<String, PlacedGenerator> entry : generators.entrySet()) {
                PlacedGenerator g = entry.getValue();

                // Validate location before exporting
                if (g == null || g.getLocation() == null || g.getLocation().getWorld() == null) {
                    Logger.warn("Skipping generator with invalid location: " + entry.getKey());
                    continue;
                }

                String path = "generators." + entry.getKey();
                try {
                    config.set(path + ".type", g.getTypeId());
                    config.set(path + ".owner", g.getOwnerUUID().toString());
                    config.set(path + ".world", g.getLocation().getWorld().getName());
                    config.set(path + ".x", g.getLocation().getBlockX());
                    config.set(path + ".y", g.getLocation().getBlockY());
                    config.set(path + ".z", g.getLocation().getBlockZ());
                    config.set(path + ".current-tick", g.getCurrentTick());
                    config.set(path + ".total-generated", g.getTotalGenerated());
                    config.set(path + ".broken", g.isBroken());
                    config.set(path + ".upgrade-level", g.getUpgradeLevel());
                    config.set(path + ".exported-time", System.currentTimeMillis());
                } catch (Exception e) {
                    Logger.warn("Error exporting generator: " + entry.getKey(), e);
                }
            }

            File backupFile = getBackupFile("generators");
            config.save(backupFile);

            Logger.info("Exported " + generators.size() + " generators to " + backupFile.getName());
            return true;
        } catch (IOException e) {
            Logger.error("Failed to export generators", e);
            return false;
        }
    }

    /**
     * Exports all multiblock structures to YAML backup.
     * Включает null-проверки для Location и World.
     *
     * @return true if successful
     */
    public boolean exportMultiBlocks() {
        YamlConfiguration config = null;
        try {
            config = new YamlConfiguration();
            Map<String, MultiBlockStructure> structures = plugin.getMultiBlockManager().getStructures();

            if (structures == null || structures.isEmpty()) {
                Logger.info("No multiblock structures to export");
                return true;
            }

            int exportedCount = 0;
            for (Map.Entry<String, MultiBlockStructure> entry : structures.entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    Logger.warn("Skipping null multiblock entry");
                    continue;
                }

                MultiBlockStructure s = entry.getValue();
                String key = entry.getKey();
                String path = "structures." + key;

                try {
                    // Валидация Location и World
                    if (s.getOwnerUUID() == null) {
                        Logger.warn("Skipping multiblock with null owner: " + key);
                        continue;
                    }

                    org.bukkit.Location drillLoc = s.getDrillLocation();
                    if (drillLoc == null || drillLoc.getWorld() == null) {
                        Logger.warn("Skipping multiblock with invalid drill location: " + key);
                        continue;
                    }

                    config.set(path + ".owner", s.getOwnerUUID().toString());
                    config.set(path + ".world", drillLoc.getWorld().getName());
                    config.set(path + ".drill-x", drillLoc.getBlockX());
                    config.set(path + ".drill-y", drillLoc.getBlockY());
                    config.set(path + ".drill-z", drillLoc.getBlockZ());
                    config.set(path + ".drill-type", s.getDrillTypeId());
                    config.set(path + ".pipe-type", s.getPipeTypeId());
                    config.set(path + ".pump-type", s.getPumpTypeId());
                    config.set(path + ".complete", s.isComplete());
                    config.set(path + ".current-tick", s.getCurrentTick());
                    config.set(path + ".total-generated", s.getTotalGenerated());
                    config.set(path + ".exported-time", System.currentTimeMillis());

                    exportedCount++;
                } catch (Exception e) {
                    Logger.warn("Error exporting multiblock: " + key, e);
                    // Продолжить с остальными структурами
                }
            }

            File backupFile = getBackupFile("multiblock");
            config.save(backupFile);

            Logger.info("Exported " + exportedCount + "/" + structures.size() +
                       " multiblock structures to " + backupFile.getName());
            return true;
        } catch (IOException e) {
            Logger.error("Failed to export multiblock structures", e);
            return false;
        } finally {
            // Явно отпустить конфиг из памяти
            config = null;
        }
    }

    /**
     * Exports all data (generators + multiblocks) at once.
     *
     * @return true if successful
     */
    public boolean exportAll() {
        boolean gen = exportGenerators();
        boolean multi = exportMultiBlocks();
        return gen && multi;
    }

    /**
     * Gets backup file with timestamp.
     *
     * @param type Type of data (generators, multiblock, etc.)
     * @return Backup file
     */
    private File getBackupFile(@NotNull String type) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        return new File(backupDir, type + "_backup_" + timestamp + ".yml");
    }

    /**
     * Lists all available backups.
     *
     * @return Array of backup files
     */
    public File[] listBackups() {
        return backupDir.listFiles((dir, name) -> name.endsWith(".yml"));
    }

    /**
     * Gets backup directory path.
     *
     * @return Backup directory
     */
    public File getBackupDirectory() {
        return backupDir;
    }
}
