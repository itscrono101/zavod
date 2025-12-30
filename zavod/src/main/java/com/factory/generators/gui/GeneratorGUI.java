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
        inventory = Bukkit.createInventory(null, 27, colorize(type.getName()));

        // Заполнение фоном
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        // Информация о генераторе (центр)
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Владелец: &f" + Bukkit.getOfflinePlayer(generator.getOwnerUUID()).getName());
        infoLore.add("&7Тип: &f" + generator.getTypeId());
        infoLore.add("");
        infoLore.add("&7📊 Статистика:");
        infoLore.add("&7  ▸ Всего произведено: &a" + generator.getTotalGenerated());
        infoLore.add("&7  ▸ Следующая генерация: &a" + generator.formatTime(generator.getRemainingTicks(type.getDelay())));
        infoLore.add("");

        // Показываем что производит
        infoLore.add("&7⚙ Производит:");
        for (int i = 0; i < type.getDrops().size(); i++) {
            var drop = type.getDrops().get(i);
            String matName = formatMaterial(drop.getMaterial());
            String chance = drop.getChance() >= 100 ? "" : " &7(" + String.format("%.1f", drop.getChance()) + "%)";
            infoLore.add("&7  " + (i + 1) + ". &f" + drop.getAmount() + "x " + matName + chance);
        }
        infoLore.add("");
        infoLore.add("&7⏱ Задержка: &f" + formatTime(type.getDelay()));

        ItemStack info = createItemWithLore(type.getBlockMaterial(), "&e⚙ Информация", infoLore);
        inventory.setItem(13, info);

        // Кнопка апгрейда
        if (type.isUpgradeEnabled()) {
            List<String> upgradeLore = new ArrayList<>();
            upgradeLore.add("");
            upgradeLore.add("&7Улучшить до следующего уровня");
            upgradeLore.add("");
            upgradeLore.add("&7💰 Стоимость:");
            upgradeLore.add("&f  " + type.getUpgradeCostAmount() + "x " + formatMaterial(type.getUpgradeCostMaterial()));
            upgradeLore.add("");
            upgradeLore.add("&eНажмите для улучшения");

            ItemStack upgrade = createItemWithLore(Material.ANVIL, "&a⬆ Улучшить", upgradeLore);
            inventory.setItem(11, upgrade);
        } else {
            ItemStack maxLevel = createItem(Material.BARRIER, "&c⚠ Максимальный уровень",
                    "",
                    "&7Этот генератор уже на",
                    "&7максимальном уровне!"
            );
            inventory.setItem(11, maxLevel);
        }

        // Кнопка забрать
        List<String> pickupLore = new ArrayList<>();
        pickupLore.add("");
        pickupLore.add("&7Чтобы забрать генератор:");
        pickupLore.add("&71. Закройте это меню");
        pickupLore.add("&72. Зажмите &fShift");
        pickupLore.add("&73. Сломайте блок");
        pickupLore.add("");
        pickupLore.add("&c⚠ Генератор выпадет предметом");

        ItemStack pickup = createItemWithLore(Material.CHEST, "&e📦 Как забрать?", pickupLore);
        inventory.setItem(15, pickup);

        // Закрыть
        ItemStack close = createItem(Material.BARRIER, "&cЗакрыть");
        inventory.setItem(22, close);

        // Регистрация слушателя
        Bukkit.getPluginManager().registerEvents(this, plugin);

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        event.setCancelled(true);

        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= 27) return;

        switch (slot) {
            case 11: // Апгрейд
                if (type.isUpgradeEnabled()) {
                    clicker.closeInventory();

                    // Проверяем материалы перед апгрейдом
                    if (!hasItems(clicker, type.getUpgradeCostMaterial(), type.getUpgradeCostAmount())) {
                        clicker.sendMessage(colorize("&c[Завод] &fНедостаточно материалов!"));
                        clicker.sendMessage(colorize("&c[Завод] &fНужно: &e" + type.getUpgradeCostAmount() + "x " + formatMaterial(type.getUpgradeCostMaterial())));
                        return;
                    }

                    boolean success = plugin.getGeneratorManager().upgradeGenerator(clicker, generator.getLocation());

                    if (!success) {
                        clicker.sendMessage(colorize("&c[Завод] &fНе удалось улучшить генератор!"));
                    }
                }
                break;

            case 15: // Забрать (информация)
                clicker.sendMessage("");
                clicker.sendMessage(colorize("&8&m-----&r &6⚙ Как забрать генератор &8&m-----"));
                clicker.sendMessage(colorize("&e1. &fЗакройте это меню"));
                clicker.sendMessage(colorize("&e2. &fЗажмите &eShift &7(присядьте)"));
                clicker.sendMessage(colorize("&e3. &fСломайте блок генератора"));
                clicker.sendMessage(colorize("&8&m--------------------------------"));
                clicker.sendMessage("");
                break;

            case 22: // Закрыть
                clicker.closeInventory();
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore.length > 0) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(colorize(line));
                }
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createItemWithLore(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(colorize(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    private String formatMaterial(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder result = new StringBuilder();

        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    private String formatTime(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}