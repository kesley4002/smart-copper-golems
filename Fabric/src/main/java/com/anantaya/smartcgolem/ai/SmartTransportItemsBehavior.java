package com.anantaya.smartcgolem.ai;

import com.google.common.collect.ImmutableMap;
import com.anantaya.smartcgolem.config.GolemSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.core.particles.ParticleTypes;
import org.jspecify.annotations.NonNull;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SmartTransportItemsBehavior extends Behavior<PathfinderMob> {

    private enum TaskState {
        IDLE,
        WALK_TO_SOURCE,
        WAIT_FOR_SOURCE,
        INTERACT_SOURCE,
        WALK_TO_DESTINATION,
        WAIT_FOR_DESTINATION,
        INTERACT_DESTINATION,
        RETURN_TO_SOURCE
    }

    private static final Set<BlockPos> LOCKED_CHESTS = new HashSet<>();
    private static final Map<BlockPos, Long> LOCKED_CHESTS_TIMESTAMP = new HashMap<>();

    private static final long LOCK_STALE_TICKS = 600;

    private static final int ROUTE_STUCK_TICKS = 140;
    private BlockPos lastMovementPos = null;
    private BlockPos temporarilyBlockedDestination = null;
    private long blockedUntilTick = 0L;

    private boolean currentDestinationIsFramedMatch = false;
    private boolean currentDestinationHadReachablePath = true;
    private double closestDistanceToWalkTargetSqr = Double.MAX_VALUE;

    private long stuckStartedAt = -1L;

    private final float speedModifier;
    private final Predicate<BlockState> sourceBlockType;
    private final Predicate<BlockState> destinationBlockType;
    private final int horizontalSearchDistance;
    private final int verticalSearchDistance;
    private final Consumer<PathfinderMob> onTravelling;

    private TaskState taskState = TaskState.IDLE;

    private BlockPos currentTarget = null;
    private BlockPos currentWalkTarget = null;
    private BlockPos lastPickupChest = null;
    private BlockPos returnToSourceChest = null;
    private BlockPos lockedChest = null;

    private Container openedContainer = null;
    private CopperGolem openedCopperGolem = null;

    private boolean actionDone = false;

    private int ticksAtTarget = 0;
    private long nextSearchTick = 0;

    private static final int SEARCH_COOLDOWN_TICKS = 20;
    private static final Map<String, Long> NOTICE_COOLDOWNS = new HashMap<>();
    private static final long NOTICE_INTERVAL_TICKS = 1200;

    public SmartTransportItemsBehavior(
            float speedModifier,
            Predicate<BlockState> sourceBlockType,
            Predicate<BlockState> destinationBlockType,
            int horizontalSearchDistance,
            int verticalSearchDistance,
            Consumer<PathfinderMob> onTravelling
    ) {
        super(ImmutableMap.of(), 200);

        this.speedModifier = speedModifier;
        this.sourceBlockType = sourceBlockType;
        this.destinationBlockType = destinationBlockType;
        this.horizontalSearchDistance = horizontalSearchDistance;
        this.verticalSearchDistance = verticalSearchDistance;
        this.onTravelling = onTravelling;
    }

    @Override
    protected void start(@NonNull ServerLevel level, PathfinderMob mob, long gameTime) {

        boolean carrying = !mob.getMainHandItem().isEmpty();

        currentTarget = null;
        currentWalkTarget = null;
        openedContainer = null;
        openedCopperGolem = null;
        actionDone = false;
        ticksAtTarget = 0;
        taskState = TaskState.IDLE;
        unlockChest();

        if (!carrying) {
            lastPickupChest = null;
            returnToSourceChest = null;
        }

        System.out.println("[SMART-GOLEM START] carrying=" + carrying
                + " returnToSourceChest=" + returnToSourceChest
                + " lastPickupChest=" + lastPickupChest);
    }

    @Override
    protected boolean canStillUse(@NonNull ServerLevel level, PathfinderMob mob, long gameTime) {
        return true;
    }

    private boolean needsChestLock(TaskState state) {
        return state == TaskState.INTERACT_SOURCE
                || state == TaskState.INTERACT_DESTINATION;
    }

    private boolean isChestLocked(BlockPos pos, long gameTime) {
        synchronized (LOCKED_CHESTS) {
            if (!LOCKED_CHESTS.contains(pos)) {
                return true;
            }

            Long lockedAt = LOCKED_CHESTS_TIMESTAMP.get(pos);

            if (lockedAt != null && gameTime - lockedAt > LOCK_STALE_TICKS) {
                System.out.println("[SMART-GOLEM CHEST-LOCK-STALE] clearing stale lock=" + pos);
                LOCKED_CHESTS.remove(pos);
                LOCKED_CHESTS_TIMESTAMP.remove(pos);
                return true;
            }

            return false;
        }
    }

    private boolean tryLockChest(BlockPos pos, long gameTime) {
        if (pos == null) {
            return true;
        }

        if (pos.equals(lockedChest)) {

            synchronized (LOCKED_CHESTS) {
                LOCKED_CHESTS_TIMESTAMP.put(pos, gameTime);
            }
            return false;
        }

        synchronized (LOCKED_CHESTS) {
            if (LOCKED_CHESTS.contains(pos)) {
                Long lockedAt = LOCKED_CHESTS_TIMESTAMP.get(pos);

                if (lockedAt != null && gameTime - lockedAt > LOCK_STALE_TICKS) {
                    System.out.println("[SMART-GOLEM CHEST-LOCK-STALE] reclaiming stale lock=" + pos);
                } else {
                    return true;
                }
            }

            LOCKED_CHESTS.add(pos);
            LOCKED_CHESTS_TIMESTAMP.put(pos, gameTime);
            lockedChest = pos;

            System.out.println("[SMART-GOLEM CHEST-LOCK] locked=" + pos);
            return false;
        }
    }

    private void unlockChest() {
        if (lockedChest == null) {
            return;
        }

        synchronized (LOCKED_CHESTS) {
            LOCKED_CHESTS.remove(lockedChest);
            LOCKED_CHESTS_TIMESTAMP.remove(lockedChest);
            System.out.println("[SMART-GOLEM CHEST-UNLOCK] unlocked=" + lockedChest);
        }

        lockedChest = null;
    }

    private void switchState(PathfinderMob mob, TaskState newState, BlockPos target, long gameTime) {

        if (needsChestLock(newState)) {
            if (tryLockChest(target, gameTime)) {
                System.out.println("[SMART-GOLEM CHEST-LOCK-FAILED] target busy=" + target);

                if (newState == TaskState.INTERACT_SOURCE) {
                    this.taskState = TaskState.WAIT_FOR_SOURCE;
                } else if (newState == TaskState.INTERACT_DESTINATION) {
                    this.taskState = TaskState.WAIT_FOR_DESTINATION;
                } else {
                    this.taskState = TaskState.IDLE;
                }

                this.currentTarget = target;
                this.currentWalkTarget = target == null ? null : findBestWalkTargetForChest(mob, target);

                this.ticksAtTarget = 0;
                this.actionDone = false;
                resetStuckTracking(mob, gameTime);

                if (newState != TaskState.WALK_TO_DESTINATION) {
                    markDestinationSelection(false, true);
                }
                return;
            }
        } else {
            unlockChest();
        }

        this.taskState = newState;
        this.currentTarget = target;
        this.currentWalkTarget = target == null ? null : findBestWalkTargetForChest(mob, target);

        this.ticksAtTarget = 0;
        this.actionDone = false;
        resetStuckTracking(mob, gameTime);

        if (newState != TaskState.WALK_TO_DESTINATION) {
            markDestinationSelection(false, true);
        }

        System.out.println("[SMART-GOLEM STATE] -> " + newState
                + " chestTarget=" + target
                + " walkTarget=" + currentWalkTarget);
    }

    @Override
    protected void tick(@NonNull ServerLevel level, PathfinderMob mob, long gameTime) {

        switch (taskState) {

            case IDLE ->
                searchForTarget(level, mob, gameTime);

            case WALK_TO_SOURCE -> {
                walkToTarget(mob, gameTime);

                if (hasArrived(mob)) {
                    switchState(mob, TaskState.WAIT_FOR_SOURCE, currentTarget, gameTime);
                }
            }

            case WAIT_FOR_SOURCE -> {
                walkToTarget(mob, gameTime);

                if (currentTarget == null) {
                    switchState(mob, TaskState.IDLE, null, gameTime);
                    return;
                }

                BlockEntity blockEntity = level.getBlockEntity(currentTarget);

                if (!(blockEntity instanceof ChestBlockEntity chest)) {
                    switchState(mob, TaskState.IDLE, null, gameTime);
                    return;
                }

                if (isChestLocked(currentTarget, gameTime) && !isChestBusy(chest)) {
                    switchState(mob, TaskState.INTERACT_SOURCE, currentTarget, gameTime);
                }
            }

            case INTERACT_SOURCE -> interactWithSource(level, mob, gameTime);

            case WALK_TO_DESTINATION -> {
                walkToTarget(mob, gameTime);

                if (retryRouteIfStuck(level, mob, gameTime)) {
                    return;
                }

                if (hasArrived(mob)) {
                    switchState(mob, TaskState.WAIT_FOR_DESTINATION, currentTarget, gameTime);
                }
            }

            case WAIT_FOR_DESTINATION -> {
                walkToTarget(mob, gameTime);

                if (currentTarget == null) {
                    switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
                    return;
                }

                BlockEntity blockEntity = level.getBlockEntity(currentTarget);

                if (!(blockEntity instanceof ChestBlockEntity chest)) {
                    switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
                    return;
                }

                if (isChestLocked(currentTarget, gameTime) && !isChestBusy(chest)) {
                    switchState(mob, TaskState.INTERACT_DESTINATION, currentTarget, gameTime);
                }
            }

            case INTERACT_DESTINATION -> interactWithDestination(level, mob, gameTime);

            case RETURN_TO_SOURCE -> {
                walkToTarget(mob, gameTime);

                if (hasArrived(mob)) {

                    if (mob instanceof CopperGolem copperGolem) {
                        copperGolem.clearOpenedChestPos();
                        copperGolem.setState(CopperGolemState.IDLE);
                    }

                    System.out.println("[SMART-GOLEM RETURNED-TO-SOURCE] source=" + currentTarget);

                    returnToSourceChest = null;
                    lastPickupChest = null;

                    taskState = TaskState.IDLE;
                    currentTarget = null;
                    currentWalkTarget = null;
                    ticksAtTarget = 0;
                    actionDone = false;
                }
            }
        }
    }

    private void walkToTarget(PathfinderMob mob, long gameTime) {

        if (currentWalkTarget == null) {
            if (!mob.getMainHandItem().isEmpty() && returnToSourceChest != null
                    && taskState != TaskState.RETURN_TO_SOURCE) {
                switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            } else {
                switchState(mob, TaskState.IDLE, null, gameTime);
            }
            return;
        }

        BehaviorUtils.setWalkAndLookTargetMemories(
                mob,
                currentWalkTarget,
                speedModifier,
                0
        );
    }

    private boolean hasArrived(PathfinderMob mob) {
        if (currentWalkTarget == null) return false;
        double dx = mob.getX() - (currentWalkTarget.getX() + 0.5D);
        double dz = mob.getZ() - (currentWalkTarget.getZ() + 0.5D);
        // The previous squared block distance (<=4) treated an overhead roof
        // as "arrived" even when a solid floor separated the golem and chest.
        return Math.abs(mob.getY() - currentWalkTarget.getY()) < 0.8D
                && dx * dx + dz * dz < 1.21D;
    }

    /**
     * Cheap squared-distance estimate used to rank candidate chests before
     * we spend any time on real pathfinding. This avoids running
     * createPath() for every block in the search volume.
     */
    private double getRoughDistance(PathfinderMob mob, BlockPos pos) {
        return mob.blockPosition().distSqr(pos);
    }

    /**
     * Only use a walkable place beside a chest, never the chest itself or
     * a tile on the roof above it. A path must really reach that position.
     * This lets vanilla navigation discover a route via stairs or corridors.
     */
    private record ChestApproach(BlockPos target, double cost) {}

    private ChestApproach findApproach(PathfinderMob mob, BlockPos chestPos) {
        ChestApproach best = null;
        for (int yOffset : new int[]{0, -1}) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    BlockPos standing = chestPos.offset(dx, yOffset, dz);
                    if (!mob.level().getBlockState(standing).getCollisionShape(mob.level(), standing).isEmpty()
                            || !mob.level().getBlockState(standing.above()).getCollisionShape(mob.level(), standing.above()).isEmpty()) {
                        continue;
                    }
                    Path path = mob.getNavigation().createPath(standing, 0);
                    if (path == null || !path.canReach()) continue;
                    double cost = path.getNodeCount();
                    if (best == null || cost < best.cost()) {
                        best = new ChestApproach(standing.immutable(), cost);
                    }
                }
            }
        }
        return best;
    }

    private double getPathCost(PathfinderMob mob, BlockPos chestPos) {
        ChestApproach approach = findApproach(mob, chestPos);
        return approach == null ? Double.MAX_VALUE : approach.cost();
    }

    private BlockPos findBestWalkTargetForChest(PathfinderMob mob, BlockPos chestPos) {
        ChestApproach approach = findApproach(mob, chestPos);
        return approach == null ? null : approach.target();
    }

    private void interactWithSource(ServerLevel level, PathfinderMob mob, long gameTime) {

        if (currentTarget == null) {
            closeOpenedContainer();
            switchState(mob, TaskState.IDLE, null, gameTime);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(currentTarget);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            System.out.println("[SMART-GOLEM ERROR] Source target is not ChestBlockEntity at " + currentTarget);
            closeOpenedContainer();
            switchState(mob, TaskState.IDLE, null, gameTime);
            return;
        }

        ticksAtTarget++;

        mob.getLookControl().setLookAt(
                currentTarget.getX() + 0.5,
                currentTarget.getY() + 0.5,
                currentTarget.getZ() + 0.5
        );

        if (ticksAtTarget == 1) {

            if (mob instanceof CopperGolem copperGolem) {

                chest.startOpen(copperGolem);
                copperGolem.setOpenedChestPos(currentTarget);

                if (copperGolem.getMainHandItem().isEmpty()) {
                    copperGolem.setState(CopperGolemState.GETTING_ITEM);
                }

                openedContainer = chest;
                openedCopperGolem = copperGolem;

                System.out.println("[SMART-GOLEM VANILLA-OPEN] source=" + currentTarget);
            }

            return;
        }

        if (ticksAtTarget < 9) {
            return;
        }

        if (!actionDone) {

            mob.swing(InteractionHand.MAIN_HAND);

            boolean movedItem = false;

            if (mob.getMainHandItem().isEmpty()) {
                movedItem = pickupFromChest(level, mob, chest);
            }

            if (mob instanceof CopperGolem copperGolem) {

                if (movedItem) {
                    copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_GET);
                    copperGolem.setState(CopperGolemState.GETTING_ITEM);
                } else {
                    copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_GET);
                    copperGolem.setState(CopperGolemState.GETTING_NO_ITEM);
                }
            }

            actionDone = true;
        }

        if (ticksAtTarget >= 60) {

            closeOpenedContainer();

            if (!mob.getMainHandItem().isEmpty()) {

                unlockChest();

                BlockPos bestDestination = findDestinationChest(level, mob);

                if (bestDestination != null) {
                    switchState(mob, TaskState.WALK_TO_DESTINATION, bestDestination, gameTime);
                } else {
                    // Put the item straight back where it came from; never leave it
                    // stranded in the golem hand when no category exists.
                    ItemStack heldItemForWarning = mob.getMainHandItem().copy();
                    ItemStack remaining = insertIntoChest(
                            level, currentTarget, getActualContainer(level, currentTarget, chest),
                            mob.getMainHandItem());
                    mob.setItemInHand(InteractionHand.MAIN_HAND, remaining);
                    notifyMissingDestination(level, heldItemForWarning, gameTime);
                    System.out.println("[SMART-GOLEM NO-DESTINATION] Item returned to source: " + currentTarget);
                    switchState(mob, TaskState.IDLE, null, gameTime);
                }

                return;
            }

            switchState(mob, TaskState.IDLE, null, gameTime);
        }
    }

    private void interactWithDestination(ServerLevel level, PathfinderMob mob, long gameTime) {

        if (currentTarget == null) {
            closeOpenedContainer();
            switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(currentTarget);

        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            System.out.println("[SMART-GOLEM ERROR] Destination target is not ChestBlockEntity at " + currentTarget);
            closeOpenedContainer();
            switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            return;
        }

        ticksAtTarget++;

        mob.getLookControl().setLookAt(
                currentTarget.getX() + 0.5,
                currentTarget.getY() + 0.5,
                currentTarget.getZ() + 0.5
        );

        if (ticksAtTarget == 1) {

            if (mob instanceof CopperGolem copperGolem) {

                chest.startOpen(copperGolem);
                copperGolem.setOpenedChestPos(currentTarget);

                if (!copperGolem.getMainHandItem().isEmpty()) {
                    copperGolem.setState(CopperGolemState.DROPPING_ITEM);
                }

                openedContainer = chest;
                openedCopperGolem = copperGolem;

                System.out.println("[SMART-GOLEM VANILLA-OPEN] destination=" + currentTarget);
            }

            return;
        }

        if (ticksAtTarget < 9) {
            return;
        }

        if (!actionDone) {

            mob.swing(InteractionHand.MAIN_HAND);

            boolean movedItem = false;

            if (!mob.getMainHandItem().isEmpty()) {

                ItemStack heldBefore = mob.getMainHandItem().copy();

                System.out.println("[SMART-GOLEM DEPOSIT START] chest=" + currentTarget
                        + " held=" + heldBefore);

                Container targetContainer = getActualContainer(level, currentTarget, chest);

                // Re-check at deposit time: frames may have changed while the golem walked.
                FrameFilterResult liveFilter = getFrameFilterResult(level, currentTarget, heldBefore);
                ItemStack remaining = heldBefore;
                if (liveFilter.hasFrame() && liveFilter.matchesHeld()
                        && !hasSpaceFor(targetContainer, heldBefore)) {
                    remaining = insertIntoChest(level, currentTarget, targetContainer, heldBefore);
                } else {
                    System.out.println("[SMART-GOLEM DEPOSIT-BLOCKED] Filter missing, empty, mismatched or chest full: " + currentTarget);
                }

                mob.setItemInHand(InteractionHand.MAIN_HAND, remaining);

                chest.setChanged();
                level.blockEntityChanged(currentTarget);

                movedItem = remaining.getCount() < heldBefore.getCount();

                System.out.println("[SMART-GOLEM DEPOSIT END] chest=" + currentTarget
                        + " remainingHand=" + mob.getMainHandItem());
            }

            if (mob instanceof CopperGolem copperGolem) {

                if (movedItem) {
                    copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_DROP);
                    copperGolem.setState(CopperGolemState.DROPPING_ITEM);
                } else {
                    copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_DROP);
                    copperGolem.setState(CopperGolemState.DROPPING_NO_ITEM);
                }
            }

            actionDone = true;

            closeOpenedContainer();
            unlockChest();


            if (!movedItem && !mob.getMainHandItem().isEmpty()) {

                System.out.println("[SMART-GOLEM DEPOSIT-FAILED] chest=" + currentTarget
                        + " still holding=" + mob.getMainHandItem()
                        + " searching for another destination");

                BlockPos alternateDestination = findDestinationChest(level, mob);

                if (alternateDestination != null && !alternateDestination.equals(currentTarget)) {
                    switchState(mob, TaskState.WALK_TO_DESTINATION, alternateDestination, gameTime);
                    return;
                }
            }

            BlockPos sourceToReturn =
                    returnToSourceChest != null
                            ? returnToSourceChest
                            : lastPickupChest;

            if (sourceToReturn != null) {

                returnToSourceChest = sourceToReturn;
                switchState(mob, TaskState.RETURN_TO_SOURCE, sourceToReturn, gameTime);

                System.out.println("[SMART-GOLEM RETURN-TO-SOURCE] returning to=" + sourceToReturn);

            } else {

                System.out.println("[SMART-GOLEM RETURN-FAILED] No saved source chest.");

                taskState = TaskState.IDLE;
                currentTarget = null;
            }

        }
    }

    private void searchForTarget(ServerLevel level, PathfinderMob mob, long gameTime) {

        if (gameTime < nextSearchTick) {
            return;
        }

        nextSearchTick = gameTime + SEARCH_COOLDOWN_TICKS;

        boolean carrying = !mob.getMainHandItem().isEmpty();

        BlockPos best = carrying
                ? findDestinationChest(level, mob)
                : findSourceChest(level, mob);

        if (best == null) {
            return;
        }

        openedContainer = null;
        openedCopperGolem = null;

        if (carrying) {
            System.out.println("[SMART-GOLEM MATCHED-DEPOSIT] Selected destination chest=" + best);
            switchState(mob, TaskState.WALK_TO_DESTINATION, best, gameTime);
        } else {
            System.out.println("[SMART-GOLEM SOURCE] Selected source chest=" + best);
            switchState(mob, TaskState.WALK_TO_SOURCE, best, gameTime);
        }
    }

    /**
     * Collects all candidate positions matching the predicate/filter within
     * the search box, sorted by cheap squared-distance (closest first).
     * Real pathfinding (getPathCost) is only ever evaluated for these
     * candidates in distance order, and we stop as soon as one is reachable
     * and accepted — avoiding O(volume * 27) pathfinding calls.
     */
    private java.util.List<BlockPos> collectCandidates(
            ServerLevel level,
            PathfinderMob mob,
            Predicate<BlockState> blockType
    ) {
        BlockPos mobPos = mob.blockPosition();
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();

        for (int x = -horizontalSearchDistance; x <= horizontalSearchDistance; x++) {
            for (int y = -verticalSearchDistance; y <= verticalSearchDistance; y++) {
                for (int z = -horizontalSearchDistance; z <= horizontalSearchDistance; z++) {

                    BlockPos pos = mobPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (!blockType.test(state)) {
                        continue;
                    }

                    candidates.add(pos.immutable());
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(getRoughDistance(mob, a), getRoughDistance(mob, b)));

        return candidates;
    }

    private BlockPos findSourceChest(ServerLevel level, PathfinderMob mob) {

        for (BlockPos pos : collectCandidates(level, mob, sourceBlockType)) {

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof ChestBlockEntity chest)) {
                continue;
            }

            if (!hasAnyItem(chest)) {
                continue;
            }

            double pathCost = getPathCost(mob, pos);

            if (pathCost == Double.MAX_VALUE) {
                continue;
            }

            return pos;
        }

        return null;
    }

    private BlockPos findDestinationChest(ServerLevel level, PathfinderMob mob) {

        ItemStack held = mob.getMainHandItem();

        if (held.isEmpty()) {
            markDestinationSelection(false, true);
            return null;

        }

        long gameTimeForBlockedChest = level.getGameTime() < blockedUntilTick ? level.getGameTime() : -1L;
        java.util.List<BlockPos> candidates = collectCandidates(level, mob, destinationBlockType);


        for (BlockPos pos : candidates) {

            if (pos.equals(lastPickupChest)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (!(blockEntity instanceof ChestBlockEntity chest)) {
                continue;
            }

            Container targetContainer = getActualContainer(level, pos, chest);

            if (hasSpaceFor(targetContainer, held)) {
                System.out.println("[SMART-GOLEM FULL-CHEST] Skipping matching chest because full: " + pos);
                continue;
            }

            FrameFilterResult frameFilter = getFrameFilterResult(level, pos, held);

            if (!frameFilter.hasFrame()) {
                continue;
            }

            System.out.println("[SMART-GOLEM MATCH-CHECK] chest=" + pos
                    + " matchedFrameItem=" + frameFilter.matchedFrameItem()
                    + " holding=" + held
                    + " matches=" + frameFilter.matchesHeld());

            if (!frameFilter.matchesHeld()) {
                continue;
            }

            // A framed chest can only be selected with a complete walking path.
            // Never route an item into a chest through a floor or ceiling.
            if (gameTimeForBlockedChest != -1L && pos.equals(temporarilyBlockedDestination)) {
                continue;
            }
            double pathCost = getPathCost(mob, pos);
            if (pathCost == Double.MAX_VALUE) {
                System.out.println("[SMART-GOLEM NO-WALKING-PATH] Skip unreachable chest=" + pos);
                continue;
            }
            markDestinationSelection(true, true);
            return pos;
        }


        // Strict routing: never deposit into an unframed or unmatched chest.
        // A missing category leaves the item with its source, not in arbitrary storage.
        System.out.println("[SMART-GOLEM NO-TARGET] No matching filtered chest found for " + held);

        return null;
    }

    private void closeOpenedContainer() {

        if (openedContainer != null && openedCopperGolem != null) {

            if (openedContainer.getEntitiesWithContainerOpen().contains(openedCopperGolem)) {
                openedContainer.stopOpen(openedCopperGolem);
            }

            openedCopperGolem.clearOpenedChestPos();
            openedCopperGolem.setState(CopperGolemState.IDLE);

            System.out.println("[SMART-GOLEM VANILLA-CLOSE]");
        }

        openedContainer = null;
        openedCopperGolem = null;
    }

    private boolean pickupFromChest(ServerLevel level, PathfinderMob mob, ChestBlockEntity chest) {

        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack stack = chest.getItem(i);

            if (!stack.isEmpty()) {

                int takeAmount = Math.min(stack.getCount(), Math.min(stack.getMaxStackSize(), GolemSettings.capacity()));
                ItemStack taken = stack.copyWithCount(takeAmount);

                System.out.println("[SMART-GOLEM PICKUP BEFORE] tick=" + level.getGameTime()
                        + " chest=" + currentTarget
                        + " slot=" + i
                        + " stack=" + stack
                        + " mobHand=" + mob.getMainHandItem());

                stack.shrink(takeAmount);

                chest.setChanged();
                level.blockEntityChanged(currentTarget);

                mob.setItemInHand(InteractionHand.MAIN_HAND, taken);

                lastPickupChest = currentTarget;
                returnToSourceChest = currentTarget;

                System.out.println("[SMART-GOLEM RETURN-SOURCE-SAVED] source=" + returnToSourceChest);

                System.out.println("[SMART-GOLEM PICKUP AFTER] tick=" + level.getGameTime()
                        + " chest=" + currentTarget
                        + " slot=" + i
                        + " remainingStack=" + chest.getItem(i)
                        + " mobHand=" + mob.getMainHandItem());

                return true;
            }
        }

        return false;
    }

    private ItemStack insertIntoChest(
            ServerLevel level,
            BlockPos chestPos,
            Container chest,
            ItemStack held
    ) {

        ItemStack remaining = held.copy();

        for (int i = 0; i < chest.getContainerSize(); i++) {

            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack slot = chest.getItem(i);

            if (slot.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(slot, remaining)) {
                continue;
            }

            if (slot.getCount() >= slot.getMaxStackSize()) {
                continue;
            }

            int moveAmount = Math.min(
                    remaining.getCount(),
                    slot.getMaxStackSize() - slot.getCount()
            );

            System.out.println("[SMART-GOLEM STACK-DEPOSIT BEFORE] chest=" + chestPos
                    + " slot=" + i
                    + " slotBefore=" + slot
                    + " remaining=" + remaining
                    + " moveAmount=" + moveAmount);

            slot.grow(moveAmount);
            remaining.shrink(moveAmount);

            chest.setChanged();
            level.blockEntityChanged(chestPos);

            System.out.println("[SMART-GOLEM STACK-DEPOSIT AFTER] chest=" + chestPos
                    + " slot=" + i
                    + " slotAfter=" + chest.getItem(i)
                    + " remaining=" + remaining);
        }

        for (int i = 0; i < chest.getContainerSize(); i++) {

            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack slot = chest.getItem(i);

            if (!slot.isEmpty()) {
                continue;
            }

            System.out.println("[SMART-GOLEM EMPTY-DEPOSIT BEFORE] chest=" + chestPos
                    + " slot=" + i
                    + " remaining=" + remaining);

            chest.setItem(i, remaining.copy());

            chest.setChanged();
            level.blockEntityChanged(chestPos);

            System.out.println("[SMART-GOLEM EMPTY-DEPOSIT AFTER] chest=" + chestPos
                    + " slot=" + i
                    + " slotAfter=" + chest.getItem(i));

            return ItemStack.EMPTY;
        }

        return remaining;
    }

    private boolean isChestBusy(ChestBlockEntity chest) {
        return !chest.getEntitiesWithContainerOpen().isEmpty();
    }

    private Container getActualContainer(
            ServerLevel level,
            BlockPos pos,
            ChestBlockEntity chest
    ) {

        Container targetContainer = chest;

        if (level.getBlockState(pos).getBlock() instanceof ChestBlock chestBlock) {

            Container combined = ChestBlock.getContainer(
                    chestBlock,
                    level.getBlockState(pos),
                    level,
                    pos,
                    true
            );

            if (combined != null) {
                targetContainer = combined;
            }
        }

        return targetContainer;
    }

    private boolean hasSpaceFor(Container chest, ItemStack item) {

        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack slot = chest.getItem(i);

            if (slot.isEmpty()) {
                return false;
            }

            if (ItemStack.isSameItemSameComponents(slot, item)
                    && slot.getCount() < slot.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private boolean hasAnyItem(ChestBlockEntity chest) {

        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (!chest.getItem(i).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private void notifyMissingDestination(ServerLevel level, ItemStack held, long gameTime) {
        if (held.isEmpty()) return;
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        String key = level.dimension().toString() + ":" + id;
        Long previous = NOTICE_COOLDOWNS.get(key);
        if (previous != null && gameTime - previous < NOTICE_INTERVAL_TICKS) return;
        NOTICE_COOLDOWNS.put(key, gameTime);
        Component message = Component.literal("[Smart Golem] Nenhum bau compativel encontrado para ")
                .append(held.getHoverName())
                .append(Component.literal(" (" + id + ")."));
        for (var player : level.players()) player.sendSystemMessage(message);
    }

    private record FrameFilterResult(
            boolean hasFrame,
            boolean matchesHeld,
            ItemStack matchedFrameItem
    ) {
    }

    private FrameFilterResult getFrameFilterResult(
            ServerLevel level,
            BlockPos chestPos,
            ItemStack held
    ) {
        boolean hasFrame = false;

        AABB box = new AABB(chestPos).inflate(1.0D);

        for (ItemFrame frame : level.getEntitiesOfClass(ItemFrame.class, box)) {

            BlockPos attachedPos
                    = frame.blockPosition().relative(frame.getDirection().getOpposite());

            if (!attachedPos.equals(chestPos)) {

                boolean matchesDoubleChest = false;

                BlockState state = level.getBlockState(chestPos);

                if (state.getBlock() instanceof ChestBlock) {

                    for (BlockPos nearby : new BlockPos[] {
                            chestPos.north(),
                            chestPos.south(),
                            chestPos.east(),
                            chestPos.west()
                    }) {

                        BlockState nearbyState = level.getBlockState(nearby);

                        if (nearbyState.getBlock() == state.getBlock()
                                && attachedPos.equals(nearby)) {

                            matchesDoubleChest = true;
                            break;
                        }
                    }
                }

                if (!matchesDoubleChest) {
                    continue;
                }
            }

            // Even an empty frame reserves this chest. It is never a fallback destination.
            hasFrame = true;
            ItemStack frameItem = frame.getItem();

            if (frameItem.isEmpty()) {
                continue;
            }

            if (!held.isEmpty() && (frame.getRotation() == 0
                    ? ItemStack.isSameItemSameComponents(frameItem, held)
                    : GolemSettings.sameCategory(frameItem, held))) {
                return new FrameFilterResult(true, true, frameItem.copy());
            }
        }

        return new FrameFilterResult(hasFrame, false, ItemStack.EMPTY);
    }

    @Override
    protected void stop(@NonNull ServerLevel level, PathfinderMob mob, long gameTime) {
        closeOpenedContainer();
        unlockChest();

        ticksAtTarget = 0;
        actionDone = false;


        if (!mob.getMainHandItem().isEmpty()) {
            System.out.println("[SMART-GOLEM STOP-CARRYING] keeping source memory, holding="
                    + mob.getMainHandItem()
                    + " returnToSourceChest=" + returnToSourceChest);

            taskState = TaskState.IDLE;
            currentTarget = null;
            currentWalkTarget = null;

            onTravelling.accept(mob);
            return;
        }

        if (returnToSourceChest != null) {
            switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);

            System.out.println("[SMART-GOLEM STOP-KEEP-RETURN] returning to=" + returnToSourceChest);
            onTravelling.accept(mob);
            return;
        }

        taskState = TaskState.IDLE;
        currentTarget = null;
        currentWalkTarget = null;

        onTravelling.accept(mob);
    }

    private void markDestinationSelection(boolean framedMatch, boolean hadReachablePath) {
        this.currentDestinationIsFramedMatch = framedMatch;
        this.currentDestinationHadReachablePath = hadReachablePath;
    }

    private void resetStuckTracking(PathfinderMob mob, long gameTime) {
        this.stuckStartedAt = gameTime;
        this.lastMovementPos = mob.blockPosition().immutable();

        if (currentWalkTarget == null) {
            this.closestDistanceToWalkTargetSqr = Double.MAX_VALUE;
        } else {
            this.closestDistanceToWalkTargetSqr = distanceToCurrentWalkTargetSqr(mob);
        }
    }

    /**
     * A stationary golem must recalculate an alternate physical route.
     * No teleport deposits: if routing fails, it takes its items back.
     */
    private boolean retryRouteIfStuck(ServerLevel level, PathfinderMob mob, long gameTime) {
        if (currentTarget == null || currentWalkTarget == null || hasArrived(mob)) return false;
        BlockPos here = mob.blockPosition();
        if (lastMovementPos == null || !lastMovementPos.equals(here)) {
            lastMovementPos = here.immutable();
            stuckStartedAt = gameTime;
            return false;
        }
        if (gameTime - stuckStartedAt < ROUTE_STUCK_TICKS) return false;

        BlockPos failed = currentTarget;
        temporarilyBlockedDestination = failed;
        blockedUntilTick = gameTime + 1200L;
        System.out.println("[SMART-GOLEM ROUTE-BLOCKED] No walking progress toward " + failed);
        BlockPos alternate = findDestinationChest(level, mob);
        if (alternate != null && !alternate.equals(failed)) {
            switchState(mob, TaskState.WALK_TO_DESTINATION, alternate, gameTime);
        } else {
            BlockPos source = returnToSourceChest != null ? returnToSourceChest : lastPickupChest;
            if (source != null) switchState(mob, TaskState.RETURN_TO_SOURCE, source, gameTime);
            else switchState(mob, TaskState.IDLE, null, gameTime);
        }
        return true;
    }

    private double distanceToCurrentWalkTargetSqr(PathfinderMob mob) {
        if (currentWalkTarget == null) {
            return Double.MAX_VALUE;
        }

        double dx = mob.getX() - (currentWalkTarget.getX() + 0.5D);
        double dy = mob.getY() - currentWalkTarget.getY();
        double dz = mob.getZ() - (currentWalkTarget.getZ() + 0.5D);

        return dx * dx + dy * dy + dz * dz;
    }

    private void spawnMagicDepositParticles(ServerLevel level, PathfinderMob mob, BlockPos chestPos) {

        ColorParticleOption magicParticle =
                ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xB85CFF);

        level.sendParticles(
                magicParticle,
                mob.getX(),
                mob.getY() + 1.0D,
                mob.getZ(),
                24,
                0.35D,
                0.45D,
                0.35D,
                0.05D
        );

        level.sendParticles(
                magicParticle,
                chestPos.getX() + 0.5D,
                chestPos.getY() + 0.8D,
                chestPos.getZ() + 0.5D,
                24,
                0.45D,
                0.35D,
                0.45D,
                0.05D
        );
    }
}
