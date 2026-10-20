package com.anantaya.smartcgolem;

import com.anantaya.smartcgolem.util.CategoryPolicy;
import com.anantaya.smartcgolem.util.SortLanguage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.Commands;
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
        LOGGER.info("Category catalog ready: {} item IDs across {} categories",
                CategoryPolicy.all().size(), CategoryPolicy.categoryKeys().size());

        // The setting lives with the save, not with any individual client.
        ServerLifecycleEvents.SERVER_STARTED.register(ServerSortSettings::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ServerSortSettings.reset());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // /scg language is namespaced for other mods; /language is the requested shortcut.
            dispatcher.register(Commands.literal("scg")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                    .then(languageNode()));
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
