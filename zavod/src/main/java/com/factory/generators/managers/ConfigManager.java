package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorDrop;
import com.factory.generators.models.GeneratorType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final IronFactory plugin;
    private FileConfiguration config;
    private final Map<String, GeneratorType> generatorTypes;

    // Настройки
    private int maxGeneratorsPerPlayer;
    private boolean onlyWorkWhenOwnerOnline;
    private boolean onlyWorkInLoadedChunks;
    private boolean dropOnBreak;
    private boolean onlyOwnerCanBreak;
    private String language;

    // НОВЫЕ НАСТРОЙКИ
    private boolean requireNearbyPlayer;
    private int nearbyPlayerDistance;

    public ConfigManager(IronFactory plugin) {
        this.plugin = plugin;
        this.generatorTypes = new HashMap<>();
    }

    public void loadConfigs() {
        // Сохранение дефолтных конфигов
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Загрузка настроек
        loadSettings();

        // Загрузка генераторов
        loadGenerators();
    }

    private void loadSettings() {
        maxGeneratorsPerPlayer = config.getInt("settings.max-generators-per-player", 50);
        onlyWorkWhenOwnerOnline = config.getBoolean("settings.only-work-when-owner-online", false);
        onlyWorkInLoadedChunks = config.getBoolean("settings.only-work-in-loaded-chunks", true);
        dropOnBreak = config.getBoolean("settings.drop-on-break", true);
        onlyOwnerCanBreak = config.getBoolean("settings.only-owner-can-break", true);
        language = config.getString("settings.language", "ru");

        // НОВЫЕ НАСТРОЙКИ
        requireNearbyPlayer = config.getBoolean("settings.require-nearby-player", true);
        nearbyPlayerDistance = config.getInt("settings.nearby-player-distance", 5);
    }

    private void loadGenerators() {
        generatorTypes.clear();

        File generatorsFolder = new File(plugin.getDataFolder(), "generators");
        if (!generatorsFolder.exists()) {
            generatorsFolder.mkdirs();
            createDefaultGenerators(generatorsFolder);
        }

        File[] files = generatorsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            loadGeneratorFile(file);
        }

        plugin.getLogger().info("Загружено " + generatorTypes.size() + " типов генераторов");
    }

    private void loadGeneratorFile(File file) {
        YamlConfiguration genConfig = YamlConfiguration.loadConfiguration(file);

        for (String key : genConfig.getKeys(false)) {
            ConfigurationSection section = genConfig.getConfigurationSection(key);
            if (section == null) continue;

            try {
                GeneratorType type = parseGeneratorType(key, section);
                generatorTypes.put(key, type);
                plugin.getLogger().info("Загружен генератор: " + key);
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки генератора " + key + ": " + e.getMessage());
            }
        }
    }

    private GeneratorType parseGeneratorType(String id, ConfigurationSection section) {
        GeneratorType type = new GeneratorType(id);

        // Предмет генератора
        ConfigurationSection itemSection = section.getConfigurationSection("generator-item");
        if (itemSection != null) {
            type.setItemMaterial(Material.valueOf(itemSection.getString("material", "FURNACE")));
            type.setName(itemSection.getString("name", "&fГенератор"));
            type.setLore(itemSection.getStringList("lore"));
        }

        // Блок в мире
        String blockMaterial = section.getString("block", section.getString("generator-item.material", "FURNACE"));
        type.setBlockMaterial(Material.valueOf(blockMaterial));

        // Задержка
        type.setDelay(section.getInt("delay", 3600));

        // Дропы
        List<Map<?, ?>> generates = section.getMapList("generates");
        for (Map<?, ?> dropMap : generates) {
            Material mat = Material.valueOf((String) dropMap.get("item"));
            int amount = dropMap.containsKey("amount") ? (Integer) dropMap.get("amount") : 1;
            double chance = dropMap.containsKey("chance") ? ((Number) dropMap.get("chance")).doubleValue() : 100.0;

            GeneratorDrop drop = new GeneratorDrop(mat, amount, chance);
            if (dropMap.containsKey("name")) {
                drop.setCustomName((String) dropMap.get("name"));
            }
            type.addDrop(drop);
        }

        // Голограмма
        ConfigurationSection holoSection = section.getConfigurationSection("hologram");
        if (holoSection != null) {
            type.setHologramEnabled(holoSection.getBoolean("enabled", true));
            type.setHologramHeight(holoSection.getDouble("height", 1.5));
            type.setHologramLines(holoSection.getStringList("lines"));
        }

        // Апгрейд
        ConfigurationSection upgradeSection = section.getConfigurationSection("upgrade");
        if (upgradeSection != null) {
            type.setUpgradeEnabled(upgradeSection.getBoolean("enabled", false));
            type.setNextGenerator(upgradeSection.getString("next-generator"));

            ConfigurationSection costSection = upgradeSection.getConfigurationSection("cost");
            if (costSection != null) {
                type.setUpgradeCostMaterial(Material.valueOf(costSection.getString("item", "IRON_INGOT")));
                type.setUpgradeCostAmount(costSection.getInt("amount", 10));
            }

            type.setOnUpgradeCommands(upgradeSection.getStringList("commands-on-upgrade"));
        }

        // Команды событий
        ConfigurationSection eventsSection = section.getConfigurationSection("events");
        if (eventsSection != null) {
            type.setOnPlaceCommands(eventsSection.getStringList("on-place"));
            type.setOnBreakCommands(eventsSection.getStringList("on-break"));
            type.setOnGenerateCommands(eventsSection.getStringList("on-generate"));
        }

        // НОВОЕ: Настройки дистанции
        ConfigurationSection distanceSection = section.getConfigurationSection("distance");
        if (distanceSection != null) {
            type.setRequireNearbyPlayer(distanceSection.getBoolean("require-nearby-player", false));
            type.setNearbyRadius(distanceSection.getInt("radius", 5));
        }

        return type;
    }

    private void createDefaultGenerators(File folder) {
        File ironFile = new File(folder, "iron_factory.yml");
        YamlConfiguration ironConfig = new YamlConfiguration();

        // LVL 1
        ironConfig.set("iron_factory_lvl1.generator-item.material", "FURNACE");
        ironConfig.set("iron_factory_lvl1.generator-item.name", "&f⚙ &6Железный завод &7(LVL 1)");
        ironConfig.set("iron_factory_lvl1.generator-item.lore", List.of(
                "",
                "  &7▸ &fВремя: &73:00 мин",
                "  &7▸ &fПроизводит: &f1 слиток",
                "  &7▸ &fРадиус работы: &c5 блоков",
                "",
                " &7&oБазовое оборудование.",
                "",
                "&8&m----------------------",
                "&6&l⚡ АПГРЕЙД:",
                " &fПКМ &7с &f10 железных слитков"
        ));
        ironConfig.set("iron_factory_lvl1.block", "FURNACE");
        ironConfig.set("iron_factory_lvl1.delay", 3600);
        ironConfig.set("iron_factory_lvl1.distance.require-nearby-player", true);
        ironConfig.set("iron_factory_lvl1.distance.radius", 5);
        ironConfig.set("iron_factory_lvl1.generates", List.of(
                Map.of("item", "IRON_INGOT", "amount", 1, "chance", 100.0)
        ));
        ironConfig.set("iron_factory_lvl1.hologram.enabled", true);
        ironConfig.set("iron_factory_lvl1.hologram.height", 1.5);
        ironConfig.set("iron_factory_lvl1.hologram.lines", List.of(
                "&f⚙ &8[&7&l1&8]",
                "&7Завод",
                "&a%time%"
        ));
        ironConfig.set("iron_factory_lvl1.upgrade.enabled", true);
        ironConfig.set("iron_factory_lvl1.upgrade.next-generator", "iron_factory_lvl2");
        ironConfig.set("iron_factory_lvl1.upgrade.cost.item", "IRON_INGOT");
        ironConfig.set("iron_factory_lvl1.upgrade.cost.amount", 10);

        // LVL 2
        ironConfig.set("iron_factory_lvl2.generator-item.material", "BLAST_FURNACE");
        ironConfig.set("iron_factory_lvl2.generator-item.name", "&f⚙ &6Железный завод &e(LVL 2)");
        ironConfig.set("iron_factory_lvl2.generator-item.lore", List.of(
                "",
                "  &7▸ &fВремя: &72:45 мин",
                "  &7▸ &fПроизводит: &f2 слитка",
                "  &7▸ &fРадиус работы: &e10 блоков",
                "",
                " &7&oУлучшенное оборудование.",
                "",
                "&8&m----------------------",
                "&6&l⚡ АПГРЕЙД:",
                " &fПКМ &7с &f20 железных слитков"
        ));
        ironConfig.set("iron_factory_lvl2.block", "BLAST_FURNACE");
        ironConfig.set("iron_factory_lvl2.delay", 3300);
        ironConfig.set("iron_factory_lvl2.distance.require-nearby-player", true);
        ironConfig.set("iron_factory_lvl2.distance.radius", 10);
        ironConfig.set("iron_factory_lvl2.generates", List.of(
                Map.of("item", "IRON_INGOT", "amount", 2, "chance", 100.0)
        ));
        ironConfig.set("iron_factory_lvl2.hologram.enabled", true);
        ironConfig.set("iron_factory_lvl2.hologram.height", 1.5);
        ironConfig.set("iron_factory_lvl2.hologram.lines", List.of(
                "&f⚙ &8[&e&l2&8]",
                "&eЗавод",
                "&a%time%"
        ));
        ironConfig.set("iron_factory_lvl2.upgrade.enabled", true);
        ironConfig.set("iron_factory_lvl2.upgrade.next-generator", "iron_factory_lvl3");
        ironConfig.set("iron_factory_lvl2.upgrade.cost.item", "IRON_INGOT");
        ironConfig.set("iron_factory_lvl2.upgrade.cost.amount", 20);

        // LVL 3
        ironConfig.set("iron_factory_lvl3.generator-item.material", "SMITHING_TABLE");
        ironConfig.set("iron_factory_lvl3.generator-item.name", "&f⚙ &6Железный завод &c(LVL 3) &4[MAX]");
        ironConfig.set("iron_factory_lvl3.generator-item.lore", List.of(
                "",
                "  &7▸ &fВремя: &72:00 мин",
                "  &7▸ &fПроизводит: &f3 слитка",
                "  &7▸ &fРадиус работы: &aБЕЗЛИМИТ",
                "",
                " &7&oМаксимальное оборудование.",
                "",
                "&c&lМАКСИМАЛЬНЫЙ УРОВЕНЬ"
        ));
        ironConfig.set("iron_factory_lvl3.block", "SMITHING_TABLE");
        ironConfig.set("iron_factory_lvl3.delay", 2400);
        ironConfig.set("iron_factory_lvl3.distance.require-nearby-player", false);
        ironConfig.set("iron_factory_lvl3.distance.radius", 0);
        ironConfig.set("iron_factory_lvl3.generates", List.of(
                Map.of("item", "IRON_INGOT", "amount", 3, "chance", 100.0),
                Map.of("item", "GOLD_INGOT", "amount", 1, "chance", 10.0)
        ));
        ironConfig.set("iron_factory_lvl3.hologram.enabled", true);
        ironConfig.set("iron_factory_lvl3.hologram.height", 1.5);
        ironConfig.set("iron_factory_lvl3.hologram.lines", List.of(
                "&f⚙ &8[&c&l3&8]",
                "&c&lMAX",
                "&a%time%"
        ));
        ironConfig.set("iron_factory_lvl3.upgrade.enabled", false);

        try {
            ironConfig.save(ironFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить iron_factory.yml: " + e.getMessage());
        }
    }

    // Getters
    public Map<String, GeneratorType> getGeneratorTypes() { return generatorTypes; }
    public GeneratorType getGeneratorType(String id) { return generatorTypes.get(id); }

    public int getMaxGeneratorsPerPlayer() { return maxGeneratorsPerPlayer; }
    public boolean isOnlyWorkWhenOwnerOnline() { return onlyWorkWhenOwnerOnline; }
    public boolean isOnlyWorkInLoadedChunks() { return onlyWorkInLoadedChunks; }
    public boolean isDropOnBreak() { return dropOnBreak; }
    public boolean isOnlyOwnerCanBreak() { return onlyOwnerCanBreak; }
    public String getLanguage() { return language; }

    // НОВЫЕ ГЕТТЕРЫ
    public boolean isRequireNearbyPlayer() { return requireNearbyPlayer; }
    public int getNearbyPlayerDistance() { return nearbyPlayerDistance; }
}