package com.anantaya.smartcgolem.ai;

import com.anantaya.smartcgolem.ServerWorkZoneSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First experimental agriculture module. Harvest mature crops, reserve one
 * actually generated seed/plant item for replanting, and spawn every other
 * drop using the normal item-entity pipeline. No synthetic seeds or drops.
 *
 * This module does not touch composter inventories (later module).
 */
public final class FarmerHarvest {
    private FarmerHarvest() {}

    private static final int SEARCH_HORIZONTAL = 12;
    private static final int SEARCH_VERTICAL = 3;
    private static final int MAX_PATH_CHECKS = 32;
    private static final double REACH_HORIZONTAL_SQR = 2.25D; // ~1.5 blocks

    public static BlockPos findCrop(ServerLevel level, CopperGolem golem) {
        BlockPos center = golem.blockPosition();
        List<BlockPos> ripe = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(
                center.offset(-SEARCH_HORIZONTAL, -SEARCH_VERTICAL, -SEARCH_HORIZONTAL),
                center.offset(SEARCH_HORIZONTAL, SEARCH_VERTICAL, SEARCH_HORIZONTAL))) {
            // Never load terrain just to find a farm.
            if (level.getChunkSource().getChunkNow(Math.floorDiv(p.getX(), 16),
                    Math.floorDiv(p.getZ(), 16)) == null
                    || !ServerWorkZoneSettings.contains(golem, level, p)) continue;
            if (!isHarvestable(level.getBlockState(p))
                    || !level.getBlockState(p.below()).is(Blocks.FARMLAND)) continue;
            ripe.add(p.immutable());
        }
        ripe.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        int checks = 0;
        for (BlockPos target : ripe) {
            if (++checks > MAX_PATH_CHECKS) break;
            // Walking is mandatory. Crop targets must be reachable, not just
            // within the generous discovery volume.
            if (canHarvestNearby(golem, target)) return target;
            Path path = golem.getNavigation().createPath(target, 1);
            if (path != null && path.canReach()) return target;
        }
        return null;
    }

    /** Golem must physically reach the crop, not remotely break blocks. */
    public static boolean canHarvestNearby(CopperGolem golem, BlockPos pos) {
        double dx = golem.getX() - (pos.getX() + 0.5D);
        double dz = golem.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dz * dz <= REACH_HORIZONTAL_SQR
                && Math.abs(golem.getY() - pos.getY()) <= 1.5D;
    }

    public static boolean isRipeAndFarmland(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(Math.floorDiv(pos.getX(), 16),
                        Math.floorDiv(pos.getZ(), 16)) != null
                && isHarvestable(level.getBlockState(pos))
                && level.getBlockState(pos.below()).is(Blocks.FARMLAND);
    }

    private static boolean isHarvestable(BlockState state) {
        Block b = state.getBlock();
        return supported(b) && b instanceof CropBlock crop && crop.isMaxAge(state);
    }

    private static boolean supported(Block b) {
        return b == Blocks.WHEAT || b == Blocks.CARROTS
                || b == Blocks.POTATOES || b == Blocks.BEETROOTS;
    }

    private static Item seedFor(Block b) {
        if (b == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (b == Blocks.CARROTS) return Items.CARROT;
        if (b == Blocks.POTATOES) return Items.POTATO;
        return Items.BEETROOT_SEEDS;
    }

    public static boolean harvest(ServerLevel level, CopperGolem golem, BlockPos pos) {
        if (!ServerWorkZoneSettings.contains(golem, level, pos)
                || !canHarvestNearby(golem, pos)
                || !isRipeAndFarmland(level, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) return false;
        if (!level.getBlockState(pos.below()).is(Blocks.FARMLAND)) return false;

        // Ask Minecraft for actual loot first. No farming output is invented.
        List<ItemStack> drops = Block.getDrops(state, level, pos,
                level.getBlockEntity(pos), golem, ItemStack.EMPTY);
        Item seed = seedFor(state.getBlock());
        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (drop.is(seed) && !drop.isEmpty()) {
                drop.shrink(1);
                replanted = true;
                break;
            }
        }

        // Remove mature plant; replant only when loot paid the seed cost.
        if (replanted) level.setBlock(pos, crop.getStateForAge(0), 3);
        else level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) Block.popResource(level, pos, drop);
        }
        return true;
    }
}
