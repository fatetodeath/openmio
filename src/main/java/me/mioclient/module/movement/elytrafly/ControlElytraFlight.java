package me.mioclient.module.movement.elytrafly;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.network.PacketSender;
import me.mioclient.module.movement.ElytraFly;
import me.mioclient.module.movement.Fireworks;
import me.mioclient.module.movement.ObstaclePasser;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;

public class ControlElytraFlight extends ElytraFlyModeBase {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static ObstaclePasser obstaclePasser;
    private static Fireworks fireworks;

    public static ObstaclePasser getObstaclePasser() {
        if (obstaclePasser == null && ClientServices.moduleInjector != null) {
            try {
                obstaclePasser = (ObstaclePasser) ClientServices.moduleInjector.getModule(ObstaclePasser.class);
            } catch (Exception e) {
            }
        }
        return obstaclePasser;
    }

    public static Fireworks getFireworks() {
        if (fireworks == null && ClientServices.moduleInjector != null) {
            try {
                fireworks = (Fireworks) ClientServices.moduleInjector.getModule(Fireworks.class);
            } catch (Exception e) {
            }
        }
        return fireworks;
    }
    public Vec3 vec3d;
    public int num;
    public double val;
    public long num2;
    public long num3;

    public ControlElytraFlight(ElytraFly elytraFly) {
        super(elytraFly);
        this.vec3d = Vec3.ZERO;
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void onEnable() {
        this.val = 0.0d;
        long currentTimeMillis = System.currentTimeMillis();
        this.num2 = currentTimeMillis;
        this.num3 = currentTimeMillis;
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTick(TickEvent tickEvent) {
        if (!minecraftClient.player.isFallFlying()) {
            this.num3 = System.currentTimeMillis();
        }
        if (minecraftClient.player.input.keyPresses.jump() || minecraftClient.player.input.keyPresses.shift()) {
            return;
        }
        this.num3 = System.currentTimeMillis();
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMove(MoveEvent moveEvent) {
        if (isObstaclePasserActive()) {
            return;
        }
        if (!minecraftClient.player.isFallFlying() || this.elytraFly.isManualVerticalActive()) {
            this.num2 = System.currentTimeMillis();
            return;
        }
        if (isElytraBoostReady() && this.elytraFly.hasElytra()) {
            if (!MovementUtils.is2181()) {
                this.num2 = System.currentTimeMillis();
            }
            float floatValue = this.elytraFly.vSpeed.getValue().floatValue();
            if (this.elytraFly.accelerate.getValue().booleanValue()) {
                this.val = (float) MovementUtils.computeAcceleratedSpeed(this.elytraFly.speed3.getValue().floatValue(), this.elytraFly.accelMin.getValue().floatValue(), this.elytraFly.accelTime.getValue().floatValue(), this.num2);
                floatValue = (float) MovementUtils.computeAcceleratedSpeed(floatValue, this.elytraFly.verAccelMin.getValue().floatValue(), this.elytraFly.accelTime.getValue().floatValue(), this.num3);
            } else {
                this.val = this.elytraFly.speed3.getValue().floatValue();
            }
            Fireworks fireworks_lcl = getFireworks();
            if (fireworks_lcl != null) {
                this.val += fireworks_lcl.getBoostFactor(false);
            }
            if (this.num == 0) {
                if (minecraftClient.player.input.keyPresses.jump() && this.elytraFly.vertical.getValue() == ElytraFly.ElytraFlyMode.STRICT && this.val >= 2.0d) {
                    this.val = 2.0d;
                }
                double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, this.val);
                minecraftClient.player.setDeltaMovement(doubleArray2507[0], minecraftClient.player.getDeltaMovement().y, doubleArray2507[1]);
                this.vec3d = minecraftClient.player.getDeltaMovement();
                if (this.elytraFly.antiKick.getValue().booleanValue() && minecraftClient.player.tickCount % 4 == 0) {
                    moveEvent.setY(moveEvent.getY() - 1.0E-4d);
                    minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().add(0.0d, (-1.0E-4d), 0.0d));
                }
            }
            if (minecraftClient.player.input.keyPresses.jump() && minecraftClient.player.isFallFlying()) {
                if (this.elytraFly.vertical.getValue() == ElytraFly.ElytraFlyMode.STRICT) {
                    this.num++;
                }
                if (this.elytraFly.vertical.getValue() != ElytraFly.ElytraFlyMode.PLAIN) {
                    if (this.elytraFly.vertical.getValue() == ElytraFly.ElytraFlyMode.STRICT && MovementUtils.is2181()) {
                        do861();
                        return;
                    }
                    return;
                }
                moveEvent.setY(floatValue);
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, floatValue));
                return;
            }
            if (minecraftClient.player.input.keyPresses.shift()) {
                this.num = 0;
                moveEvent.setY(-floatValue);
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, -floatValue));
                return;
            }
            this.num = 0;
            moveEvent.setY(0.0d);
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, 0.0d));
            if (minecraftClient.player.verticalCollision || this.elytraFly.glide.getValue().floatValue() == 0.0f) {
                return;
            }
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().add(0.0d, -this.elytraFly.glide.getValue().floatValue(), 0.0d));
            moveEvent.setY(minecraftClient.player.getDeltaMovement().y);
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            this.num2 = System.currentTimeMillis();
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handlePacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (isObstaclePasserActive()) {
            return;
        }
        if (!(sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket move)) {
            return;
        }
        if (minecraftClient.player.isFallFlying() && !this.elytraFly.isManualVerticalActive() && isElytraBoostReady() && this.elytraFly.hasElytra()) {
            Float newPitch = null;
            if (isVerticalInput()) {
                newPitch = getPitch();
            } else if (MovementUtils.is2181()) {
                if (this.elytraFly.spoofPitch.getValue().booleanValue()) {
                    newPitch = this.elytraFly.pitch.getValue().floatValue();
                }
                if (this.num > 0) {
                    newPitch = -this.elytraFly.vPitch.getValue().floatValue();
                }
            }
            if (newPitch != null) {
                // 26.2 MojMap: packets IMMUTABLE — cancel + resend with modified XRot
                sendImmediatelyEvent.cancel();
                float nx = newPitch.floatValue();
                boolean og = move.isOnGround();
                boolean hc = move.horizontalCollision();
                if (move.hasPosition() && move.hasRotation()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.PosRot(new Vec3(move.getX(0.0d), move.getY(0.0d), move.getZ(0.0d)), move.getYRot(0.0f), nx, og, hc));
                } else if (move.hasPosition()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.Pos(new Vec3(move.getX(0.0d), move.getY(0.0d), move.getZ(0.0d)), og, hc));
                } else if (move.hasRotation()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.Rot(move.getYRot(0.0f), nx, og, hc));
                } else {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(og, hc));
                }
                return;
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleMotion(MotionEvent motionEvent) {
        if (minecraftClient.player.isFallFlying() && !this.elytraFly.isManualVerticalActive() && isElytraBoostReady() && this.elytraFly.hasElytra() && motionEvent.getPhase() == KeyPearlMode.Pre && !isObstaclePasserActive()) {
            if (isVerticalInput()) {
                // 26.2: MotionEvent.setXRot -> setPitch, setYRot -> setYaw
                motionEvent.setPitch(getPitch());
                return;
            }
            if (MovementUtils.is2181()) {
                motionEvent.setYaw(getYaw());
                if (this.elytraFly.spoofPitch.getValue().booleanValue()) {
                    motionEvent.setPitch(this.elytraFly.pitch.getValue().floatValue());
                }
                if (this.num > 0) {
                    motionEvent.setPitch(-this.elytraFly.vPitch.getValue().floatValue());
                }
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleTickPost(TickPostEvent tickPostEvent) {
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleRotation(RotationEvent event_3) {
        if (this.elytraFly.hasElytra() && minecraftClient.player.isFallFlying() && !this.elytraFly.isManualVerticalActive() && isElytraBoostReady() && !isObstaclePasserActive()) {
            if (isVerticalInput()) {
                // 26.2: RotationEvent.setXRot -> setPitch, setYRot -> setYaw
                event_3.setPitch(getPitch());
            }
            if (MovementUtils.is2181()) {
                event_3.setYaw(getYaw());
            }
        }
    }

    @Override // me.mioclient.module.movement.elytrafly.ElytraFlyModeBase
    public void handleInputTick(InputTickEvent tickEvent_2) {
        if (tickEvent_2.getInput() == null || minecraftClient.player.input == null || isObstaclePasserActive() || !this.elytraFly.isManualVerticalActive()) {
            return;
        }
        { net.minecraft.client.player.ClientInput _ci = tickEvent_2.getInput(); net.minecraft.world.entity.player.Input _p = _ci.keyPresses; _ci.keyPresses = new net.minecraft.world.entity.player.Input(true, _p.backward(), _p.left(), _p.right(), true, _p.shift(), _p.sprint()); }
    }

    public void do861() {
        double d = MathUtils.getRadians(-this.elytraFly.vPitch.getValue().floatValue());
        Vec3 vec3d = this.vec3d;
        Vec3 vec3d375 = MathUtils.getRotationVector(-this.elytraFly.vPitch.getValue().floatValue(), minecraftClient.player.getYRot());
        // 26.2: Vec3.horizontalLength removed -> Math.hypot(x, z)
        double horizontalLength = Math.hypot(vec3d.x, vec3d.z);
        double horizontalLength2 = Math.hypot(vec3d375.x, vec3d375.z);
        double length = vec3d375.length();
        double cos = Math.cos(d);
        Vec3 add = vec3d.add(0.0d, 0.08d * ((-1.0d) + (cos * cos * Math.min(1.0d, length / 0.4d) * 0.75d)), 0.0d);
        if (d < 0.0d && horizontalLength2 > 0.0d) {
            double longBitsToDouble = horizontalLength * (-Math.sin(d)) * 0.04d;
            add = add.add(((-vec3d375.x) * longBitsToDouble) / horizontalLength2, longBitsToDouble * 3.2d, ((-vec3d375.z) * longBitsToDouble) / horizontalLength2);
        }
        minecraftClient.player.setDeltaMovement(add.multiply(0.99d, 0.98d, 0.99d));
        this.vec3d = minecraftClient.player.getDeltaMovement();
    }

    public float getYaw() {
        double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, 1.0d);
        return (float) (Math.toDegrees(Math.atan2(doubleArray2508[1], doubleArray2508[0])) - MathConstants.num2);
    }

    public float getPitch() {
        float pitch = minecraftClient.player.getXRot();
        if (isVerticalInput()) {
            pitch = MathConstants.num2;
            if (MovementUtils.is2181()) {
                pitch = MathConstants.num;
            }
            if (minecraftClient.player.input.keyPresses.jump()) {
                pitch = -pitch;
            }
        }
        return pitch;
    }

    public boolean isVerticalInput() {
        return minecraftClient.player.input.keyPresses.jump() || (minecraftClient.player.input.keyPresses.shift() && this.elytraFly.vertical.getValue() == ElytraFly.ElytraFlyMode.PLAIN && RotationManager.is724());
    }

    public boolean isElytraBoostReady() {
        Fireworks fireworks_lcl = getFireworks();
        if (fireworks_lcl != null && minecraftClient.player.isFallFlying() && this.elytraFly.hasElytra() && fireworks_lcl.autoLaunch.getValue().booleanValue() && fireworks_lcl.isToggled() && fireworks_lcl.stopwatch.hasElapsedMs(500L)) {
            return fireworks_lcl.hasActiveFirework();
        }
        return true;
    }

    public boolean isObstaclePasserActive() {
        ObstaclePasser obstaclePasser_lcl = getObstaclePasser();
        if (obstaclePasser_lcl == null) {
            return false;
        }
        return obstaclePasser_lcl.isPathing();
    }
}