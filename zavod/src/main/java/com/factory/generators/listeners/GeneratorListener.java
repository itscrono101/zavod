package com.factory.generators.listeners;

import com.factory.generators.IronFactory;
import com.factory.generators.gui.GeneratorGUI;
import com.factory.generators.models.GeneratorType;
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

        // Проверка, является ли предмет генератором
        String generatorId = GeneratorType.getGeneratorId(item);
        if (generatorId == null) return;

        GeneratorType type = plugin.getConfigManager().getGeneratorType(generatorId);
        if (type == null) {
            event.setCancelled(true);
            return;
        }

        // Устанавливаем правильный тип блока
        Block block = event.getBlockPlaced();
        block.setType(type.getBlockMaterial());

        // Регистрация генератора
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

        PlacedGenerator generator = plugin.getGeneratorManager().getGeneratorAt(block.getLocation());
        if (generator == null) return;

        event.setCancelled(true);

        // Проверка sneaking для слома
        if (!player.isSneaking()) {
            player.sendMessage(colorize("&e[Завод] Присядьте (Shift) чтобы сломать генератор."));
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

        PlacedGenerator generator = plugin.getGeneratorManager().getGeneratorAt(block.getLocation());
        if (generator == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());

        if (type == null) return;

        // Если держит материал для апгрейда - пытаемся апгрейдить
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (type.isUpgradeEnabled() &&
                hand != null &&
                hand.getType() == type.getUpgradeCostMaterial() &&
                hand.getAmount() >= type.getUpgradeCostAmount()) {

            plugin.getGeneratorManager().upgradeGenerator(player, block.getLocation());
            return;
        }

        // Иначе открываем GUI
        new GeneratorGUI(plugin, player, generator, type).open();
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}