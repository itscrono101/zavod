package com.factory.generators.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class PlacedGenerator {

    private final String typeId;
    private final UUID ownerUUID;
    private final String worldName;
    private final int x, y, z;
    private int currentTick;
    private long totalGenerated;
    private long placedTime;

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
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    public String getLocationKey() {
        return worldName + ";" + x + ";" + y + ";" + z;
    }

    public static String createLocationKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public void tick() {
        currentTick++;
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

    public String formatTime(int remainingTicks) {
        int totalSeconds = remainingTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // Getters & Setters
    public String getTypeId() { return typeId; }
    
    public UUID getOwnerUUID() { return ownerUUID; }
    
    public String getWorldName() { return worldName; }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    
    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int tick) { this.currentTick = tick; }
    
    public long getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(long total) { this.totalGenerated = total; }
    
    public long getPlacedTime() { return placedTime; }
    public void setPlacedTime(long time) { this.placedTime = time; }
}
