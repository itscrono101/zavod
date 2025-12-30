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
        if (!type.isHologramEnabled()) {
            plugin.getLogger().info("Голограмма отключена для типа: " + type.getId());
            return;
        }

        Location location = generator.getLocation();
        if (location == null) {
            plugin.getLogger().warning("Локация генератора null!");
            return;
        }

        if (location.getWorld() == null) {
            plugin.getLogger().warning("Мир генератора null!");
            return;
        }

        String key = generator.getLocationKey();

        plugin.getLogger().info("=== СОЗДАНИЕ ГОЛОГРАММЫ ===");
        plugin.getLogger().info("Ключ: " + key);
        plugin.getLogger().info("Локация: " + location);
        plugin.getLogger().info("Тип: " + type.getId());

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

            plugin.getLogger().info("DecentHologram создан: " + holoName);

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка создания DecentHologram: " + e.getMessage());
            e.printStackTrace();
            createArmorStandHologram(key, location, type, generator);
        }
    }

    private void createArmorStandHologram(String key, Location location, GeneratorType type, PlacedGenerator generator) {
        List<ArmorStand> stands = new ArrayList<>();

        // Начинаем сверху вниз
        double startHeight = type.getHologramHeight();
        Location holoLoc = location.clone().add(0.5, startHeight, 0.5);

        plugin.getLogger().info("Создание ArmorStand голограммы:");
        plugin.getLogger().info("  Начальная высота: " + startHeight);
        plugin.getLogger().info("  Количество строк: " + type.getHologramLines().size());

        int lineNum = 0;
        for (String line : type.getHologramLines()) {
            String parsed = parseLine(line, generator, type);

            plugin.getLogger().info("  Строка " + lineNum + ": " + parsed + " на высоте " + holoLoc.getY());

            try {
                ArmorStand stand = (ArmorStand) holoLoc.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
                stand.setCustomName(colorize(parsed));
                stand.setCustomNameVisible(true);
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.setSmall(true);
                stand.setInvulnerable(true);
                stand.setCollidable(false);
                stand.setAI(false);
                stand.setCanPickupItems(false);

                stands.add(stand);
                plugin.getLogger().info("  ArmorStand создан ID: " + stand.getEntityId());

                // Опускаемся на 0.25 блока ниже для следующей строки
                holoLoc.subtract(0, 0.25, 0);

            } catch (Exception e) {
                plugin.getLogger().severe("Ошибка создания ArmorStand: " + e.getMessage());
                e.printStackTrace();
            }

            lineNum++;
        }

        if (!stands.isEmpty()) {
            holograms.put(key, stands);
            plugin.getLogger().info("Голограмма сохранена с " + stands.size() + " ArmorStands");
        } else {
            plugin.getLogger().warning("Не удалось создать ни одного ArmorStand!");
        }
    }

    public void updateHologram(PlacedGenerator generator, GeneratorType type) {
        if (!type.isHologramEnabled()) return;

        String key = generator.getLocationKey();
        List<ArmorStand> stands = holograms.get(key);

        if (stands == null || stands.isEmpty()) {
            // Голограммы нет - создаём
            createHologram(generator, type);
            return;
        }

        // Обновляем существующие ArmorStands
        List<String> lines = type.getHologramLines();
        for (int i = 0; i < stands.size() && i < lines.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && !stand.isDead()) {
                String parsed = parseLine(lines.get(i), generator, type);
                stand.setCustomName(colorize(parsed));
            }
        }
    }

    public void removeHologram(PlacedGenerator generator) {
        String key = generator.getLocationKey();

        plugin.getLogger().info("Удаление голограммы: " + key);

        // Удаление ArmorStand голограмм
        List<ArmorStand> stands = holograms.remove(key);
        if (stands != null) {
            plugin.getLogger().info("Удаляем " + stands.size() + " ArmorStands");
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
            plugin.getLogger().info("DecentHologram удалён: " + holoName);
        }
    }

    public void removeAllHolograms() {
        plugin.getLogger().info("Удаление всех голограмм (" + holograms.size() + ")");

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
        plugin.getLogger().info("Обновление всех голограмм");
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

    // Метод для отладки
    public void debugHolograms() {
        plugin.getLogger().info("=== DEBUG ГОЛОГРАММ ===");
        plugin.getLogger().info("Всего голограмм: " + holograms.size());
        for (Map.Entry<String, List<ArmorStand>> entry : holograms.entrySet()) {
            plugin.getLogger().info("  " + entry.getKey() + " -> " + entry.getValue().size() + " stands");
        }
    }
}