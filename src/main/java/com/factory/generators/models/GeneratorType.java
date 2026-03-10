package com.factory.generators.models;

import com.factory.generators.IronFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GeneratorType {

    private final String id;
    private String name;
    private List<String> lore;
    private Material itemMaterial;
    private Material blockMaterial;
    private int delay;
    private List<GeneratorDrop> drops;
    private List<String> hologramLines;
    private double hologramHeight;
    private boolean hologramEnabled;

    private boolean upgradeEnabled;
    private String nextGenerator;
    private Material upgradeCostMaterial;
    private int upgradeCostAmount;

    private List<String> onPlaceCommands;
    private List<String> onBreakCommands;
    private List<String> onGenerateCommands;
    private List<String> onUpgradeCommands;

    private boolean requireNearbyPlayer;
    private int nearbyRadius;

    // Система поломок
    private boolean canBreak;
    private double breakChance;
    private String brokenName;
    private List<String> brokenLore;
    private Material repairMaterial;
    private int repairAmount;

    // Система обязательного ремонта (для рудников)
    private boolean repairRequired;
    private int maxHealth;
    private Material repairCostMaterial;
    private int repairCostPerPoint;

    public GeneratorType(String id) {
        this.id = id;
        this.drops = new ArrayList<>();
        this.hologramLines = new ArrayList<>();
        this.onPlaceCommands = new ArrayList<>();
        this.onBreakCommands = new ArrayList<>();
        this.onGenerateCommands = new ArrayList<>();
        this.onUpgradeCommands = new ArrayList<>();
        this.hologramHeight = 1.5;
        this.hologramEnabled = true;
        this.requireNearbyPlayer = false;
        this.nearbyRadius = 5;

        // Система поломок
        this.canBreak = false;
        this.breakChance = 0;
        this.brokenName = "&c[СЛОМАН] " + id;
        this.brokenLore = new ArrayList<>();
        this.repairMaterial = Material.IRON_INGOT;
        this.repairAmount = 5;

        // Система обязательного ремонта
        this.repairRequired = false;
        this.maxHealth = 100;
        this.repairCostMaterial = Material.IRON_INGOT;
        this.repairCostPerPoint = 1;
    }

    public ItemStack createItem() {
        return createItem(1);
    }

    public ItemStack createItem(int amount) {
        ItemStack item = new ItemStack(itemMaterial, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(colorize(name));

            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(colorize(line));
            }
            meta.setLore(coloredLore);

            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            NamespacedKey key = new NamespacedKey(IronFactory.getInstance(), "generator_id");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);

            item.setItemMeta(meta);
        }

        return item;
    }

    public static String getGeneratorId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(IronFactory.getInstance(), "generator_id");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        return null;
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public Material getItemMaterial() { return itemMaterial; }
    public void setItemMaterial(Material material) { this.itemMaterial = material; }
    public Material getBlockMaterial() { return blockMaterial; }
    public void setBlockMaterial(Material material) { this.blockMaterial = material; }
    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }
    public List<GeneratorDrop> getDrops() { return drops; }
    public void setDrops(List<GeneratorDrop> drops) { this.drops = drops; }
    public void addDrop(GeneratorDrop drop) { this.drops.add(drop); }
    public List<String> getHologramLines() { return hologramLines; }
    public void setHologramLines(List<String> lines) { this.hologramLines = lines; }
    public double getHologramHeight() { return hologramHeight; }
    public void setHologramHeight(double height) { this.hologramHeight = height; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public void setHologramEnabled(boolean enabled) { this.hologramEnabled = enabled; }
    public boolean isUpgradeEnabled() { return upgradeEnabled; }
    public void setUpgradeEnabled(boolean enabled) { this.upgradeEnabled = enabled; }
    public String getNextGenerator() { return nextGenerator; }
    public void setNextGenerator(String next) { this.nextGenerator = next; }
    public Material getUpgradeCostMaterial() { return upgradeCostMaterial; }
    public void setUpgradeCostMaterial(Material material) { this.upgradeCostMaterial = material; }
    public int getUpgradeCostAmount() { return upgradeCostAmount; }
    public void setUpgradeCostAmount(int amount) { this.upgradeCostAmount = amount; }
    public List<String> getOnPlaceCommands() { return onPlaceCommands; }
    public void setOnPlaceCommands(List<String> commands) { this.onPlaceCommands = commands; }
    public List<String> getOnBreakCommands() { return onBreakCommands; }
    public void setOnBreakCommands(List<String> commands) { this.onBreakCommands = commands; }
    public List<String> getOnGenerateCommands() { return onGenerateCommands; }
    public void setOnGenerateCommands(List<String> commands) { this.onGenerateCommands = commands; }
    public List<String> getOnUpgradeCommands() { return onUpgradeCommands; }
    public void setOnUpgradeCommands(List<String> commands) { this.onUpgradeCommands = commands; }
    public boolean isRequireNearbyPlayer() { return requireNearbyPlayer; }
    public void setRequireNearbyPlayer(boolean require) { this.requireNearbyPlayer = require; }
    public int getNearbyRadius() { return nearbyRadius; }
    public void setNearbyRadius(int radius) { this.nearbyRadius = radius; }

    // Система поломок
    public boolean canBreak() { return canBreak; }
    public void setCanBreak(boolean canBreak) { this.canBreak = canBreak; }
    public double getBreakChance() { return breakChance; }
    public void setBreakChance(double chance) { this.breakChance = chance; }
    public String getBrokenName() { return brokenName; }
    public void setBrokenName(String name) { this.brokenName = name; }
    public List<String> getBrokenLore() { return brokenLore; }
    public void setBrokenLore(List<String> lore) { this.brokenLore = lore; }
    public Material getRepairMaterial() { return repairMaterial; }
    public void setRepairMaterial(Material material) { this.repairMaterial = material; }
    public int getRepairAmount() { return repairAmount; }
    public void setRepairAmount(int amount) { this.repairAmount = amount; }

    // Система обязательного ремонта
    public boolean isRepairRequired() { return repairRequired; }
    public void setRepairRequired(boolean required) { this.repairRequired = required; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int health) { this.maxHealth = health; }
    public Material getRepairCostMaterial() { return repairCostMaterial; }
    public void setRepairCostMaterial(Material material) { this.repairCostMaterial = material; }
    public int getRepairCostPerPoint() { return repairCostPerPoint; }
    public void setRepairCostPerPoint(int cost) { this.repairCostPerPoint = cost; }
}