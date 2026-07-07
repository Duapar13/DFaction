package com.dfaction.manager;

import com.dfaction.model.FPlayer;
import com.dfaction.storage.FactionStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PowerManager {

    private final JavaPlugin plugin;
    private final FactionStorage storage;

    private final Map<UUID, FPlayer> players = new HashMap<>();
    private final Map<String, UUID> nameToUuid = new HashMap<>();

    private double maxPower;
    private double startingPower;
    private double regenAmount;
    private double lossPerDeath;

    public PowerManager(JavaPlugin plugin, FactionStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.maxPower = cfg.getDouble("power.max", 10);
        this.startingPower = cfg.getDouble("power.starting", 10);
        this.regenAmount = cfg.getDouble("power.regen-amount", 1);
        this.lossPerDeath = cfg.getDouble("power.loss-per-death", 2);
    }

    public void seed(Map<UUID, FPlayer> loaded) {
        players.clear();
        nameToUuid.clear();
        players.putAll(loaded);
        for (FPlayer fp : loaded.values()) {
            nameToUuid.put(fp.getName().toLowerCase(), fp.getUuid());
        }
    }

    public void startRegenTask() {
        long intervalTicks = Math.max(1, plugin.getConfig().getLong("power.regen-interval-minutes", 10)) * 60L * 20L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (FPlayer fp : players.values()) {
                if (fp.getPower() < maxPower) {
                    fp.setPower(Math.min(maxPower, fp.getPower() + regenAmount));
                    storage.savePlayer(fp);
                }
            }
        }, intervalTicks, intervalTicks);
    }

    public FPlayer getOrCreate(Player player) {
        FPlayer fp = players.get(player.getUniqueId());
        if (fp == null) {
            fp = new FPlayer(player.getUniqueId(), player.getName(), startingPower);
            players.put(fp.getUuid(), fp);
            nameToUuid.put(fp.getName().toLowerCase(), fp.getUuid());
            storage.savePlayer(fp);
        } else if (!fp.getName().equals(player.getName())) {
            nameToUuid.remove(fp.getName().toLowerCase());
            fp.setName(player.getName());
            nameToUuid.put(fp.getName().toLowerCase(), fp.getUuid());
            storage.savePlayer(fp);
        }
        return fp;
    }

    public FPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public FPlayer getByName(String name) {
        UUID uuid = nameToUuid.get(name.toLowerCase());
        return uuid == null ? null : players.get(uuid);
    }

    public double getMaxPower() {
        return maxPower;
    }

    public void onDeath(FPlayer fPlayer) {
        fPlayer.setPower(Math.max(0, fPlayer.getPower() - lossPerDeath));
        storage.savePlayer(fPlayer);
    }

    public void save(FPlayer fPlayer) {
        storage.savePlayer(fPlayer);
    }
}
