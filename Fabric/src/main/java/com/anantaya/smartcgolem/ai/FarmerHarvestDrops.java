package com.anantaya.smartcgolem.ai;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Unlike the Collector, the Agricultor handles ONLY the output of the crops
 * it can harvest, plus bone meal produced by its composter. Mob drops such as
 * rotten flesh, arrows, bones and equipment belong to the Collector.
 */
public final class FarmerHarvestDrops {
    private FarmerHarvestDrops() {}

    public static boolean allowed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(Items.WHEAT)
                || stack.is(Items.WHEAT_SEEDS)
                || stack.is(Items.CARROT)
                || stack.is(Items.POTATO)
                || stack.is(Items.POISONOUS_POTATO)
                || stack.is(Items.BEETROOT)
                || stack.is(Items.BEETROOT_SEEDS)
                || stack.is(Items.BONE_MEAL);
    }
}
