package com.factory.generators.listeners;

import com.factory.generators.IronFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final IronFactory plugin;

    public PlayerListener(IronFactory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().saveGenerators();
        plugin.getDataManager().saveMultiBlockStructures();
    }
}