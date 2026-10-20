package me.mioclient.command;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.util.client.ChatUtils;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class SetYawCommand extends CommandFeature {
    public SetYawCommand() {
        super("yaw");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("yaw", FloatArgumentType.floatArg((-180.0f), 180.0f)).executes(commandContext -> {
            minecraftClient.player.setYRot(((Float) commandContext.getArgument("yaw", Float.class)).floatValue());
            ChatUtils.sendMessage(Component.literal(new StringTemplate().arg(String.valueOf(commandContext.getArgument("yaw", Float.class))).format("Player's yaw has been set to \u0001.")), ChatUtils.createSignature(-1));
            return 1;
        }));
    }
}
