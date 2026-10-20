package me.mioclient.module.movement.speed;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.movement.Speed;

public abstract class SpeedModeHandler implements ClientAccessor {
    public final Speed speed;
    public final double val = 0.2873d;
    public double val2;
    public double val3;
    public int num;

    public SpeedModeHandler(Speed speed) {
        this.speed = speed;
    }

    public abstract void applyMovement(MoveEvent moveEvent);

    public void applyAntiKickMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post) {
            return;
        }
        if (!MovementUtils.is2181()) {
            this.num = 4;
            this.val2 = 0.0d;
        }
        double x = minecraftClient.player.getX() - minecraftClient.player.xo;
        double z = minecraftClient.player.getZ() - minecraftClient.player.zo;
        this.val3 = Math.sqrt((x * x) + (z * z));
    }

    public void onSpeedJump() {
    }

    public void onEnable() {
    }

    public boolean hasVerticalCollision() {
        // 26.2: AABB.offset -> move
        return !minecraftClient.level.noCollision(minecraftClient.player.getBoundingBox().move(0.0d, minecraftClient.player.getDeltaMovement().y, 0.0d));
    }
}