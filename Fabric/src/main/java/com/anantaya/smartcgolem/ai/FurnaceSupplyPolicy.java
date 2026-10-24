package com.anantaya.smartcgolem.ai;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fuel-only rule for the Fornalheiro. It NEVER processes wooden items,
 * makes charcoal, or handles ingredient/output slots.
 */
public final class FurnaceSupplyPolicy {
    private FurnaceSupplyPolicy() {}
    public static final int TARGET_COAL = 8;
    public static final int TARGET_COAL_BLOCK = 1;
    public static final int MIN_SOURCE_RESERVE = 1;

    public static boolean allowedFuel(ItemStack item) {
        return !item.isEmpty() && (item.is(Items.COAL)
                || item.is(Items.CHARCOAL) || item.is(Items.COAL_BLOCK));
    }

    /** Legacy default, retained for existing callers and tests. */
    public static int targetCount(ItemStack item) {
        return targetCount(item, TARGET_COAL);
    }

    /** One coal block smelts 80 items and remains fixed at one unit. */
    public static int targetCount(ItemStack item, int configuredCoal) {
        return configuredTarget(!item.isEmpty() && item.is(Items.COAL_BLOCK),
                configuredCoal);
    }

    /** Pure configurable target check (does not initialize Minecraft's item registry). */
    public static int configuredTarget(boolean isCoalBlock, int configuredCoal) {
        if (!com.anantaya.smartcgolem.ServerFurnaceSettings.validCoalTarget(configuredCoal))
            throw new IllegalArgumentException("Coal target must be 8..64 in steps of 8.");
        return isCoalBlock ? TARGET_COAL_BLOCK : configuredCoal;
    }

    /**
     * Pure rules for tests and predictable refills.
     * A furnace with 4 or fewer coals is topped up to 8;
     * one coal block is enough and is never mixed with other fuel types.
     */
    public static int requiredCount(int alreadyPresent, int target, int availableAfterReserve) {
        if (alreadyPresent < 0 || target < 1 || availableAfterReserve <= 0) return 0;
        if (alreadyPresent > target / 2) return 0;
        return Math.max(0, Math.min(target - alreadyPresent, availableAfterReserve));
    }
}
