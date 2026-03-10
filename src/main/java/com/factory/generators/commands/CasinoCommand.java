package com.factory.generators.commands;

import com.factory.generators.IronFactory;
import com.factory.generators.utils.Constants;
import com.factory.generators.utils.Logger;
import com.factory.generators.utils.Utils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Admin commands for casino management.
 * /casino create - Create a casino block at player's location
 * /casino remove - Remove casino block at player's location
 * /casino list - List all casino blocks
 */
public class CasinoCommand implements CommandExecutor {

    private final IronFactory plugin;

    public CasinoCommand(@NotNull IronFactory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // Check if sender is a player (for block placement)
        if (!(sender instanceof Player)) {
            sender.sendMessage(Utils.colorize("&c[Казино] Команду может использовать только игрок!"));
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission(Constants.Permissions.ADMIN)) {
            player.sendMessage(Utils.colorize("&c[Казино] Недостаточно прав! Нужно: factory.admin"));
            return true;
        }

        // Check if casino is enabled
        if (!plugin.getCasinoManager().isCasinoEnabled()) {
            player.sendMessage(Utils.colorize("&c[Казино] Казино отключено в конфиге!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player);
                break;
            case "remove":
                handleRemove(player);
                break;
            case "list":
                handleList(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                player.sendMessage(Utils.colorize("&c[Казино] Неизвестная команда! Используй /casino help"));
        }

        return true;
    }

    /**
     * Create a casino block at player's current block location.
     */
    private void handleCreate(@NotNull Player player) {
        // Get block player is looking at (or standing on)
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            block = player.getLocation().getBlock();
        }

        Location blockLoc = block.getLocation();

        // Check if already a casino block
        if (plugin.getCasinoManager().isCasinoBlock(blockLoc)) {
            player.sendMessage(Utils.colorize("&c[Казино] На этом месте уже есть казино блок!"));
            return;
        }

        // Set the block material
        block.setType(plugin.getCasinoManager().getCasinoBlockMaterial());

        // Register casino block
        plugin.getCasinoManager().registerCasinoBlock(blockLoc);

        player.sendMessage(Utils.colorize("&a[Казино] Казино блок создан в: " +
                blockLoc.getBlockX() + ", " + blockLoc.getBlockY() + ", " + blockLoc.getBlockZ()));
        Logger.info(player.getName() + " created a casino block at " + blockLoc);
    }

    /**
     * Remove casino block at player's location.
     */
    private void handleRemove(@NotNull Player player) {
        // Get block player is looking at
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage(Utils.colorize("&c[Казино] Не вижу блок перед тобой!"));
            return;
        }

        Location blockLoc = block.getLocation();

        // Check if it's a casino block
        if (!plugin.getCasinoManager().isCasinoBlock(blockLoc)) {
            player.sendMessage(Utils.colorize("&c[Казино] Это не казино блок!"));
            return;
        }

        // Remove casino block
        plugin.getCasinoManager().removeCasinoBlock(blockLoc);

        // Change block back to air
        block.setType(org.bukkit.Material.AIR);

        player.sendMessage(Utils.colorize("&a[Казино] Казино блок удален!"));
        Logger.info(player.getName() + " removed a casino block at " + blockLoc);
    }

    /**
     * List all casino blocks.
     */
    private void handleList(@NotNull Player player) {
        Map<String, Location> blocks = plugin.getCasinoManager().getAllCasinoBlocks();

        if (blocks.isEmpty()) {
            player.sendMessage(Utils.colorize("&7[Казино] Казино блоков не найдено"));
            return;
        }

        player.sendMessage(Utils.colorize("&8&m-----------&r &6Казино Блоки &8&m-----------"));
        for (Map.Entry<String, Location> entry : blocks.entrySet()) {
            Location loc = entry.getValue();
            String message = String.format("&7- &e%s&7 (%d, %d, %d)",
                    entry.getKey(),
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ());
            player.sendMessage(Utils.colorize(message));
        }
        player.sendMessage(Utils.colorize("&7Всего: &e" + blocks.size() + " казино блоков"));
    }

    /**
     * Send help message.
     */
    private void sendHelp(@NotNull Player player) {
        player.sendMessage(Utils.colorize("&8&m-----------&r &6Казино Команды &8&m-----------"));
        player.sendMessage(Utils.colorize("&e/casino create &7- Создать казино блок"));
        player.sendMessage(Utils.colorize("&e/casino remove &7- Удалить казино блок"));
        player.sendMessage(Utils.colorize("&e/casino list &7- Показать все казино блоки"));
        player.sendMessage(Utils.colorize("&e/casino help &7- Показать эту справку"));
    }
}
