package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorDrop;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import com.factory.generators.models.GeneratorUpgrade;
import com.factory.generators.models.UpgradeLevel;
import com.factory.generators.utils.Constants;
import com.factory.generators.utils.Logger;
import com.factory.generators.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all placed generators in the world.
 * Handles creation, destruction, upgrades, and ticking of generators.
 */
public class GeneratorManager {

    private static final Random RANDOM = new Random();
    private static final int MAX_GENERATORS = 100000;
    private final IronFactory plugin;
    private final Map<String, PlacedGenerator> placedGenerators;
    // Index for fast player generator lookup
    private final Map<UUID, List<PlacedGenerator>> playerGenerators;

    public GeneratorManager(IronFactory plugin) {
        this.plugin = plugin;
        // Use ConcurrentHashMap for thread-safe operations
        this.placedGenerators = new ConcurrentHashMap<>();
        this.playerGenerators = new ConcurrentHashMap<>();
    }

    /**
     * Places a new generator at the specified location.
     *
     * @param player Player who placed the generator
     * @param location Location to place generator
     * @param typeId Generator type ID
     * @return true if placement was successful
     */
    public boolean placeGenerator(@NotNull Player player, @NotNull Location location, @NotNull String typeId) {
        if (!Utils.isValidLocation(location)) {
            Logger.warn("Invalid location for generator placement");
            return false;
        }

        GeneratorType type = plugin.getConfigManager().getGeneratorType(typeId);
        if (type == null) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Генератор не найден: " + typeId);
            return false;
        }

        int count = countPlayerGenerators(player.getUniqueId());
        int max = plugin.getConfigManager().getMaxGeneratorsPerPlayer();
        if (max > 0 && count >= max && !player.hasPermission(Constants.Permissions.BYPASS)) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Лимит генераторов!");
            return false;
        }

        // Check global limit to prevent memory exhaustion
        if (placedGenerators.size() >= MAX_GENERATORS && !player.hasPermission(Constants.Permissions.ADMIN)) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Сервер достигнул лимита генераторов!");
            Logger.warn("Generator limit reached (" + MAX_GENERATORS + "), rejecting placement by " + player.getName());
            return false;
        }

        UUID initialOwner = type.isRepairRequired()
                ? PlacedGenerator.NO_OWNER   // Рудник — владелец назначится при активации
                : player.getUniqueId();       // Завод — владелец сразу тот кто поставил
        PlacedGenerator generator = new PlacedGenerator(typeId, initialOwner, location);
        placedGenerators.put(generator.getLocationKey(), generator);

        // Update player index
        playerGenerators.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(generator);

        plugin.getHologramManager().createHologram(generator, type);

        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_PLACE,
            Constants.Sound.DEFAULT_VOLUME, Constants.Sound.DEFAULT_PITCH);
        Utils.sendMessage(player, Constants.Messages.SUCCESS_PREFIX + "Генератор установлен!");
        return true;
    }

    /**
     * Breaks a generator at the specified location.
     *
     * @param player Player who broke the generator
     * @param location Generator location
     * @param drop Whether to drop the generator item
     * @return true if break was successful
     */
    public boolean breakGenerator(@NotNull Player player, @NotNull Location location, boolean drop) {
        if (!Utils.isValidLocation(location)) return false;

        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);
        if (generator == null) return false;

        if (plugin.getConfigManager().isOnlyOwnerCanBreak()) {
            boolean isOwner = generator.getOwnerUUID().equals(player.getUniqueId());
            boolean isAdmin = player.hasPermission(Constants.Permissions.BYPASS); // это factory.bypass = op
            boolean noOwner = !generator.hasOwner(); // рудник ещё не активирован никем

            if (!isOwner && !isAdmin) {
                if (noOwner) {
                    // Нет владельца — только ОП может забрать
                    Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX +
                            "&cРудник ещё не активирован!");
                } else {
                    Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Только владелец может сломать!");
                }
                return false;
            }
        }

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        plugin.getHologramManager().removeHologram(generator);
        location.getBlock().setType(Material.AIR);

        if (drop && plugin.getConfigManager().isDropOnBreak() && type != null) {
            location.getWorld().dropItemNaturally(location, type.createItem());
        }

        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_DESTROY,
            Constants.Sound.DEFAULT_VOLUME, Constants.Sound.BREAK_PITCH);
        placedGenerators.remove(key);

        // Update player index with proper synchronization
        UUID ownerUUID = generator.getOwnerUUID();
        synchronized (playerGenerators) {
            List<PlacedGenerator> playerGens = playerGenerators.get(ownerUUID);
            if (playerGens != null) {
                playerGens.remove(generator);
            }
        }

        Utils.sendMessage(player, Constants.Messages.INFO_PREFIX + "Генератор удалён.");
        return true;
    }

    /**
     * Upgrades a generator to the next level.
     *
     * @param player Player performing the upgrade
     * @param location Generator location
     * @return true if upgrade was successful
     */
    public boolean upgradeGenerator(@NotNull Player player, @NotNull Location location) {
        if (!Utils.isValidLocation(location)) return false;

        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);
        if (generator == null) return false;

        GeneratorType currentType = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (currentType == null || !currentType.isUpgradeEnabled()) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Максимальный уровень!");
            return false;
        }

        Material costMat = currentType.getUpgradeCostMaterial();
        int costAmount = currentType.getUpgradeCostAmount();

        if (!hasItems(player, costMat, costAmount)) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Недостаточно материалов!");
            return false;
        }

        removeItems(player, costMat, costAmount);

        GeneratorType nextType = plugin.getConfigManager().getGeneratorType(currentType.getNextGenerator());
        if (nextType == null) return false;

        plugin.getHologramManager().removeHologram(generator);
        placedGenerators.remove(key);

        PlacedGenerator newGen = new PlacedGenerator(nextType.getId(), generator.getOwnerUUID(), location);
        newGen.setTotalGenerated(generator.getTotalGenerated());
        placedGenerators.put(newGen.getLocationKey(), newGen);

        // Update player index with proper synchronization
        UUID ownerUUID = generator.getOwnerUUID();
        synchronized (playerGenerators) {
            List<PlacedGenerator> playerGens = playerGenerators.get(ownerUUID);
            if (playerGens != null) {
                playerGens.remove(generator);
                playerGens.add(newGen);
            } else {
                playerGenerators.put(ownerUUID, new ArrayList<PlacedGenerator>() {{ add(newGen); }});
            }
        }

        location.getBlock().setType(nextType.getBlockMaterial());
        plugin.getHologramManager().createHologram(newGen, nextType);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,
            Constants.Sound.DEFAULT_VOLUME, Constants.Sound.DEFAULT_PITCH);
        player.sendTitle(Utils.colorize("&a&lУСПЕХ!"), Utils.colorize("&eУлучшено!"),
            Constants.Title.FADE_IN, Constants.Title.STAY, Constants.Title.FADE_OUT);
        return true;
    }

    /**
     * Repairs mine generator.
     *
     * @param player Player performing repair
     * @param location Generator location
     * @param amount Amount of repair material
     * @return true if repair was successful
     */
    public boolean repairMine(@NotNull Player player, @NotNull Location location, int amount) {
        if (!Utils.isValidLocation(location)) return false;

        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);
        if (generator == null) return false;

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type == null || !type.isRepairRequired()) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Этот генератор не требует ремонта!");
            return false;
        }

        if (generator.isMineRepaired()) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Рудник уже отремонтирован!");
            return false;
        }

        int healthRestored = Math.min(amount / type.getRepairCostPerPoint(),
                                      type.getMaxHealth() - generator.getMineHealth());
        generator.addMineHealth(healthRestored);

        Location dropLoc = location.clone().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropLoc, 15, 0.3, 0.3, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE,
            Constants.Sound.DEFAULT_VOLUME, Constants.Sound.DEFAULT_PITCH);

        double percent = (generator.getMineHealth() / (double) type.getMaxHealth()) * 100;
        player.sendTitle(Utils.colorize("&e⚒ Ремонт"),
                        Utils.colorize("&a" + String.format("%.0f", percent) + "%"),
                        Constants.Title.FADE_IN, Constants.Title.STAY, Constants.Title.FADE_OUT);

        if (generator.isMineRepaired()) {
        location.getWorld().spawnParticle(Particle.WHITE_SMOKE, dropLoc, 30, 0.5, 0.5, 0.5, 0.1);
            location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            Utils.sendMessage(player, Constants.Messages.SUCCESS_PREFIX + "§aРудник полностью отремонтирован!");
        } else {
            Utils.sendMessage(player, Constants.Messages.INFO_PREFIX + "§eРемонт: " + String.format("%.0f", percent) + "%");
        }

        plugin.getHologramManager().updateHologram(generator, type);
        return true;
    }

    /**
     * Heals a generator's ailment (disease, overload, rust, etc).
     *
     * @param player Player performing the heal
     * @param location Generator location
     * @param amount Amount of healing material used
     * @return true if healing was successful
     */
    public boolean healAilment(@NotNull Player player, @NotNull Location location, int amount) {
        if (!Utils.isValidLocation(location)) return false;

        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);
        if (generator == null) return false;

        if (!generator.hasAilment()) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Генератор не болен!");
            return false;
        }

        boolean cured = plugin.getEventManager().cureAilment(generator, amount);

        Location dropLoc = location.clone().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.HEART, dropLoc, 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.5f);

        if (cured) {
            location.getWorld().spawnParticle(Particle.WHITE_SMOKE, dropLoc, 30, 0.5, 0.5, 0.5, 0.1);
            location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            Utils.sendMessage(player, Constants.Messages.SUCCESS_PREFIX + "§aГенератор исцелён!");
            Logger.info("Generator at " + location + " healed by " + player.getName());
        } else {
            Utils.sendMessage(player, Constants.Messages.INFO_PREFIX + "§eИсцеление в процессе...");
        }

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type != null) {
            plugin.getHologramManager().updateHologram(generator, type);
        }

        return true;
    }

    /**
     * Upgrades generator level (internal upgrade system).
     *
     * @param player Player performing the upgrade
     * @param location Generator location
     * @return true if upgrade was successful
     */
    public boolean upgradeGeneratorLevel(@NotNull Player player, @NotNull Location location) {
        if (!Utils.isValidLocation(location)) return false;

        String key = PlacedGenerator.createLocationKey(location);
        PlacedGenerator generator = placedGenerators.get(key);
        if (generator == null) return false;

        GeneratorUpgrade upgrade = plugin.getConfigManager().getGeneratorUpgrade(generator.getTypeId());
        if (upgrade == null || !upgrade.isEnabled()) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Апгрейды для этого генератора недоступны!");
            return false;
        }

        if (!upgrade.canUpgrade(generator.getUpgradeLevel())) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Максимальный уровень апгрейда!");
            return false;
        }

        UpgradeLevel nextLevel = upgrade.getNextLevel(generator.getUpgradeLevel());
        if (nextLevel == null) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Ошибка при загрузке уровня апгрейда!");
            return false;
        }

        Material costMat = nextLevel.getCostMaterial();
        int costAmount = nextLevel.getCostAmount();

        if (!hasItems(player, costMat, costAmount)) {
            Utils.sendMessage(player, Constants.Messages.ERROR_PREFIX + "Недостаточно " + costMat.name().toLowerCase() + "!");
            return false;
        }

        removeItems(player, costMat, costAmount);
        generator.setUpgradeLevel(generator.getUpgradeLevel() + 1);

        Location dropLoc = location.clone().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, dropLoc, 20, 0.3, 0.3, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP,
            Constants.Sound.DEFAULT_VOLUME, Constants.Sound.DEFAULT_PITCH);
        player.sendTitle(Utils.colorize("&a&lУСПЕХ!"), Utils.colorize("&eУровень: &f" + generator.getUpgradeLevel()),
            Constants.Title.FADE_IN, Constants.Title.STAY, Constants.Title.FADE_OUT);

        plugin.getHologramManager().updateHologram(generator, plugin.getConfigManager().getGeneratorType(generator.getTypeId()));
        return true;
    }

    public void tickAllGenerators() {
        for (PlacedGenerator generator : new ArrayList<>(placedGenerators.values())) {
            tickGenerator(generator);
        }
    }

    private void tickGenerator(PlacedGenerator generator) {
        if (generator.isBroken()) return;

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type == null) return;

        // Если рудник требует ремонта и не отремонтирован - не работает
        if (type.isRepairRequired() && !generator.isMineRepaired()) {
            return;
        }

        Location location = generator.getLocation();
        if (location == null) return;

        // Проверяем случайные события (болезни, перегруз и т.д.)
        plugin.getEventManager().checkRandomEvents(generator, location);

        if (plugin.getConfigManager().isOnlyWorkInLoadedChunks()) {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;
        }

        if (plugin.getConfigManager().isOnlyWorkWhenOwnerOnline()) {
            if (Bukkit.getPlayer(generator.getOwnerUUID()) == null) return;
        }

        if (type.isRequireNearbyPlayer()) {
            boolean hasNearby = false;
            for (Player p : location.getWorld().getPlayers()) {
                if (p.getLocation().distance(location) <= type.getNearbyRadius()) {
                    hasNearby = true;
                    break;
                }
            }
            if (!hasNearby) return;
        }

        int delay = getAdjustedDelay(type, generator);
        generator.tick(20);
        plugin.getHologramManager().updateHologram(generator, type);

        if (generator.getCurrentTick() >= delay) {
            generate(generator, type);
            generator.resetTick();
        }
    }

    private int getAdjustedDelay(GeneratorType type, PlacedGenerator generator) {
        int baseDelay = type.getDelay();
        GeneratorUpgrade upgrade = plugin.getConfigManager().getGeneratorUpgrade(generator.getTypeId());

        int adjustedDelay = baseDelay;
        if (upgrade != null && upgrade.isEnabled() && generator.getUpgradeLevel() > 0) {
            UpgradeLevel level = upgrade.getLevel(generator.getUpgradeLevel());
            if (level != null) {
                adjustedDelay = (int) (baseDelay * level.getDelayMultiplier());
            }
        }

        // Apply ailment speed penalty (болезнь замедляет в 2 раза)
        double ailmentMultiplier = 1.0 / plugin.getEventManager().getAilmentSpeedMultiplier(generator);
        return (int) (adjustedDelay * ailmentMultiplier);
    }

    private void generate(PlacedGenerator generator, GeneratorType type) {
        Location location = generator.getLocation();
        if (location == null) return;

        Location dropLoc = location.clone().add(0.5, 1.2, 0.5);
        GeneratorUpgrade upgrade = plugin.getConfigManager().getGeneratorUpgrade(generator.getTypeId());
        UpgradeLevel upgradeLevel = (upgrade != null && upgrade.isEnabled())
            ? upgrade.getLevel(generator.getUpgradeLevel())
            : null;

        for (GeneratorDrop drop : type.getDrops()) {
            double chance = drop.getChance();
            int amount = drop.getAmount();

            if (upgradeLevel != null) {
                chance *= upgradeLevel.getDropChanceMultiplier();
                amount = (int) (amount * upgradeLevel.getDropAmountMultiplier());
            }

            if (RANDOM.nextDouble() * 100 < chance) {
                ItemStack item = drop.createItem();
                item.setAmount(Math.max(1, amount));
                location.getWorld().dropItem(dropLoc, item);
                generator.incrementGenerated();
            }
        }

        location.getWorld().spawnParticle(Particle.FLAME, dropLoc, 10, 0.3, 0.3, 0.3, 0.01);
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);

        // Система поломок
        if (type.canBreak() && RANDOM.nextDouble() * 100 < type.getBreakChance()) {
            generator.setBroken(true);
            location.getWorld().spawnParticle(Particle.SMOKE_LARGE, dropLoc, 20, 0.3, 0.3, 0.3, 0.05);
            location.getWorld().playSound(location, Sound.ENTITY_ITEM_BREAK, 1f, 0.5f);
            plugin.getHologramManager().updateHologram(generator, type);

            // Уведомить владельца если онлайн
            Player owner = Bukkit.getPlayer(generator.getOwnerUUID());
            if (owner != null) {
                owner.sendMessage(color("&c[Завод] Ваш генератор сломался! Координаты: " +
                        location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
            }
        }
    }

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
        player.updateInventory();
    }

    public int countPlayerGenerators(UUID uuid) {
        int count = 0;
        for (PlacedGenerator g : placedGenerators.values()) {
            if (g.getOwnerUUID().equals(uuid)) count++;
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

    private String color(String text) {
        return text.replace("&", "§");
    }
}