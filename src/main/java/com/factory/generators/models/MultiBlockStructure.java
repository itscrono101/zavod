package com.factory.generators.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class MultiBlockStructure {

    public enum PartType {
        DRILL, PIPE, PUMP
    }

    private final UUID ownerUUID;
    private final String worldName;
    private final int baseX, baseY, baseZ;

    private boolean complete;
    private int currentTick;
    private long totalGenerated;
    private long placedTime;

    private String drillTypeId;
    private String pipeTypeId;
    private String pumpTypeId;

    public MultiBlockStructure(UUID ownerUUID, Location drillLocation) {
        this.ownerUUID = ownerUUID;
        this.worldName = drillLocation.getWorld().getName();
        this.baseX = drillLocation.getBlockX();
        this.baseY = drillLocation.getBlockY();
        this.baseZ = drillLocation.getBlockZ();
        this.complete = false;
        this.currentTick = 0;
        this.totalGenerated = 0;
        this.placedTime = System.currentTimeMillis();
    }

    public MultiBlockStructure(UUID ownerUUID, String worldName, int x, int y, int z) {
        this.ownerUUID = ownerUUID;
        this.worldName = worldName;
        this.baseX = x;
        this.baseY = y;
        this.baseZ = z;
        this.complete = false;
        this.currentTick = 0;
        this.totalGenerated = 0;
        this.placedTime = System.currentTimeMillis();
    }

    public Location getDrillLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, baseX, baseY, baseZ);
    }

    public Location getPipeLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, baseX, baseY + 1, baseZ);
    }

    public Location getPumpLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, baseX, baseY + 2, baseZ);
    }

    public String getStructureKey() {
        return worldName + ";" + baseX + ";" + baseY + ";" + baseZ;
    }

    public static String createStructureKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public PartType getPartAt(Location loc) {
        if (loc.getBlockX() != baseX || loc.getBlockZ() != baseZ) return null;
        if (!loc.getWorld().getName().equals(worldName)) return null;

        int yDiff = loc.getBlockY() - baseY;
        if (yDiff == 0) return PartType.DRILL;
        if (yDiff == 1) return PartType.PIPE;
        if (yDiff == 2) return PartType.PUMP;
        return null;
    }

    public void tick(int ticks) {
        currentTick += ticks;
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

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getWorldName() { return worldName; }
    public int getBaseX() { return baseX; }
    public int getBaseY() { return baseY; }
    public int getBaseZ() { return baseZ; }

    public boolean isComplete() { return complete; }
    public void setComplete(boolean complete) { this.complete = complete; }

    public int getCurrentTick() { return currentTick; }
    public void setCurrentTick(int tick) { this.currentTick = tick; }

    public long getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(long total) { this.totalGenerated = total; }

    public long getPlacedTime() { return placedTime; }
    public void setPlacedTime(long time) { this.placedTime = time; }

    public String getDrillTypeId() { return drillTypeId; }
    public void setDrillTypeId(String id) { this.drillTypeId = id; }

    public String getPipeTypeId() { return pipeTypeId; }
    public void setPipeTypeId(String id) { this.pipeTypeId = id; }

    public String getPumpTypeId() { return pumpTypeId; }
    public void setPumpTypeId(String id) { this.pumpTypeId = id; }
}