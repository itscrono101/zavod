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
 * Slot Machine GUI.
 * Players bet oil and spin the reels.
 * Win: 3 matching symbols = x3 multiplier
 * Draw: 2 matching symbols = return bet
 * Loss: All different = lose bet
 */
public class SlotMachineGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private Inventory inventory;
    private int currentBet = 0;
    private boolean spinning = false;

    private static final Material[] SYMBOLS = {
        Material.DIAMOND,
        Material.EMERALD,
        Material.GOLD_INGOT,
        Material.IRON_INGOT,
        Material.COAL
    };

    private static final Random RANDOM = new Random();

    public SlotMachineGUI(IronFactory plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, color("&6&lСлот-машина"));

        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 10, 12, 14: Spinning reels (will be updated with random symbols)
        updateReels();

        // Slot 4-7: Bet amount buttons (1, 5, 10, 50)
        List<Integer> betAmounts = new ArrayList<>(plugin.getCasinoManager().getAllowedBetAmounts());
        betAmounts.sort(Integer::compareTo);

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

        // Slot 19: Spin button (only if bet selected)
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

        // Bet buttons: slots 4, 5, 6, 7
        if (slot >= 4 && slot <= 7) {
            int[] amounts = {1, 5, 10, 50};
            int index = slot - 4;
            if (index >= 0 && index < amounts.length) {
                selectBet(clicker, amounts[index]);
            }
        } else if (slot == 19) {
            // Spin button
            if (currentBet == 0) {
                clicker.sendMessage(color("&c[Казино] Сначала выбери ставку!"));
                return;
            }
            spinReels(clicker);
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

    private void spinReels(Player player) {
        if (!plugin.getCasinoManager().hasOil(player, currentBet)) {
            player.sendMessage(color("&c[Казино] Недостаточно нефти!"));
            currentBet = 0;
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
        player.sendMessage(color("&e[Казино] Крутим барабаны..."));

        // Animate spinning
        final int[] frame = {0};
        final BukkitTask[] taskArray = new BukkitTask[1];
        taskArray[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (frame[0] >= 10) {
                // Stop spinning and show result
                taskArray[0].cancel();
                showResult(player);
                return;
            }

            // Update reels with random symbols
            updateReels();
            player.updateInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            frame[0]++;
        }, 0L, 3L); // Every 3 ticks (0.15 seconds)
    }

    private void showResult(Player player) {
        Material[] finalReels = new Material[3];
        finalReels[0] = (Material) inventory.getItem(10).getType();
        finalReels[1] = (Material) inventory.getItem(12).getType();
        finalReels[2] = (Material) inventory.getItem(14).getType();

        // Determine result
        if (finalReels[0] == finalReels[1] && finalReels[1] == finalReels[2]) {
            // JACKPOT: 3 matching
            int winAmount = (int) (currentBet * plugin.getCasinoManager().getSlotMachineMultiplier());
            plugin.getCasinoManager().giveOil(player, winAmount);
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 2f);
            player.sendMessage(color("&a&l✔ ДЖЕКПОТ! Выиграл &e" + winAmount + " &aнефти!"));
        } else if ((finalReels[0] == finalReels[1]) || (finalReels[1] == finalReels[2])) {
            // DRAW: 2 matching
            plugin.getCasinoManager().giveOil(player, currentBet);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            player.sendMessage(color("&7[Казино] Ничья! Возврат ставки: &e" + currentBet + " нефти"));
        } else {
            // LOSS: All different
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
            player.sendMessage(color("&c[Казино] Проигрыш! Ставка потеряна."));
        }

        spinning = false;
        currentBet = 0;
        plugin.getCasinoManager().setCooldown(player);
        updateSpinButton();
        player.updateInventory();
    }

    private void updateReels() {
        for (int slot : new int[]{10, 12, 14}) {
            Material symbol = SYMBOLS[RANDOM.nextInt(SYMBOLS.length)];
            inventory.setItem(slot, createItem(symbol, " "));
        }
    }

    private void updateSpinButton() {
        Material buttonMaterial = currentBet > 0 ? Material.LIME_DYE : Material.GRAY_DYE;
        String buttonName = currentBet > 0 ? "&a▶ Крутить" : "&8▶ Выбери ставку";
        List<String> lore = new ArrayList<>();
        if (currentBet > 0) {
            lore.add("");
            lore.add("&7Ставка: &e" + currentBet + " нефти");
            lore.add("&7Клик чтобы крутить");
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
