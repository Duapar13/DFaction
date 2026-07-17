package com.duapar.dfaction.manager;

import com.duapar.dfaction.model.ClaimKey;
import com.duapar.dfaction.model.FPlayer;
import com.duapar.dfaction.model.Faction;
import com.duapar.dfaction.model.Role;
import com.duapar.dfaction.storage.FactionStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FactionManager {

    private final JavaPlugin plugin;
    private final FactionStorage storage;
    private final PowerManager powerManager;

    private final Map<String, Faction> factions = new HashMap<>();
    private final Map<ClaimKey, String> claimLookup = new HashMap<>();
    private final Map<UUID, Set<String>> invites = new HashMap<>();

    private int maxNameLength;
    private double claimCost;

    public FactionManager(JavaPlugin plugin, FactionStorage storage, PowerManager powerManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.powerManager = powerManager;
    }

    public void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();
        this.maxNameLength = cfg.getInt("faction.max-name-length", 10);
        this.claimCost = cfg.getDouble("power.claim-cost", 1.0);
    }

    public void seed(Map<String, Faction> loaded) {
        factions.clear();
        claimLookup.clear();
        factions.putAll(loaded);
        for (Faction faction : factions.values()) {
            for (ClaimKey claim : faction.getClaims()) {
                claimLookup.put(claim, faction.getNameLower());
            }
        }
    }

    public Faction getFactionOf(FPlayer fPlayer) {
        if (fPlayer == null || !fPlayer.hasFaction()) return null;
        return factions.get(fPlayer.getFactionNameLower());
    }

    public Faction getByNameLower(String nameLower) {
        return factions.get(nameLower);
    }

    public Faction getByName(String name) {
        return factions.get(name.toLowerCase());
    }

    public Faction getFactionAt(Chunk chunk) {
        String owner = claimLookup.get(keyOf(chunk));
        return owner == null ? null : factions.get(owner);
    }

    public double getTotalPower(Faction faction) {
        double total = 0;
        for (UUID uuid : faction.getMembers().keySet()) {
            FPlayer fp = powerManager.get(uuid);
            if (fp != null) {
                total += fp.getPower();
            }
        }
        return total;
    }

    public int getMaxClaims(Faction faction) {
        return (int) Math.floor(getTotalPower(faction) / claimCost);
    }

    private static ClaimKey keyOf(Chunk chunk) {
        return new ClaimKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    // ---------------------------------------------------------------- create

    public Faction createFaction(Player player, String rawName) {
        FPlayer fp = powerManager.getOrCreate(player);
        if (fp.hasFaction()) {
            throw new FactionException("Vous êtes déjà membre d'une faction.");
        }
        if (rawName == null || rawName.isEmpty()) {
            throw new FactionException("Utilisation: /f create <nom>");
        }
        if (rawName.length() > maxNameLength) {
            throw new FactionException("Le nom d'une faction ne peut pas dépasser " + maxNameLength + " caractères.");
        }
        if (!rawName.matches("^[A-Za-z0-9_]+$")) {
            throw new FactionException("Le nom d'une faction ne peut contenir que des lettres, chiffres et _.");
        }
        String lower = rawName.toLowerCase();
        if (factions.containsKey(lower)) {
            throw new FactionException("Ce nom de faction est déjà utilisé.");
        }

        Faction faction = new Faction(rawName, player.getUniqueId());
        factions.put(lower, faction);
        fp.setFactionNameLower(lower);

        storage.saveFaction(faction);
        powerManager.save(fp);
        return faction;
    }

    // ---------------------------------------------------------------- invite / join / leave / disband

    public Faction invite(Player owner, String targetName) {
        FPlayer ownerFp = powerManager.getOrCreate(owner);
        Faction faction = requireFactionAndRole(ownerFp, Role.OWNER, "Seul le chef de la faction peut inviter un joueur.");

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            throw new FactionException("Joueur introuvable ou hors ligne.");
        }
        FPlayer targetFp = powerManager.getOrCreate(target);
        if (targetFp.hasFaction()) {
            throw new FactionException("Ce joueur est déjà membre d'une faction.");
        }
        invites.computeIfAbsent(target.getUniqueId(), k -> new HashSet<>()).add(faction.getNameLower());
        return faction;
    }

    public Faction join(Player player, String factionName) {
        FPlayer fp = powerManager.getOrCreate(player);
        if (fp.hasFaction()) {
            throw new FactionException("Vous êtes déjà membre d'une faction.");
        }
        Faction faction = factions.get(factionName == null ? "" : factionName.toLowerCase());
        if (faction == null) {
            throw new FactionException("Cette faction n'existe pas.");
        }
        Set<String> pending = invites.get(player.getUniqueId());
        if (pending == null || !pending.contains(faction.getNameLower())) {
            throw new FactionException("Vous n'avez pas été invité dans cette faction.");
        }
        faction.getMembers().put(player.getUniqueId(), Role.MEMBER);
        fp.setFactionNameLower(faction.getNameLower());
        pending.remove(faction.getNameLower());

        storage.saveFaction(faction);
        powerManager.save(fp);
        return faction;
    }

    public void leave(Player player) {
        FPlayer fp = powerManager.getOrCreate(player);
        Faction faction = getFactionOf(fp);
        if (faction == null) {
            throw new FactionException("Vous n'êtes membre d'aucune faction.");
        }

        if (faction.getRole(player.getUniqueId()) == Role.OWNER) {
            if (faction.getMembers().size() == 1) {
                disbandInternal(faction);
                return;
            }
            UUID nextOwner = null;
            for (UUID uuid : faction.getMembers().keySet()) {
                if (!uuid.equals(player.getUniqueId())) {
                    nextOwner = uuid;
                    break;
                }
            }
            faction.getMembers().remove(player.getUniqueId());
            faction.getMembers().put(nextOwner, Role.OWNER);
            fp.setFactionNameLower(null);
            storage.saveFaction(faction);
            powerManager.save(fp);

            Player newOwnerPlayer = Bukkit.getPlayer(nextOwner);
            if (newOwnerPlayer != null) {
                com.duapar.dfaction.util.Msg.send(newOwnerPlayer, "Vous êtes désormais le chef de la faction " + faction.getName() + ".");
            }
        } else {
            faction.getMembers().remove(player.getUniqueId());
            fp.setFactionNameLower(null);
            storage.saveFaction(faction);
            powerManager.save(fp);
        }
    }

    public void disband(Player player) {
        FPlayer fp = powerManager.getOrCreate(player);
        Faction faction = requireFactionAndRole(fp, Role.OWNER, "Seul le chef de la faction peut la dissoudre.");
        disbandInternal(faction);
    }

    private void disbandInternal(Faction faction) {
        for (ClaimKey claim : faction.getClaims()) {
            claimLookup.remove(claim);
        }
        for (UUID uuid : faction.getMembers().keySet()) {
            FPlayer memberFp = powerManager.get(uuid);
            if (memberFp != null) {
                memberFp.setFactionNameLower(null);
                powerManager.save(memberFp);
            }
        }
        factions.remove(faction.getNameLower());
        storage.deleteFaction(faction.getNameLower());
    }

    // ---------------------------------------------------------------- claim / unclaim

    public Faction claim(Player player) {
        FPlayer fp = powerManager.getOrCreate(player);
        Faction faction = getFactionOf(fp);
        if (faction == null) {
            throw new FactionException("Vous n'êtes membre d'aucune faction.");
        }

        Chunk chunk = player.getLocation().getChunk();
        ClaimKey key = keyOf(chunk);
        String existingOwner = claimLookup.get(key);
        if (existingOwner != null) {
            if (existingOwner.equals(faction.getNameLower())) {
                throw new FactionException("Ce chunk est déjà claim par votre faction.");
            }
            Faction enemy = factions.get(existingOwner);
            throw new FactionException("Ce chunk appartient déjà à la faction " + (enemy != null ? enemy.getName() : existingOwner) + ".");
        }

        int maxClaims = getMaxClaims(faction);
        if (faction.getClaims().size() + 1 > maxClaims) {
            throw new FactionException("Votre faction n'a pas assez de power pour claim ce chunk (" + faction.getClaims().size() + "/" + maxClaims + ").");
        }

        faction.getClaims().add(key);
        claimLookup.put(key, faction.getNameLower());
        storage.saveFaction(faction);
        return faction;
    }

    public Faction unclaim(Player player) {
        FPlayer fp = powerManager.getOrCreate(player);
        Faction faction = getFactionOf(fp);
        if (faction == null) {
            throw new FactionException("Vous n'êtes membre d'aucune faction.");
        }

        Chunk chunk = player.getLocation().getChunk();
        ClaimKey key = keyOf(chunk);
        String owner = claimLookup.get(key);
        if (owner == null) {
            throw new FactionException("Ce chunk n'est pas claim.");
        }
        if (!owner.equals(faction.getNameLower())) {
            throw new FactionException("Ce chunk appartient à une autre faction.");
        }

        faction.getClaims().remove(key);
        claimLookup.remove(key);
        storage.saveFaction(faction);
        return faction;
    }

    // ---------------------------------------------------------------- helpers

    private Faction requireFactionAndRole(FPlayer fp, Role role, String errorIfWrongRole) {
        Faction faction = getFactionOf(fp);
        if (faction == null) {
            throw new FactionException("Vous n'êtes membre d'aucune faction.");
        }
        if (faction.getRole(fp.getUuid()) != role) {
            throw new FactionException(errorIfWrongRole);
        }
        return faction;
    }
}
