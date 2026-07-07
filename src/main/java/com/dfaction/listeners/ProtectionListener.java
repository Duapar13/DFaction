package com.dfaction.listeners;

import com.dfaction.manager.FactionManager;
import com.dfaction.manager.PowerManager;
import com.dfaction.model.FPlayer;
import com.dfaction.model.Faction;
import com.dfaction.util.Msg;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProtectionListener implements Listener {

    // Postes de travail sans inventaire persistant (donc pas d'InventoryHolder)
    // mais que l'on protège quand même explicitement contre l'usage par un non-membre.
    private static final Set<Material> PROTECTED_WORKSTATIONS = EnumSet.of(
            Material.CRAFTING_TABLE,
            Material.ANVIL,
            Material.CHIPPED_ANVIL,
            Material.DAMAGED_ANVIL,
            Material.ENCHANTING_TABLE,
            Material.GRINDSTONE,
            Material.CARTOGRAPHY_TABLE,
            Material.LOOM,
            Material.STONECUTTER,
            Material.SMITHING_TABLE,
            Material.BEACON,
            Material.LECTERN,
            Material.JUKEBOX,
            Material.ENDER_CHEST
    );

    private static final long DENY_MESSAGE_COOLDOWN_MS = 2000L;

    private final FactionManager factionManager;
    private final PowerManager powerManager;
    private final Map<UUID, Long> lastDenyMessage = new HashMap<>();

    public ProtectionListener(FactionManager factionManager, PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Faction faction = factionManager.getFactionAt(event.getBlock().getChunk());
        if (faction == null || isMember(player, faction)) {
            return;
        }
        event.setCancelled(true);
        denyMessage(player, faction);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Faction faction = factionManager.getFactionAt(event.getBlock().getChunk());
        if (faction == null || isMember(player, faction)) {
            return;
        }
        event.setCancelled(true);
        denyMessage(player, faction);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isProtectedInteractable(block)) {
            return;
        }
        Faction faction = factionManager.getFactionAt(block.getChunk());
        if (faction == null || isMember(event.getPlayer(), faction)) {
            return;
        }
        event.setCancelled(true);
        denyMessage(event.getPlayer(), faction);
    }

    private boolean isProtectedInteractable(Block block) {
        if (PROTECTED_WORKSTATIONS.contains(block.getType())) {
            return true;
        }
        return block.getState() instanceof InventoryHolder;
    }

    private boolean isMember(Player player, Faction faction) {
        FPlayer fp = powerManager.get(player.getUniqueId());
        return fp != null && fp.hasFaction() && fp.getFactionNameLower().equals(faction.getNameLower());
    }

    private void denyMessage(Player player, Faction faction) {
        long now = System.currentTimeMillis();
        Long last = lastDenyMessage.get(player.getUniqueId());
        if (last != null && now - last < DENY_MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastDenyMessage.put(player.getUniqueId(), now);
        Msg.error(player, "Vous ne pouvez pas faire ça ici, cette zone appartient à la faction " + faction.getName() + ".");
    }
}
