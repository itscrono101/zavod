package com.factory.generators.listeners;

import com.factory.generators.IronFactory;
import com.factory.generators.gui.CasinoMainGUI;
import com.factory.generators.utils.Logger;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listens for casino block interactions.
 * Opens casino menu when right-clicked.
 */
public class CasinoListener implements Listener {

    private final IronFactory plugin;

    public CasinoListener(IronFactory plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Only handle main hand
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Check if this is a casino block
        if (!plugin.getCasinoManager().isCasinoBlock(block.getLocation())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Logger.info(player.getName() + " opened casino menu at " + block.getLocation());

        // Open casino menu
        new CasinoMainGUI(plugin, player).open();
    }
}
