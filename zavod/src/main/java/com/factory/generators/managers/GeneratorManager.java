package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorDrop;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GeneratorManager {

    private final IronFactory plugin;
    private final Map<String, PlacedGenerator> placedGenerators;

    public GeneratorManager(IronFactory plugin) {
        this.plugin = plugin;
        this.placedGenerators = new HashMap<>();
    }

    public boolean placeGenerator(Player player, Location location, String typeId) {
        GeneratorType type = plugin.getConfigManager().getGeneratorType(typeId);
        if (type == null) {
            player.sendMessage(colorize("&c[Завод] Неизвестный тип генератора!"));
            return false;
        }

        // Проверка лимита
        int count = countPlayerGenerators(player.getUniqueId());
        int max = plugin.getConfigManager().getMaxGeneratorsPerPlayer();
        if (max > 0 && count >= max && !player.hasPermission("factory.bypass")) {
            player.sendMessage(colorize("&c[Завод] Вы достигли лимита генераторов! (" + count + "/" + max + ")"));
            return false;
        }

        // Создание генератора
        PlacedGenerator generator = new PlacedGenerator(typeId, player.getUniqueId(), location);
        String key = generator.getLocationKey();
        placedGenerators.put(key, generator);

        // Голограмма
        plugin.getHologramManager().createHologram(generator, type);

        // Команды и эффекты
        executeCommands(type.getOnPlaceCommands(), player, location);
        playEffect(location, Particle.FLAME, 20);
        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_PLACE, 1f, 1f);

        player.sendMessage(colorize("&a[Завод] Генератор установлен!"));
        return true;
    }

    public boolean breakGenerator(Player player, Location location, boolean drop) {
        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);

        if (generator == null) return false;

        // Проверка владельца
        if (plugin.getConfigManager().isOnlyOwnerCanBreak()) {
            if (!generator.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("factory.bypass")) {
                player.sendMessage(colorize("&c[Завод] Только владелец может сломать этот генератор!"));
                return false;
            }
        }

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());

        // Удаление голограммы
        plugin.getHologramManager().removeHologram(generator);

        // Удаление блока
        location.getBlock().setType(Material.AIR);

        // Дроп предмета
        if (drop && plugin.getConfigManager().isDropOnBreak() && type != null) {
            location.getWorld().dropItemNaturally(location, type.createItem());
        }

        // Команды и эффекты
        if (type != null) {
            executeCommands(type.getOnBreakCommands(), player, location);
        }
        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_DESTROY, 1f, 1f);

        // Удаление из списка
        placedGenerators.remove(key);

        player.sendMessage(colorize("&e[Завод] Генератор удалён."));
        return true;
    }

    public boolean upgradeGenerator(Player player, Location location) {
        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);

        if (generator == null) return false;

        GeneratorType currentType = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (currentType == null || !currentType.isUpgradeEnabled()) {
            player.sendMessage(colorize("&c[Завод] Это максимальный уровень!"));
            return false;
        }

        // Проверка материалов
        Material costMat = currentType.getUpgradeCostMaterial();
        int costAmount = currentType.getUpgradeCostAmount();

        if (!hasItems(player, costMat, costAmount)) {
            player.sendMessage(colorize("&c[Завод] Недостаточно материалов! Нужно: " + costAmount + " " + formatMaterial(costMat)));
            return false;
        }

        // Снятие материалов
        removeItems(player, costMat, costAmount);

        // Получение следующего типа
        String nextTypeId = currentType.getNextGenerator();
        GeneratorType nextType = plugin.getConfigManager().getGeneratorType(nextTypeId);

        if (nextType == null) {
            player.sendMessage(colorize("&c[Завод] Ошибка конфигурации апгрейда!"));
            return false;
        }

        // Удаление старой голограммы
        plugin.getHologramManager().removeHologram(generator);

        // Удаление старого генератора
        placedGenerators.remove(key);

        // Создание нового генератора
        PlacedGenerator newGenerator = new PlacedGenerator(nextTypeId, generator.getOwnerUUID(), location);
        newGenerator.setTotalGenerated(generator.getTotalGenerated());
        placedGenerators.put(newGenerator.getLocationKey(), newGenerator);

        // Обновление блока
        location.getBlock().setType(nextType.getBlockMaterial());

        // Новая голограмма
        plugin.getHologramManager().createHologram(newGenerator, nextType);

        // Эффекты
        executeCommands(currentType.getOnUpgradeCommands(), player, location);
        playEffect(location, Particle.VILLAGER_HAPPY, 50);
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Титл
        player.sendTitle(
                colorize("&a&lУСПЕХ!"),
                colorize("&eЗавод улучшен!"),
                10, 40, 10
        );

        return true;
    }

    public void tickAllGenerators() {
        for (PlacedGenerator generator : new ArrayList<>(placedGenerators.values())) {
            tickGenerator(generator);
        }
    }

    private void tickGenerator(PlacedGenerator generator) {
        Location location = generator.getLocation();
        if (location == null) return;

        // Проверка загруженного чанка
        if (plugin.getConfigManager().isOnlyWorkInLoadedChunks()) {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                return;
            }
        }

        // Проверка онлайна владельца
        if (plugin.getConfigManager().isOnlyWorkWhenOwnerOnline()) {
            if (Bukkit.getPlayer(generator.getOwnerUUID()) == null) {
                return;
            }
        }

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type == null) return;

        // ПРОВЕРКА: Нужен ли игрок рядом для ЭТОГО типа генератора
        if (type.isRequireNearbyPlayer()) {
            boolean hasNearbyPlayer = false;
            int radius = type.getNearbyRadius();

            for (Player player : location.getWorld().getPlayers()) {
                if (player.getLocation().distance(location) <= radius) {
                    hasNearbyPlayer = true;
                    break;
                }
            }

            if (!hasNearbyPlayer) {
                return; // Нет игроков рядом - генератор не работает
            }
        }

        generator.tick();

        // Обновление голограммы
        plugin.getHologramManager().updateHologram(generator, type);

        // Генерация
        if (generator.getCurrentTick() >= type.getDelay()) {
            generate(generator, type);
            generator.resetTick();
        }
    }

    private void generate(PlacedGenerator generator, GeneratorType type) {
        Location location = generator.getLocation();
        if (location == null) return;

        Location dropLoc = location.clone().add(0.5, 1.2, 0.5);

        for (GeneratorDrop drop : type.getDrops()) {
            if (drop.roll()) {
                location.getWorld().dropItem(dropLoc, drop.createItem());
                generator.incrementGenerated();
            }
        }

        // Эффекты
        playEffect(location, Particle.FLAME, 10);
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);

        // Команды
        Player owner = Bukkit.getPlayer(generator.getOwnerUUID());
        if (owner != null) {
            executeCommands(type.getOnGenerateCommands(), owner, location);
        }
    }

    private void executeCommands(List<String> commands, Player player, Location location) {
        if (commands == null) return;

        for (String cmd : commands) {
            String parsed = cmd
                    .replace("%player%", player.getName())
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()))
                    .replace("%world%", location.getWorld().getName());

            if (parsed.startsWith("title ") || parsed.startsWith("playsound ") ||
                    parsed.startsWith("particle ") || parsed.startsWith("execute ")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            } else {
                player.performCommand(parsed);
            }
        }
    }

    private void playEffect(Location location, Particle particle, int count) {
        location.getWorld().spawnParticle(particle, location.clone().add(0.5, 1, 0.5), count, 0.3, 0.3, 0.3, 0.01);
    }

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
        player.updateInventory();
    }

    private String formatMaterial(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }

    public int countPlayerGenerators(UUID uuid) {
        int count = 0;
        for (PlacedGenerator gen : placedGenerators.values()) {
            if (gen.getOwnerUUID().equals(uuid)) count++;
        }
        return count;
    }

    public PlacedGenerator getGeneratorAt(Location location) {
        return placedGenerators.get(PlacedGenerator.createLocationKey(location));
    }

    public Map<String, PlacedGenerator> getPlacedGenerators() {
        return placedGenerators;
    }

    public Map<String, GeneratorType> getGeneratorTypes() {
        return plugin.getConfigManager().getGeneratorTypes();
    }
}