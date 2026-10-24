package com.anantaya.smartcgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Searches FURNACE BLOCK ENTITIES, not every block in a huge cuboid.
 * Only already-loaded chunks are inspected; this deliberately does not
 * create tickets or force-load remote furnaces.
 */
public final class FurnaceSearch {
    private FurnaceSearch() {}
    public static final int DEFAULT_RADIUS = 64;
    public static final int MAX_RADIUS = 128;
    public static final int VERTICAL_RANGE = 12;
    public static final int MAX_CANDIDATES = 96;

    public static boolean validRadius(int radius) {
        return radius >= 16 && radius <= MAX_RADIUS && radius % 8 == 0;
    }

    public static boolean inside(BlockPos origin, BlockPos furnace, int radius, int vertical) {
        return Math.abs((long)furnace.getX() - origin.getX()) <= radius
                && Math.abs((long)furnace.getZ() - origin.getZ()) <= radius
                && Math.abs((long)furnace.getY() - origin.getY()) <= vertical;
    }

    public static List<BlockPos> loadedFurnaces(ServerLevel level, BlockPos center,
                                                 int radius) {
        List<BlockPos> found = new ArrayList<>();
        int firstX = Math.floorDiv(center.getX() - radius, 16);
        int lastX = Math.floorDiv(center.getX() + radius, 16);
        int firstZ = Math.floorDiv(center.getZ() - radius, 16);
        int lastZ = Math.floorDiv(center.getZ() + radius, 16);
        for (int chunkX = firstX; chunkX <= lastX; chunkX++) {
            for (int chunkZ = firstZ; chunkZ <= lastZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (Map.Entry<BlockPos, net.minecraft.world.level.block.entity.BlockEntity> entry
                        : chunk.getBlockEntities().entrySet()) {
                    BlockPos at = entry.getKey();
                    if (entry.getValue() instanceof AbstractFurnaceBlockEntity
                            && inside(center, at, radius, VERTICAL_RANGE)) {
                        found.add(at.immutable());
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(center::distSqr));
        // Bound expensive inspection of frames, chests and navigation
        // around heavily automated bases.
        return List.copyOf(found.subList(0, Math.min(found.size(), MAX_CANDIDATES)));
    }
}
