package me.mioclient.render.shape;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.MathConstants;
import me.mioclient.util.math.MathUtils;

import java.awt.Color;
import java.util.Objects;
import me.mioclient.module.client.UI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class SnowflakeRenderer {
    public final float val2 = (float) Mth.clamp(Math.random(), 0.07000000029802322d, 0.18000000715255737d);
    public final float val3 = MathUtils.randomFloat((float) MathConstants.val2, 1.15f);
    public final double val4 = Math.random();
    public final double val5 = MathUtils.randomFloat((-0.033f), 0.033f);
    public float val6 = MathUtils.randomFloat((-0.45f), 0.0f);
    public static final float val = 0.033f;
    public static final String string = "❆";

    public void renderContent(GuiGraphicsExtractor drawContext) {
        Font textRenderer = Minecraft.getInstance().font;
        float scaledWindowWidth = (float) ((this.val4 + (this.val6 * this.val5)) * drawContext.guiWidth());
        float scaledWindowHeight = this.val6 * drawContext.guiHeight() * 0.5f;
        Objects.requireNonNull(textRenderer);
        float intBitsToFloat = (scaledWindowHeight - 9.0f) - 1.0f;
        this.val6 += ClientServices.hitmarkerHelper.getFrameDelta(0.5f) * this.val2;
        this.val6 = Math.min(this.val6, 1.0f);
        if (!UI.uI.snow.getValue().booleanValue() || this.val6 < 0.0f) {
            return;
        }
        int ceil = Mth.ceil(Math.max(255.0f - (this.val6 * 255.0f), 4.0f));
        drawContext.pose().pushMatrix();
        drawContext.pose().scale(this.val3, this.val3);
        drawContext.pose().translate(scaledWindowWidth / this.val3, intBitsToFloat / this.val3);
        drawContext.centeredText(textRenderer, "❆", 0, 0, new Color(255, 255, 255, ceil).hashCode());
        drawContext.pose().popMatrix();
    }

    public double getX() {
        return this.val4;
    }

    public boolean hasLanded() {
        return this.val6 >= 1.0f;
    }
}
