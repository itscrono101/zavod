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

public class MineGUI implements Listener {

    private final IronFactory plugin;
    private final Player player;
    private final PlacedGenerator generator;
    private final GeneratorType type;
    private Inventory inventory;

    // Требуемые ресурсы для рудника (можно получить из конфига или захардкодить)
    private static final Material[] REQUIRED_MATERIALS = {
            Material.REDSTONE,
            Material.OAK_LOG,
            Material.COBBLESTONE
    };
    private static final int[] REQUIRED_AMOUNTS = {15, 15, 30};

    public MineGUI(IronFactory plugin, Player player, PlacedGenerator generator, GeneratorType type) {
        this.plugin = plugin;
        this.player = player;
        this.generator = generator;
        this.type = type;
    }

    public void open() {
        inventory = Bukkit.createInventory(null, 27, color(type.getName()));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // Заголовок - информация о рудника
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Владелец: &f" + Bukkit.getOfflinePlayer(generator.getOwnerUUID()).getName());
        infoLore.add("&7Статус: " + (isMineFull() ? "&aАКТИВЕН" : "&cТребуется пополнение"));
        infoLore.add("");
        infoLore.add("&7Для активации нужны ресурсы:");

        ItemStack info = createItem(Material.DIAMOND_PICKAXE, "&eИнформация", infoLore);
        inventory.setItem(13, info);

        // Показываем требуемые ресурсы
        displayResourceRequirements();

        // Если рудник активен, показываем его статус, иначе показываем кнопку активации
        if (isMineFull()) {
            List<String> activeLore = new ArrayList<>();
            activeLore.add("");
            activeLore.add("&aРудник активен и производит ресурсы!");
            inventory.setItem(11, createItem(Material.EMERALD_BLOCK, "&a✓ Активен", activeLore));
        } else {
            List<String> needLore = new ArrayList<>();
            needLore.add("");
            for (int i = 0; i < REQUIRED_MATERIALS.length; i++) {
                int current = getPlayerItemCount(player, REQUIRED_MATERIALS[i]);
                int needed = REQUIRED_AMOUNTS[i];
                String status = current >= needed ? "&a✓" : "&c✗";
                needLore.add(status + " " + formatMaterial(REQUIRED_MATERIALS[i]) + ": &f" + current + "&7/" + needed);
            }
            needLore.add("");

            boolean canActivate = canActivateMine();
            if (canActivate) {
                needLore.add("&aНажмите для активации!");
                inventory.setItem(11, createItem(Material.LIME_DYE, "&a✓ Активировать", needLore));
            } else {
                needLore.add("&cНедостаточно ресурсов!");
                inventory.setItem(11, createItem(Material.RED_DYE, "&c✗ Активировать", needLore));
            }
        }

        // Кнопка закрытия
        inventory.setItem(22, createItem(Material.BARRIER, "&cЗакрыть", new ArrayList<>()));

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private void displayResourceRequirements() {
        // Слот 1-3: редстоун, дерево, булыжник
        for (int i = 0; i < REQUIRED_MATERIALS.length; i++) {
            Material material = REQUIRED_MATERIALS[i];
            int required = REQUIRED_AMOUNTS[i];
            int current = getPlayerItemCount(player, material);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("&7Требуется: &f" + required);
            lore.add("&7У вас: &f" + current);

            String name;
            if (current >= required) {
                name = "&a✓ " + formatMaterial(material);
            } else {
                name = "&c✗ " + formatMaterial(material) + " (&c" + (required - current) + " не хватает&c)";
            }

            ItemStack item = createItem(material, name, lore);
            inventory.setItem(1 + i * 2, item);
        }
    }

    private boolean isMineFull() {
        // Проверяем, активирован ли рудник (в PlacedGenerator может быть флаг или мы проверяем через mineHealth)
        // Для теперь просто проверяем, что генератор не сломан и не требует ремонта
        return !generator.isBroken() && generator.getMineHealth() >= 100;
    }

    private boolean canActivateMine() {
        for (int i = 0; i < REQUIRED_MATERIALS.length; i++) {
            if (getPlayerItemCount(player, REQUIRED_MATERIALS[i]) < REQUIRED_AMOUNTS[i]) {
                return false;
            }
        }
        return true;
    }

    private int getPlayerItemCount(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeResourcesFromPlayer() {
        for (int i = 0; i < REQUIRED_MATERIALS.length; i++) {
            removeItems(player, REQUIRED_MATERIALS[i], REQUIRED_AMOUNTS[i]);
        }
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (slot == 11 && !isMineFull()) {
            // Активация рудника
            if (!canActivateMine()) {
                clicker.sendMessage(color("&c[Рудник] Недостаточно ресурсов!"));
                clicker.playSound(clicker.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            removeResourcesFromPlayer();
            generator.setMineHealth(100); // Устанавливаем полное здоровье
            generator.setBroken(false);

            clicker.sendMessage(color("&a[Рудник] Рудник активирован! Начинает производить ресурсы!"));
            clicker.playSound(clicker.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

            // Обновляем голограмму
            plugin.getHologramManager().updateHologram(generator, type);

            clicker.closeInventory();
            // Переоткрываем GUI для обновления
            new MineGUI(plugin, clicker, generator, type).open();

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
