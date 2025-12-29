package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.*;

public class HologramManager {

    private final IronFactory plugin;
    private final Map<String, List<ArmorStand>> holograms;
    private boolean useDecentHolograms;

    public HologramManager(IronFactory plugin) {
        this.plugin = plugin;
        this.holograms = new HashMap<>();
        
        // Проверка DecentHolograms
        useDecentHolograms = Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
        if (useDecentHolograms) {
            plugin.getLogger().info("DecentHolograms найден, используем его для голограмм");
        } else {
            plugin.getLogger().info("DecentHolograms не найден, используем ArmorStand голограммы");
        }
    }

    public void createHologram(PlacedGenerator generator, GeneratorType type) {
        if (!type.isHologramEnabled()) return;
        
        Location location = generator.getLocation();
        if (location == null) return;
        
        String key = generator.getLocationKey();
        
        // Удаление старой голограммы если есть
        removeHologram(generator);
        
        if (useDecentHolograms) {
            createDecentHologram(key, location, type, generator);
        } else {
            createArmorStandHologram(key, location, type, generator);
        }
    }

    private void createDecentHologram(String key, Location location, GeneratorType type, PlacedGenerator generator) {
        try {
            Location holoLoc = location.clone().add(0.5, type.getHologramHeight(), 0.5);
            
            // Формируем команду создания голограммы
            StringBuilder lines = new StringBuilder();
            for (String line : type.getHologramLines()) {
                String parsed = parseLine(line, generator, type);
                lines.append(parsed.replace("&", "§")).append("\n");
            }
            
            // Создаём через команду
            String holoName = "factory_" + key.replace(";", "_");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
                "dh create " + holoName + " " + lines.toString().trim());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "dh movehere " + holoName + " " + holoLoc.getWorld().getName() + " " + 
                holoLoc.getX() + " " + holoLoc.getY() + " " + holoLoc.getZ());
                
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка создания DecentHologram: " + e.getMessage());
            createArmorStandHologram(key, location, type, generator);
        }
    }

    private void createArmorStandHologram(String key, Location location, GeneratorType type, PlacedGenerator generator) {
        List<ArmorStand> stands = new ArrayList<>();
        Location holoLoc = location.clone().add(0.5, type.getHologramHeight() + 0.3 * type.getHologramLines().size(), 0.5);
        
        for (String line : type.getHologramLines()) {
            String parsed = parseLine(line, generator, type);
            
            ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
            stand.setCustomName(colorize(parsed));
            stand.setCustomNameVisible(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
            
            stands.add(stand);
            holoLoc.subtract(0, 0.3, 0);
        }
        
        holograms.put(key, stands);
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
            String parsed = parseLine(lines.get(i), generator, type);
            stands.get(i).setCustomName(colorize(parsed));
        }
    }

    public void removeHologram(PlacedGenerator generator) {
        String key = generator.getLocationKey();
        
        // Удаление ArmorStand голограмм
        List<ArmorStand> stands = holograms.remove(key);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
        
        // Удаление DecentHologram
        if (useDecentHolograms) {
            String holoName = "factory_" + key.replace(";", "_");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh delete " + holoName);
        }
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : holograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand != null && !stand.isDead()) {
                    stand.remove();
                }
            }
        }
        holograms.clear();
    }

    public void refreshAllHolograms() {
        removeAllHolograms();
        
        for (PlacedGenerator generator : plugin.getGeneratorManager().getPlacedGenerators().values()) {
            GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
            if (type != null) {
                createHologram(generator, type);
            }
        }
    }

    private String parseLine(String line, PlacedGenerator generator, GeneratorType type) {
        int remaining = generator.getRemainingTicks(type.getDelay());
        String time = generator.formatTime(remaining);
        
        return line
            .replace("%time%", time)
            .replace("%total%", String.valueOf(generator.getTotalGenerated()))
            .replace("%type%", type.getName());
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}
