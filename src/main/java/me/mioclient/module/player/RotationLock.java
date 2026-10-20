package me.mioclient.module.player;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.MathConstants;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import me.mioclient.module.render.FreeLook;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class RotationLock extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving freelook null. */
    private static FreeLook freelook;

    public static FreeLook getFreelook() {
        if (freelook == null && ClientServices.moduleInjector != null) {
            try {
                freelook = (FreeLook) ClientServices.moduleInjector.getModule(FreeLook.class);
            } catch (Exception e) {
            }
        }
        return freelook;
    }
    public Setting<Boolean> yaw = add(new BooleanSetting("Yaw", true).setVisible());
    public Setting<Boolean> custom = add(new BooleanSetting("Custom", false).requiresSettings(this.yaw));
    public Setting<Float> value2 = add(new NumberSetting<>("Value", 0.0f, -180.0f, 180.0f).requiresSettings(this.yaw, this.custom));
    public Setting<Boolean> pitch = add(new BooleanSetting("Pitch", false).setVisible());
    public Setting<Float> value = add(new NumberSetting<>("Value", 0.0f, -90.0f, 90.0f).requiresSettings(this.pitch));

    public RotationLock() {
        super("RotationLock", "Locks your rotation.", Category.PLAYER, new String[0]);
        this.value2.setConfigName("YawValue");
        this.value.setConfigName("PitchValue");
    }

    @Listen(get219= RenderConstants.num)
    public void onMotion(MotionEvent motionEvent) {
        FreeLook freelook_lcl = getFreelook();
        if (motionEvent.getPhase() == KeyPearlMode.Pre && freelook_lcl != null && freelook_lcl.isToggled()) {
            if (this.yaw.getValue().booleanValue()) {
                motionEvent.setYaw(getYaw());
            }
            if (this.pitch.getValue().booleanValue()) {
                motionEvent.setPitch(getPitch());
            }
        }
    }

    @Listen(get219= RenderConstants.num)
    public void onRotation(RotationEvent event_3) {
        FreeLook freelook_lcl = getFreelook();
        if (freelook_lcl != null && freelook_lcl.isToggled()) {
            if (this.yaw.getValue().booleanValue()) {
                event_3.setYaw(getYaw());
                event_3.cancel();
            }
            if (this.pitch.getValue().booleanValue()) {
                event_3.setPitch(getPitch());
                event_3.cancel();
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.yaw.getValue().booleanValue()) {
            minecraftClient.player.setYRot(getYaw());
        }
        if (this.pitch.getValue().booleanValue()) {
            minecraftClient.player.setXRot(getPitch());
        }
    }

    public float getYaw() {
        return !this.custom.getValue().booleanValue() ? Math.round((minecraftClient.player.getYRot() + 1.0f) / MathConstants.num) * MathConstants.num : this.value2.getValue().floatValue();
    }

    public float getPitch() {
        return this.value.getValue().floatValue();
    }
}