package com.dfaction.service;

import com.dapi.service.FactionService;
import com.dfaction.manager.FactionManager;
import com.dfaction.manager.PowerManager;
import com.dfaction.model.FPlayer;
import com.dfaction.model.Faction;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Implémentation de FactionService (contrat DAPI) adossée aux managers internes de DFaction.
 * Publiée auprès de DAPI dans DFaction#onEnable() pour que d'autres plugins D(nom)
 * puissent interroger l'état des factions sans dépendre de ce plugin.
 */
public class DFactionServiceImpl implements FactionService {

    private final FactionManager factionManager;
    private final PowerManager powerManager;

    public DFactionServiceImpl(FactionManager factionManager, PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @Override
    public boolean hasFaction(UUID playerId) {
        FPlayer fp = powerManager.get(playerId);
        return fp != null && fp.hasFaction();
    }

    @Override
    public String getFactionName(UUID playerId) {
        FPlayer fp = powerManager.get(playerId);
        Faction faction = factionManager.getFactionOf(fp);
        return faction == null ? null : faction.getName();
    }

    @Override
    public boolean isClaimed(Location location) {
        return factionManager.getFactionAt(location.getChunk()) != null;
    }

    @Override
    public String getClaimOwner(Location location) {
        Faction faction = factionManager.getFactionAt(location.getChunk());
        return faction == null ? null : faction.getName();
    }

    @Override
    public boolean isSameFaction(UUID playerId, Location location) {
        Faction claimFaction = factionManager.getFactionAt(location.getChunk());
        if (claimFaction == null) {
            return false;
        }
        FPlayer fp = powerManager.get(playerId);
        return fp != null && fp.hasFaction() && fp.getFactionNameLower().equals(claimFaction.getNameLower());
    }

    @Override
    public double getPower(UUID playerId) {
        FPlayer fp = powerManager.get(playerId);
        return fp == null ? 0 : fp.getPower();
    }

    @Override
    public double getMaxPower() {
        return powerManager.getMaxPower();
    }
}
