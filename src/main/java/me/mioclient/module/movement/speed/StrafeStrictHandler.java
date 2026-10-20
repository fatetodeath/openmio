package me.mioclient.module.movement.speed;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.manager.TickRateManager;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.module.movement.LongJump;
import me.mioclient.module.movement.Speed;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class StrafeStrictHandler extends SpeedModeHandler {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static LongJump longJump;

    public static LongJump getLongJump() {
        if (longJump == null && ClientServices.moduleInjector != null) {
            try {
                longJump = (LongJump) ClientServices.moduleInjector.getModule(LongJump.class);
            } catch (Exception e) {
            }
        }
        return longJump;
    }

    public StrafeStrictHandler(Speed speed) {
        super(speed);
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void applyMovement(MoveEvent moveEvent) {
        if (shouldPause()) {
            return;
        }
        if (this.speed.useTimer.getValue().booleanValue() && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(250L)) {
            TickRateManager holeSnapHelper = ClientServices.holeSnapHelper;
            holeSnapHelper.requestTickRate(this.speed, 1.0888f);
        } else {
            ClientServices.holeSnapHelper.releaseTickRate(this.speed);
        }
        if (!MovementUtils.is2181()) {
            moveEvent.setHorizontal(0.0d, 0.0d);
            return;
        }
        double d = MovementUtils.getBaseMoveSpeed(false);
        if (ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L)) {
            d = Math.max(d, Math.hypot(moveEvent.getX(), moveEvent.getZ()));
        }
        MovementUtils.applyVelocity(moveEvent, d);
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void onSpeedJump() {
        if (!shouldPause() && MovementUtils.is2181() && minecraftClient.player.onGround()) {
            double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, 1.0d);
            float degrees = (float) (Math.toDegrees(Math.atan2(doubleArray2508[1], doubleArray2508[0])) - MathConstants.num2);
            Vec3 velocity = minecraftClient.player.getDeltaMovement();
            float radians = (float) Math.toRadians(degrees);
            float intBitsToFloat = 0.2f;
            minecraftClient.player.setDeltaMovement(velocity.x, 0.4d + MovementUtils.getJumpBoostBonus(), velocity.z);
            // 26.2: LocalPlayer.addVelocityInternal removed -> Entity.addDeltaMovement
            minecraftClient.player.addDeltaMovement(new Vec3((-Mth.sin(radians)) * intBitsToFloat, 0.0d, Mth.cos(radians) * intBitsToFloat));
            minecraftClient.player.setSprinting(true);
        }
    }

    public boolean shouldPause() {
        LongJump longJump_lcl = getLongJump();
        return this.speed.hasHeadCollision() || this.speed.isInLiquidBlocked() || PlayerUtils.isHungry() || minecraftClient.player.isFallFlying() || minecraftClient.player.isSpectator() || (longJump_lcl != null && longJump_lcl.isToggled());
    }
}