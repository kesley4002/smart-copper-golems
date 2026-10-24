package com.anantaya.smartcgolem;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import java.io.IOException;
import java.util.Locale;

/**
 * /coletor Pedro item ovo
 * /scg collector "Coletor: Pedro" item minecraft:egg
 * /coletor Pedro reset
 */
public final class CollectorCommands {
    private CollectorCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> aliasNode() {
        return root("coletor").requires(src ->
                src.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));
    }
    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return root("collector");
    }
    private static LiteralArgumentBuilder<CommandSourceStack> root(String literal) {
        return Commands.literal(literal)
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> status(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(ctx -> set(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "item")))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> set(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"), null))));
    }

    /**
     * Accept either full tag "Coletor: Pedro" or short name "Pedro".
     * Reject duplicate names to prevent changing the wrong golem's UUID.
     */
    private static CopperGolem find(CommandSourceStack source, String name) {
        CopperGolem found = null;
        for (ServerLevel level : source.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (!(e instanceof CopperGolem golem)
                        || golem.getCustomName() == null
                        || GolemProfession.fromName(golem.getCustomName().getString())
                            != GolemProfession.COLETOR) continue;
                String display = golem.getCustomName().getString();
                String shortName = display.contains(":")
                        ? display.substring(display.indexOf(':') + 1).strip() : display;
                if (!display.equalsIgnoreCase(name) && !shortName.equalsIgnoreCase(name))
                    continue;
                if (found != null) {
                    source.sendFailure(Component.literal("Há mais de um Coletor com esse nome. Use nomes únicos."));
                    return null;
                }
                found = golem;
            }
        }
        if (found == null)
            source.sendFailure(Component.literal("Coletor não encontrado entre os golems carregados: " + name));
        return found;
    }

    private static int status(CommandSourceStack src, String name) {
        CopperGolem golem = find(src, name);
        if (golem == null) return 0;
        String item = ServerCollectorSettings.itemFilter(golem.getUUID());
        src.sendSuccess(() -> Component.literal("Coletor " + name + ": "
                + (item == null ? "coleta todos os itens" : "coleta apenas " + item)
                + ". Use /coletor " + name + " reset para voltar ao padrão."), false);
        return 1;
    }

    private static String parseItem(String typed) {
        String id = typed.strip().toLowerCase(Locale.ROOT);
        id = switch (id) {
            case "ovo" -> "minecraft:egg";
            case "la", "lã" -> "minecraft:white_wool";
            case "trigo" -> "minecraft:wheat";
            case "semente", "sementes" -> "minecraft:wheat_seeds";
            default -> id;
        };
        if (!id.contains(":")) id = "minecraft:" + id;
        return id;
    }

    private static int set(CommandSourceStack src, String name, String raw) {
        CopperGolem golem = find(src, name);
        if (golem == null) return 0;
        String id = null;
        if (raw != null) {
            id = parseItem(raw);
            ResourceLocation key = ResourceLocation.tryParse(id);
            if (key == null || !BuiltInRegistries.ITEM.keySet().contains(key)) {
                src.sendFailure(Component.literal("Item não encontrado: " + raw
                        + ". Use por exemplo ovo ou minecraft:egg."));
                return 0;
            }
        }
        try {
            ServerCollectorSettings.change(golem.getUUID(), id);
        } catch (IOException | IllegalArgumentException e) {
            src.sendFailure(Component.literal("Falha ao salvar filtro: " + e.getMessage()));
            return 0;
        }
        final String selected = id;
        src.sendSuccess(() -> Component.literal(selected == null
                ? "Coletor " + name + " agora coleta todos os itens."
                : "Coletor " + name + " agora coleta SOMENTE " + selected + "."), true);
        return 1;
    }
}
