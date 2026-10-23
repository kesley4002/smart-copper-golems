package com.anantaya.smartcgolem;

import com.anantaya.smartcgolem.util.CategoryPolicy;
import com.anantaya.smartcgolem.util.SortLanguage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.commands.Commands;
import com.anantaya.smartcgolem.util.StorageFrameIndex;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartCopperGolem implements ModInitializer {
    public static final String MOD_ID = "smart-copper-golem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Copper Golems are smart now!");
        com.anantaya.smartcgolem.animation.StretchManager.initialize();
        LOGGER.info("Category catalog ready: {} item IDs across {} categories",
                CategoryPolicy.all().size(), CategoryPolicy.categoryKeys().size());

        // The setting lives with the save, not with any individual client.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerSortSettings.load(server);
            ServerWorkZoneSettings.load(server);
            ServerCategorySettings.load(server);
            ServerCarrySettings.load(server);
            StorageFrameIndex.clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerSortSettings.reset();
            ServerWorkZoneSettings.reset();
            ServerCategorySettings.unload();
            ServerCarrySettings.unload();
            StorageFrameIndex.clear();
        });

        // Capture each new golem's home as soon as it enters a server level.
        // Previously seen golems keep their persisted UUID anchors.
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CopperGolem golem) {
                ServerWorkZoneSettings.anchor(golem);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /scg language is namespaced for other mods; /language is the requested shortcut.
            dispatcher.register(Commands.literal("scg")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .then(languageNode())
                    .then(GolemWorkCommands.namedNode())
                    .then(GolemWorkCommands.nightNode())
                    .then(FrameRotationCommands.node())
                    .then(CustomCategoryCommands.node())
                    .then(Commands.literal("carry")
                            .executes(ctx -> showCarry(ctx.getSource()))
                            .then(Commands.literal("16").executes(ctx -> setCarry(ctx.getSource(), 16)))
                            .then(Commands.literal("32").executes(ctx -> setCarry(ctx.getSource(), 32)))
                            .then(Commands.literal("64").executes(ctx -> setCarry(ctx.getSource(), 64))))
                    .then(Commands.literal("radius")
                        .executes(ctx -> showRadius(ctx.getSource()))
                        .then(Commands.argument("width", IntegerArgumentType.integer(16,1000))
                            .executes(ctx -> changeRadius(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "width"))))));
            dispatcher.register(GolemWorkCommands.aliasNode());
            dispatcher.register(Commands.literal("set")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .then(Commands.literal("itemframes")
                            .then(Commands.literal("0").executes(ctx -> FrameRotationCommands.rotateLoaded(ctx.getSource(), 0)))
                            .then(Commands.literal("45").executes(ctx -> FrameRotationCommands.rotateLoaded(ctx.getSource(), 1))))
                    .then(Commands.literal("item")
                            .then(Commands.literal("frames")
                                    .then(Commands.literal("0").executes(ctx -> FrameRotationCommands.rotateLoaded(ctx.getSource(), 0)))
                                    .then(Commands.literal("45").executes(ctx -> FrameRotationCommands.rotateLoaded(ctx.getSource(), 1))))));
            dispatcher.register(Commands.literal("language")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .executes(ctx -> showLanguage(ctx.getSource()))
                    .then(Commands.literal("pt").executes(ctx -> changeLanguage(ctx.getSource(), SortLanguage.Mode.PT)))
                    .then(Commands.literal("en").executes(ctx -> changeLanguage(ctx.getSource(), SortLanguage.Mode.EN))));
        });
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack>
    languageNode() {
        return Commands.literal("language")
                .executes(ctx -> showLanguage(ctx.getSource()))
                .then(Commands.literal("pt").executes(ctx -> changeLanguage(ctx.getSource(), SortLanguage.Mode.PT)))
                .then(Commands.literal("en").executes(ctx -> changeLanguage(ctx.getSource(), SortLanguage.Mode.EN)));
    }

    private static int showCarry(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Capacidade atual de transporte: "
                + ServerCarrySettings.maximum() + " itens por pilha (limite de cada item respeitado)."), false);
        return 1;
    }

    private static int setCarry(net.minecraft.commands.CommandSourceStack source, int amount) {
        try {
            ServerCarrySettings.change(amount);
            source.sendSuccess(() -> Component.literal("Capacidade máxima por transporte definida para " + amount + "."), true);
            return 1;
        } catch (java.io.IOException | IllegalArgumentException e) {
            source.sendFailure(Component.literal("Falha ao alterar capacidade: " + e.getMessage()));
            return 0;
        }
    }

    private static int showRadius(net.minecraft.commands.CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal("Área de trabalho: " + ServerWorkZoneSettings.width() + " x " + ServerWorkZoneSettings.width() + " blocos."), false);
        return 1;
    }
    private static int changeRadius(net.minecraft.commands.CommandSourceStack src, int width) {
        if (width % 2 != 0) { src.sendFailure(Component.literal("Use uma largura par.")); return 0; }
        try {
            ServerWorkZoneSettings.setWidth(width);
            src.sendSuccess(() -> Component.literal("Área quadrada: " + width + " x " + width + " blocos (" + width/2 + " de cada lado)."), true);
            return 1;
        } catch (java.io.IOException e) {
            src.sendFailure(Component.literal("Falha ao salvar a configuração.")); return 0;
        }
    }
    private static int showLanguage(net.minecraft.commands.CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Smart Copper Golems: idioma da ordenação = " + SortLanguage.current().name()), false);
        return 1;
    }

    private static int changeLanguage(net.minecraft.commands.CommandSourceStack source, SortLanguage.Mode mode) {
        try {
            ServerSortSettings.change(source.getServer(), mode);
            source.sendSuccess(() -> Component.literal(
                    "Smart Copper Golems: ordem alfabética alterada para " + mode.name()
                    + " neste mundo. Os baús e barris serão reordenados no próximo depósito."), true);
            return 1;
        } catch (java.io.IOException e) {
            LOGGER.error("Failed to save sorting language", e);
            source.sendFailure(Component.literal("Não foi possível salvar o idioma do mundo. Nada foi alterado."));
            return 0;
        }
    }
}
