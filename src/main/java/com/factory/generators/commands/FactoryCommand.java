package com.factory.generators.commands;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import com.factory.generators.models.MultiBlockStructure;
import com.factory.generators.models.PlacedGenerator;
import com.factory.generators.utils.Constants;
import com.factory.generators.utils.Logger;
import com.factory.generators.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FactoryCommand implements CommandExecutor, TabCompleter {

    private final IronFactory plugin;

    public FactoryCommand(IronFactory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help")) {
            sendHelp(sender);
        } else if (sub.equals("give")) {
            handleGive(sender, args);
        } else if (sub.equals("drill")) {
            handleDrill(sender, args);
        } else if (sub.equals("drilldebug")) {
            handleDrillDebug(sender);
        } else if (sub.equals("list")) {
            handleList(sender);
        } else if (sub.equals("reload")) {
            handleReload(sender);
        } else if (sub.equals("info")) {
            handleInfo(sender);
        } else if (sub.equals("setdelay")) {
            handleSetDelay(sender, args);
        } else if (sub.equals("generate")) {
            handleGenerate(sender);
        } else if (sub.equals("debug")) {
            handleDebug(sender);
        } else {
            sender.sendMessage(Utils.colorize("&c[Завод] Неизвестная команда. /factory help"));
        }

        return true;
    }

    /**
     * Sends help message to player.
     *
     * @param sender Command sender
     */
    private void sendHelp(@NotNull CommandSender sender) {
        sendMessage(sender, "&8&m-----------&r &6⚙ IronFactory &8&m-----------");
        sendMessage(sender, "&e/factory give <ник> <генератор> [кол-во]");
        sendMessage(sender, "&e/factory drill <ник> <drill/pipe/pump> [кол-во]");
        sendMessage(sender, "&e/factory drilldebug &7- Дебаг буровых");
        sendMessage(sender, "&e/factory list &7- Список генераторов");
        sendMessage(sender, "&e/factory reload &7- Перезагрузка");
        sendMessage(sender, "&8&m------------------------------------");
        sendMessage(sender, "");
        sendMessage(sender, "&8&l⛽ &7Буровая вышка:");
        sendMessage(sender, "&7  1. &8Долото &7(NETHERITE_BLOCK)");
        sendMessage(sender, "&7  2. &6Труба &7(COPPER_BLOCK)");
        sendMessage(sender, "&7  3. &cСтанок &7(REDSTONE_BLOCK)");
    }

    /**
     * Sends message to command sender (handles both Player and Console).
     *
     * @param sender Command sender
     * @param message Message to send
     */
    private void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Utils.colorize(message));
    }

    /**
     * Handles /factory give command.
     *
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleGive(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(Constants.Permissions.GIVE)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, "&c/factory give <ник> <генератор> [кол-во]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Игрок не найден!");
            return;
        }

        String generatorId = args[2];
        GeneratorType type = plugin.getConfigManager().getGeneratorType(generatorId);

        if (type == null) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Генератор не найден: " + generatorId);
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Utils.clamp(Integer.parseInt(args[3]), 1, 64);
            } catch (NumberFormatException e) {
                Logger.warn("Invalid amount specified: " + args[3]);
                sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неверное количество!");
                return;
            }
        }

        target.getInventory().addItem(type.createItem(amount));
        sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Выдан " + generatorId + " x" + amount);
        Logger.info(sender.getName() + " gave " + amount + "x " + generatorId + " to " + target.getName());
    }

    /**
     * Handles /factory drill command.
     *
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleDrill(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(Constants.Permissions.GIVE)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, "&c/factory drill <ник> <drill/pipe/pump> [кол-во]");
            sendMessage(sender, "");
            sendMessage(sender, "&7Части:");
            sendMessage(sender, "&f  drill &7- Долото (NETHERITE_BLOCK)");
            sendMessage(sender, "&f  pipe &7- Труба (COPPER_BLOCK)");
            sendMessage(sender, "&f  pump &7- Станок (REDSTONE_BLOCK)");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Игрок не найден!");
            return;
        }

        String partName = args[2].toLowerCase();
        if (!partName.equals("drill") && !partName.equals("pipe") && !partName.equals("pump")) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Части: drill, pipe, pump");
            return;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Utils.clamp(Integer.parseInt(args[3]), 1, 64);
            } catch (NumberFormatException e) {
                Logger.warn("Invalid drill amount: " + args[3]);
                sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неверное количество!");
                return;
            }
        }

        ItemStack item = plugin.getMultiBlockManager().createPartItem(partName);
        if (item == null) {
            Logger.error("Failed to create drill part: " + partName, null);
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Ошибка!");
            return;
        }

        item.setAmount(amount);
        target.getInventory().addItem(item);
        sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Выдано " + partName + " x" + amount);
        Logger.info(sender.getName() + " gave " + amount + "x drill part '" + partName + "' to " + target.getName());
    }

    /**
     * Handles /factory drilldebug command.
     *
     * @param sender Command sender
     */
    private void handleDrillDebug(@NotNull CommandSender sender) {
        sendMessage(sender, "&6=== БУРОВЫЕ ВЫШКИ ===");

        Map<String, MultiBlockStructure> structures = plugin.getMultiBlockManager().getStructures();
        sendMessage(sender, "&7Всего структур: &f" + structures.size());

        if (structures.isEmpty()) {
            sendMessage(sender, "&7Нет буровых вышек");
            sendMessage(sender, "&6==================");
            return;
        }

        for (Map.Entry<String, MultiBlockStructure> entry : structures.entrySet()) {
            MultiBlockStructure s = entry.getValue();
            sendMessage(sender, "");
            sendMessage(sender, "&e" + entry.getKey());
            sendMessage(sender, "  &7Статус: " + (s.isComplete() ? "&a✔ РАБОТАЕТ" : "&c✗ НЕ ЗАВЕРШЕНА"));
            sendMessage(sender, "  &7Долото: " + (s.getDrillTypeId() != null ? "&aесть" : "&cнет"));
            sendMessage(sender, "  &7Труба: " + (s.getPipeTypeId() != null ? "&aесть" : "&cнет"));
            sendMessage(sender, "  &7Станок: " + (s.getPumpTypeId() != null ? "&aесть" : "&cнет"));
            sendMessage(sender, "  &7Тик: &f" + s.getCurrentTick() + "/1200");
            sendMessage(sender, "  &7Добыто: &f" + s.getTotalGenerated());
        }

        sendMessage(sender, "");
        sendMessage(sender, "&6==================");
    }

    /**
     * Handles /factory list command.
     *
     * @param sender Command sender
     */
    private void handleList(@NotNull CommandSender sender) {
        sendMessage(sender, "&8&m-----------&r &6Генераторы &8&m-----------");
        for (String id : plugin.getConfigManager().getGeneratorTypes().keySet()) {
            GeneratorType type = plugin.getConfigManager().getGeneratorType(id);
            if (type != null) {
                sendMessage(sender, "&7- &e" + id + " &7(" + Utils.formatMaterial(type.getBlockMaterial().name()) + ")");
            }
        }
        sendMessage(sender, "&7Всего: &f" + plugin.getConfigManager().getGeneratorTypes().size());
    }

    /**
     * Handles /factory reload command.
     *
     * @param sender Command sender
     */
    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission(Constants.Permissions.RELOAD)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return;
        }
        try {
            plugin.reload();
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Перезагружено!");
            Logger.info(sender.getName() + " reloaded the plugin");
        } catch (Exception e) {
            Logger.error("Error reloading plugin", e);
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Ошибка при перезагрузке!");
        }
    }

    /**
     * Handles /factory info command.
     *
     * @param sender Command sender
     */
    private void handleInfo(@NotNull CommandSender sender) {
        sendMessage(sender, "&8&m-----------&r &6IronFactory &8&m-----------");
        sendMessage(sender, "&7Версия: &f" + plugin.getDescription().getVersion());
        sendMessage(sender, "&7Генераторов: &f" + plugin.getGeneratorManager().getPlacedGenerators().size());
        sendMessage(sender, "&7Буровых: &f" + plugin.getMultiBlockManager().getStructures().size());
    }

    /**
     * Handles /factory debug command.
     *
     * @param sender Command sender
     */
    private void handleDebug(@NotNull CommandSender sender) {
        if (!sender.hasPermission(Constants.Permissions.ADMIN)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return;
        }
        sendMessage(sender, "&6=== ДЕБАГ ===");
        sendMessage(sender, "&7Генераторов: &f" + plugin.getGeneratorManager().getPlacedGenerators().size());
        sendMessage(sender, "&7Буровых: &f" + plugin.getMultiBlockManager().getStructures().size());
        plugin.getHologramManager().debugHolograms();
    }

    /**
     * Handles /factory setdelay command.
     *
     * @param sender Command sender
     * @param args Command arguments
     */
    private void handleSetDelay(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(Constants.Permissions.ADMIN)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "&c/factory setdelay <секунды>");
            return;
        }
        try {
            int seconds = Utils.clamp(Integer.parseInt(args[1]), 1, 3600);
            for (GeneratorType type : plugin.getConfigManager().getGeneratorTypes().values()) {
                type.setDelay(seconds * Constants.Timing.TICKS_PER_SECOND);
            }
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Задержка: " + seconds + " сек");
            Logger.info(sender.getName() + " changed generator delay to " + seconds + "s");
        } catch (NumberFormatException e) {
            Logger.warn("Invalid delay value: " + args[1]);
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неверное число!");
        }
    }

    /**
     * Handles /factory generate command.
     *
     * @param sender Command sender
     */
    private void handleGenerate(@NotNull CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Только для игроков!");
            return;
        }
        Player player = (Player) sender;
        Location loc = player.getTargetBlock(null, 10).getLocation();
        PlacedGenerator generator = plugin.getGeneratorManager().getGeneratorAt(loc);
        if (generator == null) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Смотрите на генератор!");
            return;
        }
        GeneratorType type = plugin.getConfigManager().getGeneratorType(generator.getTypeId());
        if (type != null) {
            generator.setCurrentTick(type.getDelay());
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Генератор сработает!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 0) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "give", "drill", "drilldebug", "list", "reload", "info", "debug"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("drill")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getConfigManager().getGeneratorTypes().keySet());
            } else if (args[0].equalsIgnoreCase("drill")) {
                completions.addAll(Arrays.asList("drill", "pipe", "pump"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }

}