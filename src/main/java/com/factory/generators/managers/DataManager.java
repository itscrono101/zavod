package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final IronFactory plugin;
    private File dataFile;
    private File multiBlockFile;

    public DataManager(IronFactory plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/generators.yml");
        this.multiBlockFile = new File(plugin.getDataFolder(), "data/multiblock.yml");
    }

    public void loadGenerators() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { }
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("generators");
        if (section == null) return;

        Map<String, PlacedGenerator> generators = plugin.getGeneratorManager().getPlacedGenerators();
        generators.clear();

        for (String key : section.getKeys(false)) {
            ConfigurationSection gen = section.getConfigurationSection(key);
            if (gen == null) continue;

            try {
                String typeId = gen.getString("type");
                UUID owner = UUID.fromString(gen.getString("owner"));
                String[] parts = key.split(";");

                PlacedGenerator generator = new PlacedGenerator(typeId, owner, parts[0],
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                generator.setCurrentTick(gen.getInt("current-tick", 0));
                generator.setTotalGenerated(gen.getLong("total-generated", 0));
                generator.setBroken(gen.getBoolean("broken", false));
                generator.setUpgradeLevel(gen.getInt("upgrade-level", 0));
                generator.setMineHealth(gen.getInt("mine-health", 0));

                generators.put(key, generator);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (generator.getLocation() != null) {
                        GeneratorType type = plugin.getConfigManager().getGeneratorType(typeId);
                        if (type != null) {
                            generator.getLocation().getBlock().setType(type.getBlockMaterial());
                            plugin.getHologramManager().createHologram(generator, type);
                        }
                    }
                }, 20L);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки: " + key);
            }
        }
        plugin.getLogger().info("Загружено " + generators.size() + " генераторов");
    }

    public void saveGenerators() {
        YamlConfiguration config = new YamlConfiguration();
        Map<String, PlacedGenerator> generators = plugin.getGeneratorManager().getPlacedGenerators();

        for (Map.Entry<String, PlacedGenerator> entry : generators.entrySet()) {
            PlacedGenerator g = entry.getValue();
            String path = "generators." + entry.getKey();
            config.set(path + ".type", g.getTypeId());
            config.set(path + ".owner", g.getOwnerUUID().toString());
            config.set(path + ".current-tick", g.getCurrentTick());
            config.set(path + ".total-generated", g.getTotalGenerated());
            config.set(path + ".broken", g.isBroken());
            config.set(path + ".upgrade-level", g.getUpgradeLevel());
            config.set(path + ".mine-health", g.getMineHealth());
        }

        try {
            dataFile.getParentFile().mkdirs();
            config.save(dataFile);
        } catch (IOException e) { }
    }

    public void loadMultiBlockStructures() {
        if (!multiBlockFile.exists()) {
            multiBlockFile.getParentFile().mkdirs();
            try { multiBlockFile.createNewFile(); } catch (IOException e) { }
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(multiBlockFile);
        ConfigurationSection section = config.getConfigurationSection("structures");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;

            try {
                UUID owner = UUID.fromString(s.getString("owner"));
                String[] parts = key.split(";");

                MultiBlockStructure structure = new MultiBlockStructure(owner, parts[0],
                        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));

                structure.setDrillTypeId(s.getString("drill-type"));
                structure.setPipeTypeId(s.getString("pipe-type"));
                structure.setPumpTypeId(s.getString("pump-type"));
                structure.setComplete(s.getBoolean("complete", false));
                structure.setCurrentTick(s.getInt("current-tick", 0));
                structure.setTotalGenerated(s.getLong("total-generated", 0));

                plugin.getMultiBlockManager().registerLoadedStructure(structure);

                plugin.getLogger().info("[Data] Загружена вышка: " + key + " complete=" + structure.isComplete());
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки структуры: " + key);
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getMultiBlockManager().restoreAllStructures();
        }, 40L);
    }

    public void saveMultiBlockStructures() {
        YamlConfiguration config = new YamlConfiguration();
        Map<String, MultiBlockStructure> structures = plugin.getMultiBlockManager().getStructures();

        for (Map.Entry<String, MultiBlockStructure> entry : structures.entrySet()) {
            MultiBlockStructure s = entry.getValue();
            String path = "structures." + entry.getKey();
            config.set(path + ".owner", s.getOwnerUUID().toString());
            config.set(path + ".drill-type", s.getDrillTypeId());
            config.set(path + ".pipe-type", s.getPipeTypeId());
            config.set(path + ".pump-type", s.getPumpTypeId());
            config.set(path + ".complete", s.isComplete());
            config.set(path + ".current-tick", s.getCurrentTick());
            config.set(path + ".total-generated", s.getTotalGenerated());
        }

        try {
            multiBlockFile.getParentFile().mkdirs();
            config.save(multiBlockFile);
            plugin.getLogger().info("[Data] Сохранено " + structures.size() + " вышек");
        } catch (IOException e) { }
    }

    public void startAutoSave() {
        int interval = 6000;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            saveGenerators();
            saveMultiBlockStructures();
        }, interval, interval);
    }

    // Casino block methods (simple memory-based storage for now)
    public void saveCasinoBlock(org.bukkit.Location location) {
        // In a real implementation, this would save to database
        // For now, we keep it in CasinoManager's memory map
        plugin.getLogger().info("[Casino] Казино блок сохранен: " + location);
    }

    public void removeCasinoBlock(org.bukkit.Location location) {
        // In a real implementation, this would remove from database
        plugin.getLogger().info("[Casino] Казино блок удален: " + location);
    }

    public void loadCasinoBlocks(java.util.Map<String, org.bukkit.Location> casinoBlocks) {
        // In a real implementation, this would load from database
        // For now, casino blocks persist in memory and are cleared on restart
        plugin.getLogger().info("[Casino] Загружено казино блоков: " + casinoBlocks.size());
    }
}