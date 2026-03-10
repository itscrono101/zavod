package com.factory.generators.models;

import java.util.*;

public class GeneratorUpgrade {

    private final String generatorTypeId;
    private final Map<Integer, UpgradeLevel> upgradeLevels;
    private int maxLevel;
    private boolean enabled;

    public GeneratorUpgrade(String generatorTypeId) {
        this.generatorTypeId = generatorTypeId;
        this.upgradeLevels = new HashMap<>();
        this.maxLevel = 0;
        this.enabled = false;
    }

    public void addLevel(UpgradeLevel level) {
        upgradeLevels.put(level.getLevel(), level);
        if (level.getLevel() > maxLevel) {
            maxLevel = level.getLevel();
        }
    }

    public UpgradeLevel getLevel(int level) {
        return upgradeLevels.get(level);
    }

    public boolean canUpgrade(int currentLevel) {
        return currentLevel < maxLevel && upgradeLevels.containsKey(currentLevel + 1);
    }

    public UpgradeLevel getNextLevel(int currentLevel) {
        if (canUpgrade(currentLevel)) {
            return upgradeLevels.get(currentLevel + 1);
        }
        return null;
    }

    public String getGeneratorTypeId() { return generatorTypeId; }
    public int getMaxLevel() { return maxLevel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<Integer, UpgradeLevel> getUpgradeLevels() { return upgradeLevels; }
}
