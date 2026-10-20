package me.mioclient.command;

import me.mioclient.util.client.ChatUtils;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class PrintCommand extends CommandFeature {
    public PrintCommand() {
        super("print");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("message", StringArgumentType.greedyString()).executes(commandContext -> {
            minecraftClient.gui.hud.getChat().addPlayerMessage(Component.literal((String) commandContext.getArgument("message", String.class)), ChatUtils.createSignature((int) (Math.random() * 10000.0d)), ChatUtils.GuiMessageTag);
            return 1;
        })).executes(commandContext2 -> {
            ChatUtils.sendPrefixedMessage(Component.literal("Please enter a message to print."), ChatUtils.createSignature(-1), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode3);
            return 1;
        });
    }
}
