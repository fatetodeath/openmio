package me.mioclient.module.misc;

import me.mioclient.api.Category;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class Heaven extends Module {
    public Heaven() {
        super("Heaven", "Brings you to heaven after you die.", Category.MISC, new String[0]);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.gui.screen() instanceof DeathScreen) {
            if (minecraftClient.player.verticalCollision) {
                minecraftClient.player.setBoundingBox(new AABB(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d));
            }
            LocalPlayer clientPlayerEntity = minecraftClient.player;
            Vec3 velocity = minecraftClient.player.getDeltaMovement();
            clientPlayerEntity.setDeltaMovement(velocity.with(Direction.Axis.Y, 3.0d));
        }
    }
}