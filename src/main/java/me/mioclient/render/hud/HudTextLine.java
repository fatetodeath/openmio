package me.mioclient.render.hud;

import me.mioclient.api.ClientServices;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.math.ColorUtils;

import java.awt.Color;
import java.util.function.Supplier;
import me.mioclient.module.client.HUD;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HudTextLine {
    /* Resolved lazily — eager static init ran before the module registry was ready. */
    private static HUD hud;

    public static HUD getHud() {
        if (hud == null && ClientServices.moduleInjector != null) {
            try {
                hud = (HUD) ClientServices.moduleInjector.getModule(HUD.class);
            } catch (Exception e) {
            }
        }
        return hud;
    }
    public final Supplier<Component> supplier;
    public final Supplier<Boolean> supplier2;
    public float val;
    public final InterpolatedFloat hUDHelper_2 = new InterpolatedFloat(2.0f, true);

    public HudTextLine(Supplier<Component> supplier, Supplier<Boolean> supplier2) {
        this.supplier = supplier;
        this.supplier2 = supplier2;
    }

    public void renderLine(GuiGraphicsExtractor drawContext, float f, float f2, Color color) {
        float f3 = ClientTextRenderer.INSTANCE.getStringWidth(getAsText().getString());
        HUD hud_lcl = getHud();
        if (hud_lcl != null && hud_lcl.setting3.getValue().booleanValue()) {
            this.hUDHelper_2.animate(f3);
        } else {
            this.hUDHelper_2.setImmediate(f3);
        }
        if (this.val > 0.0f) {
            ClientTextRenderer.INSTANCE.drawTextShadowObj(drawContext, getAsText(), f, f2, ColorUtils.withAlpha(color, (int) (color.getAlpha() * Mth.clamp(this.val * 2.0f, 0.1d, 1.0d))));
        }
    }

    public void recalculateLayout() {
        float intBitsToFloat = 1.0f / (ClientServices.hitmarkerHelper.getFrameRate() >> 2);
        if (isDrawn()) {
            this.val += intBitsToFloat;
        } else {
            this.val -= intBitsToFloat;
        }
        this.val = Mth.clamp(this.val, 0.0f, 1.0f);
    }

    public float getAlpha() {
        return this.val;
    }

    public boolean isDrawn() {
        return this.supplier2.get().booleanValue();
    }

    public Component getAsText() {
        return this.supplier.get();
    }
}
