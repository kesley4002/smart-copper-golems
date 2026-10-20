package com.anantaya.smartcgolem.util;

import com.anantaya.smartcgolem.ServerCategorySettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * World-level index of loaded item frames, shared by all golems.
 *
 * One loaded-entity scan per dimension per 100 ticks rather than querying the
 * entities near every block entity for every golem. Frames are the source of
 * truth; on actual pickup/deposit ItemFrameHelper revalidates the live state.
 * No chunks are loaded by this index.
 */
public final class StorageFrameIndex {
    private StorageFrameIndex() {}
    private static final long REFRESH_TICKS = 100L;
    private static final Map<ServerLevel, Snapshot> CACHE = new WeakHashMap<>();

    private record Snapshot(long atTick, int generation,
                            Map<String,List<BlockPos>> categories,
                            Map<String,List<BlockPos>> exact,
                            List<BlockPos> sources) {}

    private static Snapshot get(ServerLevel level) {
        Snapshot prev = CACHE.get(level);
        long tick = level.getGameTime();
        int gen = ServerCategorySettings.generation();
        if (prev != null && prev.generation() == gen
                && tick >= prev.atTick() && tick - prev.atTick() < REFRESH_TICKS) return prev;
        Map<String, LinkedHashSet<BlockPos>> groups = new HashMap<>();
        Map<String, LinkedHashSet<BlockPos>> exact = new HashMap<>();
        LinkedHashSet<BlockPos> sources = new LinkedHashSet<>();

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemFrame frame)) continue;
            ItemStack display = frame.getItem();
            if (display.isEmpty()) continue;
            BlockPos chest = frame.blockPosition().relative(frame.getDirection().getOpposite());
            if (level.getChunkSource().getChunkNow(Math.floorDiv(chest.getX(), 16),
                    Math.floorDiv(chest.getZ(), 16)) == null) continue;
            var state = level.getBlockState(chest);
            if (!(state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                    || state.is(Blocks.BARREL))) continue;
            int rotation = frame.getRotation();
            if (display.is(Items.STICK) && rotation == 0) {
                sources.add(chest.immutable());
                continue;
            }
            if (rotation == 0) {
                String category = SmartCategories.category(display);
                if (category != null)
                    groups.computeIfAbsent(category, k -> new LinkedHashSet<>()).add(chest.immutable());
            } else if (rotation == 1) {
                String id = BuiltInRegistries.ITEM.getKey(display.getItem()).toString();
                exact.computeIfAbsent(id, k -> new LinkedHashSet<>()).add(chest.immutable());
            }
        }
        Map<String,List<BlockPos>> frozenGroups = new HashMap<>();
        Map<String,List<BlockPos>> frozenExact = new HashMap<>();
        groups.forEach((k,v) -> frozenGroups.put(k, List.copyOf(v)));
        exact.forEach((k,v) -> frozenExact.put(k, List.copyOf(v)));
        Snapshot next = new Snapshot(tick, gen, Map.copyOf(frozenGroups),
                Map.copyOf(frozenExact), List.copyOf(sources));
        CACHE.put(level, next);
        return next;
    }

    public static List<BlockPos> destinations(ServerLevel level, ItemStack held) {
        if (held.isEmpty()) return List.of();
        Snapshot snapshot = get(level);
        LinkedHashSet<BlockPos> result = new LinkedHashSet<>();
        String group = SmartCategories.category(held);
        if (group != null) result.addAll(snapshot.categories().getOrDefault(group, List.of()));
        String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        result.addAll(snapshot.exact().getOrDefault(itemId, List.of()));
        return new ArrayList<>(result);
    }

    public static List<BlockPos> markedSources(ServerLevel level) {
        return get(level).sources();
    }

    /** Used when OP changes frames and after world unload. */
    public static void invalidate(ServerLevel level) { CACHE.remove(level); }
    public static void clear() { CACHE.clear(); }
}
