package com.factory.generators.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class GeneratorType {

    private final String id;
    private String name;
    private List<String> lore;
    private Material itemMaterial;
    private Material blockMaterial;
    private int delay; // в тиках
    private List<GeneratorDrop> drops;
    private List<String> hologramLines;
    private double hologramHeight;
    private boolean hologramEnabled;
    
    // Апгрейд
    private boolean upgradeEnabled;
    private String nextGenerator;
    private Material upgradeCostMaterial;
    private int upgradeCostAmount;
    
    // Команды
    private List<String> onPlaceCommands;
    private List<String> onBreakCommands;
    private List<String> onGenerateCommands;
    private List<String> onUpgradeCommands;

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
            
            // Свечение
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            // NBT тег для идентификации
            meta.setLocalizedName("factory_generator:" + id);
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public static String getGeneratorId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        String locName = meta.getLocalizedName();
        if (locName != null && locName.startsWith("factory_generator:")) {
            return locName.replace("factory_generator:", "");
        }
        return null;
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }

    // Getters & Setters
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
}
