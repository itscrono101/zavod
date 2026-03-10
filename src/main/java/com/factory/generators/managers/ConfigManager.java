package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorDrop;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.GeneratorUpgrade;
import com.factory.generators.models.UpgradeLevel;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConfigManager {

    private final IronFactory plugin;
    private final Map<String, GeneratorType> generatorTypes;
    private final Map<String, GeneratorUpgrade> generatorUpgrades;

    private int maxGeneratorsPerPlayer;
    private boolean onlyWorkWhenOwnerOnline;
    private boolean onlyWorkInLoadedChunks;
    private boolean dropOnBreak;
    private boolean onlyOwnerCanBreak;

    public ConfigManager(IronFactory plugin) {
        this.plugin = plugin;
        this.generatorTypes = new HashMap<>();
        this.generatorUpgrades = new HashMap<>();
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        loadSettings();
        loadGenerators();
    }

    private void loadSettings() {
        FileConfiguration config = plugin.getConfig();
        maxGeneratorsPerPlayer = config.getInt("settings.max-generators-per-player", 50);
        onlyWorkWhenOwnerOnline = config.getBoolean("settings.only-work-when-owner-online", false);
        onlyWorkInLoadedChunks = config.getBoolean("settings.only-work-in-loaded-chunks", true);
        dropOnBreak = config.getBoolean("settings.drop-on-break", true);
        onlyOwnerCanBreak = config.getBoolean("settings.only-owner-can-break", true);
    }

    private void loadGenerators() {
        generatorTypes.clear();
        generatorUpgrades.clear();
        File folder = new File(plugin.getDataFolder(), "generators");

        if (!folder.exists()) {
            folder.mkdirs();
            createDefaultGenerators(folder);
        }

        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    try {
                        GeneratorType type = parseType(key, section);
                        generatorTypes.put(key, type);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Ошибка загрузки: " + key);
                    }
                }
            }
        }

        // Загружаем апгрейды после всех генераторов
        File upgradesFolder = new File(plugin.getDataFolder(), "upgrades");
        if (!upgradesFolder.exists()) {
            upgradesFolder.mkdirs();
            createDefaultUpgrades(upgradesFolder);
        }

        File[] upgradeFiles = upgradesFolder.listFiles((d, n) -> n.endsWith(".yml"));
        if (upgradeFiles != null) {
            for (File file : upgradeFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section != null) {
                        try {
                            GeneratorUpgrade upgrade = parseUpgrade(key, section);
                            generatorUpgrades.put(key, upgrade);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Ошибка загрузки апгрейда: " + key);
                        }
                    }
                }
            }
        }

        plugin.getLogger().info("Загружено " + generatorTypes.size() + " генераторов и " + generatorUpgrades.size() + " систем апгрейдов");
    }

    private GeneratorType parseType(String id, ConfigurationSection section) {
        GeneratorType type = new GeneratorType(id);

        ConfigurationSection item = section.getConfigurationSection("generator-item");
        if (item != null) {
            type.setItemMaterial(Material.valueOf(item.getString("material", "FURNACE")));
            type.setName(item.getString("name", "&fГенератор"));
            type.setLore(item.getStringList("lore"));
        }

        type.setBlockMaterial(Material.valueOf(section.getString("block", "FURNACE")));
        type.setDelay(section.getInt("delay", 3600));

        List<Map<?, ?>> generates = section.getMapList("generates");
        for (Map<?, ?> drop : generates) {
            Material mat = Material.valueOf((String) drop.get("item"));
            int amount = drop.containsKey("amount") ? (Integer) drop.get("amount") : 1;
            double chance = drop.containsKey("chance") ? ((Number) drop.get("chance")).doubleValue() : 100.0;
            GeneratorDrop gd = new GeneratorDrop(mat, amount, chance);
            if (drop.containsKey("name")) gd.setCustomName((String) drop.get("name"));
            type.addDrop(gd);
        }

        ConfigurationSection holo = section.getConfigurationSection("hologram");
        if (holo != null) {
            type.setHologramEnabled(holo.getBoolean("enabled", true));
            type.setHologramHeight(holo.getDouble("height", 1.5));
            type.setHologramLines(holo.getStringList("lines"));
        }

        ConfigurationSection upgrade = section.getConfigurationSection("upgrade");
        if (upgrade != null) {
            type.setUpgradeEnabled(upgrade.getBoolean("enabled", false));
            type.setNextGenerator(upgrade.getString("next-generator"));
            ConfigurationSection cost = upgrade.getConfigurationSection("cost");
            if (cost != null) {
                type.setUpgradeCostMaterial(Material.valueOf(cost.getString("item", "IRON_INGOT")));
                type.setUpgradeCostAmount(cost.getInt("amount", 10));
            }
        }

        ConfigurationSection dist = section.getConfigurationSection("distance");
        if (dist != null) {
            type.setRequireNearbyPlayer(dist.getBoolean("require-nearby-player", false));
            type.setNearbyRadius(dist.getInt("radius", 5));
        }

        // Система поломок
        ConfigurationSection breakSection = section.getConfigurationSection("breaking");
        if (breakSection != null) {
            type.setCanBreak(breakSection.getBoolean("enabled", false));
            type.setBreakChance(breakSection.getDouble("chance", 5.0));
            type.setBrokenName(breakSection.getString("broken-name", "%status% " + type.getName()));
            type.setBrokenLore(breakSection.getStringList("broken-lore"));

            ConfigurationSection repair = breakSection.getConfigurationSection("repair");
            if (repair != null) {
                type.setRepairMaterial(Material.valueOf(repair.getString("material", "IRON_INGOT")));
                type.setRepairAmount(repair.getInt("amount", 5));
            }
        }

        // Система обязательного ремонта (для рудников)
        ConfigurationSection repairSection = section.getConfigurationSection("repair");
        if (repairSection != null) {
            type.setRepairRequired(repairSection.getBoolean("enabled", false));
            type.setMaxHealth(repairSection.getInt("max-health", 100));
            type.setRepairCostMaterial(Material.valueOf(repairSection.getString("cost-material", "IRON_INGOT")));
            type.setRepairCostPerPoint(repairSection.getInt("cost-per-point", 1));
        }

        return type;
    }

    private void createDefaultGenerators(File folder) {
        File file = new File(folder, "iron_factory.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("iron_factory_lvl1.generator-item.material", "FURNACE");
        config.set("iron_factory_lvl1.generator-item.name", "&6Железный завод &7(LVL 1)");
        config.set("iron_factory_lvl1.generator-item.lore", Arrays.asList("", "&7Производит железо", "&7Время: 3 мин"));
        config.set("iron_factory_lvl1.block", "FURNACE");
        config.set("iron_factory_lvl1.delay", 3600);
        config.set("iron_factory_lvl1.generates", Arrays.asList(Map.of("item", "IRON_INGOT", "amount", 1, "chance", 100.0)));
        config.set("iron_factory_lvl1.hologram.enabled", true);
        config.set("iron_factory_lvl1.hologram.height", 1.5);
        config.set("iron_factory_lvl1.hologram.lines", Arrays.asList("&6Завод &7[1]", "&a%time%"));
        config.set("iron_factory_lvl1.upgrade.enabled", true);
        config.set("iron_factory_lvl1.upgrade.next-generator", "iron_factory_lvl2");
        config.set("iron_factory_lvl1.upgrade.cost.item", "IRON_INGOT");
        config.set("iron_factory_lvl1.upgrade.cost.amount", 10);

        config.set("iron_factory_lvl2.generator-item.material", "BLAST_FURNACE");
        config.set("iron_factory_lvl2.generator-item.name", "&6Железный завод &e(LVL 2)");
        config.set("iron_factory_lvl2.generator-item.lore", Arrays.asList("", "&7Производит железо x2"));
        config.set("iron_factory_lvl2.block", "BLAST_FURNACE");
        config.set("iron_factory_lvl2.delay", 3000);
        config.set("iron_factory_lvl2.generates", Arrays.asList(Map.of("item", "IRON_INGOT", "amount", 2, "chance", 100.0)));
        config.set("iron_factory_lvl2.hologram.enabled", true);
        config.set("iron_factory_lvl2.hologram.height", 1.5);
        config.set("iron_factory_lvl2.hologram.lines", Arrays.asList("&eЗавод &7[2]", "&a%time%"));
        config.set("iron_factory_lvl2.upgrade.enabled", false);

        // Дистанционный завод (требует близость игрока)
        config.set("remote_factory_lvl1.generator-item.material", "SMOKER");
        config.set("remote_factory_lvl1.generator-item.name", "&5Дистанционный завод &7(LVL 1)");
        config.set("remote_factory_lvl1.generator-item.lore", Arrays.asList("", "&7Производит медь", "&7Требует игрока в пределах 5 блоков", "&7Время: 2 мин"));
        config.set("remote_factory_lvl1.block", "SMOKER");
        config.set("remote_factory_lvl1.delay", 2400);
        config.set("remote_factory_lvl1.generates", Arrays.asList(Map.of("item", "COPPER_INGOT", "amount", 1, "chance", 100.0)));
        config.set("remote_factory_lvl1.hologram.enabled", true);
        config.set("remote_factory_lvl1.hologram.height", 1.5);
        config.set("remote_factory_lvl1.hologram.lines", Arrays.asList("&5Дистанц. завод &7[1]", "&a%time%", "&7⊙ Требует близость"));
        config.set("remote_factory_lvl1.distance.require-nearby-player", true);
        config.set("remote_factory_lvl1.distance.radius", 5);
        config.set("remote_factory_lvl1.upgrade.enabled", true);
        config.set("remote_factory_lvl1.upgrade.next-generator", "remote_factory_lvl2");
        config.set("remote_factory_lvl1.upgrade.cost.item", "COPPER_INGOT");
        config.set("remote_factory_lvl1.upgrade.cost.amount", 15);

        config.set("remote_factory_lvl2.generator-item.material", "SMOKER");
        config.set("remote_factory_lvl2.generator-item.name", "&5Дистанционный завод &d(LVL 2)");
        config.set("remote_factory_lvl2.generator-item.lore", Arrays.asList("", "&7Производит медь x2", "&7Требует игрока в пределах 7 блоков"));
        config.set("remote_factory_lvl2.block", "SMOKER");
        config.set("remote_factory_lvl2.delay", 2000);
        config.set("remote_factory_lvl2.generates", Arrays.asList(Map.of("item", "COPPER_INGOT", "amount", 2, "chance", 100.0)));
        config.set("remote_factory_lvl2.hologram.enabled", true);
        config.set("remote_factory_lvl2.hologram.height", 1.5);
        config.set("remote_factory_lvl2.hologram.lines", Arrays.asList("&dДистанц. завод &7[2]", "&a%time%", "&7⊙ 7 блоков"));
        config.set("remote_factory_lvl2.distance.require-nearby-player", true);
        config.set("remote_factory_lvl2.distance.radius", 7);
        config.set("remote_factory_lvl2.upgrade.enabled", false);

        // Рудник LVL 1 (изначально сломан, требует ремонта)
        config.set("mine_lvl1.generator-item.material", "DEEPSLATE_ORE");
        config.set("mine_lvl1.generator-item.name", "&8⛏ Рудник &7(LVL 1)");
        config.set("mine_lvl1.generator-item.lore", Arrays.asList("", "&7Производит железную руду", "&c⚠ ТРЕБУЕТ РЕМОНТА!", "&7Стоимость ремонта: 20 железных слитков", "&7Время: 4 мин"));
        config.set("mine_lvl1.block", "DEEPSLATE_ORE");
        config.set("mine_lvl1.delay", 4800);
        config.set("mine_lvl1.generates", Arrays.asList(Map.of("item", "IRON_ORE", "amount", 1, "chance", 100.0)));
        config.set("mine_lvl1.hologram.enabled", true);
        config.set("mine_lvl1.hologram.height", 1.5);
        config.set("mine_lvl1.hologram.lines", Arrays.asList("&8⛏ Рудник &7[1]", "&a%time%", "%status%"));

        // Система ремонта для рудника
        config.set("mine_lvl1.repair.enabled", true);
        config.set("mine_lvl1.repair.max-health", 100);
        config.set("mine_lvl1.repair.cost-material", "IRON_INGOT");
        config.set("mine_lvl1.repair.cost-per-point", 1);

        config.set("mine_lvl1.upgrade.enabled", true);
        config.set("mine_lvl1.upgrade.next-generator", "mine_lvl2");
        config.set("mine_lvl1.upgrade.cost.item", "IRON_ORE");
        config.set("mine_lvl1.upgrade.cost.amount", 30);

        // Рудник LVL 2
        config.set("mine_lvl2.generator-item.material", "DEEPSLATE_IRON_ORE");
        config.set("mine_lvl2.generator-item.name", "&8⛏ Рудник &e(LVL 2)");
        config.set("mine_lvl2.generator-item.lore", Arrays.asList("", "&7Производит железную руду x2 + медную", "&c⚠ ТРЕБУЕТ РЕМОНТА!", "&7Стоимость ремонта: 30 железных слитков"));
        config.set("mine_lvl2.block", "DEEPSLATE_IRON_ORE");
        config.set("mine_lvl2.delay", 4000);
        config.set("mine_lvl2.generates", Arrays.asList(
            Map.of("item", "IRON_ORE", "amount", 2, "chance", 100.0),
            Map.of("item", "COPPER_ORE", "amount", 1, "chance", 100.0)
        ));
        config.set("mine_lvl2.hologram.enabled", true);
        config.set("mine_lvl2.hologram.height", 1.5);
        config.set("mine_lvl2.hologram.lines", Arrays.asList("&eРудник &7[2]", "&a%time%", "%status%"));

        config.set("mine_lvl2.repair.enabled", true);
        config.set("mine_lvl2.repair.max-health", 100);
        config.set("mine_lvl2.repair.cost-material", "IRON_INGOT");
        config.set("mine_lvl2.repair.cost-per-point", 1);

        config.set("mine_lvl2.upgrade.enabled", true);
        config.set("mine_lvl2.upgrade.next-generator", "mine_lvl3");
        config.set("mine_lvl2.upgrade.cost.item", "IRON_ORE");
        config.set("mine_lvl2.upgrade.cost.amount", 50);

        // Рудник LVL 3 (премиум)
        config.set("mine_lvl3.generator-item.material", "DEEPSLATE_GOLD_ORE");
        config.set("mine_lvl3.generator-item.name", "&8⛏ Рудник &6(LVL 3)");
        config.set("mine_lvl3.generator-item.lore", Arrays.asList("", "&7Производит руды &7+ &6Золото!", "&c⚠ ТРЕБУЕТ РЕМОНТА!", "&7Стоимость ремонта: 40 железных слитков"));
        config.set("mine_lvl3.block", "DEEPSLATE_GOLD_ORE");
        config.set("mine_lvl3.delay", 3600);
        config.set("mine_lvl3.generates", Arrays.asList(
            Map.of("item", "IRON_ORE", "amount", 2, "chance", 100.0),
            Map.of("item", "COPPER_ORE", "amount", 2, "chance", 100.0),
            Map.of("item", "GOLD_ORE", "amount", 1, "chance", 80.0)
        ));
        config.set("mine_lvl3.hologram.enabled", true);
        config.set("mine_lvl3.hologram.height", 1.5);
        config.set("mine_lvl3.hologram.lines", Arrays.asList("&6Рудник &7[3]", "&a%time%", "%status%"));

        config.set("mine_lvl3.repair.enabled", true);
        config.set("mine_lvl3.repair.max-health", 100);
        config.set("mine_lvl3.repair.cost-material", "IRON_INGOT");
        config.set("mine_lvl3.repair.cost-per-point", 1);

        config.set("mine_lvl3.upgrade.enabled", false);

        try { config.save(file); } catch (IOException e) { }
    }

    private GeneratorUpgrade parseUpgrade(String id, ConfigurationSection section) {
        GeneratorUpgrade upgrade = new GeneratorUpgrade(id);
        upgrade.setEnabled(section.getBoolean("enabled", false));

        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                try {
                    int level = Integer.parseInt(levelKey);
                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                    if (levelSection != null) {
                        UpgradeLevel upgradeLevel = new UpgradeLevel(level);
                        upgradeLevel.setDelayMultiplier(levelSection.getDouble("delay-multiplier", 1.0));
                        upgradeLevel.setDropChanceMultiplier(levelSection.getDouble("drop-chance-multiplier", 1.0));
                        upgradeLevel.setDropAmountMultiplier(levelSection.getDouble("drop-amount-multiplier", 1.0));
                        upgradeLevel.setDescription(levelSection.getStringList("description"));

                        ConfigurationSection costSection = levelSection.getConfigurationSection("cost");
                        if (costSection != null) {
                            upgradeLevel.setCostMaterial(Material.valueOf(costSection.getString("item", "IRON_INGOT")));
                            upgradeLevel.setCostAmount(costSection.getInt("amount", 10));
                        }

                        upgrade.addLevel(upgradeLevel);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Неверный уровень апгрейда: " + levelKey);
                }
            }
        }

        return upgrade;
    }

    private void createDefaultUpgrades(File folder) {
        File file = new File(folder, "iron_factory.yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("iron_factory.enabled", true);
        config.set("iron_factory.levels.1.delay-multiplier", 0.95);
        config.set("iron_factory.levels.1.drop-chance-multiplier", 1.0);
        config.set("iron_factory.levels.1.drop-amount-multiplier", 1.0);
        config.set("iron_factory.levels.1.description", Arrays.asList("&7Уровень 1 апгрейда"));
        config.set("iron_factory.levels.1.cost.item", "IRON_INGOT");
        config.set("iron_factory.levels.1.cost.amount", 10);

        config.set("iron_factory.levels.2.delay-multiplier", 0.90);
        config.set("iron_factory.levels.2.drop-chance-multiplier", 1.05);
        config.set("iron_factory.levels.2.drop-amount-multiplier", 1.0);
        config.set("iron_factory.levels.2.description", Arrays.asList("&7Уровень 2 апгрейда"));
        config.set("iron_factory.levels.2.cost.item", "IRON_INGOT");
        config.set("iron_factory.levels.2.cost.amount", 20);

        config.set("iron_factory.levels.3.delay-multiplier", 0.85);
        config.set("iron_factory.levels.3.drop-chance-multiplier", 1.1);
        config.set("iron_factory.levels.3.drop-amount-multiplier", 1.1);
        config.set("iron_factory.levels.3.description", Arrays.asList("&7Уровень 3 апгрейда"));
        config.set("iron_factory.levels.3.cost.item", "IRON_INGOT");
        config.set("iron_factory.levels.3.cost.amount", 30);

        try { config.save(file); } catch (IOException e) { }

        // Апгрейды для дистанционного завода
        File remoteFile = new File(folder, "remote_factory.yml");
        YamlConfiguration remoteConfig = new YamlConfiguration();

        remoteConfig.set("remote_factory.enabled", true);
        remoteConfig.set("remote_factory.levels.1.delay-multiplier", 0.95);
        remoteConfig.set("remote_factory.levels.1.drop-chance-multiplier", 1.0);
        remoteConfig.set("remote_factory.levels.1.drop-amount-multiplier", 1.0);
        remoteConfig.set("remote_factory.levels.1.description", Arrays.asList("&7Эффективность I"));
        remoteConfig.set("remote_factory.levels.1.cost.item", "COPPER_INGOT");
        remoteConfig.set("remote_factory.levels.1.cost.amount", 15);

        remoteConfig.set("remote_factory.levels.2.delay-multiplier", 0.88);
        remoteConfig.set("remote_factory.levels.2.drop-chance-multiplier", 1.08);
        remoteConfig.set("remote_factory.levels.2.drop-amount-multiplier", 1.0);
        remoteConfig.set("remote_factory.levels.2.description", Arrays.asList("&7Эффективность II"));
        remoteConfig.set("remote_factory.levels.2.cost.item", "COPPER_INGOT");
        remoteConfig.set("remote_factory.levels.2.cost.amount", 25);

        remoteConfig.set("remote_factory.levels.3.delay-multiplier", 0.80);
        remoteConfig.set("remote_factory.levels.3.drop-chance-multiplier", 1.15);
        remoteConfig.set("remote_factory.levels.3.drop-amount-multiplier", 1.15);
        remoteConfig.set("remote_factory.levels.3.description", Arrays.asList("&7Эффективность III"));
        remoteConfig.set("remote_factory.levels.3.cost.item", "COPPER_INGOT");
        remoteConfig.set("remote_factory.levels.3.cost.amount", 35);

        try { remoteConfig.save(remoteFile); } catch (IOException e) { }
    }

    public Map<String, GeneratorType> getGeneratorTypes() { return generatorTypes; }
    public GeneratorType getGeneratorType(String id) { return generatorTypes.get(id); }
    public Map<String, GeneratorUpgrade> getGeneratorUpgrades() { return generatorUpgrades; }
    public GeneratorUpgrade getGeneratorUpgrade(String id) { return generatorUpgrades.get(id); }
    public int getMaxGeneratorsPerPlayer() { return maxGeneratorsPerPlayer; }
    public boolean isOnlyWorkWhenOwnerOnline() { return onlyWorkWhenOwnerOnline; }
    public boolean isOnlyWorkInLoadedChunks() { return onlyWorkInLoadedChunks; }
    public boolean isDropOnBreak() { return dropOnBreak; }
    public boolean isOnlyOwnerCanBreak() { return onlyOwnerCanBreak; }

    // Generic config getters for casino and other features
    public String getString(String path, String defaultValue) {
        return plugin.getConfig().getString(path, defaultValue);
    }

    public List<?> getList(String path, List<?> defaultValue) {
        return plugin.getConfig().getList(path, defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return plugin.getConfig().getDouble(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return plugin.getConfig().getBoolean(path, defaultValue);
    }
}