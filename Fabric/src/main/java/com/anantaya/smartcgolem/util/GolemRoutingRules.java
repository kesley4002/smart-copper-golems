package com.anantaya.smartcgolem.util;

/**
 * Safe, testable routing invariants shared by normal and remote deposits.
 * A missing frame is NEVER a valid destination.
 */
public final class GolemRoutingRules {
    private GolemRoutingRules() {}
    public static boolean accepts(boolean hasFrame, boolean matched, boolean markedSource) {
        return hasFrame && matched && !markedSource;
    }
    public static int amountToCarry(int available, int itemMaxStack, int configuredLimit) {
        if (available <= 0 || itemMaxStack <= 0) return 0;
        if (configuredLimit != 16 && configuredLimit != 32 && configuredLimit != 64)
            throw new IllegalArgumentException("Maximum capacity must be 16, 32 or 64.");
        return Math.min(available, Math.min(itemMaxStack, configuredLimit));
    }
    public static boolean remoteDepositMayCrossHeight(int destinationY, int golemY) {
        return destinationY >= golemY - 2;
    }
}
