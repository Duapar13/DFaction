package com.dfaction.model;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Faction {

    private String name;
    private String description;
    private final long createdAt;
    private final Map<UUID, Role> members = new LinkedHashMap<>();
    private final Set<ClaimKey> claims = new HashSet<>();

    public Faction(String name, UUID owner) {
        this.name = name;
        this.description = "Aucune description définie.";
        this.createdAt = System.currentTimeMillis();
        this.members.put(owner, Role.OWNER);
    }

    public Faction(String name, String description, long createdAt) {
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public String getNameLower() {
        return name.toLowerCase();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Map<UUID, Role> getMembers() {
        return members;
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public Role getRole(UUID uuid) {
        return members.get(uuid);
    }

    public UUID getOwner() {
        for (Map.Entry<UUID, Role> entry : members.entrySet()) {
            if (entry.getValue() == Role.OWNER) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Set<ClaimKey> getClaims() {
        return claims;
    }
}
