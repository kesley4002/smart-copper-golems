package com.anantaya.smartcgolem;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;

/**
 * OP-only /scg set itemframes 0|45 and /set itemframes 0|45.
 * Only traverses entities ALREADY loaded in the command's dimension.
 * Source stick frames are intentionally left alone to avoid breaking input
 * chests and barrels after a blanket rotation.
 */
public final class FrameRotationCommands {
    private FrameRotationCommands() {}

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>
    node() {
        return Commands.literal("set")
                .then(Commands.literal("itemframes")
                        .then(Commands.literal("0")
                                .executes(ctx -> rotateLoaded(ctx.getSource(), 0)))
                        .then(Commands.literal("45")
                                .executes(ctx -> rotateLoaded(ctx.getSource(), 1))));
    }

    public static int rotateLoaded(CommandSourceStack source, int rotation) {
        ServerLevel level = source.getLevel();
        int changed = 0;
        int already = 0;
        int protectedStick = 0;
        // Snapshot is not necessary: rotating item frames does not remove
        // entities, create chunks or move entities between dimensions.
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemFrame frame) || frame.getItem().isEmpty()) continue;
            if (frame.getItem().is(Items.STICK)) {
                protectedStick++;
                continue;
            }
            if (frame.getRotation() == rotation) { already++; continue; }
            frame.setRotation(rotation);
            changed++;
        }
        final int updated = changed, unchanged = already, skipped = protectedStick;
        String degree = rotation == 0 ? "0° (categoria)" : "45° (item exato)";
        source.sendSuccess(() -> Component.literal(
                "Molduras carregadas nesta dimensão: " + updated + " alteradas para "
                + degree + ", " + unchanged + " já estavam nessa posição; "
                + skipped + " com graveto preservadas."), true);
        return updated + 1;
    }
}
