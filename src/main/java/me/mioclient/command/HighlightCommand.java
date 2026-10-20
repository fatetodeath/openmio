package me.mioclient.command;

import me.mioclient.api.ClientServices;

import me.mioclient.render.shape.BlockHighlightRenderer;

import java.awt.Color;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class HighlightCommand extends CommandFeature {
    public static final Color color = new Color(255, 0, 0, 120);
    public static final Color color2 = new Color(0, 0, 0, 0);

    public HighlightCommand() {
        super("highlight");
        ClientServices.antiPhaseBlockHelper.register(new BlockHighlightRenderer.Record(this, () -> {
            return color;
        }, () -> {
            return color2;
        }, () -> {
            return Float.valueOf(1.0f);
        }, () -> {
            return Float.valueOf(5.0f);
        }, () -> {
            return true;
        }, () -> {
            return true;
        }, 1000));
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("pos", new ExamplesArgumentType()).then(CommandFeature.literal("beam").executes(commandContext -> {
            ClientServices.antiPhaseBlockHelper.addHighlight(this, AABB.ofSize((Vec3) commandContext.getArgument("pos", Vec3.class), 0.25d, 0.25d, 0.25d).setMaxY(minecraftClient.level.getMaxY()).setMinY(minecraftClient.level.getMinY()));
            return 1;
        })).executes(commandContext2 -> {
            ClientServices.antiPhaseBlockHelper.addHighlight(this, AABB.ofSize((Vec3) commandContext2.getArgument("pos", Vec3.class), 0.25d, 0.25d, 0.25d));
            return 1;
        }));
    }
}
