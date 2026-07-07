package com.dfaction.storage;

import com.dfaction.model.ClaimKey;
import com.dfaction.model.FPlayer;
import com.dfaction.model.Faction;
import com.dfaction.model.Role;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLFactionStorage implements FactionStorage {

    private final JavaPlugin plugin;
    private final String url;
    private final String username;
    private final String password;

    private Connection connection;

    public MySQLFactionStorage(JavaPlugin plugin, String host, int port, String database,
                                String username, String password, boolean useSSL) {
        this.plugin = plugin;
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=true&autoReconnect=true";
        this.username = username;
        this.password = password;
    }

    @Override
    public void init() throws Exception {
        // Force le chargement de la classe du driver (le nom réel est remappé par le shading Maven).
        Class.forName(com.mysql.cj.jdbc.Driver.class.getName());
        connect();
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dfaction_factions (" +
                    "name_lower VARCHAR(32) PRIMARY KEY," +
                    "display_name VARCHAR(32) NOT NULL," +
                    "description VARCHAR(255)," +
                    "created_at BIGINT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dfaction_members (" +
                    "faction VARCHAR(32) NOT NULL," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "role VARCHAR(16) NOT NULL," +
                    "PRIMARY KEY (faction, uuid))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dfaction_claims (" +
                    "world VARCHAR(64) NOT NULL," +
                    "x INT NOT NULL," +
                    "z INT NOT NULL," +
                    "faction VARCHAR(32) NOT NULL," +
                    "PRIMARY KEY (world, x, z))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS dfaction_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(16) NOT NULL," +
                    "faction VARCHAR(32)," +
                    "power DOUBLE NOT NULL)");
        }
    }

    private void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, username, password);
        }
    }

    @Override
    public Map<String, Faction> loadFactions() throws SQLException {
        Map<String, Faction> result = new HashMap<>();
        connect();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT name_lower, display_name, description, created_at FROM dfaction_factions")) {
            while (rs.next()) {
                Faction faction = new Faction(rs.getString("display_name"), rs.getString("description"), rs.getLong("created_at"));
                result.put(rs.getString("name_lower"), faction);
            }
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT faction, uuid, role FROM dfaction_members")) {
            while (rs.next()) {
                Faction faction = result.get(rs.getString("faction"));
                if (faction == null) continue;
                faction.getMembers().put(UUID.fromString(rs.getString("uuid")), Role.valueOf(rs.getString("role")));
            }
        }

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT faction, world, x, z FROM dfaction_claims")) {
            while (rs.next()) {
                Faction faction = result.get(rs.getString("faction"));
                if (faction == null) continue;
                faction.getClaims().add(new ClaimKey(rs.getString("world"), rs.getInt("x"), rs.getInt("z")));
            }
        }

        return result;
    }

    @Override
    public Map<UUID, FPlayer> loadPlayers() throws SQLException {
        Map<UUID, FPlayer> result = new HashMap<>();
        connect();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT uuid, name, faction, power FROM dfaction_players")) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                FPlayer fPlayer = new FPlayer(uuid, rs.getString("name"), rs.getDouble("power"));
                String faction = rs.getString("faction");
                if (faction != null && !faction.isEmpty()) {
                    fPlayer.setFactionNameLower(faction);
                }
                result.put(uuid, fPlayer);
            }
        }
        return result;
    }

    @Override
    public void saveFaction(Faction faction) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                connect();
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dfaction_factions (name_lower, display_name, description, created_at) VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), description = VALUES(description)")) {
                    ps.setString(1, faction.getNameLower());
                    ps.setString(2, faction.getName());
                    ps.setString(3, faction.getDescription());
                    ps.setLong(4, faction.getCreatedAt());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dfaction_members WHERE faction = ?")) {
                    ps.setString(1, faction.getNameLower());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dfaction_members (faction, uuid, role) VALUES (?, ?, ?)")) {
                    for (Map.Entry<UUID, Role> entry : faction.getMembers().entrySet()) {
                        ps.setString(1, faction.getNameLower());
                        ps.setString(2, entry.getKey().toString());
                        ps.setString(3, entry.getValue().name());
                        ps.addBatch();
                    }
                    if (!faction.getMembers().isEmpty()) ps.executeBatch();
                }

                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dfaction_claims WHERE faction = ?")) {
                    ps.setString(1, faction.getNameLower());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dfaction_claims (world, x, z, faction) VALUES (?, ?, ?, ?)")) {
                    for (ClaimKey claim : faction.getClaims()) {
                        ps.setString(1, claim.getWorld());
                        ps.setInt(2, claim.getX());
                        ps.setInt(3, claim.getZ());
                        ps.setString(4, faction.getNameLower());
                        ps.addBatch();
                    }
                    if (!faction.getClaims().isEmpty()) ps.executeBatch();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur MySQL lors de la sauvegarde de la faction " + faction.getName(), e);
            }
        });
    }

    @Override
    public void deleteFaction(String nameLower) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                connect();
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dfaction_members WHERE faction = ?")) {
                    ps.setString(1, nameLower);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dfaction_claims WHERE faction = ?")) {
                    ps.setString(1, nameLower);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM dfaction_factions WHERE name_lower = ?")) {
                    ps.setString(1, nameLower);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur MySQL lors de la suppression de la faction " + nameLower, e);
            }
        });
    }

    @Override
    public void savePlayer(FPlayer fPlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                connect();
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO dfaction_players (uuid, name, faction, power) VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE name = VALUES(name), faction = VALUES(faction), power = VALUES(power)")) {
                    ps.setString(1, fPlayer.getUuid().toString());
                    ps.setString(2, fPlayer.getName());
                    ps.setString(3, fPlayer.hasFaction() ? fPlayer.getFactionNameLower() : null);
                    ps.setDouble(4, fPlayer.getPower());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Erreur MySQL lors de la sauvegarde du joueur " + fPlayer.getName(), e);
            }
        });
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur lors de la fermeture de la connexion MySQL", e);
        }
    }
}
