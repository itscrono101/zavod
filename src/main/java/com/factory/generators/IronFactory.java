package com.factory.generators;

import com.factory.generators.commands.CasinoCommand;
import com.factory.generators.commands.DatabaseCommand;
import com.factory.generators.commands.FactoryCommand;
import com.factory.generators.listeners.CasinoListener;
import com.factory.generators.listeners.GeneratorListener;
import com.factory.generators.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class IronFactory extends JavaPlugin {

    private static IronFactory instance;
    private ConfigManager configManager;
    private GeneratorManager generatorManager;
    private HologramManager hologramManager;
    private DataManager dataManager;
    private MultiBlockManager multiBlockManager;
    private EventManager eventManager;
    private CasinoManager casinoManager;
    private int taskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.hologramManager = new HologramManager(this);
        this.generatorManager = new GeneratorManager(this);
        this.multiBlockManager = new MultiBlockManager(this);
        this.eventManager = new EventManager(this);
        this.casinoManager = new CasinoManager(this, configManager);

        configManager.loadConfigs();
        dataManager.loadGenerators();
        dataManager.loadMultiBlockStructures();
        dataManager.startAutoSave();
        casinoManager.loadCasinoBlocks();

        getServer().getPluginManager().registerEvents(new GeneratorListener(this), this);
        getServer().getPluginManager().registerEvents(new CasinoListener(this), this);

        // Register main command
        getCommand("factory").setExecutor(new FactoryCommand(this));
        getCommand("factory").setTabCompleter(new FactoryCommand(this));

        // Register database commands
        DatabaseCommand dbCommand = new DatabaseCommand(this);
        getCommand("dbexport").setExecutor(dbCommand);
        getCommand("dbexport").setTabCompleter(dbCommand);
        getCommand("dbimport").setExecutor(dbCommand);
        getCommand("dbimport").setTabCompleter(dbCommand);
        getCommand("dbbackup").setExecutor(dbCommand);
        getCommand("dbbackup").setTabCompleter(dbCommand);
        getCommand("dbrestore").setExecutor(dbCommand);
        getCommand("dbrestore").setTabCompleter(dbCommand);
        getCommand("dblist").setExecutor(dbCommand);
        getCommand("dblist").setTabCompleter(dbCommand);

        // Register casino command
        CasinoCommand casinoCommand = new CasinoCommand(this);
        getCommand("casino").setExecutor(casinoCommand);

        startTask();

        getLogger().info("§a[IronFactory] Загружено!");
        getLogger().info("§a[IronFactory] Команды зарегистрированы: /factory, /dbexport, /dbimport, /dbbackup, /dbrestore, /dblist, /casino");
        getLogger().info("§a[IronFactory] Генераторов: " + generatorManager.getPlacedGenerators().size());
        getLogger().info("§a[IronFactory] Буровых: " + multiBlockManager.getStructures().size());
    }

    @Override
    public void onDisable() {
        if (taskId != -1) getServer().getScheduler().cancelTask(taskId);
        if (dataManager != null) {
            dataManager.saveGenerators();
            dataManager.saveMultiBlockStructures();
        }
        if (hologramManager != null) hologramManager.removeAllHolograms();
        getLogger().info("§c[IronFactory] Выключен!");
    }

    private void startTask() {
        taskId = getServer().getScheduler().runTaskTimer(this, () -> {
            generatorManager.tickAllGenerators();
            multiBlockManager.tickAll();
            eventManager.tickAilments();
            eventManager.checkEarthquake();
        }, 20L, 20L).getTaskId();
    }

    public void reload() {
        configManager.loadConfigs();
        dataManager.loadGenerators();
        dataManager.loadMultiBlockStructures();
        hologramManager.refreshAllHolograms();
    }

    public static IronFactory getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public GeneratorManager getGeneratorManager() { return generatorManager; }
    public HologramManager getHologramManager() { return hologramManager; }
    public DataManager getDataManager() { return dataManager; }
    public MultiBlockManager getMultiBlockManager() { return multiBlockManager; }
    public EventManager getEventManager() { return eventManager; }
    public CasinoManager getCasinoManager() { return casinoManager; }
}