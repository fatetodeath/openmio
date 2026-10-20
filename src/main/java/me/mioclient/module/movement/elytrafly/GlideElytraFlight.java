package me.mioclient.module.movement.elytrafly;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.movement.ElytraFly;
import net.minecraft.core.Direction;

public class GlideElytraFlight extends ElytraFlyModeBase {
    public long num;
    public float val;
    public final Stopwatch stopwatch;

    public GlideElytraFlight(ElytraFly elytraFly) {
        super(elytraFly);
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void onEnable() {
        this.val = 0.0f;
        this.num = System.currentTimeMillis();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTick(TickEvent tickEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMove(MoveEvent moveEvent) {
        if (this.elytraFly.isPacketFlyActive()) {
            if (!MovementUtils.is2181()) {
                this.num = System.currentTimeMillis();
            }
            if (this.elytraFly.accelerate.getValue().booleanValue()) {
                this.val = (float) MovementUtils.computeAcceleratedSpeed(this.elytraFly.speed3.getValue().floatValue(), this.elytraFly.accelMin.getValue().floatValue(), this.elytraFly.accelTime.getValue().floatValue(), this.num);
            } else {
                this.val = this.elytraFly.speed3.getValue().floatValue();
            }
            double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, this.val);
            float f = -this.elytraFly.glide.getValue().floatValue();
            minecraftClient.player.setDeltaMovement(doubleArray2507[0], f, doubleArray2507[1]);
            moveEvent.setY(f);
            if (minecraftClient.player.input.keyPresses.shift()) {
                moveEvent.setY((-0.5d));
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, (-0.5d)));
            }
            PacketSender.sendStartFallFlying();
            if (this.elytraFly.antiAfk.getValue().booleanValue() && moveEvent.getX() == 0.0d && moveEvent.getZ() == 0.0d && this.stopwatch.hasElapsedMs(2500L)) {
                moveEvent.setHorizontal(Math.sin(Math.toRadians(minecraftClient.player.tickCount % MathConstants.num3)) * 0.03d, Math.cos(Math.toRadians(minecraftClient.player.tickCount % MathConstants.num3)) * 0.03d);
            } else if (MovementUtils.is2181()) {
                this.stopwatch.reset();
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketSend(PacketSendEvent sendImmediatelyEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMotion(MotionEvent motionEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTickPost(TickPostEvent tickPostEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleRotation(RotationEvent event_3) {
    }
}