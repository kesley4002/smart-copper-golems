package com.anantaya.smartcgolem.util;

/**
 * Inclusive search box around a copper golem, with chunk coordinates derived
 * using floor division (important for negative Minecraft coordinates).
 * Independent from game classes so its boundaries can be regression-tested.
 */
public record SearchVolume(int centerX, int centerY, int centerZ, int horizontal, int vertical) {
    public SearchVolume {
        if (horizontal < 0 || vertical < 0) {
            throw new IllegalArgumentException("Search ranges cannot be negative");
        }
    }

    public int minChunkX() { return Math.floorDiv(centerX - horizontal, 16); }
    public int maxChunkX() { return Math.floorDiv(centerX + horizontal, 16); }
    public int minChunkZ() { return Math.floorDiv(centerZ - horizontal, 16); }
    public int maxChunkZ() { return Math.floorDiv(centerZ + horizontal, 16); }

    public boolean contains(int x, int y, int z) {
        return Math.abs((long)x - centerX) <= horizontal
                && Math.abs((long)y - centerY) <= vertical
                && Math.abs((long)z - centerZ) <= horizontal;
    }
}
