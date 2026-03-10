package com.factory.generators.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class UpgradeLevel {

    private final int level;
    private double delayMultiplier;
    private double dropChanceMultiplier;
    private double dropAmountMultiplier;
    private Material costMaterial;
    private int costAmount;
    private List<String> description;

    public UpgradeLevel(int level) {
        this.level = level;
        this.delayMultiplier = 1.0;
        this.dropChanceMultiplier = 1.0;
        this.dropAmountMultiplier = 1.0;
        this.description = new ArrayList<>();
    }

    public int getLevel() { return level; }

    public double getDelayMultiplier() { return delayMultiplier; }
    public void setDelayMultiplier(double multiplier) { this.delayMultiplier = multiplier; }

    public double getDropChanceMultiplier() { return dropChanceMultiplier; }
    public void setDropChanceMultiplier(double multiplier) { this.dropChanceMultiplier = multiplier; }

    public double getDropAmountMultiplier() { return dropAmountMultiplier; }
    public void setDropAmountMultiplier(double multiplier) { this.dropAmountMultiplier = multiplier; }

    public Material getCostMaterial() { return costMaterial; }
    public void setCostMaterial(Material material) { this.costMaterial = material; }

    public int getCostAmount() { return costAmount; }
    public void setCostAmount(int amount) { this.costAmount = amount; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description; }
}
