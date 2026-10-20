package me.mioclient.module.client;

import me.mioclient.event.events.client.ClientTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.module.KeybindModule;

import me.mioclient.util.math.ColorUtils;

import me.mioclient.event.EventBus;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import java.awt.Color;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.movement.Jesus;
import me.mioclient.module.movement.SafeWalk;
import me.mioclient.module.movement.Step;
import me.mioclient.module.player.AutoTool;

public class Baritone extends KeybindModule {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving module refs null. */
    private static AntiCheat antiCheat;
    private static Step step;
    private static AutoTool autoTool;
    private static SafeWalk safeWalk;
    private static Jesus jesus;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }

    public static Step getStep() {
        if (step == null && ClientServices.moduleInjector != null) {
            try {
                step = (Step) ClientServices.moduleInjector.getModule(Step.class);
            } catch (Exception e) {
            }
        }
        return step;
    }

    public static AutoTool getAutoTool() {
        if (autoTool == null && ClientServices.moduleInjector != null) {
            try {
                autoTool = (AutoTool) ClientServices.moduleInjector.getModule(AutoTool.class);
            } catch (Exception e) {
            }
        }
        return autoTool;
    }

    public static SafeWalk getSafeWalk() {
        if (safeWalk == null && ClientServices.moduleInjector != null) {
            try {
                safeWalk = (SafeWalk) ClientServices.moduleInjector.getModule(SafeWalk.class);
            } catch (Exception e) {
            }
        }
        return safeWalk;
    }

    public static Jesus getJesus() {
        if (jesus == null && ClientServices.moduleInjector != null) {
            try {
                jesus = (Jesus) ClientServices.moduleInjector.getModule(Jesus.class);
            } catch (Exception e) {
            }
        }
        return jesus;
    }
    public Setting<Boolean> setting;
    public Setting<Boolean> setting2;
    public Setting<Boolean> setting3;
    public Setting<Boolean> setting4;
    public Setting<Boolean> setting5;
    public Setting<Color> setting6;
    public Setting<Color> setting7;
    public boolean flag;
    public boolean flag2;

    public Baritone() {
        super("Baritone", "Manages baritone settings.", Category.CLIENT, new String[0]);
        BaritoneAPI.getSettings().chatControl.value = false;
        eventBus.register(this);
    }

    @Listen
    public void onClientTick(ClientTickEvent fontsEvent) {
        if (this.setting == null || this.setting2 == null || this.setting3 == null || this.setting4 == null || this.setting5 == null || this.setting6 == null || this.setting7 == null) {
            return;
        }
        syncAntiCheatCompat();
        BaritoneAPI.getSettings().disconnectOnArrival.value = this.setting2.getValue();
        BaritoneAPI.getSettings().freeLook.value = this.setting.getValue();
        BaritoneAPI.getSettings().blockFreeLook.value = this.setting.getValue();
        BaritoneAPI.getSettings().elytraFreeLook.value = this.setting.getValue();
        BaritoneAPI.getSettings().censorCoordinates.value = this.setting4.getValue();
        BaritoneAPI.getSettings().censorRanCommands.value = this.setting4.getValue();
        if (this.setting5.getValue().booleanValue()) {
            Color color816 = ColorUtils.withAlpha(this.setting6.getValue(), 255);
            Color color8162 = ColorUtils.withAlpha(this.setting7.getValue(), 255);
            BaritoneAPI.getSettings().colorBestPathSoFar.value = color816;
            BaritoneAPI.getSettings().colorGoalBox.value = color8162;
            BaritoneAPI.getSettings().colorInvertedGoalBox.value = color8162;
            BaritoneAPI.getSettings().colorCurrentPath.value = color816;
            BaritoneAPI.getSettings().colorMostRecentConsidered.value = color816;
            BaritoneAPI.getSettings().colorBlocksToBreak.value = color816;
            BaritoneAPI.getSettings().colorBlocksToPlace.value = color816;
            BaritoneAPI.getSettings().colorBlocksToWalkInto.value = color816;
            BaritoneAPI.getSettings().colorNextPath.value = color816;
            BaritoneAPI.getSettings().pathRenderLineWidthPixels.value = Float.valueOf(1.5f);
            BaritoneAPI.getSettings().goalRenderLineWidthPixels.value = Float.valueOf(1.5f);
        }
        if (this.setting3.getValue().booleanValue()) {
            Step step_lcl = getStep();
            AutoTool autoTool_lcl = getAutoTool();
            SafeWalk safeWalk_lcl = getSafeWalk();
            Jesus jesus_lcl = getJesus();
            BaritoneAPI.getSettings().assumeStep.value = Boolean.valueOf(step_lcl != null && step_lcl.isToggled());
            BaritoneAPI.getSettings().assumeExternalAutoTool.value = Boolean.valueOf(autoTool_lcl != null && autoTool_lcl.isToggled());
            BaritoneAPI.getSettings().assumeSafeWalk.value = Boolean.valueOf(safeWalk_lcl != null && safeWalk_lcl.isToggled());
            BaritoneAPI.getSettings().assumeWalkOnWater.value = Boolean.valueOf(jesus_lcl != null && jesus_lcl.isToggled() && jesus_lcl.canJesus());
        }
    }

    public void syncAntiCheatCompat() {
        Settings.Setting setting = BaritoneAPI.getSettings().antiCheatCompatibility;
        AntiCheat antiCheat_lcl = getAntiCheat();
        boolean isYawSpoofActive = antiCheat_lcl != null && antiCheat_lcl.getYawSpoofHandler().isYawSpoofActive();
        if (isYawSpoofActive != this.flag2) {
            if (isYawSpoofActive) {
                this.flag = ((Boolean) setting.value).booleanValue();
            } else {
                setting.value = Boolean.valueOf(this.flag);
            }
        } else if (isYawSpoofActive) {
            setting.value = false;
        }
        this.flag2 = isYawSpoofActive;
    }
}