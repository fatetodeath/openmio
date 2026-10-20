package me.mioclient.module.movement;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.util.math.Tuple;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockRaycastFilter;
import me.mioclient.util.world.CustomRaycastContext;
import me.mioclient.util.world.HoleData;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.module.movement.holesnap.WarpChargeTracker;

import java.util.concurrent.TimeUnit;
import me.mioclient.render.RenderConstants;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.module.Module;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class HoleSnap extends Module {
    public Setting<Boolean> autoDisable = add(new BooleanSetting("AutoDisable", true));
    public Setting<Boolean> directional = add(new BooleanSetting("Directional", false));
    public Setting<Boolean> shift = add(new BooleanSetting("Shift", false));
    public Setting<Boolean> pauseStep = add(new BooleanSetting("PauseStep", false));
    public Setting<Float> height = add(new NumberSetting<>("Height", 2.0f, 1.0f, 6.0f).setSuffix("m"));
    public Setting<Float> timeout = add(new NumberSetting<>("Timeout", 0.5f, 0.0f, 2.0f).setSuffix("s"));
    public Setting<Float> range = add(new NumberSetting<>("Range", 1.0f, 0.1f, 3.0f).setSuffix("m"));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.5f, 3.0f, v -> !RotationManager.is724()));
    public Setting<Float> pitch = add(new NumberSetting<>("Pitch", 0.0f, 0.0f, 90.0f).setHoleSnapConfig("None", HoleSnapMode.MIN));
    public final WarpChargeTracker holeSnapSearchHelper4_7;
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public boolean flag;
    public boolean flag2;

    public HoleSnap() {
        super("HoleSnap", "Pushes you into holes as you go past them.", Category.MOVEMENT, "holetp", "anchor");
        this.holeSnapSearchHelper4_7 = new WarpChargeTracker(this);
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.flag2 = false;
        this.pitch.setHoleSnapConfig("None", HoleSnapMode.MIN);
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.stopwatch2.setTime(-1L);
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        boolean isInHole = ClientServices.holeSnapOverlayHelper.isInHole();
        if (minecraftClient.player.onGround() && isInHole != this.flag) {
            this.flag = isInHole;
            this.stopwatch.reset();
        }
        if (!shouldPause()) {
            if (this.stopwatch2.hasElapsed(this.timeout.getValue().floatValue(), TimeUnit.SECONDS)) {
                Tuple<Vec3, Double> holeSnapData_2129 = getHoleSnapData_2129();
                if (holeSnapData_2129 == null) {
                    this.flag2 = false;
                    ClientServices.inner.releaseTickRate(this);
                    return;
                }
                if (RotationManager.is724()) {
                    ClientServices.RotationManager.requestRotations(RotationManager.getRotationsTo(holeSnapData_2129.getLeft()), 1337, true);
                    this.flag2 = true;
                }
                WarpChargeTracker.flag = this.holeSnapSearchHelper4_7.tryConsumeCharge();
                if (WarpChargeTracker.flag) {
                    return;
                }
                ClientServices.inner.releaseTickRate(this);
                return;
            }
        }
        ClientServices.inner.releaseTickRate(this);
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        if (shouldPause()) {
            return;
        }
        if (ClientServices.holeSnapOverlayHelper.isHoleAt(PlayerUtils.getRoundedBlockPos())) {
            if (this.autoDisable.getValue().booleanValue()) {
                disable();
                this.flag2 = false;
                return;
            }
            this.stopwatch2.reset();
        }
        if (!this.stopwatch2.hasElapsed(this.timeout.getValue().floatValue(), TimeUnit.SECONDS) || RotationManager.is724()) {
            return;
        }
        Tuple<Vec3, Double> holeSnapData_2129 = getHoleSnapData_2129();
        if (holeSnapData_2129 == null) {
            this.flag2 = false;
            return;
        }
        Vec3 multiply = holeSnapData_2129.getLeft().subtract(minecraftClient.player.position()).normalize().scale(Math.min(MovementUtils.getBaseMoveSpeed(true) * this.speed.getValue().floatValue(), holeSnapData_2129.getRight().doubleValue()));
        moveEvent.setHorizontal(multiply.x, multiply.z);
    }

    @Listen(get219= RenderConstants.num4)
    public void onInputTick(InputTickEvent tickEvent_2) {
        if (!this.flag2 || shouldPause()) {
            return;
        }
        if (this.stopwatch2.hasElapsed(this.timeout.getValue().floatValue(), TimeUnit.SECONDS)) {
            { net.minecraft.client.player.ClientInput _ci = tickEvent_2.getInput(); net.minecraft.world.entity.player.Input _p = _ci.keyPresses; _ci.keyPresses = new net.minecraft.world.entity.player.Input(true, _p.backward(), _p.left(), _p.right(), _p.jump(), _p.shift(), _p.sprint()); }
        }
    }

    @Listen
    public void onRotation(RotationEvent event_3) {
        Tuple<Vec3, Double> holeSnapData_2129;
        if (!this.flag2 || shouldPause()) {
            return;
        }
        if (!this.stopwatch2.hasElapsed(this.timeout.getValue().floatValue(), TimeUnit.SECONDS) || (holeSnapData_2129 = getHoleSnapData_2129()) == null) {
            return;
        }
        float[] floatArray2484 = RotationManager.getRotationsTo(holeSnapData_2129.getLeft());
        event_3.setYaw(floatArray2484[0]);
        event_3.setPitch(floatArray2484[1]);
        event_3.cancel();
    }

    public Tuple<Vec3, Double> getHoleSnapData_2129() {
        double longBitsToDouble = 9999.0d;
        Vec3 vec3d = null;
        Vec3 pos = minecraftClient.player.position();
        for (HoleData holeSnapData : ClientServices.holeSnapOverlayHelper.getHoles()) {
            Vec3 center = holeSnapData.getBox().getCenter();
            Vec3 withAxis = center.with(Direction.Axis.Y, Mth.clamp(pos.y, center.y, (center.y + this.height.getValue().floatValue()) - 0.5d));
            if (holeSnapData.getBlockPos().getY() < pos.y && !holeSnapData.isTrapped()) {
                double distanceTo = pos.distanceTo(withAxis);
                if (!this.directional.getValue().booleanValue() || MovementUtils.isLocalPlayerMovingToward(withAxis) || distanceTo <= 0.05d) {
                    if (distanceTo <= this.range.getValue().floatValue()) {
                        AABB holeBox = holeSnapData.getBox();
                        AABB withMaxY = holeBox.setMaxY(minecraftClient.player.getBoundingBox().maxY);
                        if (minecraftClient.level.noCollision(withMaxY) && !isBlockedRay(pos, withAxis)) {
                            if (!isBlockedRay(pos.add(0.0d, 1.7999999523162842d, 0.0d), withAxis.add(0.0d, 1.7999999523162842d, 0.0d)) && distanceTo < longBitsToDouble) {
                                longBitsToDouble = distanceTo;
                                vec3d = withAxis;
                            }
                        }
                    }
                }
            }
        }
        if (longBitsToDouble > this.range.getValue().floatValue()) {
            return null;
        }
        return new Tuple<>(vec3d, Double.valueOf(longBitsToDouble));
    }

    public boolean shouldPause() {
        if (minecraftClient.player.isCrouching()) {
            return true;
        }
        return (minecraftClient.player.getXRot() < this.pitch.getValue().floatValue() && this.pitch.getValue().floatValue() != 0.0f) || !ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(250L) || minecraftClient.player.isSpectator() || minecraftClient.player.isFallFlying();
    }

    public boolean isBlockedRay(Vec3 vec3d, Vec3 vec3d2) {
        return RaycastUtils.performCustomBlockRaycast(new CustomRaycastContext(vec3d, vec3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player, BlockRaycastFilter.holeSnapHelper)).getType() != HitResult.Type.MISS;
    }

    public boolean shouldPauseStep() {
        return !this.stopwatch.hasElapsedMs(400L) && ClientServices.holeSnapOverlayHelper.isInHole() && this.pauseStep.getValue().booleanValue();
    }
}