package me.mioclient.module.movement;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.network.PacketSender;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.SprintUpdateEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class FastWeb extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving antiCheat null (NPE). */
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
    public Setting<FastWebPredicateMode> mode = add(new EnumSetting<>("Mode", FastWebPredicateMode.PLAIN));
    public Setting<Boolean> horizontal = add(new BooleanSetting("Horizontal", false, v -> this.mode.getValue() != FastWeb.FastWebPredicateMode.GRIM).setVisible());
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 1.0f, 2.0f));
    public Setting<Boolean> vertical = add(new BooleanSetting("Vertical", false, v -> this.mode.getValue() != FastWeb.FastWebPredicateMode.GRIM).setVisible());
    public Setting<Float> vSpeed = add(new NumberSetting<>("VSpeed", 1.0f, 1.0f, 2.0f));
    public Setting<Boolean> upwards = add(new BooleanSetting("Upwards", false));
    public boolean flag;

    
    public enum FastWebPredicateMode implements NamedValue {
        PLAIN("Plain"),
        STRICT("Strict"),
        LAGBACK("LagBack"),
        GRIM("Grim");

        public final String name;

        FastWebPredicateMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public FastWeb() {
        super("FastWeb", "Makes you faster in cobwebs.", Category.MOVEMENT, new String[0]);
        this.flag = false;
        setDrawn(false);
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() != KeyPearlMode.Pre) {
            return;
        }
        Vec3 vec3d = new Vec3(minecraftClient.player.getX() - minecraftClient.player.xo, minecraftClient.player.getY() - minecraftClient.player.yo, minecraftClient.player.getZ() - minecraftClient.player.zo);
        if (isGrimMode() && vec3d.lengthSqr() > 0.0d) {
            BlockPos.betweenClosedStream(minecraftClient.player.getBoundingBox()).map((v0) -> {
                return v0.immutable();
            }).filter(blockPos -> {
                return minecraftClient.level.getBlockState(blockPos).is(Blocks.COBWEB);
            }).forEach(blockPos2 -> {
                AntiCheat antiCheat_lcl = getAntiCheat();
                if (antiCheat_lcl == null || !antiCheat_lcl.isF2b2t()) {
                    PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, blockPos2, Direction.UP);
                }
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos2, Direction.UP);
            });
        }
        if (this.mode.getValue() == FastWebPredicateMode.GRIM && minecraftClient.player.input.keyPresses.shift() && !minecraftClient.player.onGround() && PlayerUtils.isInCobweb(minecraftClient.player)) {
            motionEvent.setY(minecraftClient.player.getY() + Math.random() + 1.0d);
        }
    }

    @Listen(get219= RenderConstants.num4)
    public void onSprintUpdate(SprintUpdateEvent sprintUpdateEvent) {
        if (isGrimMode() && !minecraftClient.player.onGround() && minecraftClient.player.input.keyPresses.jump() && PlayerUtils.isInCobweb(minecraftClient.player)) {
            minecraftClient.player.setSprinting(false);
        }
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        if (!ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L) || !PlayerUtils.isInCobweb(minecraftClient.player) || this.mode.getValue() == FastWebPredicateMode.GRIM) {
            reset();
            return;
        }
        if (this.vertical.getValue().booleanValue()) {
            if (!minecraftClient.player.input.keyPresses.shift() || minecraftClient.player.onGround()) {
                reset();
            } else if (isStrictMode()) {
                this.flag = true;
                ClientServices.inner.requestTickRate(this, this.vSpeed.getValue().floatValue() * 6.0f);
            } else {
                reset();
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, -this.vSpeed.getValue().floatValue()));
                moveEvent.setY(-this.vSpeed.getValue().floatValue());
            }
            if (this.upwards.getValue().booleanValue() && minecraftClient.player.input.keyPresses.jump() && !isStrictMode()) {
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, this.vSpeed.getValue().floatValue()));
                moveEvent.setY(this.vSpeed.getValue().floatValue());
            }
        }
        if (MovementUtils.is2181() && this.horizontal.getValue().booleanValue()) {
            double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, MovementUtils.getBaseMoveSpeed(true) * this.speed.getValue().floatValue());
            minecraftClient.player.setDeltaMovement(doubleArray2507[0], minecraftClient.player.getDeltaMovement().y, doubleArray2507[1]);
        }
    }

    public boolean isStrictMode() {
        return this.mode.getValue() == FastWebPredicateMode.STRICT;
    }

    public boolean isTickRateActive() {
        return this.flag;
    }

    public void reset() {
        ClientServices.inner.releaseTickRate(this);
        this.flag = false;
    }

    public boolean isGrimMode() {
        return isToggled() && this.mode.getValue() == FastWebPredicateMode.GRIM;
    }
}