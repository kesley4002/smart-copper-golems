package com.anantaya.smartcgolem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Anvil-custom-named chest/barrel belongs to exactly the golem whose name
 * matches it. Ordinary, unnamed containers remain shared. Does NOT change
 * item-frame filtering or restrict player/hopper access.
 *
 * A double chest is ONE inventory, so a private name on EITHER half protects
 * BOTH halves. Conflicting custom names make the combined inventory unavailable
 * to every golem (fail closed).
 */
public final class GolemStorageOwnership {
    private GolemStorageOwnership() {}

    public static boolean canAccess(ServerLevel level, BlockPos pos, CopperGolem golem) {
        if (golem == null || pos == null) return false;
        if (level.getChunkSource().getChunkNow(Math.floorDiv(pos.getX(), 16),
                Math.floorDiv(pos.getZ(), 16)) == null) return false;
        if (!(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity block)) return false;
        String first = customName(block);
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (level.getChunkSource().getChunkNow(Math.floorDiv(other.getX(), 16),
                    Math.floorDiv(other.getZ(), 16)) == null) return false;
            if (!(level.getBlockEntity(other) instanceof BaseContainerBlockEntity second))
                return false;
            String partner = customName(second);
            // An explicitly named second half controls the same combined inventory.
            if (first == null) first = partner;
            else if (!namesCompatible(first, partner)) return false;
        }
        return accepts(first, golem.getCustomName() == null
                ? null : golem.getCustomName().getString());
    }

    public static boolean isPrivateFor(ServerLevel level, BlockPos pos, CopperGolem golem) {
        if (!canAccess(level, pos, golem)) return false;
        if (level.getBlockEntity(pos) instanceof BaseContainerBlockEntity block
                && customName(block) != null) return true;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock
                && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            if (level.getBlockEntity(other) instanceof BaseContainerBlockEntity block)
                return customName(block) != null;
        }
        return false;
    }

    private static String customName(BaseContainerBlockEntity block) {
        Component name = block.getCustomName();
        return name == null || name.getString().isBlank() ? null : name.getString().strip();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").strip().toLowerCase(Locale.ROOT);
    }

    /** Conflicting names on the halves must not expose a shared double chest. */
    public static boolean namesCompatible(String first, String second) {
        return first == null || first.isBlank() || second == null || second.isBlank()
                || normalize(first).equals(normalize(second));
    }

    /**
     * Users may tag golems as `Coletor: Roger` while naming the chest
     * just `Roger`. Exact full-name matching also works. No fuzzy/partial
     * matches, so `Rog` cannot accidentally unlock `Roger`'s container.
     */
    public static boolean accepts(String storedOwner, String golemName) {
        if (storedOwner == null || storedOwner.isBlank()) return true;
        if (golemName == null || golemName.isBlank()) return false;
        String owner = normalize(storedOwner);
        String full = normalize(golemName);
        if (owner.equals(full)) return true;
        int colon = full.indexOf(':');
        String nick = colon >= 0 ? full.substring(colon + 1).strip() : full;
        return !nick.isBlank() && owner.equals(nick);
    }
}
