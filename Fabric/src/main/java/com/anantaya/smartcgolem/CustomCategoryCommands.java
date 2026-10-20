package com.anantaya.smartcgolem;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.io.IOException;

/** All mutations require OP via the parent /scg command. */
public final class CustomCategoryCommands {
    private CustomCategoryCommands() {}

    public static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("categories")
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("clear").executes(ctx -> invoke(ctx.getSource(), () -> ServerCategorySettings.clearAll(),
                        "Categorias removidas deste mundo. Use /scg categories restore para recuperar as 43 originais.")))
                .then(Commands.literal("restore").executes(ctx -> invoke(ctx.getSource(), () -> ServerCategorySettings.restoreDefaults(),
                        "43 categorias originais restauradas; categorias personalizadas foram apagadas.")))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return invoke(ctx.getSource(), () -> ServerCategorySettings.create(name), "Categoria criada: " + name);
                        })))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("item_id", StringArgumentType.greedyString()).executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String id = StringArgumentType.getString(ctx, "item_id");
                                    return invoke(ctx.getSource(), () -> ServerCategorySettings.add(name, id), id + " adicionado em " + name);
                                }))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("item_id", StringArgumentType.greedyString()).executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    String id = StringArgumentType.getString(ctx, "item_id");
                                    return invoke(ctx.getSource(), () -> ServerCategorySettings.remove(name, id), id + " removido de " + name);
                                }))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            return invoke(ctx.getSource(), () -> ServerCategorySettings.delete(name), "Categoria excluída: " + name);
                        })));
    }

    private static int info(CommandSourceStack source) {
        String list = String.join(", ", ServerCategorySettings.customCategories().keySet());
        source.sendSuccess(() -> Component.literal("Categorias vanilla: "
                + (ServerCategorySettings.defaultsEnabled() ? "43 ativas" : "desativadas")
                + " | Personalizadas: " + (list.isEmpty() ? "nenhuma" : list)), false);
        return 1;
    }

    private interface Mutation { void run() throws IOException; }
    private static int invoke(CommandSourceStack src, Mutation fn, String success) {
        try {
            fn.run();
            src.sendSuccess(() -> Component.literal(success), true);
            return 1;
        } catch (IOException | IllegalArgumentException e) {
            src.sendFailure(Component.literal("Smart Golem: " + e.getMessage()));
            return 0;
        }
    }
}
