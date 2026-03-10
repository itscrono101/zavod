package com.factory.generators.listeners;

import com.factory.generators.IronFactory;
import com.factory.generators.gui.GeneratorGUI;
import com.factory.generators.gui.MineGUI;
import com.factory.generators.gui.RepairGUI;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Bukkit;           // ← ДОБАВЛЕНО: нужен для Bukkit.getScheduler()
import org.bukkit.Location;         // ← ДОБАВЛЕНО: нужен для block.getLocation().clone()
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;  // ← ДОБАВЛЕНО: нужен для applyFacingToBlock
import org.bukkit.block.data.Directional; // ← ДОБАВЛЕНО: нужен для applyFacingToBlock
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class GeneratorListener implements Listener {

    private final IronFactory plugin;

    public GeneratorListener(IronFactory plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block block = event.getBlockPlaced();


// Часть буровой?
        String partType = plugin.getMultiBlockManager().getPartTypeFromItem(item);
        if (partType != null) {
            // Отменяем стандартную установку блока Minecraft — мы сами управляем блоком
            event.setCancelled(true);

            // Сохраняем переменные для использования внутри лямбды (они должны быть final/effectively final)
            final String finalPartType = partType;
            final Location placedLocation = block.getLocation().clone(); // клонируем чтобы не изменился

            // ВАЖНО: запускаем через scheduler с задержкой 1 тик!
            // Проблема была в том что event.setCancelled(true) откатывает блок ПОСЛЕ нашего кода.
            // Если мы ставим setType(DRILL_MATERIAL) прямо здесь — Minecraft потом ставит AIR поверх.
            // Задержка 1 тик гарантирует что откат Minecraft уже произошёл, и только потом мы ставим блок.
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getMultiBlockManager().placePart(player, placedLocation, finalPartType);

                if (success) {
                    // Забираем предмет из инвентаря только при успехе
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItemInMainHand(null);
                    }
                }
                // При неуспехе блок уже AIR (Minecraft откатил), ничего делать не надо
            });
            return;
        }

        // Генератор?
        String generatorId = GeneratorType.getGeneratorId(item);
        if (generatorId == null) return;

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generatorId);
        if (type == null) {
            event.setCancelled(true);
            return;
        }

        block.setType(type.getBlockMaterial());

        // Если блок генератора имеет направление (например Печь, Дымовая печь,
        // Доменная печь и т.д.) — поворачиваем его "лицом" к игроку который ставит.
        // Без этого печка всегда смотрит на север независимо от игрока.
        applyFacingToBlock(block, player);

        boolean success = plugin.getGeneratorManager().placeGenerator(player, block.getLocation(), generatorId);

        if (!success) {
            block.setType(Material.AIR);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Буровая?
        if (plugin.getMultiBlockManager().isPartOfStructure(block.getLocation())) {
            event.setCancelled(true);

            if (!player.isSneaking()) {
                player.sendMessage(color("&e[Буровая] Присядьте (Shift) чтобы разобрать."));
                return;
            }

            plugin.getMultiBlockManager().breakPart(player, block.getLocation(), true);
            return;
        }

        // Генератор?
        PlacedGenerator generator = plugin.getGeneratorManager().getGeneratorAt(block.getLocation());
        if (generator == null) return;

        event.setCancelled(true);

        if (!player.isSneaking()) {
            player.sendMessage(color("&e[Завод] Присядьте (Shift) чтобы сломать."));
            return;
        }

        plugin.getGeneratorManager().breakGenerator(player, block.getLocation(), true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() == Material.AIR) return;

        // Буровая?
        MultiBlockStructure structure = plugin.getMultiBlockManager().getStructureAt(block.getLocation());
        if (structure != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            player.sendMessage(color("&8&m---------&r &8⛽ Буровая &8&m---------"));

            if (structure.isComplete()) {
                player.sendMessage(color("&7Статус: &a✔ РАБОТАЕТ"));
                player.sendMessage(color("&7Добыто: &f" + structure.getTotalGenerated()));
                player.sendMessage(color("&7След: &f" + structure.formatTime(structure.getRemainingTicks(1200))));
            } else {
                player.sendMessage(color("&7Статус: &c✗ НЕ ЗАВЕРШЕНА"));
                player.sendMessage(color(""));
                player.sendMessage(color("&7Долото: " + (structure.getDrillTypeId() != null ? "&a✔" : "&c✗ нужно")));
                player.sendMessage(color("&7Труба: " + (structure.getPipeTypeId() != null ? "&a✔" : "&c✗ нужно")));
                player.sendMessage(color("&7Станок: " + (structure.getPumpTypeId() != null ? "&a✔" : "&c✗ нужно")));
            }

            player.sendMessage(color("&8&m---------------------------"));
            return;
        }

        // Генератор?
        PlacedGenerator generator = plugin.getGeneratorManager().getGeneratorAt(block.getLocation());
        if (generator == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type == null) return;

        // Апгрейд?
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (type.isUpgradeEnabled() && hand != null &&
                hand.getType() == type.getUpgradeCostMaterial() &&
                hand.getAmount() >= type.getUpgradeCostAmount()) {
            plugin.getGeneratorManager().upgradeGenerator(player, block.getLocation());
            return;
        }

        // Если это рудник, требующий ремонта - открыть MineGUI
        if (type.isRepairRequired()) {
            new MineGUI(plugin, player, generator, type).open();
            return;
        }

        // Если сломан - открыть меню ремонта
        if (generator.isBroken()) {
            new RepairGUI(plugin, player, generator, type).open();
            return;
        }

        new GeneratorGUI(plugin, player, generator, type).open();
    }
    /**
     * Поворачивает блок "лицом" к игроку если блок поддерживает направление.
     * Нужно для блоков типа Печь (FURNACE), Доменная печь (BLAST_FURNACE),
     * Дымовая печь (SMOKER) и других directional блоков.
     *
     * Minecraft хранит направление блока в его BlockData через интерфейс Directional.
     * getFacing() у игрока возвращает куда он СМОТРИТ, а нам нужна ОБРАТНАЯ сторона —
     * чтобы "лицо" печки смотрело НА игрока, а не вместе с ним в одну сторону.
     */
    private void applyFacingToBlock(Block block, Player player) {
        // Проверяем что BlockData этого блока поддерживает направление
        // Не все блоки это поддерживают — камень, руда и т.д. не имеют facing
        if (block.getBlockData() instanceof org.bukkit.block.data.Directional) {
            org.bukkit.block.data.Directional directional =
                    (org.bukkit.block.data.Directional) block.getBlockData();

            // getCardinalDirection() возвращает куда смотрит игрок: NORTH, SOUTH, EAST, WEST
            // Нам нужно OPPOSITE (противоположное) — чтобы печка смотрела НА игрока
            org.bukkit.block.BlockFace playerFacing =
                    player.getFacing().getOppositeFace();

            // Проверяем что это направление допустимо для данного блока
            // (некоторые блоки поддерживают только горизонтальные стороны)
            if (directional.getFaces().contains(playerFacing)) {
                directional.setFacing(playerFacing);
                block.setBlockData(directional); // применяем обратно к блоку
            }
        }
    }
    private String color(String text) {
        return text.replace("&", "§");
    }
}