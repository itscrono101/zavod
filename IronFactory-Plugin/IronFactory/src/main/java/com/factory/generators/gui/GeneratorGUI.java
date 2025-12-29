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

import java.util.Arrays;
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
        inventory = Bukkit.createInventory(null, 27, colorize("&8⚙ Генератор"));
        
        // Заполнение фоном
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }
        
        // Информация о генераторе (центр)
        ItemStack info = createItem(type.getBlockMaterial(), type.getName(),
            "",
            "&7Владелец: &f" + Bukkit.getOfflinePlayer(generator.getOwnerUUID()).getName(),
            "&7Тип: &f" + generator.getTypeId(),
            "&7Всего произведено: &f" + generator.getTotalGenerated(),
            "",
            "&7Следующая генерация:",
            "&a" + generator.formatTime(generator.getRemainingTicks(type.getDelay()))
        );
        inventory.setItem(13, info);
        
        // Кнопка апгрейда
        if (type.isUpgradeEnabled()) {
            ItemStack upgrade = createItem(Material.ANVIL, "&a⬆ Улучшить",
                "",
                "&7Стоимость:",
                "&f" + type.getUpgradeCostAmount() + " " + formatMaterial(type.getUpgradeCostMaterial()),
                "",
                "&eНажмите для улучшения"
            );
            inventory.setItem(11, upgrade);
        }
        
        // Кнопка забрать
        ItemStack pickup = createItem(Material.CHEST, "&e📦 Забрать генератор",
            "",
            "&7Shift+ЛКМ на блоке",
            "&7для удаления генератора",
            "",
            "&cГенератор выпадет предметом"
        );
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
        
        switch (slot) {
            case 11: // Апгрейд
                if (type.isUpgradeEnabled()) {
                    clicker.closeInventory();
                    plugin.getGeneratorManager().upgradeGenerator(clicker, generator.getLocation());
                }
                break;
            case 15: // Забрать
                clicker.closeInventory();
                clicker.sendMessage(colorize("&e[Завод] Присядьте (Shift) и сломайте блок чтобы забрать генератор."));
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
                List<String> coloredLore = Arrays.stream(lore)
                    .map(this::colorize)
                    .toList();
                meta.setLore(coloredLore);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private String formatMaterial(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}
