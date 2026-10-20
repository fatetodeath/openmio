package me.mioclient.module.movement.elytrafly;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.network.PacketSender;
import me.mioclient.module.movement.ElytraFly;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public class BounceElytraFlight extends ElytraFlyModeBase {
    public long num;
    public boolean flag;
    public boolean flag2;
    public boolean flag3;

    public BounceElytraFlight(ElytraFly elytraFly) {
        super(elytraFly);
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void onEnable() {
        if (minecraftClient.player.isFallFlying() && !minecraftClient.player.onGround()) {
            this.flag = true;
            minecraftClient.player.stopFallFlying();
        }
        this.flag2 = false;
        this.flag3 = false;
        this.num = System.currentTimeMillis();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTick(TickEvent tickEvent) {
        if (minecraftClient.player.onGround()) {
            this.flag = false;
        } else if (minecraftClient.player.isFallFlying()) {
            this.flag = true;
        }
        boolean z = minecraftClient.player.input.keyPresses.jump();
        if (this.elytraFly.takeoff.getValue() == ElytraFly.ElytraFlyMode_2.STRICT) {
            z = !minecraftClient.player.onGround() && minecraftClient.player.getDeltaMovement().y < 0.1d;
        } else {
            ClientServices.holeSnapHelper.releaseTickRate(this.elytraFly);
        }
        if (z || this.flag2) {
            this.elytraFly.sendStartFallFlying();
            this.flag2 = !minecraftClient.player.isFallFlying();
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMove(MoveEvent moveEvent) {
        if (!minecraftClient.player.isFallFlying() && this.flag && !minecraftClient.player.horizontalCollision && minecraftClient.player.getDeltaMovement().y < 0.0d) {
            this.flag2 = true;
        }
        if (minecraftClient.player.isFallFlying()) {
            if (!MovementUtils.is2181()) {
                this.num = System.currentTimeMillis();
            }
            float floatValue = this.elytraFly.speed2.getValue().floatValue();
            double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, Mth.lerp(Mth.clamp(((float) (System.currentTimeMillis() - this.num)) / 500.0f, 0.0f, 1.0f), this.elytraFly.start.getValue().floatValue(), floatValue));
            minecraftClient.player.setDeltaMovement(doubleArray2507[0], 0.0d, doubleArray2507[1]);
            moveEvent.setY(0.0d);
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, 0.0d));
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
        if ((channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) && minecraftClient.player.isFallFlying() && !minecraftClient.player.onGround()) {
            this.num = System.currentTimeMillis();
            minecraftClient.player.stopFallFlying();
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (!(sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket move)) {
            return;
        }
        if (!minecraftClient.player.isFallFlying() || minecraftClient.player.onGround() || minecraftClient.player.getDeltaMovement().y >= 0.1d || !MovementUtils.is2181()) {
            return;
        }
        // 26.2 MojMap: ServerboundMovePlayerPacket is IMMUTABLE — cancel + resend with modified XRot
        sendImmediatelyEvent.cancel();
        float newXRot = (-5.0f);
        boolean og = move.isOnGround();
        boolean hc = move.horizontalCollision();
        if (move.hasPosition() && move.hasRotation()) {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.PosRot(new Vec3(move.getX(0.0d), move.getY(0.0d), move.getZ(0.0d)), move.getYRot(0.0f), newXRot, og, hc));
        } else if (move.hasPosition()) {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.Pos(new Vec3(move.getX(0.0d), move.getY(0.0d), move.getZ(0.0d)), og, hc));
        } else if (move.hasRotation()) {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.Rot(move.getYRot(0.0f), newXRot, og, hc));
        } else {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(og, hc));
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() != KeyPearlMode.Pre) {
            return;
        }
        boolean z = false;
        // 26.2: MotionEvent.onGround -> isOnGround, setYRot/getYRot -> setYaw/getYaw, setXRot/getXRot -> setPitch/getPitch
        if (this.elytraFly.hasElytra() && !motionEvent.isOnGround()) {
            if (!minecraftClient.player.isFallFlying()) {
                z = true;
                this.flag3 = false;
            } else if (!this.flag3) {
                if (MovementUtils.is2181()) {
                    this.flag3 = true;
                } else {
                    z = true;
                }
            }
        }
        if (z) {
            motionEvent.setYaw(motionEvent.getYaw() + (minecraftClient.player.tickCount % 2 == 0 ? -2 : 2));
            motionEvent.setPitch(motionEvent.getPitch() + (minecraftClient.player.tickCount % 2 == 0 ? -2 : 2));
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTickPost(TickPostEvent tickPostEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleRotation(RotationEvent event_3) {
    }
}