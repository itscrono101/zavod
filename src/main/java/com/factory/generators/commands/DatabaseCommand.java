package com.factory.generators.commands;

import com.factory.generators.IronFactory;
import com.factory.generators.database.DataExporter;
import com.factory.generators.database.DataImporter;
import com.factory.generators.utils.Constants;
import com.factory.generators.utils.Logger;
import com.factory.generators.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commands for database management: export, import, backup, restore.
 * Usage: /dbexport, /dbimport, /dbbackup, /dbrestore
 */
public class DatabaseCommand implements CommandExecutor, TabCompleter {

    private final IronFactory plugin;
    private final DataExporter exporter;
    private final DataImporter importer;

    public DatabaseCommand(@NotNull IronFactory plugin) {
        this.plugin = plugin;
        this.exporter = new DataExporter(plugin);
        this.importer = new DataImporter(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission(Constants.Permissions.ADMIN)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Недостаточно прав!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "export":
                handleExport(sender, args);
                break;
            case "import":
                handleImport(sender, args);
                break;
            case "backup":
                handleBackup(sender);
                break;
            case "restore":
                handleRestore(sender, args);
                break;
            case "list":
                handleListBackups(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            default:
                sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неизвестная команда!");
                sendHelp(sender);
        }

        return true;
    }

    /**
     * Exports data to backup file.
     */
    private void handleExport(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&c/dbexport <generators|multiblock|all>");
            return;
        }

        String type = args[1].toLowerCase();
        boolean success = false;

        switch (type) {
            case "generators":
                success = exporter.exportGenerators();
                break;
            case "multiblock":
                success = exporter.exportMultiBlocks();
                break;
            case "all":
                success = exporter.exportAll();
                break;
            default:
                sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неверный тип данных!");
                return;
        }

        if (success) {
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Данные экспортированы!");
            Logger.info(sender.getName() + " exported " + type + " data");
        } else {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Ошибка при экспорте!");
        }
    }

    /**
     * Imports data from backup file.
     */
    private void handleImport(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "&c/dbimport <filename> [merge|replace]");
            return;
        }

        String filename = args[1];
        boolean merge = args.length >= 3 && args[2].equalsIgnoreCase("merge");

        File backupDir = exporter.getBackupDirectory();
        File backupFile = new File(backupDir, filename);

        if (!backupFile.exists()) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Файл не найден: " + filename);
            sendMessage(sender, "&7Используйте /dbexport list для просмотра доступных файлов");
            return;
        }

        if (!importer.isValidBackup(backupFile)) {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Неверный формат файла!");
            return;
        }

        int imported = importer.importAll(backupFile, merge);

        if (imported > 0) {
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Импортировано " + imported + " объектов!");
            Logger.info(sender.getName() + " imported " + imported + " objects from " + filename);
        } else {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Ошибка при импорте!");
        }
    }

    /**
     * Creates backup of all data.
     */
    private void handleBackup(@NotNull CommandSender sender) {
        sendMessage(sender, Constants.Messages.INFO_PREFIX + "Создание резервной копии...");

        boolean success = exporter.exportAll();

        if (success) {
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Резервная копия создана!");
            Logger.info(sender.getName() + " created full backup");
        } else {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Ошибка при создании резервной копии!");
        }
    }

    /**
     * Restores from latest backup.
     */
    private void handleRestore(@NotNull CommandSender sender, @NotNull String[] args) {
        String type = args.length >= 2 ? args[1].toLowerCase() : "all";

        File latestGen = importer.getLatestBackup("generators");
        File latestMulti = importer.getLatestBackup("multiblock");

        int imported = 0;

        if (("generators".equals(type) || "all".equals(type)) && latestGen != null) {
            imported += importer.importGenerators(latestGen, false);
        }

        if (("multiblock".equals(type) || "all".equals(type)) && latestMulti != null) {
            imported += importer.importMultiBlocks(latestMulti, false);
        }

        if (imported > 0) {
            sendMessage(sender, Constants.Messages.SUCCESS_PREFIX + "Восстановлено " + imported + " объектов!");
            Logger.info(sender.getName() + " restored " + imported + " objects");
        } else {
            sendMessage(sender, Constants.Messages.ERROR_PREFIX + "Нет доступных резервных копий!");
        }
    }

    /**
     * Lists all available backups.
     */
    private void handleListBackups(@NotNull CommandSender sender) {
        File[] backups = exporter.listBackups();

        sendMessage(sender, "&8&m-----------&r &6Резервные Копии &8&m-----------");

        if (backups == null || backups.length == 0) {
            sendMessage(sender, "&7Нет резервных копий");
            return;
        }

        for (File backup : backups) {
            long sizeKb = backup.length() / 1024;
            long lastMod = backup.lastModified();
            sendMessage(sender, "&7- &e" + backup.getName() + " &7(" + sizeKb + " KB)");
        }

        sendMessage(sender, "&7Всего: " + backups.length);
    }

    /**
     * Sends help message.
     */
    private void sendHelp(@NotNull CommandSender sender) {
        sendMessage(sender, "&8&m-----------&r &6Database Commands &8&m-----------");
        sendMessage(sender, "&e/dbexport <generators|multiblock|all> &7- Экспорт данных");
        sendMessage(sender, "&e/dbimport <filename> [merge|replace] &7- Импорт данных");
        sendMessage(sender, "&e/dbbackup &7- Создать полную резервную копию");
        sendMessage(sender, "&e/dbrestore [type] &7- Восстановить из последней копии");
        sendMessage(sender, "&e/dblist &7- Показать все резервные копии");
    }

    /**
     * Sends colored message to sender.
     */
    private void sendMessage(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(Utils.colorize(message));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 0) {
            return completions;
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("export", "import", "backup", "restore", "list", "help"));
        } else if (args.length == 2 && "export".equals(args[0].toLowerCase())) {
            completions.addAll(Arrays.asList("generators", "multiblock", "all"));
        } else if (args.length == 2 && "import".equals(args[0].toLowerCase())) {
            File[] backups = exporter.listBackups();
            if (backups != null) {
                for (File backup : backups) {
                    completions.add(backup.getName());
                }
            }
        } else if (args.length == 3 && "import".equals(args[0].toLowerCase())) {
            completions.addAll(Arrays.asList("merge", "replace"));
        } else if (args.length == 2 && "restore".equals(args[0].toLowerCase())) {
            completions.addAll(Arrays.asList("generators", "multiblock", "all"));
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
