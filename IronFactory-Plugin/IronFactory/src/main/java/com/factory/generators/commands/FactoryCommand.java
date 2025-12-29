package com.factory.generators.commands;

import com.factory.generators.IronFactory;
import com.factory.generators.models.GeneratorType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        
        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
                
            case "give":
                handleGive(sender, args);
                break;
                
            case "list":
                handleList(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "info":
                handleInfo(sender);
                break;
                
            default:
                sender.sendMessage(colorize("&c[Завод] Неизвестная команда. Используйте /factory help"));
        }
        
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&8&m-----------&r &6⚙ IronFactory &8&m-----------"));
        sender.sendMessage(colorize("&e/factory give <игрок> <генератор> [кол-во] &7- Выдать генератор"));
        sender.sendMessage(colorize("&e/factory list &7- Список генераторов"));
        sender.sendMessage(colorize("&e/factory info &7- Информация"));
        sender.sendMessage(colorize("&e/factory reload &7- Перезагрузить конфиг"));
        sender.sendMessage(colorize("&8&m------------------------------------"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("factory.give")) {
            sender.sendMessage(colorize("&c[Завод] Недостаточно прав!"));
            return;
        }
        
        if (args.length < 3) {
            sender.sendMessage(colorize("&c[Завод] Использование: /factory give <игрок> <генератор> [кол-во]"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(colorize("&c[Завод] Игрок не найден!"));
            return;
        }
        
        String generatorId = args[2];
        GeneratorType type = plugin.getConfigManager().getGeneratorType(generatorId);
        
        if (type == null) {
            sender.sendMessage(colorize("&c[Завод] Генератор не найден: " + generatorId));
            sender.sendMessage(colorize("&7Доступные: " + String.join(", ", plugin.getConfigManager().getGeneratorTypes().keySet())));
            return;
        }
        
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize("&c[Завод] Неверное количество!"));
                return;
            }
        }
        
        target.getInventory().addItem(type.createItem(amount));
        
        sender.sendMessage(colorize("&a[Завод] Выдан генератор &f" + generatorId + " &ax" + amount + " &aигроку &f" + target.getName()));
        target.sendMessage(colorize("&a[Завод] Вы получили генератор: &f" + type.getName()));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(colorize("&8&m-----------&r &6Список генераторов &8&m-----------"));
        
        for (String id : plugin.getConfigManager().getGeneratorTypes().keySet()) {
            GeneratorType type = plugin.getConfigManager().getGeneratorType(id);
            sender.sendMessage(colorize("&7- &e" + id + " &7(" + type.getBlockMaterial().name() + ")"));
        }
        
        sender.sendMessage(colorize("&8&m-----------------------------------------"));
        sender.sendMessage(colorize("&7Всего: &f" + plugin.getConfigManager().getGeneratorTypes().size()));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("factory.reload")) {
            sender.sendMessage(colorize("&c[Завод] Недостаточно прав!"));
            return;
        }
        
        plugin.reload();
        sender.sendMessage(colorize("&a[Завод] Конфигурация перезагружена!"));
    }

    private void handleInfo(CommandSender sender) {
        sender.sendMessage(colorize("&8&m-----------&r &6IronFactory &8&m-----------"));
        sender.sendMessage(colorize("&7Версия: &f" + plugin.getDescription().getVersion()));
        sender.sendMessage(colorize("&7Автор: &f" + plugin.getDescription().getAuthors()));
        sender.sendMessage(colorize("&7Генераторов загружено: &f" + plugin.getConfigManager().getGeneratorTypes().size()));
        sender.sendMessage(colorize("&7Размещено генераторов: &f" + plugin.getGeneratorManager().getPlacedGenerators().size()));
        sender.sendMessage(colorize("&8&m-----------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "give", "list", "reload", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(plugin.getConfigManager().getGeneratorTypes().keySet());
        }
        
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        
        return completions;
    }

    private String colorize(String text) {
        return text.replace("&", "§");
    }
}
