package me.mioclient.command;

import me.mioclient.util.math.MathConstants;

import me.mioclient.network.PacketSender;

import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;

public final class ClipCommand extends CommandFeature {
    public ClipCommand() {
        super("clip");
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.literal("v").then(CommandFeature.argument("value", FloatArgumentType.floatArg()).executes(commandContext -> {
            float clamp = Math.clamp(((Float) commandContext.getArgument("value", Float.class)).floatValue(), (-3.0E7f), 3.0E7f);
            int ceil = (int) Math.ceil(Math.abs(clamp / 10.0f));
            if (ceil > 20) {
                ceil = 1;
            }
            if (minecraftClient.player.isPassenger()) {
                for (int i = 0; i < ceil - 1; i++) {
                    PacketSender.sendPacket(net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.fromEntity(minecraftClient.player.getVehicle()));
                }
                minecraftClient.player.getVehicle().setPos(minecraftClient.player.getVehicle().getX(), minecraftClient.player.getVehicle().getY() + clamp, minecraftClient.player.getVehicle().getZ());
                return 1;
            }
            for (int i2 = 0; i2 < ceil - 1; i2++) {
                PacketSender.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(true, minecraftClient.player.horizontalCollision));
            }
            minecraftClient.player.setPos(minecraftClient.player.getX(), minecraftClient.player.getY() + clamp, minecraftClient.player.getZ());
            PacketSender.sendPosition(minecraftClient.player.getX(), minecraftClient.player.getY() + clamp, minecraftClient.player.getZ(), true);
            return 1;
        }))).then(CommandFeature.literal("h").then(CommandFeature.argument("value", FloatArgumentType.floatArg()).executes(commandContext2 -> {
            float clamp = Math.clamp(((Float) commandContext2.getArgument("value", Float.class)).floatValue(), (-3.0E7f), 3.0E7f);
            double cos = Math.cos(Math.toRadians(minecraftClient.player.getYRot() + MathConstants.num2));
            double sin = Math.sin(Math.toRadians(minecraftClient.player.getYRot() + MathConstants.num2));
            if (minecraftClient.player.isPassenger()) {
                minecraftClient.player.getVehicle().setPos(minecraftClient.player.getVehicle().getX() + (clamp * cos), minecraftClient.player.getVehicle().getY(), minecraftClient.player.getVehicle().getZ() + (clamp * sin));
                return 1;
            }
            minecraftClient.player.setPos(minecraftClient.player.getX() + (clamp * cos), minecraftClient.player.getY(), minecraftClient.player.getZ() + (clamp * sin));
            PacketSender.sendPosition(minecraftClient.player.getX() + (clamp * cos), minecraftClient.player.getY(), minecraftClient.player.getZ() + (clamp * sin), true);
            return 1;
        })));
    }
}
