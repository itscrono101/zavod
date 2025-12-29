package com.factory.generators.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GeneratorDrop {

    private Material material;
    private int amount;
    private double chance;
    private String customName;

    public GeneratorDrop(Material material, int amount, double chance) {
        this.material = material;
        this.amount = amount;
        this.chance = chance;
    }

    public boolean roll() {
        return Math.random() * 100 <= chance;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(material, amount);
        
        if (customName != null && !customName.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(customName.replace("&", "§"));
                item.setItemMeta(meta);
            }
        }
        
        return item;
    }

    // Getters & Setters
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    
    public double getChance() { return chance; }
    public void setChance(double chance) { this.chance = chance; }
    
    public String getCustomName() { return customName; }
    public void setCustomName(String name) { this.customName = name; }
}
