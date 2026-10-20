package me.mioclient.module.render;

import me.mioclient.util.anim.AnimationProgress;

import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class ViewClip extends Module {
    public Setting<Double> range = add(new NumberSetting<>("Range", 4.5, 0.1, 30.0));
    public Setting<Boolean> smooth = add(new BooleanSetting("Smooth", true));
    public final AnimationProgress progress;

    public ViewClip() {
        super("ViewClip", "Allows you to clip into blocks using 3rd person camera.", Category.RENDER, new String[0]);
        this.progress = new AnimationProgress(1.3f, true);
        setDrawn(false);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (!minecraftClient.options.getCameraType().isFirstPerson()) {
            this.progress.animateToggle(true);
            return;
        }
        this.progress.setImmediate(1.0f);
    }
}