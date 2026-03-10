package com.factory.generators.models;

import org.bukkit.Material;

public class RepairCost {

    private final Material material;
    private final int amount;

    public RepairCost(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
}