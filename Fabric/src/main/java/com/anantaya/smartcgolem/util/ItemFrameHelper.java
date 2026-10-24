/*
 * Modified for Smart Copper Golems Extended by Kesley, 2026-10-19.
 * Original project: GameMech007, Apache-2.0.
 */
package com.anantaya.smartcgolem.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemFrameHelper {
    private ItemFrameHelper() {}

    /** Only the real partner of a double chest shares its filter. */
    private static BlockPos partner(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
        BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
        BlockState adjacent = level.getBlockState(other);
        if (adjacent.getBlock() != state.getBlock()
                || adjacent.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                || adjacent.getValue(ChestBlock.TYPE) == state.getValue(ChestBlock.TYPE)
                || adjacent.getValue(ChestBlock.FACING) != state.getValue(ChestBlock.FACING)
                || !other.relative(ChestBlock.getConnectedDirection(adjacent)).equals(pos)) return null;
        return other;
    }

    private static List<ItemFrame> frames(ServerLevel level, BlockPos pos) {
        BlockPos other = partner(level, pos);
        AABB box = new AABB(pos).inflate(2.0D);
        List<ItemFrame> result = new ArrayList<>();
        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, box)) {
            BlockPos attached = frame.blockPosition().relative(frame.getDirection().getOpposite());
            if ((attached.equals(pos) || attached.equals(other)) && !frame.getItem().isEmpty()) {
                result.add(frame);
            }
        }
        return result;
    }

    /**
     * A non-rotated stick marks a normal chest / double chest / barrel as an
     * INPUT. All frame faces of a barrel and both halves of a real double
     * chest are recognized. A rotated stick is an exact-item filter.
     */
    public static boolean hasSourceMarker(ServerLevel level, BlockPos pos) {
        return frames(level, pos).stream().anyMatch(frame ->
                frame.getRotation() == 0 && frame.getItem().is(Items.STICK));
    }

    /**
     * A composter item rotated 90 degrees identifies an opt-in compost input
     * chest/barrel. It does not replace the ordinary 0/45-degree destination
     * frames that accept harvest drops.
     */
    public static boolean hasCompostSourceMarker(ServerLevel level, BlockPos pos) {
        return frames(level, pos).stream().anyMatch(frame ->
                frame.getRotation() == 2 && frame.getItem().is(Items.COMPOSTER));
    }

    public record FilterResult(boolean hasFrame, boolean matchesHeld, ItemStack matchedFrameItem) {}

    public static FilterResult filter(ServerLevel level, BlockPos pos, ItemStack held) {
        List<ItemFrame> frames = frames(level, pos);
        String selectedCategory = null;
        ItemStack representative = ItemStack.EMPTY;
        boolean hasCategory = false;
        boolean exactMatch = false;
        ItemStack exactItem = ItemStack.EMPTY;
        for (ItemFrame frame : frames) {
            ItemStack sample = frame.getItem();
            // Visually aligned frames (0°) select a category. Reserve an
            // unrotated stick for the source marker; never use it as a filter.
            if (sample.is(Items.STICK) && frame.getRotation() == 0) {
                continue;
            }
            if (frame.getRotation() == 0) {
                String category = SmartCategories.category(sample);
                // Unknown representative or competing categories: reject, never fallback.
                if (category == null || (hasCategory && !Objects.equals(selectedCategory, category))) {
                    return new FilterResult(true, false, ItemStack.EMPTY);
                }
                hasCategory = true;
                selectedCategory = category;
                representative = sample;
            } else if (frame.getRotation() == 1 && !held.isEmpty() && ItemStack.isSameItemSameComponents(sample, held)) {
                exactMatch = true;
                exactItem = sample;
            }
        }
        // One category owns the entire logical container, including both chest halves.
        if (hasCategory) {
            boolean matches = !held.isEmpty() && selectedCategory.equals(SmartCategories.category(held));
            return new FilterResult(true, matches, representative.copy());
        }
        // An unrotated source stick is never a destination filter.
        return new FilterResult(!frames.isEmpty(), exactMatch, exactItem.copy());
    }

    public static List<ItemStack> getFramedItems(ServerLevel level, BlockPos pos) {
        return frames(level, pos).stream().map(frame -> frame.getItem().copy()).toList();
    }

    public static boolean hasMatchingFrame(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return false;
        FilterResult filter = filter(level, pos, stack);
        return !filter.hasFrame() || filter.matchesHeld();
    }
}
