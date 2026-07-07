package com.dfaction;

import com.dfaction.commands.FCommand;
import com.dfaction.listeners.ClaimDisplayListener;
import com.dfaction.listeners.ExplosionListener;
import com.dfaction.listeners.PlayerDataListener;
import com.dfaction.listeners.ProtectionListener;
import com.dapi.DAPI;
import com.dapi.service.FactionService;
import com.dfaction.manager.FactionManager;
import com.dfaction.manager.PowerManager;
import com.dfaction.service.DFactionServiceImpl;
import com.dfaction.storage.FactionStorage;
import com.dfaction.storage.MySQLFactionStorage;
import com.dfaction.storage.YamlFactionStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DFaction extends JavaPlugin {

    private FactionStorage storage;
    private FactionManager factionManager;
    private PowerManager powerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String storageType = getConfig().getString("storage.type", "local");
        if ("mysql".equalsIgnoreCase(storageType)) {
            storage = new MySQLFactionStorage(this,
                    getConfig().getString("storage.mysql.host", "localhost"),
                    getConfig().getInt("storage.mysql.port", 3306),
                    getConfig().getString("storage.mysql.database", "dfaction"),
                    getConfig().getString("storage.mysql.username", "root"),
                    getConfig().getString("storage.mysql.password", ""),
                    getConfig().getBoolean("storage.mysql.useSSL", false));
        } else {
            storage = new YamlFactionStorage(getDataFolder(), getLogger());
        }

        try {
            storage.init();
        } catch (Exception e) {
            getLogger().severe("Impossible d'initialiser le stockage (" + storageType + "): " + e.getMessage());
            getLogger().severe("Le plugin va se désactiver.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        powerManager = new PowerManager(this, storage);
        powerManager.loadConfig();
        factionManager = new FactionManager(this, storage, powerManager);
        factionManager.loadConfig();

        try {
            powerManager.seed(storage.loadPlayers());
            factionManager.seed(storage.loadFactions());
        } catch (Exception e) {
            getLogger().severe("Erreur lors du chargement des données existantes: " + e.getMessage());
        }

        powerManager.startRegenTask();

        FCommand fCommand = new FCommand(factionManager, powerManager);
        PluginCommand command = getCommand("f");
        if (command != null) {
            command.setExecutor(fCommand);
            command.setTabCompleter(fCommand);
        }

        getServer().getPluginManager().registerEvents(new ProtectionListener(factionManager, powerManager), this);
        getServer().getPluginManager().registerEvents(new ExplosionListener(factionManager, this), this);
        getServer().getPluginManager().registerEvents(new ClaimDisplayListener(factionManager, powerManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDataListener(powerManager), this);

        DAPI.registerPlugin(this, "FactionService");
        DAPI.registerService(FactionService.class, new DFactionServiceImpl(factionManager, powerManager), this);

        getLogger().info("DFaction activé (stockage: " + storageType + ").");
    }

    @Override
    public void onDisable() {
        if (storage != null) {
            storage.close();
        }
    }
}
