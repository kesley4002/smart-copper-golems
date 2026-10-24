package com.anantaya.smartcgolem.ai;

import org.junit.Test;
import static org.junit.Assert.*;

/** The cosmetic scissors must be distinguishable from legitimate player items. */
public class ShepherdShearsTest {
    @Test
    public void cosmeticSignatureIsStrictAndCannotMatchRealShears() {
        assertFalse(ShepherdShears.isVisualMarker(null));
        assertFalse(ShepherdShears.isVisualMarker(""));
        assertFalse(ShepherdShears.isVisualMarker("Shears"));
        assertFalse(ShepherdShears.isVisualMarker("Tesoura"));
        assertFalse(ShepherdShears.isVisualMarker("minecraft:shears"));
        assertFalse(ShepherdShears.isVisualMarker("__smart_copper_golems_pastor_virtual_shears_"));
    }
}
