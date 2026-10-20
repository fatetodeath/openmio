package me.mioclient.module.movement.elytrafly;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import java.util.concurrent.TimeUnit;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.movement.ElytraFly;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;

public class BoostElytraFlight extends ElytraFlyModeBase {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static AntiCheat antiCheat;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }
    public final Stopwatch stopwatch;
    public boolean flag;
    public boolean flag2;
    public float val;

    public BoostElytraFlight(ElytraFly elytraFly) {
        super(elytraFly);
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void onEnable() {
        this.flag = true;
        this.val = this.elytraFly.boostPitch.getValue().floatValue();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTick(TickEvent tickEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMove(MoveEvent moveEvent) {
        float floatValue = this.elytraFly.limit.getValue().floatValue();
        if (floatValue > 200.0f && isNcpBoostInitial()) {
            floatValue = 200.0f;
        }
        if (!this.elytraFly.limit.isAtMaximum() || isNcpBoostInitial()) {
            Vec3 vec3d689 = moveEvent.getMovement();
            if (Math.hypot(vec3d689.x, vec3d689.z) * 20.0d * 3.6d <= floatValue) {
                return;
            }
            // 26.2: Vec3.multiply(double) -> scale(double)
            Vec3 multiply = moveEvent.getMovement().normalize().scale((floatValue / 3.6d) / 20.0d);
            moveEvent.setHorizontal(multiply.x, multiply.z);
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundSetHealthPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundSetHealthPacket ? (ClientboundSetHealthPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundSetHealthPacket) {
            ClientboundSetHealthPacket healthUpdateS2CPacket = packet904;
            if (healthUpdateS2CPacket.getHealth() + healthUpdateS2CPacket.getSaturation() < EntityUtil.getPlayerHealth()) {
                onDisable();
                onEnable();
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketSend(PacketSendEvent sendImmediatelyEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMotion(MotionEvent motionEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTickPost(TickPostEvent tickPostEvent) {
        this.flag2 = false;
        if (PlayerUtils.isInWater(minecraftClient.player)) {
            return;
        }
        AntiCheat antiCheat_lcl = getAntiCheat();
        if (antiCheat_lcl != null && antiCheat_lcl.isF2b2t() && minecraftClient.player.isUsingItem()) {
            return;
        }
        boolean z = minecraftClient.player.input.keyPresses.jump();
        if (this.elytraFly.takeoff.getValue() == ElytraFly.ElytraFlyMode_2.STRICT) {
            z = !minecraftClient.player.onGround() && minecraftClient.player.getDeltaMovement().y < 0.1d;
        } else {
            ClientServices.holeSnapHelper.releaseTickRate(this.elytraFly);
        }
        if (minecraftClient.player.isFallFlying()) {
            this.stopwatch.reset();
        }
        if ((z || !this.stopwatch.hasElapsedMs(1500L)) && !minecraftClient.player.isFallFlying() && !EntityUtil.isOnGround(minecraftClient.player) && this.elytraFly.hasElytra()) {
            PacketSender.sendStartFallFlying();
            minecraftClient.player.startFallFlying();
        }
        if (this.elytraFly.isReallyFallFlying()) {
            if (this.elytraFly.autoBoost.getValue().booleanValue()) {
                oscillateBoostPitch();
            }
            float radians = (float) Math.toRadians(minecraftClient.player.getYRot());
            float floatValue = this.elytraFly.minBoost.getValue().floatValue() * 0.1f;
            if (this.elytraFly.minBoost.isAtMaximum() && antiCheat_lcl != null && antiCheat_lcl.isF2b2t()) {
                floatValue = minecraftClient.player.getXRot() > 0.0f ? 0.025f : 0.018f + (this.elytraFly.verticalBoost.getValue().intValue() * 0.001f);
                if (minecraftClient.player.getXRot() <= 0.0f) {
                    if (!ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsed(10.0d, TimeUnit.SECONDS)) {
                        floatValue = 0.008f;
                    }
                }
            }
            if (isNcpBoostInitial()) {
                floatValue = 3.0f;
            }
            checkNcpBoostTiming();
            if (this.flag2) {
                return;
            }
            if (this.elytraFly.inLava.getValue().booleanValue() && minecraftClient.player.isInLava()) {
                floatValue = 0.2f * this.elytraFly.speed.getValue().floatValue();
            }
            // 26.2: Options.forwardKey/backKey -> keyUp/keyDown, KeyMapping.isPressed -> isDown,
            // LocalPlayer.addVelocity -> Entity.addDeltaMovement
            if (!this.elytraFly.factorize.getValue().booleanValue() ? !(minecraftClient.options.keyUp.isDown() || this.elytraFly.always.getValue().booleanValue()) : minecraftClient.player.getXRot() <= 0.0f) {
                minecraftClient.player.addDeltaMovement(new Vec3(Mth.sin(radians) * (-floatValue), 0.0d, Mth.cos(radians) * floatValue));
            } else if (minecraftClient.options.keyDown.isDown()) {
                minecraftClient.player.addDeltaMovement(new Vec3(Mth.sin(radians) * floatValue, 0.0d, Mth.cos(radians) * (-floatValue)));
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleRotation(RotationEvent event_3) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleInputTick(InputTickEvent tickEvent_2) {
        if (this.elytraFly.autoJump.getValue().booleanValue() && minecraftClient.player.isFallFlying()) {
            { net.minecraft.client.player.ClientInput _ci = tickEvent_2.getInput(); net.minecraft.world.entity.player.Input _p = _ci.keyPresses; _ci.keyPresses = new net.minecraft.world.entity.player.Input(_p.forward(), _p.backward(), _p.left(), _p.right(), true, _p.shift(), _p.sprint()); }
        }
    }

    public void checkNcpBoostTiming() {
        if (minecraftClient.player.getXRot() >= 0.0f || !this.elytraFly.nCPBoost.getValue().booleanValue()) {
            return;
        }
        int fallFlyingTicks = minecraftClient.player.getFallFlyingTicks();
        if (fallFlyingTicks % 6 > 2 || (fallFlyingTicks >= 1 && fallFlyingTicks < 10)) {
            this.flag2 = true;
        } else {
            ClientServices.RotationManager.requestRotations(new float[]{minecraftClient.player.getYRot(), 0.0f}, 10000, true);
            ClientServices.RotationManager.markRotationForced();
        }
    }

    public boolean isNcpBoostInitial() {
        return this.elytraFly.nCPBoost.getValue().booleanValue() && minecraftClient.player.getFallFlyingTicks() <= 5;
    }

    public void oscillateBoostPitch() {
        float floatValue = this.elytraFly.boostPitch.getValue().floatValue();
        if (minecraftClient.player.getY() >= this.elytraFly.minY.getValue().intValue() + this.elytraFly.add.getValue().intValue()) {
            this.flag = true;
        }
        if (minecraftClient.player.getY() <= this.elytraFly.minY.getValue().intValue() && this.flag) {
            this.flag = false;
        }
        if (this.flag) {
            this.val += 4.0f;
        } else {
            this.val -= 4.0f;
        }
        this.val = Mth.clamp(this.val, -floatValue, Math.max(floatValue, minecraftClient.player.getXRot()));
    }

    public float getPitch() {
        return this.val;
    }
}