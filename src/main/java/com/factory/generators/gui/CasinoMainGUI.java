package com.factory.generators.gui;

import com.factory.generators.IronFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Main casino menu GUI.
 * Players select which game to play.
 */
public class CasinoMainGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private Inventory inventory;

    public CasinoMainGUI(IronFactory plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        // Create inventory
        inventory = Bukkit.createInventory(null, 27, color("&6&lКазино"));

        // Fill background with filler
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 11: Slot Machine
        List<String> slotLore = new ArrayList<>();
        slotLore.add("");
        slotLore.add("&7Крути барабаны и выигрывай!");
        slotLore.add("&7Шанс выигрыша: &e25%");
        slotLore.add("&7Множитель: &ex3");
        ItemStack slotMachine = createItem(Material.LEVER, "&e🎰 Слот-машина", slotLore);
        inventory.setItem(11, slotMachine);

        // Slot 13: Info
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Выбери игру для игры");
        infoLore.add("&7Ставь только нефть (INK_SAC)");
        ItemStack info = createItem(Material.BOOK, "&6Информация", infoLore);
        inventory.setItem(13, info);

        // Slot 15: Roulette
        List<String> rouletteLore = new ArrayList<>();
        rouletteLore.add("");
        rouletteLore.add("&7Выбери красное или черное");
        rouletteLore.add("&7Шанс выигрыша: &e47%");
        rouletteLore.add("&7Множитель: &ex1.8");
        ItemStack roulette = createItem(Material.REDSTONE, "&c🎡 Рулетка", rouletteLore);
        inventory.setItem(15, roulette);

        // Slot 22: Close
        ItemStack close = createItem(Material.BARRIER, "&cЗакрыть");
        inventory.setItem(22, close);

        // Add High/Low option to slot 8 (right side)
        List<String> highLowLore = new ArrayList<>();
        highLowLore.add("");
        highLowLore.add("&7Угадай число выше или ниже 50");
        highLowLore.add("&7Шанс выигрыша: &e48%");
        highLowLore.add("&7Множитель: &ex1.9");
        ItemStack highLow = createItem(Material.COMPARATOR, "&e⬆⬇ Высокое/Низкое", highLowLore);
        inventory.setItem(16, highLow);

        // Register this listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Open inventory
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Slot Machine
            clicker.closeInventory();
            new SlotMachineGUI(plugin, clicker).open();
            playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        } else if (slot == 15) {
            // Roulette
            clicker.closeInventory();
            new RouletteGUI(plugin, clicker).open();
            playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        } else if (slot == 16) {
            // High/Low
            clicker.closeInventory();
            new HighLowGUI(plugin, clicker).open();
            playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        } else if (slot == 22) {
            // Close
            clicker.closeInventory();
            playSound(clicker, Sound.UI_BUTTON_CLICK, 1f, 1f);
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
            for (String line : lore) {
                coloredLore.add(color(line));
            }
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

    private void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}
