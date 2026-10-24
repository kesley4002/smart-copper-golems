package com.anantaya.smartcgolem.ai;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Cosmetic scissors, NEVER part of the Pastor's inventory/cargo.
 *
 * These appear only while a Pastor is free or following a sheep.
 * Actual wool replaces the scissors while the golem is transporting.
 * The private custom-name marker distinguishes them from real shears.
 */
public final class ShepherdShears {
    private ShepherdShears() {}

    private static final String MARKER = "__smart_copper_golems_pastor_virtual_shears__";

    public static ItemStack newVisual() {
        ItemStack display = new ItemStack(Items.SHEARS);
        display.set(DataComponents.CUSTOM_NAME, Component.literal(MARKER));
        return display;
    }

    public static boolean isVirtual(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.SHEARS)) return false;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null && isVisualMarker(name.getString());
    }

    /** Pure check to prevent treating player-renamed real shears as cosmetic. */
    public static boolean isVisualMarker(String name) {
        return MARKER.equals(name);
    }

    /** A cosmetic tool does not count as cargo and never enters a chest. */
    public static boolean isFreeHand(ItemStack stack) {
        return stack.isEmpty() || isVirtual(stack);
    }

    public static void protectIfVirtual(CopperGolem golem) {
        if (isVirtual(golem.getMainHandItem())) {
            golem.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    public static void showIfFree(CopperGolem golem) {
        if (!golem.getMainHandItem().isEmpty()) return;
        // Never allow the cosmetic item to become loot on death.
        golem.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        golem.setItemInHand(InteractionHand.MAIN_HAND, newVisual());
    }

    public static boolean hideIfVirtual(CopperGolem golem) {
        if (!isVirtual(golem.getMainHandItem())) return false;
        golem.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return true;
    }
}
