package com.factory.generators.models;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

/**
 * Система ремонта рудника
 * Рудник изначально сломан и требует ресурсов для восстановления
 */
public class MineRepair {

    private int currentHealth;      // Текущее здоровье (0-100)
    private int maxHealth;          // Максимальное здоровье
    private Material repairMaterial; // Материал для ремонта
    private int repairCostPerPoint; // Стоимость за 1 пункт здоровья
    private List<String> repairDescription; // Описание процесса ремонта

    public MineRepair() {
        this.currentHealth = 0;      // Изначально сломан
        this.maxHealth = 100;
        this.repairMaterial = Material.IRON_INGOT;
        this.repairCostPerPoint = 1;
        this.repairDescription = new ArrayList<>();
    }

    /**
     * Починить рудник на X пунктов
     * @param amount Количество ресурсов потрачено
     * @return Сколько здоровья восстановилось
     */
    public int repair(int amount) {
        int healthRestored = Math.min(amount / repairCostPerPoint, maxHealth - currentHealth);
        currentHealth += healthRestored;
        return healthRestored;
    }

    /**
     * Рудник полностью рабочий?
     */
    public boolean isRepaired() {
        return currentHealth >= maxHealth;
    }

    /**
     * Процент ремонта (0-100%)
     */
    public double getRepairPercent() {
        return (double) currentHealth / maxHealth * 100;
    }

    /**
     * Сколько ресурсов нужно для полного ремонта
     */
    public int getFullRepairCost() {
        return (maxHealth - currentHealth) * repairCostPerPoint;
    }

    // Getters и Setters
    public int getCurrentHealth() { return currentHealth; }
    public void setCurrentHealth(int health) { this.currentHealth = Math.min(health, maxHealth); }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int max) { this.maxHealth = max; }

    public Material getRepairMaterial() { return repairMaterial; }
    public void setRepairMaterial(Material material) { this.repairMaterial = material; }

    public int getRepairCostPerPoint() { return repairCostPerPoint; }
    public void setRepairCostPerPoint(int cost) { this.repairCostPerPoint = cost; }

    public List<String> getRepairDescription() { return repairDescription; }
    public void setRepairDescription(List<String> description) { this.repairDescription = description; }
}
