package com.anantaya.smartcgolem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import java.io.IOException;

/**
 * Portuguese and English aliases use the same server settings:
 *
 * /fornalheiro combustivel 16        /furnace_tender fuel 16
 * /fornalheiro Carlos combustivel 32 /furnace_tender Alex fuel 32
 * /fornalheiro Carlos reset          /furnace_tender Alex reset
 * /scg furnace fuel 16               /scg fornalheiro combustivel 16
 */
public final class FurnaceCommands {
    private FurnaceCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return root("furnace", "fuel");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nodePortuguese() {
        return root("fornalheiro", "combustivel");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> aliasEnglish() {
        return root("furnace_tender", "fuel")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> aliasPortuguese() {
        return root("fornalheiro", "combustivel")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String literal, String fuelLiteral) {
        return Commands.literal(literal)
                .executes(ctx -> showGlobal(ctx.getSource()))
                .then(Commands.literal(fuelLiteral)
                        .executes(ctx -> showGlobal(ctx.getSource()))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(8, 64))
                                .executes(ctx -> setGlobal(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> setGlobal(ctx.getSource(), 8)))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> showNamed(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name")))
                        .then(Commands.literal(fuelLiteral)
                                .executes(ctx -> showNamed(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(8, 64))
                                        .executes(ctx -> setNamed(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> setNamed(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"), null))));
    }

    private static CopperGolem findNamed(CommandSourceStack src, String requested) {
        CopperGolem match = null;
        for (ServerLevel level : src.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof CopperGolem golem) || golem.getCustomName() == null)
                    continue;
                String visible = golem.getCustomName().getString();
                if (GolemProfession.fromName(visible) != GolemProfession.FORNALHEIRO) continue;
                String individual = visible.contains(":")
                        ? visible.substring(visible.indexOf(':') + 1).strip() : visible;
                if (!visible.equalsIgnoreCase(requested) &&
                        !individual.equalsIgnoreCase(requested)) continue;
                if (match != null) {
                    src.sendFailure(Component.literal(
                            "Há mais de um Fornalheiro/Furnace Tender com esse nome. Use nomes únicos."));
                    return null;
                }
                match = golem;
            }
        }
        if (match == null)
            src.sendFailure(Component.literal("Fornalheiro/Furnace Tender não encontrado entre golems carregados: " + requested));
        return match;
    }

    private static int showGlobal(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(
                "Fornalheiro / Furnace Tender: limite global "
                + ServerFurnaceSettings.globalCoal()
                + " carvões (coal/charcoal), padrão 8. Bloco de carvão: 1."
                + " Use /fornalheiro combustivel 16 ou /furnace_tender fuel 16."), false);
        return 1;
    }

    private static int showNamed(CommandSourceStack src, String name) {
        CopperGolem golem = findNamed(src, name);
        if (golem == null) return 0;
        Integer personal = ServerFurnaceSettings.individualCoal(golem.getUUID());
        int target = ServerFurnaceSettings.coal(golem.getUUID());
        src.sendSuccess(() -> Component.literal("Fornalheiro / Furnace Tender "
                + name + ": " + target + " carvões"
                + (personal == null ? " (global)" : " (individual)")
                + ". Bloco de carvão: 1."), false);
        return 1;
    }

    private static int setGlobal(CommandSourceStack src, int target) {
        try {
            ServerFurnaceSettings.setGlobalCoal(target);
            src.sendSuccess(() -> Component.literal(
                    "Fornalheiros / Furnace Tenders: objetivo global de "
                    + target + " carvões. Blocos de carvão continuam limitados a 1."), true);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            src.sendFailure(Component.literal("Falha na configuração: " + ex.getMessage()));
            return 0;
        }
    }

    private static int setNamed(CommandSourceStack src, String name, Integer target) {
        CopperGolem golem = findNamed(src, name);
        if (golem == null) return 0;
        try {
            ServerFurnaceSettings.setIndividualCoal(golem.getUUID(), target);
            int effective = ServerFurnaceSettings.coal(golem.getUUID());
            src.sendSuccess(() -> Component.literal(
                    "Fornalheiro / Furnace Tender " + name + ": alvo de " + effective
                    + " carvões" + (target == null
                        ? " (configuração individual removida)." : " (configuração individual).")
                    + " Blocos de carvão: 1."), true);
            return 1;
        } catch (IOException | IllegalArgumentException ex) {
            src.sendFailure(Component.literal("Falha na configuração: " + ex.getMessage()));
            return 0;
        }
    }
}
