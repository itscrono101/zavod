package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final IronFactory plugin;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public DataManager(IronFactory plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/generators.yml");
    }

    public void loadGenerators() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать файл данных: " + e.getMessage());
            }
            return;
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection gensSection = dataConfig.getConfigurationSection("generators");
        
        if (gensSection == null) return;
        
        Map<String, PlacedGenerator> generators = plugin.getGeneratorManager().getPlacedGenerators();
        generators.clear();
        
        for (String key : gensSection.getKeys(false)) {
            ConfigurationSection genSection = gensSection.getConfigurationSection(key);
            if (genSection == null) continue;
            
            try {
                String typeId = genSection.getString("type");
                UUID ownerUUID = UUID.fromString(genSection.getString("owner"));
                
                String[] parts = key.split(";");
                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                
                PlacedGenerator generator = new PlacedGenerator(typeId, ownerUUID, worldName, x, y, z);
                generator.setCurrentTick(genSection.getInt("current-tick", 0));
                generator.setTotalGenerated(genSection.getLong("total-generated", 0));
                generator.setPlacedTime(genSection.getLong("placed-time", System.currentTimeMillis()));
                
                generators.put(key, generator);
                
                // Создание голограммы
                GeneratorType type = plugin.getConfigManager().getGeneratorType(typeId);
                if (type != null && generator.getLocation() != null) {
                    plugin.getHologramManager().createHologram(generator, type);
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки генератора " + key + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Загружено " + generators.size() + " размещённых генераторов");
    }

    public void saveGenerators() {
        dataConfig = new YamlConfiguration();
        
        Map<String, PlacedGenerator> generators = plugin.getGeneratorManager().getPlacedGenerators();
        
        for (Map.Entry<String, PlacedGenerator> entry : generators.entrySet()) {
            String key = entry.getKey();
            PlacedGenerator gen = entry.getValue();
            
            String path = "generators." + key;
            dataConfig.set(path + ".type", gen.getTypeId());
            dataConfig.set(path + ".owner", gen.getOwnerUUID().toString());
            dataConfig.set(path + ".current-tick", gen.getCurrentTick());
            dataConfig.set(path + ".total-generated", gen.getTotalGenerated());
            dataConfig.set(path + ".placed-time", gen.getPlacedTime());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить данные генераторов: " + e.getMessage());
        }
    }

    public void startAutoSave() {
        int interval = plugin.getConfig().getInt("settings.auto-save-interval", 6000);
        
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveGenerators();
        }, interval, interval);
    }
}
