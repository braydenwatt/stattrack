package com.arco.stattrack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.midnightdust.lib.config.MidnightConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import com.arco.stattrack.util.setLore;
import com.arco.stattrack.util.statTrackLore;

public class ItemLore implements ModInitializer {
    public static final String MOD_ID = "itemLore";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String STATTRACK_LIMIT_OBJECTIVE = "stattrack_active";

    private static Objective getOrCreateStatTrackObjective(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Objective obj = scoreboard.getObjective(STATTRACK_LIMIT_OBJECTIVE);
        if (obj != null) return obj;

        // In your mappings this overload expects 6 args
        return scoreboard.addObjective(
                STATTRACK_LIMIT_OBJECTIVE,
                ObjectiveCriteria.DUMMY,
                Component.literal("StatTrack Active"),
                ObjectiveCriteria.RenderType.INTEGER,
                false,
                null
        );
    }

    private static int getActiveStatTrackCount(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        Objective obj = getOrCreateStatTrackObjective(server);

        // 1.21+ expects a ScoreHolder, not a String name
        return server.getScoreboard()
                .getOrCreatePlayerScore(player, obj)
                .get();
    }

    private static void setActiveStatTrackCount(MinecraftServer server, net.minecraft.server.level.ServerPlayer player, int value) {
        Objective obj = getOrCreateStatTrackObjective(server);
        int clamped = Math.max(0, value);

        server.getScoreboard()
                .getOrCreatePlayerScore(player, obj)
                .set(clamped);
    }

    private static CompletableFuture<Suggestions> suggestTrackedStats(SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                List.of(
                        "player_kills",
                        "mob_kills",
                        "blocks_mined",
                        "damage_absorbed",
                        "blocks_flown",
                        "arrows_fired",
                        "logs_stripped",
                        "most_killed",
                        "most_killed_player"
                ),
                builder
        );
    }

    @SuppressWarnings({"null"})
    @Override
    public void onInitialize() {

        LOGGER.info(MOD_ID + " Initialized");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("stattrack")
                            .then(Commands.literal("add")
                                    .requires(Permissions.require("stattrack.command.add", 4))
                                    .then(Commands.argument("stat", StringArgumentType.word())
                                            .suggests((context, builder) -> suggestTrackedStats(builder))
                                            .executes(context -> handleStatTrackAdd(
                                                    context,
                                                    StringArgumentType.getString(context, "stat")
                                            ))))
                            .then(Commands.literal("remove")
                                    .requires(Permissions.require("stattrack.command.remove", 4))
                                    .executes(ItemLore::handleStatTrackRemove))
            );
        });
    }

    private static int handleStatTrackAdd(CommandContext<CommandSourceStack> context, String stat) {
        var player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must be holding an item."));
            return 0;
        }

        if (stack.getCount() > 1) {
            context.getSource().sendFailure(Component.literal("You must be holding a single item."));
            return 0;
        }

        if (!statTrackLore.isSupportedTrackedStat(stat)) {
            context.getSource().sendFailure(Component.literal("Unknown stat: " + stat));
            return 0;
        }

        // NEW: If someone else already owns this StatTrack, block changes
        boolean itemHasOwner = statTrackLore.hasTrackedBy(stack);
        if (itemHasOwner && !statTrackLore.isTrackedBy(stack, player)) {
            context.getSource().sendFailure(Component.literal("This item is already StatTracked by another player."));
            return 0;
        }

        boolean alreadyActiveOnThisItem =
                statTrackLore.isTrackedBy(stack, player) && statTrackLore.hasActiveSelection(stack);

        if (!alreadyActiveOnThisItem) {
            int max = statTrackLore.getMaxTracksForPlayer(player);
            int cur = getActiveStatTrackCount(context.getSource().getServer(), player);

            if (cur >= max) {
                context.getSource().sendFailure(Component.literal("You have reached your StatTrack limit (" + max + ")."));
                return 0;
            }
        }

        setLore.applyNewLore(player, stack);
        statTrackLore.setTrackedBy(stack, player);
        statTrackLore.setSelectedTrackedStat(stack, stat);
        statTrackLore.refreshLore(stack);

        if (!alreadyActiveOnThisItem) {
            MinecraftServer server = context.getSource().getServer();
            int cur = getActiveStatTrackCount(server, player);
            setActiveStatTrackCount(server, player, cur + 1);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("StatTrack applied: " + stat).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static int handleStatTrackRemove(CommandContext<CommandSourceStack> context) {
        var player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must be holding an item."));
            return 0;
        }

        boolean wasActiveOnThisItem = statTrackLore.hasActiveSelection(stack);

        if (wasActiveOnThisItem && !statTrackLore.isTrackedBy(stack, player)) {
            context.getSource().sendFailure(Component.literal("You are not the tracked-by player for this item."));
            return 0;
        }

        statTrackLore.removeStatTrackAndAllLore(stack, true);

        if (wasActiveOnThisItem) {
            MinecraftServer server = context.getSource().getServer();
            int cur = getActiveStatTrackCount(server, player);
            setActiveStatTrackCount(server, player, cur - 1);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("StatTrack removed from this item.").withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }
}