package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.*;



public class HologramManager {

    private final IronFactory plugin;
    private final Map<String, List<ArmorStand>> holograms;
    private final Map<String, List<ArmorStand>> multiBlockHolograms;

    public HologramManager(IronFactory plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
        this.multiBlockHolograms = new HashMap<>();
    }

    public void createHologram(PlacedGenerator generator, GeneratorType type) {
        if (!type.isHologramEnabled()) return;
        Location location = generator.getLocation();
        if (location == null || location.getWorld() == null) return;

        String key = generator.getLocationKey();
        removeHologram(generator);

        List<ArmorStand> stands = new ArrayList<>();
        Location holoLoc = location.clone().add(0.5, type.getHologramHeight(), 0.5);

        for (String line : type.getHologramLines()) {
            String parsed = parseLine(line, generator, type);
            try {
                ArmorStand stand = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                stand.setCustomName(color(parsed));
                stand.setCustomNameVisible(true);
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stands.add(stand);
                holoLoc.subtract(0, 0.25, 0);
            } catch (Exception e) { }
        }
        if (!stands.isEmpty()) holograms.put(key, stands);
    }

    public void updateHologram(PlacedGenerator generator, GeneratorType type) {
        if (!type.isHologramEnabled()) return;
        String key = generator.getLocationKey();
        List<ArmorStand> stands = holograms.get(key);

        if (stands == null || stands.isEmpty()) {
            createHologram(generator, type);
            return;
        }

        List<String> lines = type.getHologramLines();
        for (int i = 0; i < stands.size() && i < lines.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && !stand.isDead()) {
                stand.setCustomName(color(parseLine(lines.get(i), generator, type)));
            }
        }
    }

    public void removeHologram(PlacedGenerator generator) {
        List<ArmorStand> stands = holograms.remove(generator.getLocationKey());
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
    }

    public void createMultiBlockHologram(MultiBlockStructure structure) {
        Location pump = structure.getPumpLocation();
        if (pump == null || pump.getWorld() == null) return;

        String key = structure.getStructureKey();
        removeMultiBlockHologram(structure);

        List<ArmorStand> stands = new ArrayList<>();
        Location holoLoc = pump.clone().add(0.5, 2.0, 0.5);

        String[] lines = {"&8&l⛽ &7Буровая", "&aРаботает", "&7%time%"};

        for (String line : lines) {
            String parsed = parseMultiLine(line, structure, 1200);
            try {
                ArmorStand stand = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                stand.setCustomName(color(parsed));
                stand.setCustomNameVisible(true);
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stands.add(stand);
                holoLoc.subtract(0, 0.25, 0);
            } catch (Exception e) { }
        }
        if (!stands.isEmpty()) multiBlockHolograms.put(key, stands);
    }

    public void updateMultiBlockHologram(MultiBlockStructure structure, int delay) {
        String key = structure.getStructureKey();
        List<ArmorStand> stands = multiBlockHolograms.get(key);

        if (stands == null || stands.isEmpty()) {
            createMultiBlockHologram(structure);
            return;
        }

        String[] lines = {"&8&l⛽ &7Буровая", "&aРаботает", "&7%time%"};
        for (int i = 0; i < stands.size() && i < lines.length; i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && !stand.isDead()) {
                stand.setCustomName(color(parseMultiLine(lines[i], structure, delay)));
            }
        }
    }

    public void removeMultiBlockHologram(MultiBlockStructure structure) {
        List<ArmorStand> stands = multiBlockHolograms.remove(structure.getStructureKey());
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : holograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
        holograms.clear();

        for (List<ArmorStand> stands : multiBlockHolograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) stand.remove();
            }
        }
        multiBlockHolograms.clear();
    }

    public void refreshAllHolograms() {
        removeAllHolograms();
        for (PlacedGenerator g : plugin.getGeneratorManager().getPlacedGenerators().values()) {
            GeneratorType type = plugin.getConfigManager().getGeneratorType(g.getTypeId());
            if (type != null) createHologram(g, type);
        }
        for (MultiBlockStructure s : plugin.getMultiBlockManager().getStructures().values()) {
            if (s.isComplete()) createMultiBlockHologram(s);
        }
    }

    private String parseLine(String line, PlacedGenerator generator, GeneratorType type) {
        // Определяем статус рудника
        String statusText;
        if (generator.isBroken()) {
            statusText = "&c[СЛОМАН]";
        } else if (type.isRepairRequired() && generator.getMineHealth() < type.getMaxHealth()) {
            statusText = "&c[ТРЕБУЕТ АКТИВАЦИИ]";
        } else if (type.isRepairRequired() && generator.getMineHealth() >= type.getMaxHealth()) {
            statusText = "&a[АКТИВЕН]";
        } else {
            statusText = "";
        }

        // Если сломан — строку с таймером тоже заменяем
        if (generator.isBroken() && line.contains("%time%")) {
            return "&c⚠ СЛОМАН";
        }

        int remaining = generator.getRemainingTicks(type.getDelay());

        return line
                .replace("%time%", generator.formatTime(remaining))
                .replace("%total%", String.valueOf(generator.getTotalGenerated()))
                .replace("%type%", type.getName())
                .replace("%status%", statusText);
    }

    private String parseMultiLine(String line, MultiBlockStructure structure, int delay) {
        int remaining = structure.getRemainingTicks(delay);
        return line.replace("%time%", structure.formatTime(remaining))
                .replace("%total%", String.valueOf(structure.getTotalGenerated()));
    }

    public void debugHolograms() {
        plugin.getLogger().info("Голограмм: " + holograms.size() + " + " + multiBlockHolograms.size() + " (multi)");
    }

    private String color(String text) {
        return text.replace("&", "§");
    }
}