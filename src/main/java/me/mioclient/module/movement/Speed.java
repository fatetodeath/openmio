package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.player.MoveSpeedEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.registry.ModeValueMap;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.movement.speed.GrimSpeedHandler;
import me.mioclient.module.movement.speed.OnGroundSpeedHandler;
import me.mioclient.module.movement.speed.SpeedModeHandler;
import me.mioclient.module.movement.speed.StrafeSpeedHandler;
import me.mioclient.module.movement.speed.StrafeStrictHandler;
import me.mioclient.module.movement.speed.VanillaSpeedHandler;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.ExplosionVelocityEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.SprintUpdateEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Speed extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving obstaclePasser null (NPE). */
    private static ObstaclePasser obstaclePasser;

    public static ObstaclePasser getObstaclePasser() {
        if (obstaclePasser == null && ClientServices.moduleInjector != null) {
            try {
                obstaclePasser = (ObstaclePasser) ClientServices.moduleInjector.getModule(ObstaclePasser.class);
            } catch (Exception e) {
            }
        }
        return obstaclePasser;
    }
    public Setting<SpeedPredicateMode> mode = add(new EnumSetting<>("Mode", SpeedPredicateMode.speedPredicateMode));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.0f, 10.0f, v -> (this.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode2 || this.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode5) ? false : true));
    public Setting<Boolean> boost = add(new BooleanSetting("Boost", false, v -> this.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode2 || this.mode.getValue() == Speed.SpeedPredicateMode.speedPredicateMode));
    public Setting<Boolean> inLiquid = add(new BooleanSetting("InLiquid", false, v -> this.mode.getValue() != Speed.SpeedPredicateMode.speedPredicateMode5));
    public Setting<Boolean> useTimer = add(new BooleanSetting("UseTimer", true, v -> this.mode.getValue() != Speed.SpeedPredicateMode.speedPredicateMode5));
    public Setting<Boolean> pauseInBlocks = add(new BooleanSetting("PauseInBlocks", true, v -> this.mode.getValue() != Speed.SpeedPredicateMode.speedPredicateMode5));
    public boolean flag;
    public final Stopwatch stopwatch;
    public final ModeValueMap<SpeedPredicateMode, SpeedModeHandler> elytraFlyHelper_4;

    
    class Inner extends SpeedModeHandler {
        public Inner(Speed speed, Speed speed2) {
            super(speed2);
        }

        @Override // me.mioclient.module.movement.speed.SpeedModeHandler
        public void applyMovement(MoveEvent moveEvent) {
        }
    }

    

    public static enum SpeedPredicateMode implements NamedValue {
        speedPredicateMode {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "Strafe";
            }
        },
        speedPredicateMode2 {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "StrafeStrict";
            }
        },
        speedPredicateMode3 {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "Vanilla";
            }
        },
        speedPredicateMode4 {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "OnGround";
            }
        },
        speedPredicateMode5 {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "Grim";
            }
        },
        speedPredicateMode6 {
            @Override // me.mioclient.api.NamedValue
            public String getName() {
                return "None";
            }
        };

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return null;
        }
    }

    public Speed() {
        super("Speed", "Makes you move faster.", Category.MOVEMENT, new String[0]);
        this.stopwatch = new Stopwatch();
        this.elytraFlyHelper_4 = new ModeValueMap<>(this.mode);
        ModeValueMap elytraFlyHelper_4 = this.elytraFlyHelper_4;
        elytraFlyHelper_4.put(SpeedPredicateMode.speedPredicateMode, new StrafeSpeedHandler(this));
        ModeValueMap elytraFlyHelper_42 = this.elytraFlyHelper_4;
        elytraFlyHelper_42.put(SpeedPredicateMode.speedPredicateMode2, new StrafeStrictHandler(this));
        ModeValueMap elytraFlyHelper_43 = this.elytraFlyHelper_4;
        elytraFlyHelper_43.put(SpeedPredicateMode.speedPredicateMode3, new VanillaSpeedHandler(this));
        ModeValueMap elytraFlyHelper_44 = this.elytraFlyHelper_4;
        elytraFlyHelper_44.put(SpeedPredicateMode.speedPredicateMode4, new OnGroundSpeedHandler(this));
        ModeValueMap elytraFlyHelper_45 = this.elytraFlyHelper_4;
        elytraFlyHelper_45.put(SpeedPredicateMode.speedPredicateMode5, new GrimSpeedHandler(this));
        ModeValueMap elytraFlyHelper_46 = this.elytraFlyHelper_4;
        elytraFlyHelper_46.put(SpeedPredicateMode.speedPredicateMode6, new Inner(this, this));
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return this.mode.getValue().getName();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.elytraFlyHelper_4.getCurrent().onEnable();
    }

    @Listen
    public void onSprintUpdate(SprintUpdateEvent sprintUpdateEvent) {
        if (this.mode.getValue() != SpeedPredicateMode.speedPredicateMode2 || shouldPause()) {
            return;
        }
        minecraftClient.player.setSprinting(true);
    }

    @Listen
    public void onRotation(RotationEvent event_3) {
        if (event_3.isCurrent()) {
            return;
        }
        this.elytraFlyHelper_4.getCurrent().onSpeedJump();
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        if (!ClientServices.holeSnapRenderHelper.getTeleportStopwatch().hasElapsedMs(10L) || this.flag) {
            return;
        }
        this.elytraFlyHelper_4.getCurrent().applyMovement(moveEvent);
    }

    @Listen
    public void onMoveSpeed(MoveSpeedEvent moveEvent_2) {
        if (moveEvent_2.getPhase() == KeyPearlMode.Post && minecraftClient.player.onGround() && moveEvent_2.getSpeed() > 0.6d) {
            this.flag = true;
        }
    }

    @Listen(get219= RenderConstants.num2)
    public void onMotion(MotionEvent motionEvent) {
        this.flag = false;
        if (ClientServices.holeSnapRenderHelper.getTeleportStopwatch().hasElapsedMs(10L)) {
            this.elytraFlyHelper_4.getCurrent().applyAntiKickMotion(motionEvent);
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            reset();
        }
    }

    @Listen
    public void onExplosionVelocity(ExplosionVelocityEvent explosionVelocityEvent) {
        Vec3 vec3d = new Vec3(explosionVelocityEvent.getX(), explosionVelocityEvent.getY(), explosionVelocityEvent.getZ());
        double d = ExplosionDamageCalculator.getExplosionDamage(vec3d, minecraftClient.player, minecraftClient.player.getBoundingBox(), 6.0d, true, (BlockPos) null, (BlockPos) null);
        if (MovementUtils.is2181() && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L) && this.boost.getValue().booleanValue() && Math.sqrt(minecraftClient.player.distanceToSqr(vec3d)) <= 6.0d && d >= 4.0d) {
            this.stopwatch.reset();
        }
    }

    public void reset() {
        this.elytraFlyHelper_4.getCurrent().val3 = 0.0d;
        this.elytraFlyHelper_4.getCurrent().val2 = 0.0d;
        this.elytraFlyHelper_4.getCurrent().num = 4;
        if (this.mode.getValue() != SpeedPredicateMode.speedPredicateMode4) {
            return;
        }
        this.elytraFlyHelper_4.getCurrent().num = 2;
    }

    public boolean isInLiquidBlocked() {
        return PlayerUtils.isInLiquid(minecraftClient.player) && !this.inLiquid.getValue().booleanValue();
    }

    public boolean hasHeadCollision() {
        AABB boundingBox = minecraftClient.player.getBoundingBox();
        return !minecraftClient.level.noCollision(minecraftClient.player, new AABB((double) minecraftClient.player.blockPosition().getX(), boundingBox.minY, (double) minecraftClient.player.blockPosition().getZ(), ((double) minecraftClient.player.blockPosition().getX()) + 1.0d, boundingBox.maxY, ((double) minecraftClient.player.blockPosition().getZ()) + 1.0d).deflate(1.0E-7d)) && this.pauseInBlocks.getValue().booleanValue();
    }

    public boolean is2824() {
        ObstaclePasser obstaclePasser_lcl = getObstaclePasser();
        if (obstaclePasser_lcl == null) {
            return false;
        }
        return obstaclePasser_lcl.is929();
    }

    public boolean shouldPause() {
        return isNotInGame() || hasHeadCollision() || isInLiquidBlocked() || PlayerUtils.isHungry() || minecraftClient.player.isFallFlying() || minecraftClient.player.isSpectator();
    }
}