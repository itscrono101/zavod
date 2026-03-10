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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Roulette GUI.
 * Players bet oil and choose red or black.
 * Win chance: 47% (18 red + 18 black out of 37 numbers)
 * House edge: 2.7% (from zero)
 */
public class RouletteGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private Inventory inventory;
    private int currentBet = 0;
    private String selectedColor = null;
    private boolean spinning = false;

    private static final Random RANDOM = new Random();

    // Red and black numbers on a roulette wheel
    private static final Set<Integer> RED_NUMBERS = new HashSet<>();
    private static final Set<Integer> BLACK_NUMBERS = new HashSet<>();

    static {
        // Red numbers
        int[] red = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
        for (int num : red) RED_NUMBERS.add(num);

        // Black numbers
        int[] black = {2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35};
        for (int num : black) BLACK_NUMBERS.add(num);
    }

    public RouletteGUI(IronFactory plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, color("&c&lРулетка"));

        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 13: Display current result (spinning wheel initially)
        updateWheelDisplay();

        // Slot 11: Red
        List<String> redLore = new ArrayList<>();
        redLore.add("");
        redLore.add("&7Красное число - выигрыш x1.8");
        redLore.add("&7Шанс: 47%");
        ItemStack redButton = createItem(Material.RED_WOOL, "&cКрасное", redLore);
        inventory.setItem(11, redButton);

        // Slot 15: Black
        List<String> blackLore = new ArrayList<>();
        blackLore.add("");
        blackLore.add("&7Черное число - выигрыш x1.8");
        blackLore.add("&7Шанс: 47%");
        ItemStack blackButton = createItem(Material.BLACK_WOOL, "&8Черное", blackLore);
        inventory.setItem(15, blackButton);

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
            // Red button
            selectColor(clicker, "red");
        } else if (slot == 15) {
            // Black button
            selectColor(clicker, "black");
        } else if (slot == 19) {
            // Spin button
            if (currentBet == 0) {
                clicker.sendMessage(color("&c[Казино] Сначала выбери ставку!"));
            } else if (selectedColor == null) {
                clicker.sendMessage(color("&c[Казино] Сначала выбери цвет!"));
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

    private void selectColor(Player player, String color) {
        if (currentBet == 0) {
            player.sendMessage(color("&c[Казино] Сначала выбери ставку!"));
            return;
        }

        selectedColor = color;
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
        String colorName = color.equals("red") ? "&cкрасное" : "&8черное";
        player.sendMessage(color("&a[Казино] Выбрано: " + colorName));

        updateSpinButton();
        player.updateInventory();
    }

    private void spin(Player player) {
        if (!plugin.getCasinoManager().hasOil(player, currentBet)) {
            player.sendMessage(color("&c[Казино] Недостаточно нефти!"));
            currentBet = 0;
            selectedColor = null;
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
        player.sendMessage(color("&e[Казино] Кручу рулетку..."));

        // Animate spinning wheel
        final int[] frame = {0};
        final BukkitTask[] taskArray = new BukkitTask[1];
        taskArray[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (frame[0] >= 20) {
                // Stop spinning
                taskArray[0].cancel();
                showResult(player);
                return;
            }

            // Update wheel with random colors
            Material wheelMaterial = RANDOM.nextBoolean() ? Material.RED_WOOL : Material.BLACK_WOOL;
            inventory.setItem(13, createItem(wheelMaterial, " "));
            player.updateInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1f);
            frame[0]++;
        }, 0L, 2L); // Every 2 ticks (0.1 seconds)
    }

    private void showResult(Player player) {
        // Spin a random number 0-36
        int result = RANDOM.nextInt(37); // 0-36 inclusive

        boolean isRed = RED_NUMBERS.contains(result);
        boolean isBlack = BLACK_NUMBERS.contains(result);
        boolean isZero = result == 0;

        Material resultMaterial;
        String resultText;

        if (isZero) {
            resultMaterial = Material.GREEN_WOOL;
            resultText = "ЗЕРО";
        } else if (isRed) {
            resultMaterial = Material.RED_WOOL;
            resultText = "КРАСНОЕ (" + result + ")";
        } else {
            resultMaterial = Material.BLACK_WOOL;
            resultText = "ЧЕРНОЕ (" + result + ")";
        }

        inventory.setItem(13, createItem(resultMaterial, " "));

        boolean playerWon = false;
        if (!isZero) {
            if ((selectedColor.equals("red") && isRed) || (selectedColor.equals("black") && isBlack)) {
                playerWon = true;
            }
        }

        if (playerWon) {
            // WIN
            int winAmount = (int) (currentBet * plugin.getCasinoManager().getRouletteMultiplier());
            plugin.getCasinoManager().giveOil(player, winAmount);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            String colorName = selectedColor.equals("red") ? "&cКРАСНОЕ" : "&8ЧЕРНОЕ";
            player.sendMessage(color("&a&l✔ Выпало " + colorName + "! Выигрыш: &e" + winAmount + " &aнефти!"));
        } else {
            // LOSS
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.5f);
            player.sendMessage(color("&c[Казино] Выпало " + resultText + ". Ставка потеряна."));
        }

        spinning = false;
        currentBet = 0;
        selectedColor = null;
        plugin.getCasinoManager().setCooldown(player);
        updateSpinButton();
        player.updateInventory();
    }

    private void updateWheelDisplay() {
        Material wheelMaterial = RANDOM.nextBoolean() ? Material.RED_WOOL : Material.BLACK_WOOL;
        ItemStack wheel = createItem(wheelMaterial, " ");
        inventory.setItem(13, wheel);
    }

    private void updateSpinButton() {
        Material buttonMaterial = (currentBet > 0 && selectedColor != null) ? Material.LIME_DYE : Material.GRAY_DYE;
        String buttonName = (currentBet > 0 && selectedColor != null) ? "&a▶ Крутить" : "&8▶ Выбери ставку и цвет";
        List<String> lore = new ArrayList<>();
        if (currentBet > 0 && selectedColor != null) {
            String colorName = selectedColor.equals("red") ? "Красное" : "Черное";
            lore.add("");
            lore.add("&7Ставка: &e" + currentBet + " нефти");
            lore.add("&7Выбор: &e" + colorName);
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
