package com.factory.generators.database;


import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import com.factory.generators.utils.Logger;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Imports IronFactory data from YAML backups.
 * Restores generators and multiblock structures.
 * Включает защиту от path traversal и валидацию размера файла.
 */
public class DataImporter {

    private final IronFactory plugin;
    private final File backupDir;

    // Ограничения безопасности
    private static final long MAX_BACKUP_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final int MAX_FILENAME_LENGTH = 255;

    public DataImporter(@NotNull IronFactory plugin) {
        this.plugin = plugin;
        this.backupDir = new File(plugin.getDataFolder(), "backups");
    }

    /**
     * Imports generators from backup file.
     * Валидирует путь файла (защита от directory traversal) и размер.
     *
     * @param backupFile Backup file to import
     * @param merge true to merge with existing, false to replace
     * @return Number of generators imported
     */
    public int importGenerators(@NotNull File backupFile, boolean merge) {
        // Базовые проверки
        if (backupFile == null) {
            Logger.warn("Backup file is null");
            return 0;
        }

        if (!backupFile.exists()) {
            Logger.warn("Backup file not found: " + backupFile.getName());
            return 0;
        }

        // Валидация имени файла
        String filename = backupFile.getName();
        if (filename == null || filename.isEmpty() || filename.length() > MAX_FILENAME_LENGTH) {
            Logger.warn("Invalid filename: " + filename);
            return 0;
        }

        // Защита от path traversal attacks
        try {
            File canonicalBackupFile = backupFile.getCanonicalFile();
            File canonicalBackupDir = backupDir.getCanonicalFile();

            if (!canonicalBackupFile.getAbsolutePath().startsWith(canonicalBackupDir.getAbsolutePath())) {
                Logger.warn("Invalid backup path (directory traversal detected): " + backupFile.getPath());
                return 0;
            }
        } catch (IOException e) {
            Logger.warn("Error validating backup path: " + e.getMessage());
            return 0;
        }

        // Проверка размера файла
        long fileSize = backupFile.length();
        if (fileSize == 0) {
            Logger.warn("Backup file is empty: " + filename);
            return 0;
        }

        if (fileSize > MAX_BACKUP_SIZE) {
            Logger.warn("Backup file too large: " + fileSize + " bytes (max: " + MAX_BACKUP_SIZE + ")");
            return 0;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
            ConfigurationSection section = config.getConfigurationSection("generators");

            if (section == null) {
                Logger.warn("No generators found in backup");
                return 0;
            }

            if (!merge) {
                plugin.getGeneratorManager().getPlacedGenerators().clear();
            }

            int count = 0;
            for (String key : section.getKeys(false)) {
                ConfigurationSection gen = section.getConfigurationSection(key);
                if (gen == null) continue;

                try {
                    PlacedGenerator generator = loadGeneratorFromSection(key, gen);
                    if (generator != null) {
                        // Сохраняем генератор в память (Map)
                        plugin.getGeneratorManager().getPlacedGenerators().put(key, generator);

                        // ВАЖНО: восстанавливаем физический блок в мире!
                        // При импорте мир может быть "чистым" — данные есть, но блока нет.
                        // Без этой строки будет HUD в воздухе и пустое место.
                        Location loc = generator.getLocation();
                        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());

                        if (loc != null && loc.getWorld() != null && type != null) {
                            // Ставим блок нужного типа (FURNACE для завода, DEEPSLATE_ORE для рудника и т.д.)
                            loc.getBlock().setType(type.getBlockMaterial());

                            // Создаём голограмму над блоком
                            plugin.getHologramManager().createHologram(generator, type);
                        }

                        count++;
                    }
                } catch (Exception e) {
                    Logger.warn("Error importing generator: " + key);
                }
            }

            Logger.info("Imported " + count + " generators from " + backupFile.getName());
            return count;
        } catch (Exception e) {
            Logger.error("Error importing generators", e);
            return 0;
        }
    }

    /**
     * Imports multiblock structures from backup file.
     * Валидирует путь файла и размер перед импортом.
     *
     * @param backupFile Backup file to import
     * @param merge true to merge with existing, false to replace
     * @return Number of structures imported
     */
    public int importMultiBlocks(@NotNull File backupFile, boolean merge) {
        // Базовые проверки
        if (backupFile == null) {
            Logger.warn("Backup file is null");
            return 0;
        }

        if (!backupFile.exists()) {
            Logger.warn("Backup file not found: " + backupFile.getName());
            return 0;
        }

        // Валидация имени файла
        String filename = backupFile.getName();
        if (filename == null || filename.isEmpty() || filename.length() > MAX_FILENAME_LENGTH) {
            Logger.warn("Invalid filename: " + filename);
            return 0;
        }

        // Защита от path traversal attacks
        try {
            File canonicalBackupFile = backupFile.getCanonicalFile();
            File canonicalBackupDir = backupDir.getCanonicalFile();

            if (!canonicalBackupFile.getAbsolutePath().startsWith(canonicalBackupDir.getAbsolutePath())) {
                Logger.warn("Invalid backup path (directory traversal detected): " + backupFile.getPath());
                return 0;
            }
        } catch (IOException e) {
            Logger.warn("Error validating backup path: " + e.getMessage());
            return 0;
        }

        // Проверка размера файла
        long fileSize = backupFile.length();
        if (fileSize == 0) {
            Logger.warn("Backup file is empty: " + filename);
            return 0;
        }

        if (fileSize > MAX_BACKUP_SIZE) {
            Logger.warn("Backup file too large: " + fileSize + " bytes (max: " + MAX_BACKUP_SIZE + ")");
            return 0;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
            ConfigurationSection section = config.getConfigurationSection("structures");

            if (section == null) {
                Logger.warn("No structures found in backup");
                return 0;
            }

            if (!merge) {
                plugin.getMultiBlockManager().getStructures().clear();
            }

            int count = 0;
            for (String key : section.getKeys(false)) {
                ConfigurationSection struct = section.getConfigurationSection(key);
                if (struct == null) continue;

                try {
                    MultiBlockStructure structure = loadStructureFromSection(key, struct);
                    if (structure != null) {
                        // Сохраняем структуру в память
                        plugin.getMultiBlockManager().registerLoadedStructure(structure);

                        // ВАЖНО: восстанавливаем физические блоки буровой в мире!
                        // registerLoadedStructure только регистрирует данные, но не ставит блоки.
                        // restoreStructure ставит нужные блоки (незерит, медь, редстоун) на нужные координаты.
                        plugin.getMultiBlockManager().restoreStructurePublic(structure);

                        count++;
                    }
                } catch (Exception e) {
                    Logger.warn("Error importing structure: " + key);
                }
            }

            Logger.info("Imported " + count + " multiblock structures from " + backupFile.getName());
            return count;
        } catch (Exception e) {
            Logger.error("Error importing multiblock structures", e);
            return 0;
        }
    }

    /**
     * Imports all data from backup file.
     *
     * @param backupFile Backup file to import
     * @param merge true to merge with existing, false to replace
     * @return true if successful
     */
    public int importAll(@NotNull File backupFile, boolean merge) {
        // Try to import as generators first
        if (backupFile.getName().contains("generators")) {
            return importGenerators(backupFile, merge);
        }
        // Try to import as multiblocks
        else if (backupFile.getName().contains("multiblock")) {
            return importMultiBlocks(backupFile, merge);
        }
        // Try both if can't determine
        else {
            int gen = importGenerators(backupFile, merge);
            int multi = importMultiBlocks(backupFile, merge);
            return gen + multi;
        }
    }

    /**
     * Loads a generator from configuration section.
     *
     * @param key Location key
     * @param section Configuration section
     * @return Loaded generator or null
     */
    @Nullable
    private PlacedGenerator loadGeneratorFromSection(@NotNull String key, @NotNull ConfigurationSection section) {
        try {
            String typeId = section.getString("type");
            String ownerStr = section.getString("owner");
            String worldName = section.getString("world");

            if (typeId == null || ownerStr == null || worldName == null) {
                return null;
            }

            UUID owner = UUID.fromString(ownerStr);
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Logger.warn("World not found: " + worldName);
                return null;
            }

            int x = section.getInt("x");
            int y = section.getInt("y");
            int z = section.getInt("z");

            Location location = new Location(world, x, y, z);
            PlacedGenerator generator = new PlacedGenerator(typeId, owner, location);

            generator.setCurrentTick(section.getInt("current-tick", 0));
            generator.setTotalGenerated(section.getLong("total-generated", 0));
            generator.setBroken(section.getBoolean("broken", false));
            generator.setUpgradeLevel(section.getInt("upgrade-level", 0));
            // Восстанавливаем здоровье рудника — без этого рудник после импорта
// будет показывать "требует активации" даже если был активирован
            generator.setMineHealth(section.getInt("mine-health", 0));

            return generator;
        } catch (Exception e) {
            Logger.warn("Error loading generator: " + e.getMessage());
            return null;
        }
    }

    /**
     * Loads a multiblock structure from configuration section.
     *
     * @param key Structure key
     * @param section Configuration section
     * @return Loaded structure or null
     */
    @Nullable
    private MultiBlockStructure loadStructureFromSection(@NotNull String key, @NotNull ConfigurationSection section) {
        try {
            String ownerStr = section.getString("owner");
            String worldName = section.getString("world");

            if (ownerStr == null || worldName == null) {
                return null;
            }

            UUID owner = UUID.fromString(ownerStr);
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                Logger.warn("World not found: " + worldName);
                return null;
            }

            int x = section.getInt("drill-x");
            int y = section.getInt("drill-y");
            int z = section.getInt("drill-z");

            Location location = new Location(world, x, y, z);
            MultiBlockStructure structure = new MultiBlockStructure(owner, worldName, x, y, z);

            structure.setDrillTypeId(section.getString("drill-type"));
            structure.setPipeTypeId(section.getString("pipe-type"));
            structure.setPumpTypeId(section.getString("pump-type"));
            structure.setComplete(section.getBoolean("complete", false));
            structure.setCurrentTick(section.getInt("current-tick", 0));
            structure.setTotalGenerated(section.getLong("total-generated", 0));

            return structure;
        } catch (Exception e) {
            Logger.warn("Error loading structure: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets latest backup file.
     *
     * @param type Type of backup (generators, multiblock, etc.)
     * @return Latest backup file or null
     */
    @Nullable
    public File getLatestBackup(String type) {
        if (!backupDir.exists()) {
            return null;
        }

        File[] backups = backupDir.listFiles((dir, name) ->
            name.contains(type) && name.endsWith(".yml")
        );

        if (backups == null || backups.length == 0) {
            return null;
        }

        // Get latest by modification time
        File latest = backups[0];
        for (File backup : backups) {
            if (backup.lastModified() > latest.lastModified()) {
                latest = backup;
            }
        }

        return latest;
    }

    /**
     * Checks if backup file is valid.
     *
     * @param backupFile File to check
     * @return true if valid
     */
    public boolean isValidBackup(@NotNull File backupFile) {
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(backupFile);
            return config.contains("generators") || config.contains("structures");
        } catch (Exception e) {
            return false;
        }
    }
}
