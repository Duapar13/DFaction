package com.duapar.dfaction.listeners;

import com.duapar.dfaction.manager.FactionManager;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionListener implements Listener {

    private final FactionManager factionManager;
    private final JavaPlugin plugin;

    public ExplosionListener(FactionManager factionManager, JavaPlugin plugin) {
        this.factionManager = factionManager;
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        boolean onlyTntBreaksClaims = plugin.getConfig().getBoolean("claims.only-tnt-breaks-claims", true);
        boolean isTnt = event.getEntity() != null && event.getEntity().getType() == EntityType.TNT;

        if (isTnt && onlyTntBreaksClaims) {
            // La TNT bypass entièrement la protection des claims (mécanique de raid).
            return;
        }

        event.blockList().removeIf(block -> factionManager.getFactionAt(block.getChunk()) != null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        // Explosions sans entité source (lit, ancre de résurrection...) : jamais autorisées dans un claim.
        event.blockList().removeIf(block -> factionManager.getFactionAt(block.getChunk()) != null);
    }
}
