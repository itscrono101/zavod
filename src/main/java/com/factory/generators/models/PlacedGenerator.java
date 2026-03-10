package com.factory.generators.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PlacedGenerator {

    private final String typeId;
    private UUID ownerUUID;
    private final String worldName;
    private final int x, y, z;
    private int currentTick;
    private long totalGenerated;
    private long placedTime;
    private boolean broken;
    private int upgradeLevel;
    private int mineHealth;  // Здоровье рудника (0-100)
    private GeneratorAilment ailment;  // Текущее заболевание

    public PlacedGenerator(String typeId, UUID ownerUUID, Location location) {
        this.typeId = typeId;
        this.ownerUUID = ownerUUID;
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
        this.currentTick = 0;
        this.totalGenerated = 0;
        this.placedTime = System.currentTimeMillis();
        this.broken = false;
        this.upgradeLevel = 0;
        this.mineHealth = 0;  // Рудник изначально сломан
    }

    public PlacedGenerator(String typeId, UUID ownerUUID, String worldName, int x, int y, int z) {
        this.typeId = typeId;
        this.ownerUUID = ownerUUID;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.currentTick = 0;
        this.totalGenerated = 0;
        this.placedTime = System.currentTimeMillis();
        this.broken = false;
        this.upgradeLevel = 0;
        this.mineHealth = 0;  // Рудник изначально сломан
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public static final UUID NO_OWNER = new UUID(0, 0);

    public String getLocationKey() {
        return worldName + ";" + x + ";" + y + ";" + z;
    }

    public static String createLocationKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public void tick(int amount) {
        if (!broken) {
            currentTick += amount;
        }
    }

    public void resetTick() {
        currentTick = 0;
    }

    public void incrementGenerated() {
        totalGenerated++;
    }

    public int getRemainingTicks(int maxDelay) {
        return Math.max(0, maxDelay - currentTick);
}

    public boolean hasOwner() {
        return ownerUUID != null && !ownerUUID.equals(NO_OWNER);
    }

    public String formatTime(int remainingTicks) {
        int totalSeconds = remainingTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getTypeId() { return typeId; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getWorldName() { return worldName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int tick) { this.currentTick = tick; }

    public long getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(long total) { this.totalGenerated = total; }

    public long getPlacedTime() { return placedTime; }
    public void setPlacedTime(long time) { this.placedTime = time; }

    public boolean isBroken() { return broken; }
    public void setBroken(boolean broken) { this.broken = broken; }

    public int getUpgradeLevel() { return upgradeLevel; }
    public void setUpgradeLevel(int level) { this.upgradeLevel = level; }

    public int getMineHealth() { return mineHealth; }
    public void setMineHealth(int health) { this.mineHealth = Math.min(health, 100); }
    public void addMineHealth(int health) { this.mineHealth = Math.min(mineHealth + health, 100); }
    public boolean isMineRepaired() { return mineHealth >= 100; }

    public GeneratorAilment getAilment() { return ailment; }
    public void setAilment(GeneratorAilment ailment) { this.ailment = ailment; }
    public boolean hasAilment() { return ailment != null && ailment.isActive(); }
    public void cureAilment() { this.ailment = null; }
}