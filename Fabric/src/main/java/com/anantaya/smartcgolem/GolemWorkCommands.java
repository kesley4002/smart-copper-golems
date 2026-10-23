package com.anantaya.smartcgolem;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
 * Commands for named golems and night shifts. All writes are server-side and
 * backed by UUIDs rather than nicknames, so a rename cannot swap another
 * golem's configuration.
 */
public final class GolemWorkCommands {
    private GolemWorkCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> namedNode() {
        return namedRoot("golem");
    }

    /** Stand-alone compatibility shortcut: /copper_golem "João" radius 30 */
    public static LiteralArgumentBuilder<CommandSourceStack> aliasNode() {
        return namedRoot("copper_golem")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> namedRoot(String literal) {
        return Commands.literal(literal)
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> showNamed(ctx.getSource(),
                                StringArgumentType.getString(ctx, "name")))
                        .then(Commands.literal("radius")
                                .executes(ctx -> showNamed(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")))
                                .then(Commands.argument("width", IntegerArgumentType.integer(16, 1000))
                                        .executes(ctx -> setNamed(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                IntegerArgumentType.getInteger(ctx, "width")))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> setNamed(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"), null))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> nightNode() {
        return Commands.literal("night")
                .executes(ctx -> showNight(ctx.getSource()))
                .then(Commands.literal("work")
                        .then(Commands.literal("on")
                                .executes(ctx -> setNightWork(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setNightWork(ctx.getSource(), false))))
                .then(Commands.literal("radius")
                        .executes(ctx -> showNight(ctx.getSource()))
                        .then(Commands.argument("width", IntegerArgumentType.integer(16, 1000))
                                .executes(ctx -> setNightWidth(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "width"))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> resetNightWidth(ctx.getSource()))));
    }

    /** Iterate only loaded entities, across all dimensions; never load chunks. */
    private static CopperGolem findNamed(CommandSourceStack source, String name) {
        CopperGolem found = null;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof CopperGolem golem)
                        || golem.getCustomName() == null
                        || !golem.getCustomName().getString().equals(name)) continue;
                if (found != null) {
                    source.sendFailure(Component.literal(
                            "Existem dois ou mais golems carregados com esse nome. Use nomes únicos."));
                    return null;
                }
                found = golem;
            }
        }
        if (found == null) {
            source.sendFailure(Component.literal(
                    "Golem não encontrado entre as entidades carregadas: " + name));
        }
        return found;
    }

    private static int showNamed(CommandSourceStack source, String name) {
        CopperGolem golem = findNamed(source, name);
        if (golem == null) return 0;
        Integer set = ServerWorkZoneSettings.individualWidth(golem.getUUID());
        int width = set == null ? ServerWorkZoneSettings.width() : set;
        source.sendSuccess(() -> Component.literal(
                "Golem " + name + ": área diurna " + width + "x" + width
                + (set == null ? " (padrão)" : " (individual)")
                + ". No turno noturno, o limite individual continua valendo."), false);
        return 1;
    }

    private static int setNamed(CommandSourceStack source, String name, Integer newWidth) {
        if (newWidth != null && newWidth % 2 != 0) {
            source.sendFailure(Component.literal("A largura deve ser par (ex.: 30)."));
            return 0;
        }
        CopperGolem golem = findNamed(source, name);
        if (golem == null) return 0;
        try {
            ServerWorkZoneSettings.setIndividualWidth(golem.getUUID(), newWidth);
            int width = newWidth == null ? ServerWorkZoneSettings.width() : newWidth;
            source.sendSuccess(() -> Component.literal(
                    "Golem " + name + ": área de " + width + "x" + width
                    + (newWidth == null ? " restaurada ao padrão." : " blocos, centrada em sua origem.")), true);
            return 1;
        } catch (IOException | IllegalArgumentException e) {
            source.sendFailure(Component.literal("Não foi possível salvar o raio: " + e.getMessage()));
            return 0;
        }
    }

    private static int showNight(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Turno noturno: " + (ServerWorkZoneSettings.nightWorkEnabled() ? "ATIVO" : "descanso")
                + "; área: " + ServerWorkZoneSettings.nightWidth() + "x"
                + ServerWorkZoneSettings.nightWidth()
                + (ServerWorkZoneSettings.hasNightWidthOverride() ? " (noturna)" : " (mesma do dia)")
                + ". Limites individuais menores são preservados."), false);
        return 1;
    }

    private interface Change { void apply() throws IOException; }
    private static int apply(CommandSourceStack source, Change action, String feedback) {
        try {
            action.apply();
            source.sendSuccess(() -> Component.literal(feedback), true);
            return 1;
        } catch (IOException | IllegalArgumentException e) {
            source.sendFailure(Component.literal("Falha ao salvar configuração: " + e.getMessage()));
            return 0;
        }
    }

    private static int setNightWork(CommandSourceStack source, boolean enabled) {
        return apply(source, () -> ServerWorkZoneSettings.setNightWork(enabled),
                enabled ? "Os golems podem trabalhar à noite." : "Descanso noturno restaurado.");
    }

    private static int setNightWidth(CommandSourceStack source, int width) {
        if (width % 2 != 0) {
            source.sendFailure(Component.literal("A largura noturna deve ser par."));
            return 0;
        }
        return apply(source, () -> ServerWorkZoneSettings.setNightWidth(width),
                "Área de trabalho noturna definida em " + width + "x" + width + " blocos.");
    }

    private static int resetNightWidth(CommandSourceStack source) {
        return apply(source, ServerWorkZoneSettings::resetNightWidth,
                "Área noturna restaurada ao raio global.");
    }
}
