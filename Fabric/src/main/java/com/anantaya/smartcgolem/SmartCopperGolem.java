package com.anantaya.smartcgolem;

import com.anantaya.smartcgolem.config.GolemSettings;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartCopperGolem implements ModInitializer {
    public static final String MOD_ID = "smart-copper-golem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override public void onInitialize() {
        GolemSettings.load();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("smartgolem")
                .then(Commands.literal("capacity")
                    .executes(ctx -> feedback(ctx.getSource(), "Capacidade: " + GolemSettings.capacity()))
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .executes(ctx -> run(ctx.getSource(), () -> GolemSettings.capacity(IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("category")
                    .then(Commands.literal("list")
                        .executes(ctx -> feedback(ctx.getSource(), "Categorias: " + String.join(", ", GolemSettings.names()))))
                    .then(Commands.literal("show")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> feedback(ctx.getSource(), "Itens: " + String.join(", ", GolemSettings.members(StringArgumentType.getString(ctx, "name")))))))
                    .then(Commands.literal("create").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> run(ctx.getSource(), () -> GolemSettings.create(StringArgumentType.getString(ctx, "name"))))))
                    .then(Commands.literal("delete").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> run(ctx.getSource(), () -> GolemSettings.delete(StringArgumentType.getString(ctx, "name"))))))
                    .then(Commands.literal("add").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                            .then(Commands.argument("items", StringArgumentType.greedyString())
                                .executes(ctx -> run(ctx.getSource(), () -> GolemSettings.add(StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "items")))))))
                    .then(Commands.literal("remove").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                            .then(Commands.argument("items", StringArgumentType.greedyString())
                                .executes(ctx -> run(ctx.getSource(), () -> GolemSettings.remove(StringArgumentType.getString(ctx, "name"), StringArgumentType.getString(ctx, "items")))))))
                    .then(Commands.literal("reset").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("confirm").executes(ctx -> run(ctx.getSource(), GolemSettings::reset))))
                    .then(Commands.literal("restore").requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("confirm").executes(ctx -> run(ctx.getSource(), GolemSettings::restore))))
                )
            )
        );
        LOGGER.info("Smart Copper Golem configuration loaded");
    }
    private static int feedback(net.minecraft.commands.CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Smart Golem] " + message), false);
        return 1;
    }
    private static int run(net.minecraft.commands.CommandSourceStack source, Runnable action) {
        try { action.run(); return feedback(source, "Configuracao atualizada."); }
        catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("[Smart Golem] " + e.getMessage())); return 0;
        }
    }
}
