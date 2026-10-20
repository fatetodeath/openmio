package me.mioclient.command;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.network.irc.CoordLogEntry;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.IBaritoneChatControl;
import baritone.api.event.events.ChatEvent;
import baritone.api.event.events.TabCompleteEvent;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class BaritoneCommand extends CommandFeature {
    public static final String string = "#";

    public BaritoneCommand() {
        super("baritone");
        setAliases("b");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("command", StringArgumentType.greedyString()).suggests((commandContext, suggestionsBuilder) -> {
            IBaritone primaryBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            String str = "#";
            try {
                str = new StringTemplate().arg(suggestionsBuilder.getRemaining()).arg(str).format("\u0001\u0001");
            } catch (Exception e) {
            }
            TabCompleteEvent tabCompleteEvent = new TabCompleteEvent(str);
            do2699(() -> {
                primaryBaritone.getGameEventHandler().onPreTabComplete(tabCompleteEvent);
            });
            if (tabCompleteEvent.completions == null || tabCompleteEvent.completions.length == 0) {
                return suggestionsBuilder.buildFuture();
            }
            String input = commandContext.getInput();
            String substring = input.substring(0, Math.min(input.length(), suggestionsBuilder.getStart() + suggestionsBuilder.getRemaining().length()));
            StringRange between = StringRange.between(substring.lastIndexOf(" ") + 1, substring.length());
            return CompletableFuture.completedFuture(new Suggestions(between, (List) Arrays.stream(tabCompleteEvent.completions).map(str2 -> {
                return new Suggestion(between, str2.startsWith("#") ? str2.substring(1) : str2);
            }).collect(Collectors.toList())));
        }).executes(commandContext2 -> {
            BlockPos blockPos2700;
            IBaritone primaryBaritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            String str = (String) commandContext2.getArgument("command", String.class);
            if ("goto".equalsIgnoreCase(str)) {
                ClientServices.obstaclePasserHelper.pathTo(minecraftClient.gameRenderer.mainCamera().blockPosition());
                return 1;
            }
            if ("goto ping".equalsIgnoreCase(str)) {
                BlockPos blockPos27002 = findNearbyTrackedPos();
                if (blockPos27002 != null) {
                    ClientServices.obstaclePasserHelper.pathTo(blockPos27002);
                    return 1;
                }
            } else if ("elytra ping".equalsIgnoreCase(str) && (blockPos2700 = findNearbyTrackedPos()) != null) {
                ClientServices.obstaclePasserHelper.elytraPathTo(new BlockPos(blockPos2700.getX(), minecraftClient.player.getBlockY(), blockPos2700.getZ()));
                return 1;
            }
            primaryBaritone.getGameEventHandler().onSendChatMessage(new ChatEvent(new StringTemplate().arg(str).arg(IBaritoneChatControl.FORCE_COMMAND_PREFIX).format("\u0001\u0001")));
            return 1;
        }));
    }

    public void do2699(java.lang.Runnable runnable) {
        String str = (String) BaritoneAPI.getSettings().prefix.value;
        BaritoneAPI.getSettings().prefix.value = "#";
        runnable.run();
        BaritoneAPI.getSettings().prefix.value = str;
    }

    public BlockPos findNearbyTrackedPos() {
        synchronized (ClientServices.nameTagsManager.getCoordLogEntries()) {
            for (CoordLogEntry spawnTimeHelper : ClientServices.nameTagsManager.getCoordLogEntries()) {
                if (spawnTimeHelper.isOnSameServer()) {
                    if (!spawnTimeHelper.getPlayerName().equalsIgnoreCase(minecraftClient.player.getName().getString())) {
                        if (((float) minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(spawnTimeHelper.getBlockPos()))) < 9999.0f) {
                            return spawnTimeHelper.getBlockPos();
                        }
                    }
                }
            }
            return null;
        }
    }
}
