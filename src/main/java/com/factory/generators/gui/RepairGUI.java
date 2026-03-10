package com.factory.generators.gui;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.PlacedGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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

public class RepairGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private final PlacedGenerator generator;
    private final GeneratorType type;
    private Inventory inventory;

    public RepairGUI(IronFactory plugin, Player player, PlacedGenerator generator, GeneratorType type) {
        this.plugin = plugin;
        this.player = player;
        this.generator = generator;
        this.type = type;
    }

    public void open() {
        String title = type.getBrokenName() != null ? type.getBrokenName() : "&c[СЛОМАН]";
        inventory = Bukkit.createInventory(null, 27, color(title));

        // Фон
        ItemStack filler = createItem(Material.RED_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // Информация о поломке
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&c⚠ Генератор сломан!");
        infoLore.add("");
        infoLore.add("&7Для ремонта нужно:");
        infoLore.add("&f  " + type.getRepairAmount() + "x " + formatMaterial(type.getRepairMaterial()));
        infoLore.add("");
        infoLore.add("&7Произведено до поломки: &f" + generator.getTotalGenerated());

        ItemStack info = createItem(Material.BARRIER, "&c⚠ СЛОМАН", infoLore);
        inventory.setItem(13, info);

        // Кнопка ремонта
        List<String> repairLore = new ArrayList<>();
        repairLore.add("");
        repairLore.add("&7Стоимость:");
        repairLore.add("&f  " + type.getRepairAmount() + "x " + formatMaterial(type.getRepairMaterial()));
        repairLore.add("");

        boolean hasItems = hasItems(player, type.getRepairMaterial(), type.getRepairAmount());
        if (hasItems) {
            repairLore.add("&aНажмите для ремонта!");
        } else {
            repairLore.add("&cНедостаточно материалов!");
        }

        ItemStack repair = createItem(
                hasItems ? Material.ANVIL : Material.GRAY_DYE,
                hasItems ? "&a🔧 Починить" : "&c🔧 Починить",
                repairLore
        );
        inventory.setItem(11, repair);

        // Закрыть
        inventory.setItem(15, createItem(Material.ARROW, "&7Закрыть", new ArrayList<>()));

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11) {
            // Ремонт
            if (!hasItems(clicker, type.getRepairMaterial(), type.getRepairAmount())) {
                clicker.sendMessage(color("&c[Завод] Недостаточно материалов!"));
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            removeItems(clicker, type.getRepairMaterial(), type.getRepairAmount());
            generator.setBroken(false);

            clicker.closeInventory();
            clicker.sendMessage(color("&a[Завод] Генератор отремонтирован!"));
            clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

            // Обновляем голограмму
            plugin.getHologramManager().updateHologram(generator, type);

        } else if (slot == 15) {
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

    private boolean hasItems(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count >= amount;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material && remaining > 0) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
            }
        }
        player.updateInventory();
    }

    private String formatMaterial(Material material) {
        if (material == null) return "???";
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String color(String text) {
        return text.replace("&", "§");
    }
}