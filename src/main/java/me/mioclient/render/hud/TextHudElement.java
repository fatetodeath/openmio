package me.mioclient.render.hud;

import me.mioclient.module.HudModule;

import me.mioclient.render.font.ClientTextRenderer;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

public class TextHudElement extends DraggableHudElement {
    public final List<? extends HudTextLine> list;

    public TextHudElement(HudModule moduleList, HudTextLine cryptoHelper) {
        this(moduleList, (List<? extends HudTextLine>) Collections.singletonList(cryptoHelper));
    }

    public TextHudElement(HudModule moduleList, List<? extends HudTextLine> list) {
        super(moduleList);
        this.list = list;
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        super.render(drawContext, matrixStack, d, d2);
        float f = getAlignedY(getFontHeight());
        for (HudTextLine cryptoHelper : this.list) {
            matrixStack.pushPose();
            float clamp = (float) Mth.clamp(1.0d - Math.pow(1.0f - cryptoHelper.getAlpha(), 4.0d), 0.0d, 1.0d);
            Color color489 = getLineColor(f, cryptoHelper);
            float f2 = cryptoHelper.hUDHelper_2.getValue();
            matrixStack.translate(getAlignedX(f2) - ((f2 * (1.0f - clamp)) * getHorizontalSide()), f, 0.0f);
            cryptoHelper.renderLine(drawContext, 0.0f, 0.0f, color489);
            if (clamp != 0.0f) {
                f += ((getFontHeight() * clamp) + 1.0f) * getVerticalSide();
            }
            matrixStack.popPose();
        }
    }

    @Override
    public float[] getSize() {
        float f = 0.0f;
        float f2 = 0.0f;
        for (HudTextLine cryptoHelper : this.list) {
            cryptoHelper.recalculateLayout();
            if (cryptoHelper.getAlpha() != 0.0f) {
                float clamp = (float) Mth.clamp(1.0d - Math.pow(1.0f - cryptoHelper.getAlpha(), 4.0d), 0.0d, 1.0d);
                float f3 = ClientTextRenderer.INSTANCE.getStringWidth(cryptoHelper.getAsText().getString());
                f += (getFontHeight() * clamp) + 1.0f;
                if (f3 > f2) {
                    f2 = f3;
                }
            }
        }
        if (!flag && f2 == 0.0f) {
            f2 = 50.0f;
            f = getFontHeight();
        }
        return new float[]{f2, f};
    }

    public List<? extends HudTextLine> getLines() {
        return this.list;
    }

    public Color getLineColor(float f, HudTextLine cryptoHelper) {
        return this.moduleList.getHudColor(f);
    }
}
