package com.factory.generators.managers;

import com.factory.generators.IronFactory;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.MultiBlockStructure.PartType;
import com.factory.generators.utils.Logger;
import com.factory.generators.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MultiBlockManager {

    private static final Random RANDOM = new Random();
    private static final int MAX_STRUCTURES = 50000;
    private final IronFactory plugin;
    private final Map<String, MultiBlockStructure> structures;
    private final Map<String, String> partToStructure;

    // БОЛЬШИЕ ЗАМЕТНЫЕ БЛОКИ
    public static final Material DRILL_MATERIAL = Material.NETHERITE_BLOCK;    // Долото - незеритовый блок
    public static final Material PIPE_MATERIAL = Material.COPPER_BLOCK;        // Труба - медный блок
    public static final Material PUMP_MATERIAL = Material.REDSTONE_BLOCK;      // Станок - редстоун блок

    private static final int GENERATION_DELAY = 1200;
    private int animationTick = 0;

    public MultiBlockManager(IronFactory plugin) {
        this.plugin = plugin;
        this.structures = new HashMap<>();
        this.partToStructure = new HashMap<>();
    }

    public void registerLoadedStructure(MultiBlockStructure structure) {
        String key = structure.getStructureKey();

        // Check global limit to prevent memory exhaustion
        if (structures.size() >= MAX_STRUCTURES) {
            Logger.warn("MultiBlock structure limit reached (" + MAX_STRUCTURES + "), rejecting registration");
            return;
        }

        structures.put(key, structure);

        partToStructure.put(key, key);

        if (structure.getPipeTypeId() != null) {
            Location pipeLoc = structure.getPipeLocation();
            if (pipeLoc != null) {
                partToStructure.put(MultiBlockStructure.createStructureKey(pipeLoc), key);
            }
        }

        if (structure.getPumpTypeId() != null) {
            Location pumpLoc = structure.getPumpLocation();
            if (pumpLoc != null) {
                partToStructure.put(MultiBlockStructure.createStructureKey(pumpLoc), key);
            }
        }

        plugin.getLogger().info("[MultiBlock] Зарегистрирована: " + key);
    }

    public void restoreAllStructures() {
        plugin.getLogger().info("[MultiBlock] Восстановление " + structures.size() + " вышек...");

        for (MultiBlockStructure structure : structures.values()) {
            restoreStructure(structure);
        }
    }

    private void restoreStructure(MultiBlockStructure structure) {
        boolean hasDrill = false;
        boolean hasPipe = false;
        boolean hasPump = false;

        if (structure.getDrillTypeId() != null) {
            Location loc = structure.getDrillLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(DRILL_MATERIAL, false);
                hasDrill = true;
            }
        }

        if (structure.getPipeTypeId() != null) {
            Location loc = structure.getPipeLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(PIPE_MATERIAL, false);
                hasPipe = true;
            }
        }

        if (structure.getPumpTypeId() != null) {
            Location loc = structure.getPumpLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(PUMP_MATERIAL, false);
                hasPump = true;
            }
        }

        // ПРИНУДИТЕЛЬНО ставим complete если все части есть
        if (hasDrill && hasPipe && hasPump) {
            structure.setComplete(true);
            plugin.getLogger().info("[MultiBlock] ✓ " + structure.getStructureKey() + " - РАБОТАЕТ");
            plugin.getHologramManager().createMultiBlockHologram(structure);
        } else {
            structure.setComplete(false);
            plugin.getLogger().info("[MultiBlock] ✗ " + structure.getStructureKey() + " - НЕ ЗАВЕРШЕНА");
        }
    }

    public boolean placePart(Player player, Location location, String partTypeId) {
        PartType partType = getPartTypeFromId(partTypeId);
        if (partType == null) return false;

        switch (partType) {
            case DRILL: return placeDrill(player, location, partTypeId);
            case PIPE: return placePipe(player, location, partTypeId);
            case PUMP: return placePump(player, location, partTypeId);
        }
        return false;
    }

    private boolean placeDrill(Player player, Location location, String typeId) {
        String key = MultiBlockStructure.createStructureKey(location);

        if (structures.containsKey(key)) {
            player.sendMessage(Utils.colorize("&c[Буровая] Здесь уже есть долото!"));
            return false;
        }

        // Check global limit to prevent memory exhaustion
        if (structures.size() >= MAX_STRUCTURES) {
            player.sendMessage(Utils.colorize("&c[Буровая] Сервер достигнул лимита буровых!"));
            Logger.warn("MultiBlock structure limit reached (" + MAX_STRUCTURES + "), rejecting drill placement by " + player.getName());
            return false;
        }

        MultiBlockStructure structure = new MultiBlockStructure(player.getUniqueId(), location);
        structure.setDrillTypeId(typeId);

        structures.put(key, structure);
        partToStructure.put(key, key);

        // Удаляем предыдущий блок перед установкой нового БЕЗ физики (чтобы не падал)
        location.getBlock().setType(Material.AIR, false);
        location.getBlock().setType(DRILL_MATERIAL, false);

        location.getWorld().playSound(location, Sound.BLOCK_ANVIL_PLACE, 1f, 0.8f);
        location.getWorld().spawnParticle(Particle.SMOKE_NORMAL, location.clone().add(0.5, 0.5, 0.5), 10);

        player.sendMessage(color("&e[Буровая] &fДолото установлено! (Незеритовый блок)"));
        player.sendMessage(color("&7Поставьте &fТрубу &7(медный блок) сверху"));

        plugin.getDataManager().saveMultiBlockStructures();
        return true;
    }

    private boolean placePipe(Player player, Location location, String typeId) {
        Location drillLoc = location.clone().subtract(0, 1, 0);
        String drillKey = MultiBlockStructure.createStructureKey(drillLoc);

        MultiBlockStructure structure = structures.get(drillKey);

        if (structure == null) {
            player.sendMessage(color("&c[Буровая] Труба должна быть НАД Долотом!"));
            return false;
        }

        if (structure.getPipeTypeId() != null) {
            player.sendMessage(color("&c[Буровая] Труба уже установлена!"));
            return false;
        }

        structure.setPipeTypeId(typeId);

        String pipeKey = MultiBlockStructure.createStructureKey(location);
        partToStructure.put(pipeKey, drillKey);

        // Удаляем предыдущий блок перед установкой нового БЕЗ физики
        location.getBlock().setType(Material.AIR, false);
        location.getBlock().setType(PIPE_MATERIAL, false);
        location.getWorld().playSound(location, Sound.BLOCK_COPPER_PLACE, 1f, 1f);

        player.sendMessage(color("&e[Буровая] &fТруба установлена! (Медный блок)"));
        player.sendMessage(color("&7Поставьте &fСтанок &7(редстоун блок) сверху"));

        plugin.getDataManager().saveMultiBlockStructures();
        return true;
    }

    private boolean placePump(Player player, Location location, String typeId) {
        Location drillLoc = location.clone().subtract(0, 2, 0);
        String drillKey = MultiBlockStructure.createStructureKey(drillLoc);

        MultiBlockStructure structure = structures.get(drillKey);

        if (structure == null || structure.getPipeTypeId() == null) {
            player.sendMessage(color("&c[Буровая] Станок должен быть НАД Трубой!"));
            return false;
        }

        if (structure.getPumpTypeId() != null) {
            player.sendMessage(color("&c[Буровая] Станок уже установлен!"));
            return false;
        }

        structure.setPumpTypeId(typeId);

        String pumpKey = MultiBlockStructure.createStructureKey(location);
        partToStructure.put(pumpKey, drillKey);

        // Удаляем предыдущий блок перед установкой нового БЕЗ физики
        location.getBlock().setType(Material.AIR, false);
        location.getBlock().setType(PUMP_MATERIAL, false);

        // ЗАВЕРШЕНО!
        structure.setComplete(true);

        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        plugin.getHologramManager().createMultiBlockHologram(structure);

        player.sendMessage(color("&a[Буровая] &fСтанок установлен!"));
        player.sendMessage(color("&a&l✔ БУРОВАЯ ВЫШКА СОБРАНА И РАБОТАЕТ!"));

        player.sendTitle(color("&a&l✔ СОБРАНО!"), color("&7Буровая работает"), 10, 40, 10);

        plugin.getDataManager().saveMultiBlockStructures();
        return true;
    }

    public boolean breakPart(Player player, Location location, boolean drop) {
        String partKey = MultiBlockStructure.createStructureKey(location);
        String structureKey = partToStructure.get(partKey);

        if (structureKey == null) return false;

        MultiBlockStructure structure = structures.get(structureKey);
        if (structure == null) return false;

        if (plugin.getConfigManager().isOnlyOwnerCanBreak()) {
            if (!structure.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("factory.bypass")) {
                Logger.security("Access denied for " + player.getName() + ": attempted to break multiblock structure");
                player.sendMessage(Utils.colorize("&c[Буровая] Только владелец может сломать!"));
                return false;
            }
        }

        plugin.getHologramManager().removeMultiBlockHologram(structure);
        dismantleStructure(structure, drop);

        player.sendMessage(color("&e[Буровая] Вышка разобрана."));
        plugin.getDataManager().saveMultiBlockStructures();

        return true;
    }

    private void dismantleStructure(MultiBlockStructure structure, boolean drop) {
        String structureKey = structure.getStructureKey();

        if (structure.getDrillTypeId() != null) {
            Location loc = structure.getDrillLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(Material.AIR);
                if (drop) loc.getWorld().dropItemNaturally(loc, createPartItem("drill"));
                partToStructure.remove(MultiBlockStructure.createStructureKey(loc));
            }
        }

        if (structure.getPipeTypeId() != null) {
            Location loc = structure.getPipeLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(Material.AIR);
                if (drop) loc.getWorld().dropItemNaturally(loc, createPartItem("pipe"));
                partToStructure.remove(MultiBlockStructure.createStructureKey(loc));
            }
        }

        if (structure.getPumpTypeId() != null) {
            Location loc = structure.getPumpLocation();
            if (loc != null && loc.getWorld() != null) {
                loc.getBlock().setType(Material.AIR);
                if (drop) loc.getWorld().dropItemNaturally(loc, createPartItem("pump"));
                partToStructure.remove(MultiBlockStructure.createStructureKey(loc));
            }
        }

        structures.remove(structureKey);
    }

    public ItemStack createPartItem(String partName) {
        Material mat;
        String name;
        List<String> lore;

        switch (partName.toLowerCase()) {
            case "drill":
                mat = DRILL_MATERIAL;
                name = "&8⛏ &7Долото";
                lore = Arrays.asList("", "&7Часть буровой (1/3)", "&7Ставится первым");
                break;
            case "pipe":
                mat = PIPE_MATERIAL;
                name = "&6| &7Труба";
                lore = Arrays.asList("", "&7Часть буровой (2/3)", "&7Ставится над долотом");
                break;
            case "pump":
                mat = PUMP_MATERIAL;
                name = "&c⚙ &7Станок";
                lore = Arrays.asList("", "&7Часть буровой (3/3)", "&7Ставится над трубой");
                break;
            default:
                return null;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));

            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(color(line));
            meta.setLore(coloredLore);

            NamespacedKey key = new NamespacedKey(plugin, "drill_part");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, partName.toLowerCase());

            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            item.setItemMeta(meta);
        }
        return item;
    }

    public String getPartTypeFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey key = new NamespacedKey(plugin, "drill_part");
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }
        return null;
    }

    private PartType getPartTypeFromId(String id) {
        if (id == null) return null;
        id = id.toLowerCase();
        if (id.equals("drill") || id.startsWith("drill_")) return PartType.DRILL;
        if (id.equals("pipe") || id.startsWith("pipe_")) return PartType.PIPE;
        if (id.equals("pump") || id.startsWith("pump_")) return PartType.PUMP;
        return null;
    }

    public void tickAll() {
        animationTick++;

        for (MultiBlockStructure structure : new ArrayList<>(structures.values())) {
            if (!structure.isComplete()) continue;

            Location drillLoc = structure.getDrillLocation();
            if (drillLoc == null || drillLoc.getWorld() == null) continue;

            if (!drillLoc.getWorld().isChunkLoaded(drillLoc.getBlockX() >> 4, drillLoc.getBlockZ() >> 4)) {
                continue;
            }

            structure.tick(20);

            if (animationTick % 3 == 0) {
                playAnimation(structure);
            }

            plugin.getHologramManager().updateMultiBlockHologram(structure, GENERATION_DELAY);

            if (structure.getCurrentTick() >= GENERATION_DELAY) {
                generateOil(structure);
                structure.resetTick();
            }
        }
    }

    private void playAnimation(MultiBlockStructure structure) {
        Location drill = structure.getDrillLocation();
        Location pump = structure.getPumpLocation();

        if (drill == null || pump == null || drill.getWorld() == null) return;

        drill.getWorld().spawnParticle(Particle.BLOCK_CRACK, drill.clone().add(0.5, 0.3, 0.5),
                5, 0.2, 0.1, 0.2, 0.01, Material.STONE.createBlockData());

        pump.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, pump.clone().add(0.5, 1.2, 0.5),
                1, 0.1, 0.1, 0.1, 0.01);

        if (RANDOM.nextDouble() < 0.3) {
            Location pipe = structure.getPipeLocation();
            if (pipe != null) {
                pipe.getWorld().spawnParticle(Particle.SQUID_INK, pipe.clone().add(0.5, 0.5, 0.5),
                        2, 0.1, 0.2, 0.1, 0.01);
            }
        }

        if (RANDOM.nextDouble() < 0.15) {
            drill.getWorld().playSound(drill, Sound.BLOCK_PISTON_EXTEND, 0.3f, 0.5f);
        }
    }

    private void generateOil(MultiBlockStructure structure) {
        Location pump = structure.getPumpLocation();
        if (pump == null || pump.getWorld() == null) return;

        ItemStack oil = new ItemStack(Material.INK_SAC, 1);
        ItemMeta meta = oil.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color("&8&l⚫ &0Нефть"));
            meta.setLore(Arrays.asList("", "&7Сырая нефть"));
            oil.setItemMeta(meta);
        }

        Location dropLoc = pump.clone().add(0.5, 1.5, 0.5);
        pump.getWorld().dropItem(dropLoc, oil);

        structure.incrementGenerated();

        pump.getWorld().spawnParticle(Particle.SQUID_INK, dropLoc, 20, 0.2, 0.4, 0.2, 0.08);
        pump.getWorld().playSound(pump, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6f, 0.5f);
    }

    public MultiBlockStructure getStructureAt(Location location) {
        String partKey = MultiBlockStructure.createStructureKey(location);
        String structureKey = partToStructure.get(partKey);
        if (structureKey == null) return null;
        return structures.get(structureKey);
    }

    public boolean isPartOfStructure(Location location) {
        return partToStructure.containsKey(MultiBlockStructure.createStructureKey(location));
    }

    public Map<String, MultiBlockStructure> getStructures() {
        return structures;
    }

    private String color(String text) {
        return text.replace("&", "§");
    }
}