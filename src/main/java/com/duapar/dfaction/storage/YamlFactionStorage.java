package com.duapar.dfaction.storage;

import com.duapar.dfaction.model.ClaimKey;
import com.duapar.dfaction.model.FPlayer;
import com.duapar.dfaction.model.Faction;
import com.duapar.dfaction.model.Role;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class YamlFactionStorage implements FactionStorage {

    private final File factionsFile;
    private final File playersFile;
    private final Logger logger;

    private YamlConfiguration factionsConfig;
    private YamlConfiguration playersConfig;

    public YamlFactionStorage(File dataFolder, Logger logger) {
        File dir = new File(dataFolder, "data");
        this.factionsFile = new File(dir, "factions.yml");
        this.playersFile = new File(dir, "players.yml");
        this.logger = logger;
    }

    @Override
    public void init() throws IOException {
        File dir = factionsFile.getParentFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Impossible de créer le dossier de données " + dir);
        }
        if (!factionsFile.exists() && !factionsFile.createNewFile()) {
            throw new IOException("Impossible de créer " + factionsFile);
        }
        if (!playersFile.exists() && !playersFile.createNewFile()) {
            throw new IOException("Impossible de créer " + playersFile);
        }
        factionsConfig = YamlConfiguration.loadConfiguration(factionsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }

    @Override
    public Map<String, Faction> loadFactions() {
        Map<String, Faction> result = new HashMap<>();
        ConfigurationSection root = factionsConfig.getConfigurationSection("factions");
        if (root == null) {
            return result;
        }
        for (String nameLower : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(nameLower);
            if (section == null) continue;

            String displayName = section.getString("name", nameLower);
            String description = section.getString("description", "Aucune description définie.");
            long createdAt = section.getLong("createdAt", System.currentTimeMillis());

            Faction faction = new Faction(displayName, description, createdAt);

            ConfigurationSection membersSection = section.getConfigurationSection("members");
            if (membersSection != null) {
                for (String uuidStr : membersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        Role role = Role.valueOf(membersSection.getString(uuidStr, "MEMBER"));
                        faction.getMembers().put(uuid, role);
                    } catch (IllegalArgumentException ex) {
                        logger.log(Level.WARNING, "UUID/role invalide pour la faction " + nameLower + ": " + uuidStr);
                    }
                }
            }

            List<String> claims = section.getStringList("claims");
            for (String serialized : claims) {
                try {
                    faction.getClaims().add(ClaimKey.fromString(serialized));
                } catch (RuntimeException ex) {
                    logger.log(Level.WARNING, "Claim invalide pour la faction " + nameLower + ": " + serialized);
                }
            }

            result.put(nameLower, faction);
        }
        return result;
    }

    @Override
    public Map<UUID, FPlayer> loadPlayers() {
        Map<UUID, FPlayer> result = new HashMap<>();
        ConfigurationSection root = playersConfig.getConfigurationSection("players");
        if (root == null) {
            return result;
        }
        for (String uuidStr : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(uuidStr);
            if (section == null) continue;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String name = section.getString("name", "Inconnu");
                double power = section.getDouble("power", 0);
                FPlayer fPlayer = new FPlayer(uuid, name, power);
                String faction = section.getString("faction", "");
                if (faction != null && !faction.isEmpty()) {
                    fPlayer.setFactionNameLower(faction);
                }
                result.put(uuid, fPlayer);
            } catch (IllegalArgumentException ex) {
                logger.log(Level.WARNING, "UUID joueur invalide: " + uuidStr);
            }
        }
        return result;
    }

    @Override
    public synchronized void saveFaction(Faction faction) {
        String base = "factions." + faction.getNameLower();
        factionsConfig.set(base + ".name", faction.getName());
        factionsConfig.set(base + ".description", faction.getDescription());
        factionsConfig.set(base + ".createdAt", faction.getCreatedAt());

        factionsConfig.set(base + ".members", null);
        for (Map.Entry<UUID, Role> entry : faction.getMembers().entrySet()) {
            factionsConfig.set(base + ".members." + entry.getKey(), entry.getValue().name());
        }

        List<String> claims = faction.getClaims().stream().map(ClaimKey::serialize).collect(java.util.stream.Collectors.toList());
        factionsConfig.set(base + ".claims", claims);

        saveFactionsFile();
    }

    @Override
    public synchronized void deleteFaction(String nameLower) {
        factionsConfig.set("factions." + nameLower, null);
        saveFactionsFile();
    }

    @Override
    public synchronized void savePlayer(FPlayer fPlayer) {
        String base = "players." + fPlayer.getUuid();
        playersConfig.set(base + ".name", fPlayer.getName());
        playersConfig.set(base + ".power", fPlayer.getPower());
        playersConfig.set(base + ".faction", fPlayer.hasFaction() ? fPlayer.getFactionNameLower() : "");
        savePlayersFile();
    }

    @Override
    public void close() {
        saveFactionsFile();
        savePlayersFile();
    }

    private void saveFactionsFile() {
        try {
            factionsConfig.save(factionsFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossible de sauvegarder " + factionsFile, e);
        }
    }

    private void savePlayersFile() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Impossible de sauvegarder " + playersFile, e);
        }
    }
}
