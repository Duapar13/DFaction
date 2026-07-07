package com.dfaction.model;

import java.util.UUID;

public class FPlayer {

    private final UUID uuid;
    private String name;
    private String factionNameLower;
    private double power;

    public FPlayer(UUID uuid, String name, double startingPower) {
        this.uuid = uuid;
        this.name = name;
        this.factionNameLower = null;
        this.power = startingPower;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactionNameLower() {
        return factionNameLower;
    }

    public void setFactionNameLower(String factionNameLower) {
        this.factionNameLower = factionNameLower;
    }

    public boolean hasFaction() {
        return factionNameLower != null;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }
}
