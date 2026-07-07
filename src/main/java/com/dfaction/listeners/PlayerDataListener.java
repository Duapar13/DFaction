package com.dfaction.listeners;

import com.dfaction.manager.PowerManager;
import com.dfaction.model.FPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerDataListener implements Listener {

    private final PowerManager powerManager;

    public PlayerDataListener(PowerManager powerManager) {
        this.powerManager = powerManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        powerManager.getOrCreate(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        FPlayer fp = powerManager.getOrCreate(event.getEntity());
        powerManager.onDeath(fp);
    }
}
