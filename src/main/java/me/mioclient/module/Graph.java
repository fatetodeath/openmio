package me.mioclient.module;

import me.mioclient.api.ClientServices;

import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.util.math.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import me.mioclient.setting.NumberSetting;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class Graph extends me.mioclient.module.HudModule {
    public Setting<Float> setting;
    public Setting<Integer> setting2;
    public Setting<Integer> setting3;
    public final List<Double> list;

    public Graph() {
        super("Graph", new String[0]);
        this.setting = add(new NumberSetting("Ceil", Float.valueOf(26.0f), Float.valueOf(10.0f), Float.valueOf(100.0f)));
        this.setting2 = add(new NumberSetting("Height", 30, 10, 40));
        this.setting3 = add(new NumberSetting("Width", 100, 50, 150));
        this.list = new ArrayList();
        setElement(new ResizableHudElement(this));
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.list.clear();
        for (int i = 0; i < 150; i++) {
            this.list.add(Double.valueOf(0.0d));
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.list.add(Double.valueOf(ClientServices.feetPlaceHelper.getSmoothedSpeed()));
        while (this.list.size() > this.setting3.getValue().intValue()) {
            this.list.remove(0);
        }
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        // 26.2: immediate Tessellator/BufferRenderer removed (GpuBackend).
        // Real behavior: draw speed line-strip as 1px fill segments in GUI space.
        int w = this.setting3.getValue().intValue();
        int h = this.setting2.getValue().intValue();
        float ceil = this.setting.getValue().floatValue();
        if (ceil <= 0.0f) {
            ceil = 1.0f;
        }
        int n = Math.min(this.list.size(), w);
        for (int i = 1; i < n; i++) {
            double v0 = Math.min(this.list.get(i - 1).doubleValue(), (double) ceil);
            double v1 = Math.min(this.list.get(i).doubleValue(), (double) ceil);
            int y0 = (int) (h - (v0 / ceil) * h);
            int y1 = (int) (h - (v1 / ceil) * h);
            y0 = (int) Mth.clamp(y0, 0, h - 1);
            y1 = (int) Mth.clamp(y1, 0, h - 1);
            int alpha = Math.min(get2579(i - 1), get2579(i));
            int col = ((alpha & 255) << 24) | 0x00FF00;
            int top = Math.min(y0, y1);
            int bottom = Math.max(y0, y1);
            if (bottom == top) {
                bottom = top + 1;
            } else {
                bottom = bottom + 1;
            }
            drawContext.fill(i - 1, top, i, bottom, col);
        }
    }

    public int get2579(int i) {
        if (i <= 10) {
            return (int) Mth.clamp((i / 10.0f) * 255.0f, 0.0f, 255.0f);
        }
        if (i >= this.setting3.getValue().intValue() - 10) {
            return (int) Mth.clamp((1.0f - (((i - this.setting3.getValue().intValue()) + 10) / 10.0f)) * 255.0f, 0.0f, 255.0f);
        }
        return 255;
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{this.setting3.getValue().intValue(), this.setting2.getValue().intValue()};
    }
}