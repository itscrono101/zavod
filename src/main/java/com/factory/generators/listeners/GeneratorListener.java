package com.factory.generators.listeners;

import com.factory.generators.IronFactory;
import com.factory.generators.gui.GeneratorGUI;
import com.factory.generators.gui.MineGUI;
import com.factory.generators.gui.RepairGUI;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
            event.setCancelled(true);

            boolean success = plugin.getMultiBlockManager().placePart(player, block.getLocation(), partType);

            if (!success) {
                block.setType(Material.AIR);
            } else {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }
            }
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

    private String color(String text) {
        return text.replace("&", "§");
    }
}