package com.anantaya.smartcgolem.ai;

/**
 * Strict physical reach for an Agricultor storing harvests.
 * These are intentionally pure numeric predicates to allow fast JUnit
 * regression tests without Minecraft registry initialization.
 */
public final class FarmerStorageProximity {
    private FarmerStorageProximity() {}

    public static boolean normal(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= 3.24D && Math.abs(dy) <= 1.5D;
    }

    public static boolean elevated(double dx, int blocksAbove, double dz) {
        return blocksAbove >= 2 && blocksAbove <= 5
                && dx * dx + dz * dz <= 3.24D;
    }
}
