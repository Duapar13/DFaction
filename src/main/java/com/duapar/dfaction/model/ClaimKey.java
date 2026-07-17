package com.duapar.dfaction.model;

import java.util.Objects;

public final class ClaimKey {

    private final String world;
    private final int x;
    private final int z;

    public ClaimKey(String world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public static ClaimKey fromString(String serialized) {
        String[] parts = serialized.split(";");
        return new ClaimKey(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String serialize() {
        return world + ";" + x + ";" + z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaimKey)) return false;
        ClaimKey claimKey = (ClaimKey) o;
        return x == claimKey.x && z == claimKey.z && world.equals(claimKey.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, z);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
