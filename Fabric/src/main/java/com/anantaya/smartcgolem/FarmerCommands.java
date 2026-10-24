package com.anantaya.smartcgolem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.io.IOException;

/** Subcommands of the existing OP-only /scg command tree. */
public final class FarmerCommands {
    private FarmerCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return root("farmer", "compost", "reserve");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nodePortuguese() {
        return root("agricultor", "compostagem", "reserva");
    }

    public static LiteralArgumentBuilder<CommandSourceStack> aliasNodeEnglish() {
        return root("farmer", "compost", "reserve")
                .requires(src -> src.permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> aliasNodePortuguese() {
        return root("agricultor", "compostagem", "reserva")
                .requires(src -> src.permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(
            String literal, String compostKey, String reserveKey) {
        return Commands.literal(literal)
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal(compostKey)
                        .executes(ctx -> status(ctx.getSource()))
                        .then(Commands.literal("on")
                                .executes(ctx -> compost(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> compost(ctx.getSource(), false)))
                        .then(Commands.literal("ligar")
                                .executes(ctx -> compost(ctx.getSource(), true)))
                        .then(Commands.literal("desligar")
                                .executes(ctx -> compost(ctx.getSource(), false))))
                .then(Commands.literal(reserveKey)
                        .executes(ctx -> status(ctx.getSource()))
                        .then(Commands.argument("count", IntegerArgumentType.integer(0, 256))
                                .executes(ctx -> reserve(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "count")))));
    }

    private static int status(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Agricultor: composteira "
                + (ServerFarmerSettings.compostEnabled() ? "LIGADA" : "DESLIGADA")
                + "; reserva mínima por item e recipiente: "
                + ServerFarmerSettings.reserve()
                + ". A mesma Name Tag Agricultor faz colheita, replantio, "
                + "compostagem e armazenamento de farinha de osso. "
                + "Coloque uma composteira a até 3 blocos de um baú/barril "
                + "de colheita com moldura comum (0° categoria ou 45° item). "
                + "Deve existir excedente acima da reserva e um destino filtrado "
                + "para farinha de osso."), false);
        return 1;
    }

    private static int compost(CommandSourceStack src, boolean enabled) {
        try {
            ServerFarmerSettings.changeCompost(enabled);
            src.sendSuccess(() -> Component.literal(enabled
                    ? "Compostagem ativada para Agricultores nas estações marcadas."
                    : "Compostagem desativada."), true);
            return 1;
        } catch (IOException e) {
            src.sendFailure(Component.literal("Falha ao salvar compostagem: " + e.getMessage()));
            return 0;
        }
    }

    private static int reserve(CommandSourceStack src, int count) {
        try {
            ServerFarmerSettings.changeReserve(count);
            src.sendSuccess(() -> Component.literal(
                    "Reserva mínima para compostagem: " + count
                    + " de cada item elegível, por recipiente."), true);
            return 1;
        } catch (IOException e) {
            src.sendFailure(Component.literal("Falha ao salvar reserva: " + e.getMessage()));
            return 0;
        }
    }
}
