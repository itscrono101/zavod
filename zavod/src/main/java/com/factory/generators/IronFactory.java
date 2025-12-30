package com.factory.generators;

import com.factory.generators.commands.FactoryCommand;
import com.factory.generators.listeners.GeneratorListener;
import com.factory.generators.listeners.PlayerListener;
import com.factory.generators.managers.ConfigManager;
import com.factory.generators.managers.GeneratorManager;
import com.factory.generators.managers.HologramManager;
import com.factory.generators.managers.DataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class IronFactory extends JavaPlugin {

    private static IronFactory instance;
    private ConfigManager configManager;
    private GeneratorManager generatorManager;
    private HologramManager hologramManager;
    private DataManager dataManager;
    private int taskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        // Инициализация менеджеров
        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.generatorManager = new GeneratorManager(this);
        this.hologramManager = new HologramManager(this);

        // Загрузка данных
        configManager.loadConfigs();
        dataManager.loadGenerators();
        dataManager.startAutoSave();

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Регистрация команд
        getCommand("factory").setExecutor(new FactoryCommand(this));
        getCommand("factory").setTabCompleter(new FactoryCommand(this));

        // Запуск таска генерации
        startGeneratorTask();

        getLogger().info("§a[IronFactory] Плагин успешно загружен!");
        getLogger().info("§a[IronFactory] Загружено типов генераторов: " + generatorManager.getGeneratorTypes().size());
        getLogger().info("§a[IronFactory] Размещено генераторов: " + generatorManager.getPlacedGenerators().size());
    }

    @Override
    public void onDisable() {
        // Остановка таска
        if (taskId != -1) {
            getServer().getScheduler().cancelTask(taskId);
        }

        // Сохранение данных
        if (dataManager != null) {
            dataManager.saveGenerators();
        }

        // Удаление голограмм
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }

        getLogger().info("§c[IronFactory] Плагин выключен!");
    }

    private void startGeneratorTask() {
        taskId = getServer().getScheduler().runTaskTimer(this, () -> {
            generatorManager.tickAllGenerators();
        }, 20L, 20L).getTaskId();
    }

    public void reload() {
        configManager.loadConfigs();
        dataManager.loadGenerators();
        hologramManager.refreshAllHolograms();
    }

    public static IronFactory getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GeneratorManager getGeneratorManager() {
        return generatorManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }
}