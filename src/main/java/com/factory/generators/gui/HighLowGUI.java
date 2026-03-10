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
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * High/Low GUI.
 * Players bet oil and guess if a random number (1-100) is high (51-100) or low (1-49).
 * Number 50 is a draw (return bet).
 */
public class HighLowGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private Inventory inventory;
    private int currentBet = 0;
    private String selectedChoice = null;
    private boolean spinning = false;

    private static final Random RANDOM = new Random();

    public HighLowGUI(IronFactory plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, color("&e&lВысокое/Низкое"));

        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 13: Display number (will be updated when spinning)
        List<String> numberLore = new ArrayList<>();
        numberLore.add("");
        numberLore.add("&7Случайное число: ?");
        ItemStack numberDisplay = createItem(Material.BOOK, "&eЧисло", numberLore);
        inventory.setItem(13, numberDisplay);

        // Slot 11: High (51-100)
        List<String> highLore = new ArrayList<>();
        highLore.add("");
        highLore.add("&7Число будет: 51-100");
        highLore.add("&7Выигрыш: x1.9");
        highLore.add("&7Шанс: 48%");
        ItemStack highButton = createItem(Material.LIME_DYE, "&a⬆ Высокое", highLore);
        inventory.setItem(11, highButton);

        // Slot 15: Low (1-49)
        List<String> lowLore = new ArrayList<>();
        lowLore.add("");
        lowLore.add("&7Число будет: 1-49");
        lowLore.add("&7Выигрыш: x1.9");
        lowLore.add("&7Шанс: 48%");
        ItemStack lowButton = createItem(Material.RED_DYE, "&c⬇ Низкое", lowLore);
        inventory.setItem(15, lowButton);

        // Slot 4-7: Bet amount buttons
        int[] amounts = {1, 5, 10, 50};
        int[] slots = {4, 5, 6, 7};

        for (int i = 0; i < Math.min(amounts.length, slots.length); i++) {
            int amount = amounts[i];
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&7Ставка: &e" + amount + " нефти");
            lore.add("&7Клик чтобы выбрать");
            ItemStack betItem = createItem(Material.PAPER, "&f" + amount + " нефти", lore);
            inventory.setItem(slots[i], betItem);
        }

        // Slot 19: Spin button
        updateSpinButton();

        // Slot 22: Back button
        ItemStack back = createItem(Material.ARROW, "&cНазад");
        inventory.setItem(22, back);

        // Register listener
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

        if (spinning) {
            clicker.sendMessage(color("&c[Казино] Дождись окончания вращения!"));
            return;
        }

        // Bet buttons: slots 4-7
        if (slot >= 4 && slot <= 7) {
            int[] amounts = {1, 5, 10, 50};
            int index = slot - 4;
            if (index >= 0 && index < amounts.length) {
                selectBet(clicker, amounts[index]);
            }
        } else if (slot == 11) {
            // High button
            selectChoice(clicker, "high");
        } else if (slot == 15) {
            // Low button
            selectChoice(clicker, "low");
        } else if (slot == 19) {
            // Spin button
            if (currentBet == 0) {
                clicker.sendMessage(color("&c[Казино] Сначала выбери ставку!"));
            } else if (selectedChoice == null) {
                clicker.sendMessage(color("&c[Казино] Сначала выбери: Высокое или Низкое!"));
            } else {
                spin(clicker);
            }
        } else if (slot == 22) {
            // Back button
            clicker.closeInventory();
            new CasinoMainGUI(plugin, clicker).open();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }

    private void selectBet(Player player, int amount) {
        if (!plugin.getCasinoManager().hasOil(player, amount)) {
            player.sendMessage(color("&c[Казино] Недостаточно нефти! Нужно: " + amount));
            return;
        }

        currentBet = amount;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1f, 1.5f);
        player.sendMessage(color("&a[Казино] Ставка выбрана: &e" + amount + " нефти"));

        updateSpinButton();
        player.updateInventory();
    }

    private void selectChoice(Player player, String choice) {
        if (currentBet == 0) {
            player.sendMessage(color("&c[Казино] Сначала выбери ставку!"));
            return;
        }

        selectedChoice = choice;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        String choiceName = choice.equals("high") ? "&aВысокое (51-100)" : "&cНизкое (1-49)";
        player.sendMessage(color("&a[Казино] Выбрано: " + choiceName));

        updateSpinButton();
        player.updateInventory();
    }

    private void spin(Player player) {
        if (!plugin.getCasinoManager().hasOil(player, currentBet)) {
            player.sendMessage(color("&c[Казино] Недостаточно нефти!"));
            currentBet = 0;
            selectedChoice = null;
            return;
        }

        // Check cooldown
        if (plugin.getCasinoManager().isOnCooldown(player)) {
            player.sendMessage(color("&c[Казино] Подожди перед следующей попыткой!"));
            return;
        }

        // Remove bet
        plugin.getCasinoManager().removeOil(player, currentBet);

        spinning = true;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        player.sendMessage(color("&e[Казино] Генерирую число..."));

        // Animate spinning
        final int[] frame = {0};
        final BukkitTask[] taskArray = new BukkitTask[1];
        taskArray[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (frame[0] >= 15) {
                // Stop spinning
                taskArray[0].cancel();
                showResult(player);
                return;
            }

            // Update with random number display
            int randomNum = RANDOM.nextInt(100) + 1; // 1-100
            ItemStack numberItem = createItem(Material.BOOK, "&e" + randomNum);
            inventory.setItem(13, numberItem);
            player.updateInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1f);
            frame[0]++;
        }, 0L, 2L); // Every 2 ticks (0.1 seconds)
    }

    private void showResult(Player player) {
        // Generate random number 1-100
        int result = RANDOM.nextInt(100) + 1;

        // Update display with final result
        ItemStack resultItem = createItem(Material.BOOK, "&e" + result);
        inventory.setItem(13, resultItem);

        boolean playerWon = false;
        String resultText;

        if (result == 50) {
            // Draw
            resultText = "50 - НИЧЬЯ";
            plugin.getCasinoManager().giveOil(player, currentBet); // Return bet
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            player.sendMessage(color("&7[Казино] Выпало число 50 - НИЧЬЯ! Ставка возвращена."));
        } else if (result > 50 && selectedChoice.equals("high")) {
            // Win on high
            resultText = "ВЫСОКОЕ (" + result + ") - ВЫИГРЫШ!";
            playerWon = true;
        } else if (result < 50 && selectedChoice.equals("low")) {
            // Win on low
            resultText = "НИЗКОЕ (" + result + ") - ВЫИГРЫШ!";
            playerWon = true;
        } else {
            // Loss
            resultText = "ПРОИГРЫШ! Число: " + result;
        }

        if (playerWon) {
            // WIN
            int winAmount = (int) (currentBet * plugin.getCasinoManager().getHighLowMultiplier());
            plugin.getCasinoManager().giveOil(player, winAmount);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            String choiceName = selectedChoice.equals("high") ? "&aВысокое" : "&cНизкое";
            player.sendMessage(color("&a&l✔ Выпало " + choiceName + " число " + result + "! Выигрыш: &e" + winAmount + " &aнефти!"));
        } else if (result != 50) {
            // LOSS (not a draw)
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
            String choiceName = selectedChoice.equals("high") ? "&aВысокое (51-100)" : "&cНизкое (1-49)";
            player.sendMessage(color("&c[Казино] Число " + result + " не " + choiceName + ". Ставка потеряна."));
        }

        spinning = false;
        currentBet = 0;
        selectedChoice = null;
        plugin.getCasinoManager().setCooldown(player);
        updateSpinButton();
        player.updateInventory();
    }

    private void updateSpinButton() {
        Material buttonMaterial = (currentBet > 0 && selectedChoice != null) ? Material.LIME_DYE : Material.GRAY_DYE;
        String buttonName = (currentBet > 0 && selectedChoice != null) ? "&a▶ Крутить" : "&8▶ Выбери ставку и вариант";
        List<String> lore = new ArrayList<>();
        if (currentBet > 0 && selectedChoice != null) {
            String choiceName = selectedChoice.equals("high") ? "Высокое" : "Низкое";
            lore.add("");
            lore.add("&7Ставка: &e" + currentBet + " нефти");
            lore.add("&7Выбор: &e" + choiceName);
        }
        ItemStack spinButton = createItem(buttonMaterial, buttonName, lore);
        inventory.setItem(19, spinButton);
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
}
