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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    public static BlockPos findCrop(ServerLevel level, CopperGolem golem) {
        BlockPos center = golem.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-10, -3, -10),
                                                   center.offset(10, 3, 10))) {
            if (!level.hasChunkAt(p) || !ServerWorkZoneSettings.contains(golem, level, p)) continue;
            if (!isHarvestable(level.getBlockState(p))) continue;
            double distance = p.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = p.immutable();
            }
        }
        return best;
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
        if (!level.hasChunkAt(pos) || !ServerWorkZoneSettings.contains(golem, level, pos)
                || golem.blockPosition().distSqr(pos) > 9) return false;
        BlockState state = level.getBlockState(pos);
        if (!isHarvestable(state)) return false;
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
