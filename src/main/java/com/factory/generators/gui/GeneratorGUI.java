package com.factory.generators.gui;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GeneratorGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private final PlacedGenerator generator;
    private final GeneratorType type;
    private Inventory inventory;

    public GeneratorGUI(IronFactory plugin, Player player, PlacedGenerator generator, GeneratorType type) {
        this.plugin = plugin;
        this.player = player;
        this.generator = generator;
        this.type = type;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, color(type.getName()));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Владелец: &f" + Bukkit.getOfflinePlayer(generator.getOwnerUUID()).getName());
        infoLore.add("&7Произведено: &a" + generator.getTotalGenerated());
        infoLore.add("&7След: &a" + generator.formatTime(generator.getRemainingTicks(type.getDelay())));

        ItemStack info = createItem(type.getBlockMaterial(), "&eИнформация", infoLore);
        inventory.setItem(13, info);

        if (type.isUpgradeEnabled()) {
            List<String> upgradeLore = new ArrayList<>();
            upgradeLore.add("");
            upgradeLore.add("&7Стоимость: &f" + type.getUpgradeCostAmount() + "x " + type.getUpgradeCostMaterial().name());
            inventory.setItem(11, createItem(Material.ANVIL, "&aУлучшить", upgradeLore));
        } else {
            inventory.setItem(11, createItem(Material.BARRIER, "&cМакс. уровень", new ArrayList<>()));
        }

        inventory.setItem(22, createItem(Material.BARRIER, "&cЗакрыть", new ArrayList<>()));

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11 && type.isUpgradeEnabled()) {
            clicker.closeInventory();
            plugin.getGeneratorManager().upgradeGenerator(clicker, generator.getLocation());
        } else if (slot == 22) {
            clicker.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(color(line));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, new ArrayList<>());
    }

    private String color(String text) {
        return text.replace("&", "§");
    }
}