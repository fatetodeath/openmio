package me.mioclient.module.movement.speed;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.render.RenderUtil;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.movement.Speed;
import net.minecraft.world.entity.Entity;

public final class OnGroundSpeedHandler extends SpeedModeHandler {
    public OnGroundSpeedHandler(Speed speed) {
        super(speed);
        this.num = 2;
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void applyMovement(MoveEvent moveEvent) {
        if (is594()) {
            this.speed.reset();
            MovementUtils.applyVelocity(moveEvent, MovementUtils.getBaseMoveSpeed(true));
            return;
        }
        if (this.num == 4) {
            this.speed.reset();
        }
        if (minecraftClient.player.onGround() || this.num == 3) {
            if ((!minecraftClient.player.horizontalCollision && ((minecraftClient.player.input.keyPresses.forward() ? 1.0f : 0.0f) - (minecraftClient.player.input.keyPresses.backward() ? 1.0f : 0.0f)) != 0.0f) || ((minecraftClient.player.input.keyPresses.left() ? 1.0f : 0.0f) - (minecraftClient.player.input.keyPresses.right() ? 1.0f : 0.0f)) != 0.0f) {
                if (this.num == 2) {
                    this.val2 *= 2.149d;
                    this.num = 3;
                } else if (this.num == 3) {
                    this.num = 2;
                    this.val2 = this.val3 - (0.66d * (this.val3 - MovementUtils.getBaseMoveSpeed(true)));
                }
            }
            this.val2 = Math.min(this.val2, this.speed.speed.getValue().floatValue());
            this.val2 = Math.max(this.val2, MovementUtils.getBaseMoveSpeed(true));
            double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, this.val2);
            // 26.2: AABB.stretch -> expandTowards
            if (!minecraftClient.level.noCollision(minecraftClient.player.getBoundingBox().expandTowards(doubleArray2508[0], 0.0d, doubleArray2508[1]))) {
                this.speed.reset();
                this.val2 = MovementUtils.getBaseMoveSpeed(true);
            }
            MovementUtils.applyVelocity(moveEvent, this.val2);
        }
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void applyAntiKickMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post || is594()) {
            return;
        }
        super.applyAntiKickMotion(motionEvent);
        if (this.num == 3) {
            motionEvent.setY(motionEvent.getY() + (hasVerticalCollision() ? 0.2d : 0.4d) + MovementUtils.getJumpBoostBonus());
        }
    }

    public boolean is594() {
        Entity entity = minecraftClient.player;
        double x = entity.getX() - entity.xo;
        double y = entity.getY() - entity.yo;
        double z = entity.getZ() - entity.zo;
        // 26.2: AABB.offset -> move
        if (!minecraftClient.level.noCollision(minecraftClient.player.getBoundingBox().move(x, y, z))) {
            return true;
        }
        // 26.2: AABB.stretch -> expandTowards
        if (!minecraftClient.level.noCollision(minecraftClient.player.getBoundingBox().expandTowards(0.0d, (-1.0d), 0.0d)) && !minecraftClient.player.horizontalCollision && minecraftClient.player.getY() <= Math.floor(minecraftClient.player.getY()) && is1827()) {
            if (ClientServices.mainhandHelper.getPredictedBox(minecraftClient.player, 3).minY >= minecraftClient.player.getY()) {
                return false;
            }
        }
        return true;
    }

    public boolean is1827() {
        return ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L);
    }
}