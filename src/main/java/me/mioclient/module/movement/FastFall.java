package me.mioclient.module.movement;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.util.math.Vec3dUtils;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.RaycastUtils;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.exploit.Phase;
import me.mioclient.module.movement.Speed;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ClipContext;
import me.mioclient.mixin.ducks.DuckAbstractBlock;
import me.mioclient.mixin.ducks.DuckClientPlayerEntity;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class FastFall extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static Speed speed2;
    private static HoleSnap holeSnap;
    private static AntiCheat antiCheat;
    private static Phase phase;

    public static Speed getSpeed2() {
        if (speed2 == null && ClientServices.moduleInjector != null) {
            try {
                speed2 = (Speed) ClientServices.moduleInjector.getModule(Speed.class);
            } catch (Exception e) {
            }
        }
        return speed2;
    }

    public static HoleSnap getHoleSnap() {
        if (holeSnap == null && ClientServices.moduleInjector != null) {
            try {
                holeSnap = (HoleSnap) ClientServices.moduleInjector.getModule(HoleSnap.class);
            } catch (Exception e) {
            }
        }
        return holeSnap;
    }

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }

    public static Phase getPhase() {
        if (phase == null && ClientServices.moduleInjector != null) {
            try {
                phase = (Phase) ClientServices.moduleInjector.getModule(Phase.class);
            } catch (Exception e) {
            }
        }
        return phase;
    }
    public Setting<FastFallMode> mode = add(new EnumSetting<>("Mode", FastFallMode.PLAIN));
    public Setting<Float> height = add(new NumberSetting<>("Height", 2.0f, 1.0f, 15.0f).setSuffix("m"));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.0f, 5.0f, v -> this.mode.getValue() == FastFall.FastFallMode.PLAIN));
    public Setting<Boolean> stopInLiquids = add(new BooleanSetting("StopInLiquids", true));
    public Setting<Boolean> onlyHole = add(new BooleanSetting("OnlyHole", false));
    public Setting<Boolean> safeDisable = add(new BooleanSetting("SafeDisable", false));
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public boolean flag4;
    public boolean flag5;
    public boolean flag6;
    public double val;
    public double val2;
    public final Stopwatch stopwatch;

    
    public enum FastFallMode implements NamedValue {
        PLAIN("Plain"),
        STRICT("Strict"),
        INSTANT("Instant");

        public final String name;

        FastFallMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public FastFall() {
        super("FastFall", "Allows you to get in holes with ease!", Category.MOVEMENT, "reversestep");
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.flag = false;
        this.flag6 = false;
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (this.safeDisable.getValue().booleanValue() && ClientServices.holeSnapOverlayHelper.isInHole() && this.flag6) {
            disable();
            return;
        }
        if (!shouldActivateStrictFall() || (!this.flag4 && this.onlyHole.getValue().booleanValue())) {
            releaseStrictTickRate();
            return;
        }
        if (minecraftClient.player.onGround()) {
            this.flag2 = true;
            ClientServices.inner.releaseTickRate(this);
            if (ClientServices.antiPhaseHelper.getGroundTicks() > 3) {
                this.stopwatch.reset();
                this.flag3 = true;
                return;
            }
            return;
        }
        if (this.stopwatch.hasElapsedMs(500L)) {
            this.flag3 = !this.flag3;
            this.stopwatch.reset();
        }
        if (minecraftClient.player.getDeltaMovement().y >= 0.0d || !this.flag2 || !this.flag3) {
            releaseStrictTickRate();
        } else {
            ClientServices.inner.requestTickRate(this, 2.0f);
            this.flag6 = true;
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        HoleSnap holeSnap_lcl = getHoleSnap();
        boolean z = MovementUtils.is2181() || (holeSnap_lcl != null && holeSnap_lcl.isToggled() && holeSnap_lcl.getHoleSnapData_2129() != null);
        if ((this.flag4 || !this.onlyHole.getValue().booleanValue()) && z) {
            this.flag5 = false;
            if (!((me.mioclient.mixin.ducks.DuckClientPlayerEntity) minecraftClient.player).lastOnGround() || minecraftClient.player.getDeltaMovement().y > 0.0d || !canFastFall() || isStrictMode()) {
                return;
            }
            AABB box = minecraftClient.player.getBoundingBox();
            AABB withMaxY = new AABB(box.minX, box.minY, box.minZ, box.maxX, minecraftClient.player.getY(), box.maxZ);
            withMaxY = minecraftClient.player.getBoundingBox().expandTowards(0.0d, -this.height.getValue().floatValue(), 0.0d);
            withMaxY = new AABB(withMaxY.minX, withMaxY.minY, withMaxY.minZ, withMaxY.maxX, minecraftClient.player.getY(), withMaxY.maxZ);
            if (!minecraftClient.level.noCollision(minecraftClient.player, withMaxY)) {
                Iterable<net.minecraft.world.phys.shapes.VoxelShape> blockCollisions = minecraftClient.level.getBlockCollisions(minecraftClient.player, withMaxY);
                AtomicReference atomicReference = new AtomicReference(Double.valueOf(minecraftClient.level.getMinY()));
                blockCollisions.forEach(voxelShape -> {
                    atomicReference.set(Double.valueOf(Math.max(((Double) atomicReference.get()).doubleValue(), voxelShape.max(Direction.Axis.Y))));
                });
                if (((Double) atomicReference.get()).doubleValue() < minecraftClient.player.getY()) {
                    double doubleValue = ((Double) atomicReference.get()).doubleValue();
                    Vec3 velocity = minecraftClient.player.getDeltaMovement();
                    boolean z2 = minecraftClient.level.getBlockState(BlockPos.containing(minecraftClient.player.getX(), doubleValue, minecraftClient.player.getZ()).offset(new Vec3i((int) velocity.x, -1, (int) velocity.z))).isAir() || !ClientServices.holeSnapRenderHelper.getTeleportStopwatch().hasElapsedMs(25L) || this.mode.getValue() == FastFallMode.PLAIN;
                    if (minecraftClient.player.getY() - doubleValue > 2.0d || z2) {
                        doubleValue = minecraftClient.player.getY() - 2.0d;
                        this.flag5 = true;
                        float speedValue = this.mode.getValue() == FastFallMode.PLAIN ? this.speed.getValue().floatValue() : 1.0f;
                        if (speedValue != 0.0f) {
                            minecraftClient.player.setDeltaMovement(velocity.with(Direction.Axis.Y, -speedValue));
                        }
                    }
                    if (!z2) {
                        minecraftClient.player.setPos(minecraftClient.player.getX(), doubleValue, minecraftClient.player.getZ());
                    }
                    Phase phase_lcl = getPhase();
                    if (phase_lcl != null) {
                        phase_lcl.resetGroundTicks();
                    }
                    this.flag6 = true;
                }
            }
        }
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        this.val = RaycastUtils.getDistanceToCeiling();
        this.val2 = RaycastUtils.getDistanceToFloor();
        this.flag4 = ClientServices.holeSnapOverlayHelper.isHoleAt(minecraftClient.player.blockPosition().below((int) this.val2));
        if (this.flag4 || !this.onlyHole.getValue().booleanValue()) {
            double longBitsToDouble = 0.3d;
            HoleSnap holeSnap_lcl = getHoleSnap();
            boolean z = holeSnap_lcl != null && holeSnap_lcl.isToggled() && holeSnap_lcl.stopwatch2.hasElapsedMs(500L) && holeSnap_lcl.getHoleSnapData_2129() != null;
            if ((isStrictMode() && canFastFall() && hasGroundBelow() && minecraftClient.player.onGround()) || (this.flag5 && !z && hasBlockAhead())) {
                moveEvent.setMovement(new Vec3(moveEvent.getX() * longBitsToDouble, moveEvent.getY(), moveEvent.getZ() * longBitsToDouble));
            }
        }
    }

    public boolean hasGroundBelow() {
        AABB boundingBox = minecraftClient.player.getBoundingBox();
        Vec3 center = boundingBox.getCenter();
        double d = boundingBox.minX;
        double d2 = boundingBox.minZ;
        double d3 = boundingBox.maxX;
        double d4 = boundingBox.maxZ;
        HashMap<Vec3, Vec3> hashMap = new HashMap<>();
        hashMap.put(center, new Vec3(center.x, center.y - 1.0d, center.z));
        hashMap.put(new Vec3(d, center.y, d2), new Vec3(d, center.y - 1.0d, d2));
        hashMap.put(new Vec3(d3, center.y, d2), new Vec3(d3, center.y - 1.0d, d2));
        hashMap.put(new Vec3(d, center.y, d4), new Vec3(d, center.y - 1.0d, d4));
        hashMap.put(new Vec3(d3, center.y, d4), new Vec3(d3, center.y - 1.0d, d4));
        for (Vec3 vec3d : hashMap.keySet()) {
            BlockHitResult raycast = minecraftClient.level.clip(new ClipContext(vec3d, (Vec3) hashMap.get(vec3d), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player));
            if (raycast != null && raycast.getType() == HitResult.Type.BLOCK) {
                return false;
            }
        }
        if (!hasBlockAhead()) {
            return false;
        }
        BlockState blockState = minecraftClient.level.getBlockState(BlockPos.containing(minecraftClient.player.getX(), minecraftClient.player.getY() - 1.0d, minecraftClient.player.getZ()));
        return blockState == null || blockState.getBlock() == Blocks.AIR;
    }

    public boolean hasBlockAhead() {
        Vec3 velocity = minecraftClient.player.getDeltaMovement();
        double d = velocity.x;
        double d2 = velocity.z;
        double signum = Math.abs(d2) > Math.abs(d) ? 0.0d : Math.signum(d);
        BlockState blockState = minecraftClient.level.getBlockState(BlockPos.containing(minecraftClient.player.getX() + signum, minecraftClient.player.getY() - 1.0d, minecraftClient.player.getZ() + (Math.abs(signum) > Math.abs(d2) ? 0.0d : Math.signum(d2))));
        if (blockState != null) {
            return ((me.mioclient.mixin.ducks.DuckAbstractBlock) (Object) blockState.getBlock()).isCollidable();
        }
        return true;
    }

    public boolean canFastFall() {
        Speed speed2_lcl = getSpeed2();
        if (((speed2_lcl == null || !speed2_lcl.isToggled() || speed2_lcl.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode4 || speed2_lcl.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode3)) && !minecraftClient.player.isShiftKeyDown() && !minecraftClient.player.isFallFlying() && !minecraftClient.player.isInWall() && ((!PlayerUtils.isInLiquid(minecraftClient.player) || !this.stopInLiquids.getValue().booleanValue()) && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L))) {
            if (!(minecraftClient.level.getBlockState(BlockPos.containing(minecraftClient.player.position())).getBlock() instanceof BedBlock)) {
                return true;
            }
        }
        return false;
    }

    public boolean isStrictMode() {
        return this.mode.getValue() == FastFallMode.STRICT;
    }

    public boolean shouldActivateStrictFall() {
        return canFastFall() && isStrictMode() && this.val != 0.0d && this.val2 != 0.0d && this.val2 <= ((double) this.height.getValue().floatValue());
    }

    public void releaseStrictTickRate() {
        if (isStrictMode()) {
            this.flag2 = false;
            ClientServices.inner.releaseTickRate(this);
        }
    }
}