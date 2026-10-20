package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.mode.combat.CrystalRotationMode;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.player.AttackHookPostEvent;
import me.mioclient.event.events.network.ExplosionVelocityEvent;
import me.mioclient.event.events.interaction.InteractBlockEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.SprintUpdateEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.movement.ElytraFly;
import me.mioclient.module.player.RotationLock;
import me.mioclient.module.player.Scaffold;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Sprint extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static Scaffold scaffold;
    private static ElytraFly efly;
    private static LongJump longJump;
    private static FastFall fastFall;
    private static Speed speed;
    private static FastWeb fastWeb;
    private static AntiCheat antiCheat;
    private static RotationLock rotationLock;

    public static Scaffold getScaffold() {
        if (scaffold == null && ClientServices.moduleInjector != null) {
            try {
                scaffold = (Scaffold) ClientServices.moduleInjector.getModule(Scaffold.class);
            } catch (Exception e) {
            }
        }
        return scaffold;
    }

    public static ElytraFly getEfly() {
        if (efly == null && ClientServices.moduleInjector != null) {
            try {
                efly = (ElytraFly) ClientServices.moduleInjector.getModule(ElytraFly.class);
            } catch (Exception e) {
            }
        }
        return efly;
    }

    public static LongJump getLongJump() {
        if (longJump == null && ClientServices.moduleInjector != null) {
            try {
                longJump = (LongJump) ClientServices.moduleInjector.getModule(LongJump.class);
            } catch (Exception e) {
            }
        }
        return longJump;
    }

    public static FastFall getFastFall() {
        if (fastFall == null && ClientServices.moduleInjector != null) {
            try {
                fastFall = (FastFall) ClientServices.moduleInjector.getModule(FastFall.class);
            } catch (Exception e) {
            }
        }
        return fastFall;
    }

    public static Speed getSpeed() {
        if (speed == null && ClientServices.moduleInjector != null) {
            try {
                speed = (Speed) ClientServices.moduleInjector.getModule(Speed.class);
            } catch (Exception e) {
            }
        }
        return speed;
    }

    public static FastWeb getFastWeb() {
        if (fastWeb == null && ClientServices.moduleInjector != null) {
            try {
                fastWeb = (FastWeb) ClientServices.moduleInjector.getModule(FastWeb.class);
            } catch (Exception e) {
            }
        }
        return fastWeb;
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

    public static RotationLock getRotationLock() {
        if (rotationLock == null && ClientServices.moduleInjector != null) {
            try {
                rotationLock = (RotationLock) ClientServices.moduleInjector.getModule(RotationLock.class);
            } catch (Exception e) {
            }
        }
        return rotationLock;
    }
    public Setting<SprintPredicateMode> mode = add(new EnumSetting<>("Mode", SprintPredicateMode.RAGE));
    public Setting<Boolean> forceSprint = add(new BooleanSetting("ForceSprint", true, v -> this.mode.getValue() == Sprint.SprintPredicateMode.INSTANT));
    public Setting<Boolean> vanillaSlowdown = add(new BooleanSetting("VanillaSlowdown", false, v -> this.mode.getValue() == Sprint.SprintPredicateMode.INSTANT));
    public Setting<Boolean> boost = add(new BooleanSetting("Boost", false, v -> this.mode.getValue() == Sprint.SprintPredicateMode.INSTANT).setVisible());
    public Setting<Float> boostSpeed = add(new NumberSetting<>("BoostSpeed", 0.5f, 0.1f, 1.0f, v -> this.mode.getValue() == Sprint.SprintPredicateMode.INSTANT && this.boost.isShown()));
    public Setting<Boolean> stopInLiquids = add(new BooleanSetting("StopInLiquids", true));
    public Setting<Boolean> directionSpoof = add(new BooleanSetting("DirectionSpoof", false));
    public Setting<Boolean> keep = add(new BooleanSetting("Keep", true, v -> this.mode.getValue() != Sprint.SprintPredicateMode.INSTANT));
    public final Stopwatch stopwatch;
    public float val;
    public boolean flag;
    public boolean flag2;

    
    public enum SprintPredicateMode implements NamedValue {
        LEGIT("Legit"),
        RAGE("Rage"),
        INSTANT("Instant");

        public final String name;

        SprintPredicateMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Sprint() {
        super("Sprint", "Sprints automatically.", Category.MOVEMENT, new String[0]);
        this.stopwatch = new Stopwatch();
        this.val = 0.0f;
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return ClientTextRenderer.capitalize(this.mode.getValue());
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            this.val = 0.0f;
        }
    }

    @Listen
    public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
        AntiCheat antiCheat_lcl = getAntiCheat();
        if (!ClientServices.obstaclePasserHelper.isPathing() && antiCheat_lcl != null && antiCheat_lcl.movementSync.getValue().booleanValue() && antiCheat_lcl.rotations.getValue() == CrystalRotationMode.SILENT && this.directionSpoof.getValue().booleanValue() && ClientServices.RotationManager.getActiveRequest() == null && MovementUtils.is2181() && MovementUtils.getMovementYaw() != minecraftClient.player.getYRot() && !this.flag2) {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.PosRot(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), minecraftClient.player.getYRot(), minecraftClient.player.getXRot(), minecraftClient.player.onGround(), minecraftClient.player.horizontalCollision));
        }
    }

    @Listen
    public void onExplosionVelocity(ExplosionVelocityEvent explosionVelocityEvent) {
        if (ClientServices.obstaclePasserHelper.isPathing()) {
            return;
        }
        Vec3 vec3d = new Vec3(explosionVelocityEvent.getX(), explosionVelocityEvent.getY(), explosionVelocityEvent.getZ());
        if (MovementUtils.is2181() && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(750L) && this.boost.getValue().booleanValue() && Math.sqrt(minecraftClient.player.distanceToSqr(vec3d)) <= 6.0d) {
            this.stopwatch.reset();
            this.val = ((float) Math.hypot(explosionVelocityEvent.getPlayerVelocityX(), explosionVelocityEvent.getPlayerVelocityZ())) * this.boostSpeed.getValue().floatValue();
        }
    }

    @Listen(get219= RenderConstants.num2)
    public void onMotion(MotionEvent motionEvent) {
        RotationLock rotationLock_lcl = getRotationLock();
        if (ClientServices.obstaclePasserHelper.isPathing() || isNotInGame() || shouldStopSprint() || (rotationLock_lcl != null && rotationLock_lcl.isToggled()) || !this.directionSpoof.getValue().booleanValue() || !MovementUtils.is2181() || minecraftClient.player.isFallFlying()) {
            return;
        }
        motionEvent.setYaw(MovementUtils.getMovementYaw());
    }

    @Listen
    public void onSprintUpdate(SprintUpdateEvent sprintUpdateEvent) {
        if (ClientServices.obstaclePasserHelper.isPathing() || isNotInGame() || shouldStopSprint()) {
            return;
        }
        if (this.mode.getValue() != SprintPredicateMode.INSTANT || this.forceSprint.getValue().booleanValue()) {
            setSprinting(is2181());
        }
    }

    @Listen(get219= 1000)
    public void onMove(MoveEvent moveEvent) {
        FastFall fastFall_lcl = getFastFall();
        LongJump longJump_lcl = getLongJump();
        Speed speed_lcl = getSpeed();
        if (ClientServices.obstaclePasserHelper.isPathing() || isNotInGame() || shouldStopSprint() || this.mode.getValue() != SprintPredicateMode.INSTANT || PlayerUtils.isHungry() || minecraftClient.player.isFallFlying() || minecraftClient.player.getAbilities().flying || minecraftClient.player.isInWater() || minecraftClient.player.isInLava() || minecraftClient.player.isShiftKeyDown() || minecraftClient.player.isSwimming() || (fastFall_lcl != null && fastFall_lcl.flag) || (longJump_lcl != null && longJump_lcl.isToggled()) || (speed_lcl != null && speed_lcl.isToggled())) {
            return;
        }
        double d = MovementUtils.getBaseMoveSpeed(this.vanillaSlowdown.getValue().booleanValue());
        if (ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L)) {
            d = Math.max(d, Math.hypot(moveEvent.getX(), moveEvent.getZ()));
        }
        if (this.vanillaSlowdown.getValue().booleanValue()) {
            net.minecraft.world.level.block.Block r0 = minecraftClient.level.getBlockState(minecraftClient.player.blockPosition()).getBlock();
            if (r0 == Blocks.SLIME_BLOCK) {
                d *= 0.7d;
            }
        }
        if (this.boost.getValue().booleanValue() && !this.stopwatch.hasElapsedMs(500L)) {
            d += this.val;
        }
        MovementUtils.applyVelocity(moveEvent, d);
    }

    @Listen
    public void onRotation(RotationEvent event_3) {
        if (!ClientServices.obstaclePasserHelper.isPathing() && this.directionSpoof.getValue().booleanValue() && MovementUtils.is2181() && !minecraftClient.player.isFallFlying()) {
            if ((event_3.isCurrent() || minecraftClient.player.isSwimming()) && !shouldStopSprint()) {
                RotationLock rotationLock_lcl = getRotationLock();
                if ((!minecraftClient.player.isVisuallyCrawling() || event_3.isCurrent()) && (rotationLock_lcl == null || !rotationLock_lcl.isToggled())) {
                    event_3.setYaw(MovementUtils.getMovementYaw());
                    event_3.cancel();
                }
            }
        }
    }

    @Listen
    public void onAttackHookPost(AttackHookPostEvent attackHookPostEvent) {
        if (!ClientServices.obstaclePasserHelper.isPathing() && this.keep.getValue().booleanValue()) {
            if (attackHookPostEvent.getPhase() == KeyPearlMode.Pre) {
                this.flag = minecraftClient.player.isSprinting();
                if (this.flag) {
                    PacketSender.sendClientCommand(minecraftClient.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING, 0);
                    return;
                }
                return;
            }
            if (this.flag) {
                minecraftClient.player.setSprinting(true);
                PacketSender.sendClientCommand(minecraftClient.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING, 0);
            }
        }
    }

    public boolean is2181() {
        if (shouldStopSprint()) {
            return false;
        }
        if (this.directionSpoof.getValue().booleanValue() && (ClientServices.RotationManager.getActiveRequest() != null || minecraftClient.player.isFallFlying())) {
            return false;
        }
        boolean z = this.mode.getValue() == SprintPredicateMode.LEGIT || minecraftClient.player.isInWater();
        if (PlayerUtils.isHungry()) {
            return false;
        }
        if ((z && (((minecraftClient.player.input.keyPresses.forward() ? 1.0f : 0.0f) - (minecraftClient.player.input.keyPresses.backward() ? 1.0f : 0.0f)) <= 0.0f || minecraftClient.player.horizontalCollision)) || isAboveWater()) {
            return false;
        }
        FastWeb fastWeb_lcl = getFastWeb();
        if (fastWeb_lcl != null && fastWeb_lcl.isGrimMode() && PlayerUtils.isInCobweb(minecraftClient.player) && (ClientServices.antiPhaseHelper.getGroundTicks() < 2 || minecraftClient.player.input.keyPresses.jump())) {
            return false;
        }
        return (((minecraftClient.player.input.keyPresses.forward() ? 1.0f : 0.0f) - (minecraftClient.player.input.keyPresses.backward() ? 1.0f : 0.0f)) == 0.0f && ((minecraftClient.player.input.keyPresses.left() ? 1.0f : 0.0f) - (minecraftClient.player.input.keyPresses.right() ? 1.0f : 0.0f)) == 0.0f) ? false : true;
    }

    public boolean shouldStopSprint() {
        if (ClientServices.obstaclePasserHelper.isBaritoneLoaded() && ClientServices.obstaclePasserHelper.isPathing()) {
            return true;
        }
        Scaffold scaffold_lcl = getScaffold();
        ElytraFly efly_lcl = getEfly();
        return (scaffold_lcl != null && scaffold_lcl.isToggled() && scaffold_lcl.stopSprinting.getValue().booleanValue()) || (PlayerUtils.isInLiquid(minecraftClient.player) && this.stopInLiquids.getValue().booleanValue()) || (efly_lcl != null && efly_lcl.isToggled() && efly_lcl.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE && efly_lcl.hasElytra());
    }

    public void setSprinting(boolean z) {
        try {
            minecraftClient.player.setSprinting(z);
        } catch (Exception e) {
        }
    }

    public boolean isAboveWater() {
        if (minecraftClient.player.onGround()) {
            return false;
        }
        try {
            net.minecraft.world.phys.AABB box = minecraftClient.player.getBoundingBox().expandTowards(0.0d, -0.5d, 0.0d);
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosedStream(box).toList()) {
                if (minecraftClient.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WATER)) {
                    return true;
                }
                if (!minecraftClient.level.getFluidState(pos).isEmpty()
                        && minecraftClient.level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}