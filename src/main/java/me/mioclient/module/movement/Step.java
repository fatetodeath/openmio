package me.mioclient.module.movement;

import me.mioclient.event.events.player.MoveSpeedEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Step extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static HoleSnap holeSnap;
    private static AntiCheat antiCheat;

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
    public Setting<StepMode> mode = add(new EnumSetting<>("Mode", StepMode.VANILLA));
    public Setting<Float> height = add(new NumberSetting<>("Height", 2.0f, 1.0f, 4.0f).setSuffix("m"));
    public Setting<Boolean> safeDisable = add(new BooleanSetting("SafeDisable", false));
    public Setting<Boolean> useTimer = add(new BooleanSetting("UseTimer", true, v -> this.mode.getValue() == Step.StepMode.NORMAL).setVisible());
    public Setting<Float> timerSpeed = add(new NumberSetting<>("TimerSpeed", 0.3f, 0.01f, 1.0f, v -> this.mode.getValue() == Step.StepMode.NORMAL && this.useTimer.isShown()));
    public boolean flag;
    public int num;

    
    public enum StepMode implements NamedValue {
        VANILLA("Vanilla"),
        NORMAL("Normal");

        public final String name;

        StepMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Step() {
        super("Step", "Allows you to step up blocks without jumping.", Category.MOVEMENT, new String[0]);
        this.num = 0;
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        this.flag = false;
        this.num = 0;
        if (isNotInGame()) {
            return;
        }
        EntityUtil.setStepHeight(minecraftClient.player, 0.6f);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return ClientTextRenderer.capitalize(this.mode.getValue());
    }

    @Listen(get219= RenderConstants.num4)
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (this.num > 0) {
            if (ClientServices.holeSnapOverlayHelper.isHoleAt(PlayerUtils.getRoundedBlockPos()) && this.safeDisable.getValue().booleanValue()) {
                disable();
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.flag = false;
    }

    @Listen
    public void onMoveSpeed(MoveSpeedEvent moveEvent_2) {
        HoleSnap holeSnap_lcl = getHoleSnap();
        if (holeSnap_lcl != null && holeSnap_lcl.shouldPauseStep()) {
            return;
        }
        if (!minecraftClient.player.onGround() || minecraftClient.player.fallDistance > 0.0f || !minecraftClient.player.verticalCollision) {
            ClientServices.holeSnapHelper.releaseTickRate(this);
            return;
        }
        if (moveEvent_2.getPhase() == KeyPearlMode.Pre) {
            moveEvent_2.setSpeed(this.height.getValue().floatValue());
        }
        if (moveEvent_2.getPhase() == KeyPearlMode.Post) {
            if (moveEvent_2.getSpeed() <= 0.6d) {
                ClientServices.holeSnapHelper.releaseTickRate(this);
                return;
            }
            if (this.mode.getValue() == StepMode.VANILLA) {
                this.num++;
                return;
            }
            ArrayList arrayList = new ArrayList();
            if (this.useTimer.getValue().booleanValue() && !PlayerUtils.isInCobweb(minecraftClient.player)) {
                ClientServices.holeSnapHelper.requestTickRate(this, this.timerSpeed.getValue().floatValue());
                this.flag = true;
            }
            this.num++;
            if (moveEvent_2.getSpeed() <= 1.0f) {
                arrayList.addAll(List.of(Double.valueOf(0.42d * moveEvent_2.getSpeed()), Double.valueOf(0.75d * moveEvent_2.getSpeed())));
            } else if (moveEvent_2.getSpeed() <= 1.5d) {
                arrayList.addAll(List.of(Double.valueOf(0.42d), Double.valueOf(0.75d), Double.valueOf(1.0d), Double.valueOf(1.16d), Double.valueOf(1.23d), Double.valueOf(1.2d)));
            } else if (moveEvent_2.getSpeed() <= this.height.getValue().floatValue()) {
                arrayList.addAll(List.of(Double.valueOf(0.42d), Double.valueOf(0.78d), Double.valueOf(0.63d), Double.valueOf(0.51d), Double.valueOf(0.9d), Double.valueOf(1.21d), Double.valueOf(1.45d), Double.valueOf(1.43d)));
            }
            if (arrayList.isEmpty()) {
                return;
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                minecraftClient.player.connection.send(new ServerboundMovePlayerPacket.Pos(minecraftClient.player.getX(), minecraftClient.player.getY() + ((Double) it.next()).doubleValue(), minecraftClient.player.getZ(), minecraftClient.player.onGround(), minecraftClient.player.horizontalCollision));
            }
        }
    }

    public boolean isStepping() {
        return this.flag;
    }
}