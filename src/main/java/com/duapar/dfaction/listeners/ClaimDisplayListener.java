package com.duapar.dfaction.listeners;

import com.duapar.dfaction.manager.FactionManager;
import com.duapar.dfaction.manager.PowerManager;
import com.duapar.dfaction.model.FPlayer;
import com.duapar.dfaction.model.Faction;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class ClaimDisplayListener implements Listener {

    private final FactionManager factionManager;
    private final PowerManager powerManager;

    public ClaimDisplayListener(FactionManager factionManager, PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || !changedChunk(from, to)) {
            return;
        }

        Faction oldFaction = factionManager.getFactionAt(from.getChunk());
        Faction newFaction = factionManager.getFactionAt(to.getChunk());
        if (oldFaction == newFaction) {
            return;
        }

        Player player = event.getPlayer();
        String title;
        String subtitle;

        if (newFaction == null) {
            title = ChatColor.GRAY + "" + ChatColor.BOLD + "Zone sauvage";
            subtitle = ChatColor.DARK_GRAY + "Territoire non revendiqué";
        } else {
            FPlayer fp = powerManager.get(player.getUniqueId());
            boolean own = fp != null && fp.hasFaction() && fp.getFactionNameLower().equals(newFaction.getNameLower());
            if (own) {
                title = ChatColor.GREEN + "" + ChatColor.BOLD + newFaction.getName();
                subtitle = ChatColor.GREEN + "Votre territoire";
            } else {
                title = ChatColor.RED + "" + ChatColor.BOLD + newFaction.getName();
                subtitle = ChatColor.RED + "Territoire ennemi";
            }
        }

        player.sendTitle(title, subtitle, 10, 40, 10);
    }

    private boolean changedChunk(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return true;
        }
        return (from.getBlockX() >> 4) != (to.getBlockX() >> 4) || (from.getBlockZ() >> 4) != (to.getBlockZ() >> 4);
    }
}
