/* Modified by Kesley for Smart Copper Golems Extended, 2026-10-19: category filters and deposit revalidation. Original: GameMech007, Apache-2.0. */
package com.anantaya.smartcgolem.ai;

import com.google.common.collect.ImmutableMap;
import com.anantaya.smartcgolem.util.SearchVolume;
import com.anantaya.smartcgolem.ServerWorkZoneSettings;
import com.anantaya.smartcgolem.util.ItemFrameHelper;
import com.anantaya.smartcgolem.util.StorageFrameIndex;
import com.anantaya.smartcgolem.util.GolemRoutingRules;
import com.anantaya.smartcgolem.ServerCarrySettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import com.anantaya.smartcgolem.util.ContainerAlphabetizer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.core.particles.ParticleTypes;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SmartTransportItemsBehavior extends Behavior<PathfinderMob> {
    private long stretchStageStartTick = -1L;
    private boolean stretchActive = false;
    private BlockPos stretchedChestTarget;
    private int stretchMaxProgress = 1000;

    /** Keep the body and head directed at the actual chest throughout the reach. */
    private void faceStretchTarget(PathfinderMob mob) {
        BlockPos chest = stretchedChestTarget;
        if (chest == null) return;
        double dx = chest.getX() + 0.5D - mob.getX();
        double dz = chest.getZ() + 0.5D - mob.getZ();
        double horizontal = dx * dx + dz * dz;
        if (horizontal < 0.0001D) return;
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        mob.getNavigation().stop();
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.getLookControl().setLookAt(chest.getX() + 0.5D, chest.getY() + 0.5D, chest.getZ() + 0.5D, 360.0F, 360.0F);
    }

    private enum TaskState {
        IDLE,
        WALK_TO_SOURCE,
        WAIT_FOR_SOURCE,
        INTERACT_SOURCE,
        WALK_TO_DESTINATION,
        WAIT_FOR_DESTINATION,
        INTERACT_DESTINATION,
        REMOTE_CLOSE,
        STRETCH_GROW,
        STRETCH_SHRINK,
        RETURN_TO_SOURCE
    }

    // A block position alone is not unique: different dimensions share coordinates.
    private record ChestLockKey(ResourceKey<Level> dimension, BlockPos pos) {}
    private record ChestLockLease(SmartTransportItemsBehavior owner, long lastTouchedTick) {}

    private static final Map<ChestLockKey, ChestLockLease> LOCKED_CHESTS = new HashMap<>();

    private static final long LOCK_STALE_TICKS = 600;

    // Short fallback for non-elevated unreachable chests. Elevated chests use
    // immediate, distance-gated animation, never a fixed 100-tick delay.
    private static final int MAGIC_DEPOSIT_STUCK_TICKS = 20;
    // Keep the remote lid visibly open long enough for a client animation.
    private static final int REMOTE_LID_OPEN_TICKS = 10;
    private static final int REMOTE_CHEST_LID_OPEN_TICKS = 24;
    private long remoteCloseAt = -1L;
    private ServerLevel remoteChestLevel;
    private BlockPos remoteChestPos;

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
    private BlockPos cachedHopGoal = null;
    private BlockPos cachedHopTarget = null;
    private BlockPos cachedDescentGoal = null;
    private BlockPos cachedDescentWaypoint = null;
    private long nextDescentProbeTick = 0L;
    private BlockPos lastPickupChest = null;
    private BlockPos returnToSourceChest = null;
    private ChestLockKey lockedChest = null;

    private Container openedContainer = null;
    private CopperGolem openedCopperGolem = null;

    private boolean actionDone = false;

    private int ticksAtTarget = 0;
    private long nextSearchTick = 0;
    private long lastNoDestinationAlertTick = -100000L;
    private String lastNoDestinationAlertItemId = "";

    private static final int ARRIVAL_DISTANCE_SQUARED = 4;
    // Wider work zones are scanned only once every 5 seconds per golem.
    private static final int SEARCH_COOLDOWN_TICKS = 100;

    public SmartTransportItemsBehavior(
            float speedModifier,
            Predicate<BlockState> sourceBlockType,
            Predicate<BlockState> destinationBlockType,
            int horizontalSearchDistance,
            int verticalSearchDistance,
            Consumer<PathfinderMob> onTravelling
    ) {
        // A complete remote stretch includes 100 ticks of stuck detection,
        // 101 ticks growing, chest animation and 101 ticks shrinking.
        // Prevent vanilla Behavior timeout from interrupting midway.
        super(ImmutableMap.of(), 1200);

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
        remoteChestLevel = null;
        remoteChestPos = null;
        remoteCloseAt = -1L;
        actionDone = false;
        ticksAtTarget = 0;
        taskState = TaskState.IDLE;
        stretchActive = false;
        stretchedChestTarget = null;
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

    /**
     * Reserve a chest for this golem. Leases are scoped to a dimension and owner;
     * an expired owner's eventual cleanup must never remove a successor's lease.
     * Returns true when another golem currently owns the reservation.
     */
    private boolean tryLockChest(ResourceKey<Level> dimension, BlockPos pos, long gameTime) {
        if (pos == null) return true;

        ChestLockKey key = new ChestLockKey(dimension, pos);
        synchronized (LOCKED_CHESTS) {
            ChestLockLease existing = LOCKED_CHESTS.get(key);
            if (existing != null && existing.owner() != this
                    && gameTime - existing.lastTouchedTick() <= LOCK_STALE_TICKS) {
                return true;
            }

            if (existing != null && existing.owner() != this) {
                System.out.println("[SMART-GOLEM CHEST-LOCK-STALE] reclaiming stale lock=" + key);
            }

            LOCKED_CHESTS.put(key, new ChestLockLease(this, gameTime));
            lockedChest = key;
            return false;
        }
    }

    /** Keep the original availability check used by both WAIT states. */
    private boolean isChestLocked(ResourceKey<Level> dimension, BlockPos pos, long gameTime) {
        ChestLockKey key = new ChestLockKey(dimension, pos);
        synchronized (LOCKED_CHESTS) {
            ChestLockLease existing = LOCKED_CHESTS.get(key);
            if (existing == null) return true;
            if (gameTime - existing.lastTouchedTick() > LOCK_STALE_TICKS) {
                LOCKED_CHESTS.remove(key);
                return true;
            }
            return false;
        }
    }

    private void unlockChest() {
        if (lockedChest == null) return;
        synchronized (LOCKED_CHESTS) {
            ChestLockLease existing = LOCKED_CHESTS.get(lockedChest);
            if (existing != null && existing.owner() == this) {
                LOCKED_CHESTS.remove(lockedChest);
                System.out.println("[SMART-GOLEM CHEST-UNLOCK] unlocked=" + lockedChest);
            }
        }
        lockedChest = null;
    }

    private void switchState(PathfinderMob mob, TaskState newState, BlockPos target, long gameTime) {

        if (needsChestLock(newState)) {
            if (tryLockChest(mob.level().dimension(), target, gameTime)) {
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

                BaseContainerBlockEntity chest = getSourceBlockEntity(level, currentTarget);

                if (chest == null) {
                    switchState(mob, TaskState.IDLE, null, gameTime);
                    return;
                }

                if (isChestLocked(level.dimension(), currentTarget, gameTime) && !isChestBusy(chest)) {
                    switchState(mob, TaskState.INTERACT_SOURCE, currentTarget, gameTime);
                }
            }

            case INTERACT_SOURCE -> interactWithSource(level, mob, gameTime);

            case WALK_TO_DESTINATION -> {
                // Start stretching as soon as we are physically near an elevated
                // filtered destination, even if navigation considers it arrived.
                if (startStretchNearElevatedChest(level, mob, gameTime)) return;
                walkToTarget(mob, gameTime);

                if (tryMagicDepositIfStuck(level, mob, gameTime)) {
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

                BaseContainerBlockEntity chest = getDestinationBlockEntity(level, currentTarget);

                if (chest == null) {
                    switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
                    return;
                }

                if (isChestLocked(level.dimension(), currentTarget, gameTime) && !isChestBusy(chest)) {
                    if (!startStretchNearElevatedChest(level, mob, gameTime))
                        switchState(mob, TaskState.INTERACT_DESTINATION, currentTarget, gameTime);
                }
            }

            case INTERACT_DESTINATION -> interactWithDestination(level, mob, gameTime);

            case STRETCH_GROW -> {
                faceStretchTarget(mob);
                long elapsed = gameTime - stretchStageStartTick;
                if (mob instanceof CopperGolem copper && elapsed % 20 == 0)
                    com.anantaya.smartcgolem.animation.StretchManager.set(level, copper,
                            com.anantaya.smartcgolem.animation.StretchManager.GROW, (int)Math.min(elapsed,101L), stretchMaxProgress);
                if (elapsed >= com.anantaya.smartcgolem.animation.StretchManager.DURATION) {
                    if (mob instanceof CopperGolem copper)
                        com.anantaya.smartcgolem.animation.StretchManager.set(level, copper,
                                com.anantaya.smartcgolem.animation.StretchManager.HOLD, 0, stretchMaxProgress);
                    // Interaction only after the visual growth has completed.
                    if (!magicDepositIntoMatchedChest(level, mob, gameTime)) {
                        startShrink(level, mob, gameTime);
                    } else if (taskState != TaskState.REMOTE_CLOSE) {
                        startShrink(level, mob, gameTime);
                    }
                }
            }

            case STRETCH_SHRINK -> {
                faceStretchTarget(mob);
                long elapsed = gameTime - stretchStageStartTick;
                if (mob instanceof CopperGolem copper && elapsed % 20 == 0)
                    com.anantaya.smartcgolem.animation.StretchManager.set(level, copper,
                            com.anantaya.smartcgolem.animation.StretchManager.SHRINK, (int)Math.min(elapsed,101L), stretchMaxProgress);
                if (elapsed >= com.anantaya.smartcgolem.animation.StretchManager.DURATION) {
                    if (mob instanceof CopperGolem copper)
                        com.anantaya.smartcgolem.animation.StretchManager.stop(level, copper);
                    stretchActive = false;
                    BlockPos previousTarget = stretchedChestTarget;
                    stretchedChestTarget = null;
                    continueAfterRemoteDeposit(level, mob, gameTime, previousTarget);
                }
            }

            case REMOTE_CLOSE -> {
                // Native opener state and sound must persist across ticks:
                // startOpen+stopOpen in the same tick would not visibly animate.
                if (stretchActive) faceStretchTarget(mob);
                else {
                    mob.getNavigation().stop();
                    if (currentTarget != null) mob.getLookControl().setLookAt(
                            currentTarget.getX() + 0.5,
                            currentTarget.getY() + 0.5,
                            currentTarget.getZ() + 0.5);
                }
                if (remoteChestLevel != null && remoteChestPos != null && gameTime < remoteCloseAt) {
                    // Vanilla periodic opener checks may reset the lid because
                    // this golem is physically too far away to count as an opener.
                    // Reassert only the visual lid event; NEVER replay the sound.
                    sendRemoteChestLidEvent(remoteChestLevel, remoteChestPos, true);
                }
                if (gameTime >= remoteCloseAt) {
                    closeOpenedContainer();
                    unlockChest();
                    if (stretchActive) startShrink(level, mob, gameTime);
                    else continueAfterRemoteDeposit(level, mob, gameTime, currentTarget);
                }
            }

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
            switchState(mob, TaskState.IDLE, null, gameTime);
            return;
        }

        // Minecraft navigation has a much shorter practical path range
        // than the configured 500-block work square. Travel in short hops
        // towards distant storage, while retaining the actual target for
        // arrival checks and the existing stuck/magic-deposit logic.
        BlockPos navTarget = shortHop(mob, currentWalkTarget);
        // When a basement storage room lies directly below, a straight
        // X/Z waypoint can point at solid floor. Try a reachable staircase
        // or ladder leading down before giving up on natural movement.
        if (currentTarget != null
                && currentTarget.getY() < mob.blockPosition().getY() - 2) {
            if (gameTime >= nextDescentProbeTick
                    || !currentTarget.equals(cachedDescentGoal)) {
                cachedDescentGoal = currentTarget.immutable();
                cachedDescentWaypoint = findNearbyDescent(mob, currentTarget);
                nextDescentProbeTick = gameTime + 100L;
            }
            if (cachedDescentWaypoint != null
                    && mob.blockPosition().distSqr(cachedDescentWaypoint) > 4) {
                navTarget = cachedDescentWaypoint;
            }
            // Re-evaluate a newly reachable point beside the lower chest
            // as the golem approaches a staircase entrance.
            if (gameTime % 60L == 0L && mob.blockPosition().distSqr(currentTarget) < 32 * 32) {
                BlockPos better = findBestWalkTargetForChest(mob, currentTarget);
                if (!better.equals(currentTarget)) currentWalkTarget = better;
            }
        } else {
            cachedDescentGoal = null;
            cachedDescentWaypoint = null;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(mob, navTarget, speedModifier, 0);
    }

    private BlockPos findNearbyDescent(PathfinderMob mob, BlockPos destination) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        BlockPos here = mob.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        int tested = 0;
        // Bounded, infrequent scan (16 block radius) using ONLY loaded
        // chunks. Do not search paths for every floor block.
        for (int x = here.getX() - 16; x <= here.getX() + 16; x++) {
            for (int z = here.getZ() - 16; z <= here.getZ() + 16; z++) {
                if (level.getChunkSource().getChunkNow(Math.floorDiv(x,16),
                        Math.floorDiv(z,16)) == null) continue;
                for (int y = here.getY() - 5; y <= here.getY(); y++) {
                    BlockPos base = new BlockPos(x, y, z);
                    var state = level.getBlockState(base);
                    if (!(state.getBlock() instanceof StairBlock)
                            && !state.is(Blocks.LADDER)
                            && !state.is(Blocks.SCAFFOLDING)) continue;
                    BlockPos waypoint = base.above();
                    if (waypoint.distSqr(here) <= 4) continue;
                    // Limit expensive navigation probes even for a huge
                    // decorative stairway covered in stair blocks.
                    if (++tested > 20) return best;
                    Path path = mob.getNavigation().createPath(waypoint, 1);
                    if (path == null || !path.canReach()) continue;
                    double score = here.distSqr(waypoint)
                            + waypoint.distSqr(destination) * 0.15
                            + Math.max(0, waypoint.getY() - destination.getY()) * 1.5;
                    if (score < bestScore) {
                        bestScore = score;
                        best = waypoint.immutable();
                    }
                }
            }
        }
        return best;
    }

    private BlockPos shortHop(PathfinderMob mob, BlockPos target) {
        BlockPos origin = mob.blockPosition();
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance <= 20) {
            cachedHopGoal = null;
            cachedHopTarget = null;
            return target;
        }
        // Keep the waypoint fixed until the golem reaches it. Otherwise
        // moving the waypoint every tick causes avoidable path recalculations.
        if (target.equals(cachedHopGoal) && cachedHopTarget != null
                && origin.distSqr(cachedHopTarget) > 9) return cachedHopTarget;
        double scale = 16.0 / distance;
        cachedHopGoal = target.immutable();
        cachedHopTarget = new BlockPos(
                origin.getX() + (int)Math.round(dx * scale),
                origin.getY(),
                origin.getZ() + (int)Math.round(dz * scale));
        return cachedHopTarget;
    }

    private boolean hasArrived(PathfinderMob mob) {
        return currentWalkTarget != null
                && mob.blockPosition().distSqr(currentWalkTarget)
                <= ARRIVAL_DISTANCE_SQUARED;
    }

    /**
     * Cheap squared-distance estimate used to rank candidate chests before
     * we spend any time on real pathfinding. This avoids running
     * createPath() for every block in the search volume.
     */
    private double getRoughDistance(PathfinderMob mob, BlockPos pos) {
        return mob.blockPosition().distSqr(pos);
    }

    private double getPathCost(PathfinderMob mob, BlockPos chestPos) {

        double bestCost = Double.MAX_VALUE;

        for (BlockPos nearby : BlockPos.betweenClosed(
                chestPos.offset(-1, 0, -1),
                chestPos.offset(1, 1, 1))) {

            Path path = mob.getNavigation().createPath(nearby, 1);

            if (path == null || !path.canReach()) {
                continue;
            }

            double cost = path.getNodeCount();

            if (cost < bestCost) {
                bestCost = cost;
            }
        }

        if (bestCost == Double.MAX_VALUE) {
            System.out.println("[SMART-GOLEM PATH-SKIP] Cannot path near " + chestPos);
        }

        return bestCost;
    }

    private BlockPos findBestWalkTargetForChest(PathfinderMob mob, BlockPos chestPos) {

        BlockPos best = chestPos;
        double bestCost = Double.MAX_VALUE;

        for (BlockPos nearby : BlockPos.betweenClosed(
                chestPos.offset(-1, 0, -1),
                chestPos.offset(1, 1, 1))) {

            Path path = mob.getNavigation().createPath(nearby, 1);

            if (path == null || !path.canReach()) {
                continue;
            }

            double cost = path.getNodeCount();

            if (returnToSourceChest != null) {
                cost += Math.abs(nearby.getY() - returnToSourceChest.getY()) * 200;
            }

            if (cost < bestCost) {
                bestCost = cost;
                best = nearby.immutable();
            }
        }

        return best;
    }

    private void interactWithSource(ServerLevel level, PathfinderMob mob, long gameTime) {

        if (currentTarget == null) {
            closeOpenedContainer();
            switchState(mob, TaskState.IDLE, null, gameTime);
            return;
        }

        BaseContainerBlockEntity chest = getSourceBlockEntity(level, currentTarget);

        if (chest == null) {
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
                    System.out.println("[SMART-GOLEM NO-DESTINATION] No valid destination found after pickup.");
                    switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
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

        BaseContainerBlockEntity chest = getDestinationBlockEntity(level, currentTarget);

        if (chest == null || ItemFrameHelper.hasSourceMarker(level, currentTarget)) {
            System.out.println("[SMART-GOLEM ERROR] Destination target is not a supported chest/barrel at " + currentTarget);
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

                // The vanilla opener counter produces BARREL_OPEN, OPEN=true
                // and later BARREL_CLOSE when stopOpen is called.
                copperGolem.setOpenedChestPos(currentTarget);
                chest.startOpen(copperGolem);

                if (!copperGolem.getMainHandItem().isEmpty()) {
                    copperGolem.setState(CopperGolemState.DROPPING_ITEM);
                }

                openedContainer = chest;
                openedCopperGolem = copperGolem;

                System.out.println("[SMART-GOLEM VANILLA-OPEN] destination=" + currentTarget);
            }

            return;
        }

        // Accessible chest lids previously closed in only eight ticks.
        // Give the vanilla lid time to open before the deposit completes.
        // Barrels keep their previously validated timing and sounds.
        int interactDelay = chest instanceof ChestBlockEntity
                ? REMOTE_CHEST_LID_OPEN_TICKS + 1 : 9;
        if (ticksAtTarget < interactDelay) {
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

                ItemStack remaining =
                        insertIntoChest(level, currentTarget, targetContainer, heldBefore);

                mob.setItemInHand(InteractionHand.MAIN_HAND, remaining);

                chest.setChanged();
                level.blockEntityChanged(currentTarget);

                movedItem = remaining.getCount() < heldBefore.getCount();
                if (movedItem) {
                    // Sorting happens while this golem still owns the lock.
                    // Works for both single barrels and combined double chests.
                    ContainerAlphabetizer.sortAfterDeposit(targetContainer);
                }

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
        if (!carrying && mob instanceof CopperGolem golem && isNight(level)) {
            returnHome(mob, golem);
            return;
        }

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
     * Enumerate ONLY loaded chunk block-entity positions instead of probing
     * all 65 x 17 x 65 (71,825) world blocks every time a golem searches.
     *
     * Both copper and regular chests own block entities; preserving the same
     * inclusive range and nearest-first ordering keeps sorting behaviour.
     * getChunkNow never force-loads chunks while the golem searches.
     */
    private java.util.List<BlockPos> collectCandidates(
            ServerLevel level,
            PathfinderMob mob,
            Predicate<BlockState> blockType
    ) {
        BlockPos mobPos = mob.blockPosition();
        BlockPos home = mob instanceof CopperGolem golem
                ? ServerWorkZoneSettings.anchor(golem) : mobPos;
        SearchVolume area = new SearchVolume(
                home.getX(), mobPos.getY(), home.getZ(),
                ServerWorkZoneSettings.halfWidth(), verticalSearchDistance
        );
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();

        // Local loaded chunks only: never ticket or load a distant chunk.
        for (int cx = area.minChunkX(); cx <= area.maxChunkX(); cx++) {
            for (int cz = area.minChunkZ(); cz <= area.maxChunkZ(); cz++) {
                // The sorter should not load chunks just to search for storage.
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (!area.contains(pos.getX(), pos.getY(), pos.getZ())
                            || !ServerWorkZoneSettings.contains(
                                    home, pos)) continue;
                    BlockState state = chunk.getBlockState(pos);
                    if (!blockType.test(state)) continue;
                    candidates.add(pos.immutable());
                }
            }
        }

        candidates.sort((a, b) -> Double.compare(getRoughDistance(mob, a), getRoughDistance(mob, b)));
        return candidates;
    }

    private BlockPos findSourceChest(ServerLevel level, PathfinderMob mob) {

        // Copper source chests remain backwards compatible. Stick-marked
        // ordinary chests and barrels come from the shared frame index.
        java.util.LinkedHashSet<BlockPos> combined = new java.util.LinkedHashSet<>(
                collectCandidates(level, mob, sourceBlockType));
        combined.addAll(StorageFrameIndex.markedSources(level));
        java.util.List<BlockPos> sources = new java.util.ArrayList<>(combined);
        sources.sort((a,b) -> Double.compare(getRoughDistance(mob,a), getRoughDistance(mob,b)));
        BlockPos home = mob instanceof CopperGolem c ? ServerWorkZoneSettings.anchor(c) : mob.blockPosition();
        for (BlockPos pos : sources) {
            if (!ServerWorkZoneSettings.contains(home, pos)
                    || Math.abs((long)pos.getY() - mob.blockPosition().getY()) > 64) continue;

            BaseContainerBlockEntity chest = getSourceBlockEntity(level, pos);
            if (chest == null || !hasAnyItem(getActualContainer(level, pos, chest))) continue;

            // Only require an immediate path when close. For distant
            // marked inputs, the golem travels in navigation-sized hops.
            if (mob.blockPosition().distSqr(pos) <= 24 * 24
                    && getPathCost(mob, pos) == Double.MAX_VALUE) continue;

            return pos;
        }

        return null;
    }

    /**
     * Strict routing: only indexed, loaded frames that explicitly match this
     * item can be selected. There is NO fallback to unframed containers and
     * NO last-resort return deposit into the source chest.
     */
    private BlockPos findDestinationChest(ServerLevel level, PathfinderMob mob) {
        ItemStack held = mob.getMainHandItem();
        if (held.isEmpty()) {
            markDestinationSelection(false, true);
            return null;
        }
        BlockPos home = mob instanceof CopperGolem c
                ? ServerWorkZoneSettings.anchor(c) : mob.blockPosition();
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>(
                StorageFrameIndex.destinations(level, held));
        candidates.sort((a,b) -> Double.compare(getRoughDistance(mob,a), getRoughDistance(mob,b)));
        for (BlockPos pos : candidates) {
            if (!ServerWorkZoneSettings.contains(home, pos)
                    || Math.abs((long)pos.getY() - mob.blockPosition().getY()) > 64) continue;
            if (pos.equals(lastPickupChest) || ItemFrameHelper.hasSourceMarker(level, pos)) continue;
            BaseContainerBlockEntity chest = getDestinationBlockEntity(level, pos);
            if (chest == null) continue;
            FrameFilterResult filter = getFrameFilterResult(level, pos, held);
            if (!GolemRoutingRules.accepts(filter.hasFrame(), filter.matchesHeld(), false)) continue;
            Container actual = getActualContainer(level, pos, chest);
            if (hasSpaceFor(actual, held)) continue;
            double pathCost = getPathCost(mob, pos);
            boolean reachable = pathCost != Double.MAX_VALUE;
            // Even when directly above a basement chest, keep it selected:
            // the staircase detour logic needs the target to remain active.
            // Remote depositing below solid floors is separately forbidden.
            markDestinationSelection(true, reachable);
            return pos;
        }
        markDestinationSelection(false, true);
        reportMissingDestination(level, mob, held);
        return null;
    }

    private void reportMissingDestination(ServerLevel level, PathfinderMob mob, ItemStack held) {
        String id = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        long now = level.getGameTime();
        if (lastNoDestinationAlertItemId.equals(id)
                && now - lastNoDestinationAlertTick < 2400L) return;
        lastNoDestinationAlertTick = now;
        lastNoDestinationAlertItemId = id;
        String category = com.anantaya.smartcgolem.util.SmartCategories.category(held);
        String message = "[Smart Golem] Nenhum baú com moldura compatível para "
                + held.getHoverName().getString() + " (" + id + ")."
                + (category == null ? " Item sem categoria definida." : " Categoria: " + category + ".");
        for (var player : level.players()) {
            if (player.distanceToSqr(mob) <= 64 * 64)
                player.sendSystemMessage(Component.literal(message));
        }
        System.out.println(message);
    }

    /**
     * Block event 1 is the vanilla chest lid animation (0=closed, 1=open).
     * A double chest requires notifications for both physical chest blocks.
     * This affects only the remote chest visual path; reachable chests and
     * barrels continue to use vanilla startOpen/stopOpen.
     */
    private void sendRemoteChestLidEvent(ServerLevel level, BlockPos pos, boolean opening) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return;
        level.blockEvent(pos, state.getBlock(), 1, opening ? 1 : 0);
        var type = state.getValue(ChestBlock.TYPE);
        if (type != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
            BlockPos partnerPos = pos.relative(ChestBlock.getConnectedDirection(state));
            BlockState partner = level.getBlockState(partnerPos);
            if (partner.getBlock() == state.getBlock()
                    && partner.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE) {
                level.blockEvent(partnerPos, partner.getBlock(), 1, opening ? 1 : 0);
            }
        }
    }

    private void closeOpenedContainer() {
        // Distant chests cannot rely on the vanilla opener counter: it
        // periodically checks nearby entities and excludes a distant golem.
        // Close the client lid explicitly, including both halves of a double chest.
        if (remoteChestLevel != null && remoteChestPos != null) {
            sendRemoteChestLidEvent(remoteChestLevel, remoteChestPos, false);
            remoteChestLevel.playSound(null, remoteChestPos, SoundEvents.CHEST_CLOSE,
                    SoundSource.BLOCKS, 0.5F, 1.0F);
            remoteChestLevel = null;
            remoteChestPos = null;
        }

        if (openedContainer != null && openedCopperGolem != null) {

            if (openedContainer.getEntitiesWithContainerOpen().contains(openedCopperGolem)) {
                // Preserve vanilla animation and sound for both container types.
                openedContainer.stopOpen(openedCopperGolem);
            }

            openedCopperGolem.clearOpenedChestPos();
            openedCopperGolem.setState(CopperGolemState.IDLE);

            System.out.println("[SMART-GOLEM VANILLA-CLOSE]");
        }

        openedContainer = null;
        openedCopperGolem = null;
    }

    private boolean pickupFromChest(ServerLevel level, PathfinderMob mob, BaseContainerBlockEntity chest) {

        Container contents = getActualContainer(level, currentTarget, chest);
        for (int i = 0; i < contents.getContainerSize(); i++) {
            ItemStack stack = contents.getItem(i);

            if (!stack.isEmpty()) {

                int takeAmount = GolemRoutingRules.amountToCarry(stack.getCount(), stack.getMaxStackSize(), ServerCarrySettings.maximum());
                ItemStack taken = stack.copyWithCount(takeAmount);

                System.out.println("[SMART-GOLEM PICKUP BEFORE] tick=" + level.getGameTime()
                        + " chest=" + currentTarget
                        + " slot=" + i
                        + " stack=" + stack
                        + " mobHand=" + mob.getMainHandItem());

                stack.shrink(takeAmount);
                contents.setChanged();
                chest.setChanged();
                level.blockEntityChanged(currentTarget);

                mob.setItemInHand(InteractionHand.MAIN_HAND, taken);

                lastPickupChest = currentTarget;
                returnToSourceChest = currentTarget;

                System.out.println("[SMART-GOLEM RETURN-SOURCE-SAVED] source=" + returnToSourceChest);

                System.out.println("[SMART-GOLEM PICKUP AFTER] tick=" + level.getGameTime()
                        + " chest=" + currentTarget
                        + " slot=" + i
                        + " remainingStack=" + contents.getItem(i)
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

        // Re-read filters at insertion time: frames may change while the golem walks.
        FrameFilterResult filter = getFrameFilterResult(level, chestPos, held);
        // Never deposit into an empty/unframed destination, even if the
        // user removes its moldura between path selection and insertion.
        if (!GolemRoutingRules.accepts(filter.hasFrame(), filter.matchesHeld(),
                ItemFrameHelper.hasSourceMarker(level, chestPos))) return held.copy();
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

    private boolean isChestBusy(BaseContainerBlockEntity chest) {
        // Both vanilla chests and barrels expose their current ContainerUser openers.
        return !chest.getEntitiesWithContainerOpen().isEmpty();
    }

    private BaseContainerBlockEntity getSourceBlockEntity(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        if (sourceBlockType.test(state) && entity instanceof ChestBlockEntity chest) return chest;
        if (ItemFrameHelper.hasSourceMarker(level, pos)) return getDestinationBlockEntity(level, pos);
        return null;
    }

    private boolean isNight(ServerLevel level) {
        long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return t >= 13000L && t < 23000L;
    }

    private void returnHome(PathfinderMob mob, CopperGolem golem) {
        BlockPos home = ServerWorkZoneSettings.anchor(golem);
        if (mob.blockPosition().distSqr(home) > 9) {
            BehaviorUtils.setWalkAndLookTargetMemories(mob,
                    shortHop(mob, home), speedModifier, 2);
        } else {
            mob.getNavigation().stop();
        }
    }

    /** Copper chests remain sources; shulker boxes are not destinations. */
    private BaseContainerBlockEntity getDestinationBlockEntity(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity entity = level.getBlockEntity(pos);
        if ((state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST))
                && entity instanceof ChestBlockEntity chest) return chest;
        if (state.is(Blocks.BARREL) && entity instanceof BarrelBlockEntity barrel) return barrel;
        return null;
    }

    private Container getActualContainer(
            ServerLevel level,
            BlockPos pos,
            BaseContainerBlockEntity chest
    ) {

        // A barrel is a single container; only chests can combine.
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

    private boolean hasAnyItem(Container chest) {

        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (!chest.getItem(i).isEmpty()) {
                return true;
            }
        }

        return false;
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
        var result = com.anantaya.smartcgolem.util.ItemFrameHelper.filter(level, chestPos, held);
        return new FrameFilterResult(result.hasFrame(), result.matchesHeld(), result.matchedFrameItem());
    }

    @Override
    protected void stop(@NonNull ServerLevel level, PathfinderMob mob, long gameTime) {
        closeOpenedContainer();
        unlockChest();
        if (mob instanceof CopperGolem copper) {
            com.anantaya.smartcgolem.animation.StretchManager.stop(level, copper);
        }
        stretchActive = false;
        stretchedChestTarget = null;
        stretchMaxProgress = 1000;

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

        if (currentWalkTarget == null) {
            this.closestDistanceToWalkTargetSqr = Double.MAX_VALUE;
        } else {
            this.closestDistanceToWalkTargetSqr = distanceToCurrentWalkTargetSqr(mob);
        }
    }

    private boolean tryMagicDepositIfStuck(ServerLevel level, PathfinderMob mob, long gameTime) {

        if (!currentDestinationIsFramedMatch) {
            return false;
        }
        // Underground chests must be reached via real paths (e.g. stairs);
        // do not teleport inventory vertically through a solid floor.
        if (currentTarget != null && !GolemRoutingRules.remoteDepositMayCrossHeight(
                    currentTarget.getY(), mob.blockPosition().getY())) {
            return false;
        }

        if (currentTarget == null || currentWalkTarget == null) {
            return false;
        }

        if (mob.getMainHandItem().isEmpty()) {
            return false;
        }

        if (hasArrived(mob)) {
            resetStuckTracking(mob, gameTime);
            return false;
        }

        double currentDistance = distanceToCurrentWalkTargetSqr(mob);

        if (closestDistanceToWalkTargetSqr == Double.MAX_VALUE) {
            closestDistanceToWalkTargetSqr = currentDistance;
            stuckStartedAt = gameTime;
            return false;
        }


        if (currentDistance < closestDistanceToWalkTargetSqr - 0.25D) {
            closestDistanceToWalkTargetSqr = currentDistance;
            stuckStartedAt = gameTime;
            return false;
        }

        if (stuckStartedAt < 0L) {
            stuckStartedAt = gameTime;
            return false;
        }

        if (gameTime - stuckStartedAt < MAGIC_DEPOSIT_STUCK_TICKS) {
            return false;
        }

        System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-TRY] target=" + currentTarget
                + " holding=" + mob.getMainHandItem()
                + " reachableAtSearch=" + currentDestinationHadReachablePath
                + " distanceSqr=" + currentDistance
                + " bestDistanceSqr=" + closestDistanceToWalkTargetSqr);

        if (startStretchNearElevatedChest(level, mob, gameTime)) return true;
        return magicDepositIntoMatchedChest(level, mob, gameTime);
    }

    /**
     * One Blockbench animation for 2-5-high chests. Animation progress is
     * clamped to the height difference instead of always reaching five blocks.
     * Only start remotely when next to the chest horizontally, preventing
     * teleporting items from across the room.
     */
    private boolean startStretchNearElevatedChest(ServerLevel level, PathfinderMob mob, long gameTime) {
        if (!(mob instanceof CopperGolem copper) || stretchActive || currentTarget == null
                || !currentDestinationIsFramedMatch || mob.getMainHandItem().isEmpty()) return false;
        int height = currentTarget.getY() - mob.blockPosition().getY();
        if (height < 2 || height > 5) return false;
        double dx = mob.getX() - (currentTarget.getX() + 0.5);
        double dz = mob.getZ() - (currentTarget.getZ() + 0.5);
        if (dx*dx + dz*dz > 9.0) return false;
        // Validate frame and destination before commencing long animation.
        if (getDestinationBlockEntity(level, currentTarget) == null
                || ItemFrameHelper.hasSourceMarker(level, currentTarget)) return false;
        FrameFilterResult filter = getFrameFilterResult(level, currentTarget, mob.getMainHandItem());
        if (!filter.hasFrame() || !filter.matchesHeld()) return false;

        // The animation's max displacement is authored for a chest at ~5 blocks;
        // 2/3/4-block chests use 25/50/75% of its authored timeline.
        stretchMaxProgress = Math.max(250, Math.min(1000, (height - 1) * 250));
        stretchActive = true;
        stretchedChestTarget = currentTarget.immutable();
        stretchStageStartTick = gameTime;
        taskState = TaskState.STRETCH_GROW;
        faceStretchTarget(mob);
        com.anantaya.smartcgolem.animation.StretchManager.set(level, copper,
                com.anantaya.smartcgolem.animation.StretchManager.GROW, 0, stretchMaxProgress);
        return true;
    }

    private void startShrink(ServerLevel level, PathfinderMob mob, long gameTime) {
        if (!stretchActive) return;
        stretchStageStartTick = gameTime;
        taskState = TaskState.STRETCH_SHRINK;
        faceStretchTarget(mob);
        if (mob instanceof CopperGolem copper)
            com.anantaya.smartcgolem.animation.StretchManager.set(level, copper,
                    com.anantaya.smartcgolem.animation.StretchManager.SHRINK, 0, stretchMaxProgress);
    }

    private boolean magicDepositIntoMatchedChest(ServerLevel level, PathfinderMob mob, long gameTime) {

        BlockPos depositTarget = currentTarget;

        BaseContainerBlockEntity chest = getDestinationBlockEntity(level, depositTarget);

        if (chest == null || ItemFrameHelper.hasSourceMarker(level, depositTarget)) {
            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-CANCEL] Target is not a supported chest/barrel: " + depositTarget);
            markDestinationSelection(false, true);
            switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            return true;
        }

        if (isChestBusy(chest)) {
            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-WAIT] Chest busy: " + depositTarget);
            resetStuckTracking(mob, gameTime);
            return false;
        }

        ItemStack heldBefore = mob.getMainHandItem().copy();

        if (heldBefore.isEmpty()) {
            return false;
        }

        FrameFilterResult frameFilter = getFrameFilterResult(level, depositTarget, heldBefore);

        if (!frameFilter.hasFrame() || !frameFilter.matchesHeld()) {

            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-CANCEL] No matching frame anymore. chest="
                    + depositTarget
                    + " matchedFrameItem=" + frameFilter.matchedFrameItem()
                    + " held=" + heldBefore);

            markDestinationSelection(false, true);

            BlockPos alternateDestination = findDestinationChest(level, mob);

            if (alternateDestination != null && !alternateDestination.equals(depositTarget)) {
                switchState(mob, TaskState.WALK_TO_DESTINATION, alternateDestination, gameTime);
            } else {
                switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            }

            return true;
        }

        Container targetContainer = getActualContainer(level, depositTarget, chest);

        if (hasSpaceFor(targetContainer, heldBefore)) {
            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-CANCEL] Matched chest full: " + depositTarget);

            markDestinationSelection(false, true);

            BlockPos alternateDestination = findDestinationChest(level, mob);

            if (alternateDestination != null && !alternateDestination.equals(depositTarget)) {
                switchState(mob, TaskState.WALK_TO_DESTINATION, alternateDestination, gameTime);
            } else {
                switchState(mob, TaskState.RETURN_TO_SOURCE, returnToSourceChest, gameTime);
            }

            return true;
        }

        if (tryLockChest(level.dimension(), depositTarget, gameTime)) {
            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-WAIT] Chest locked by another golem: " + depositTarget);
            resetStuckTracking(mob, gameTime);
            return false;
        }

        mob.getNavigation().stop();

        ItemStack remaining;
        boolean deposited = false;

        try {
            remaining = insertIntoChest(level, depositTarget, targetContainer, heldBefore);
            deposited = remaining.getCount() < heldBefore.getCount();
            if (deposited) {
                // Sorting is part of the same locked operation as insertion.
                ContainerAlphabetizer.sortAfterDeposit(targetContainer);
            }
        } finally {
            // A successful remote deposit retains the chest lock until the
            // lid has finished its native open/close sequence.
            if (!deposited) unlockChest();
        }

        mob.setItemInHand(InteractionHand.MAIN_HAND, remaining);

        chest.setChanged();
        level.blockEntityChanged(depositTarget);

        boolean movedItem = remaining.getCount() < heldBefore.getCount();

        if (movedItem) {
            spawnMagicDepositParticles(level, mob, depositTarget);

            if (mob instanceof CopperGolem copperGolem) {
                copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_DROP);
                copperGolem.setState(CopperGolemState.DROPPING_ITEM);
            }

            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-SUCCESS] chest=" + depositTarget
                    + " heldBefore=" + heldBefore
                    + " remaining=" + remaining);
        } else {
            if (mob instanceof CopperGolem copperGolem) {
                copperGolem.playSound(SoundEvents.COPPER_GOLEM_ITEM_NO_DROP);
                copperGolem.setState(CopperGolemState.DROPPING_NO_ITEM);
            }

            System.out.println("[SMART-GOLEM MAGIC-DEPOSIT-FAILED] Nothing moved. chest=" + depositTarget);
        }

        markDestinationSelection(false, true);
        resetStuckTracking(mob, gameTime);

        if (movedItem && mob instanceof CopperGolem copperGolem) {
            // This is a remote deposit. We validated the filter and inserted
            // the item already. Use the exact vanilla opener used for a
            // reachable chest or barrel, without skipping the 100-tick wait.
            try {
                copperGolem.setOpenedChestPos(depositTarget);
                if (chest instanceof ChestBlockEntity) {
                    // Unlike barrels, chest opener counters are distance-aware.
                    // The distant golem is not counted, so the lid closes early
                    // and the regular opening sound is often never emitted.
                    // Send the actual vanilla lid block events and play the
                    // normal chest sound at the chest's location instead.
                    remoteChestLevel = level;
                    remoteChestPos = depositTarget.immutable();
                    sendRemoteChestLidEvent(level, depositTarget, true);
                    level.playSound(null, depositTarget, SoundEvents.CHEST_OPEN,
                            SoundSource.BLOCKS, 0.5F, 1.0F);
                    remoteCloseAt = gameTime + REMOTE_CHEST_LID_OPEN_TICKS;
                } else {
                    chest.startOpen(copperGolem);
                    openedContainer = chest;
                    openedCopperGolem = copperGolem;
                    remoteCloseAt = gameTime + REMOTE_LID_OPEN_TICKS;
                }
                taskState = TaskState.REMOTE_CLOSE;
                System.out.println("[SMART-GOLEM MAGIC-VANILLA-OPEN] target=" + depositTarget);
                return true;
            } catch (RuntimeException exception) {
                closeOpenedContainer();
                unlockChest();
                throw exception;
            }
        }

        unlockChest();
        continueAfterRemoteDeposit(level, mob, gameTime, depositTarget);
        return true;
    }

    private void continueAfterRemoteDeposit(ServerLevel level, PathfinderMob mob,
                                            long gameTime, BlockPos depositTarget) {
        if (!mob.getMainHandItem().isEmpty()) {
            BlockPos alternate = findDestinationChest(level, mob);
            if (alternate != null && !alternate.equals(depositTarget)) {
                switchState(mob, TaskState.WALK_TO_DESTINATION, alternate, gameTime);
                return;
            }
        }

        BlockPos source = returnToSourceChest != null ? returnToSourceChest : lastPickupChest;
        if (source != null) {
            returnToSourceChest = source;
            switchState(mob, TaskState.RETURN_TO_SOURCE, source, gameTime);
            System.out.println("[SMART-GOLEM MAGIC-RETURN-TO-SOURCE] returning to=" + source);
        } else {
            taskState = TaskState.IDLE;
            currentTarget = null;
            currentWalkTarget = null;
            System.out.println("[SMART-GOLEM MAGIC-RETURN-FAILED] No saved source chest.");
        }
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

