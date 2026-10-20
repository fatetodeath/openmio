package me.mioclient.command;

import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.client.ChatUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class CoordsCommand extends CommandFeature {
    public CoordsCommand() {
        super("coords");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("name", new ExamplesArgumentType_3()).executes(commandContext -> {
            do1387((String) commandContext.getArgument("name", String.class), getFormattedCoordinates());
            return 1;
        })).executes(commandContext2 -> {
            try {
                minecraftClient.keyboardHandler.setClipboard(getFormattedCoordinates());
                ChatUtils.sendMessage(Component.literal("Your position has been copied to the clipboard."), ChatUtils.createSignature(-1));
                return 1;
            } catch (Exception e) {
                return 1;
            }
        });
    }

    public String getFormattedCoordinates() {
        if (minecraftClient.player == null) {
            return "";
        }
        return new StringTemplate().arg((int) Math.floor(Math.random() * 10000.0d)).arg(WorldUtils.getDimension().getDisplayName()).arg((int) minecraftClient.player.getZ()).arg((int) minecraftClient.player.getY()).arg((int) minecraftClient.player.getX()).format("X: \u0001, Y: \u0001, Z: \u0001 in The \u0001 [\u0001]");
    }

    public void do1387(String str, String str2) {
        minecraftClient.player.connection.sendCommand(new StringTemplate().arg(str2).arg(str).format("w \u0001 \u0001"));
    }
}
