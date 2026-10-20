package me.mioclient.render.hud;

import me.mioclient.api.ClientServices;
import me.mioclient.module.HudModule;

import me.mioclient.gui.screen.HudEditorScreen;
import me.mioclient.mode.client.ModuleListMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.ValueAnimator;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.Rectangle;

import me.mioclient.api.ClientAccessor;
import me.mioclient.gui.GuiComponent;

import java.awt.Color;
import me.mioclient.module.client.Fonts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

public abstract class DraggableHudElement implements ClientAccessor, GuiComponent {
    public static final Color color = new Color(0, 0, 0, 120);
    public static boolean flag = false;
    public final HudModule moduleList;
    public final ValueAnimator zoomHelper = new ValueAnimator();
    public final ValueAnimator zoomHelper2 = new ValueAnimator();
    public ModuleListMode moduleListMode = ModuleListMode.NONE;
    public float val = 0.5f;
    public float val2 = 0.5f;
    public int num;
    public int num2;
    public float[] floatArr;
    public boolean flag2;
    public long num3;

    public DraggableHudElement(HudModule moduleList) {
        this.moduleList = moduleList;
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        float[] floatArray2950 = getMeasuredSize();
        if (!containsPoint(d, d2) || this.flag2) {
            this.num3 = -1L;
        } else {
            if (this.num3 == -1) {
                this.num3 = System.currentTimeMillis();
            }
            if (this.num3 + 300 <= System.currentTimeMillis()) {
                HudEditorScreen.helper_22.initialize(this.moduleList.getName(), getHorizontalSide() == 1 ? getX() + floatArray2950[0] + 2.0f : (getX() - 2.0f) - ClientTextRenderer.INSTANCE.getStringWidth(this.moduleList.getName()), getY(), getHorizontalSide());
            }
        }
        if (flag) {
            return;
        }
        float f = getX();
        float f2 = getY();
        Shape2DRenderer.drawRect(matrixStack, f, f2, f + Math.max(floatArray2950[0], 4.0f), f2 + Math.max(floatArray2950[1], 4.0f), ColorUtils.toIntWithAlphaPercent(color, (ClientServices.getHud().getAlpha() * color.getAlpha()) / 255.0f));
        float f3 = this.zoomHelper.getValue();
        if (f3 > 0.0f) {
            Shape2DRenderer.drawOutline(matrixStack, f - 1.0f, f2 - 1.0f, f + Math.max(floatArray2950[0], 4.0f), f2 + Math.max(floatArray2950[1], 4.0f), ColorUtils.withAlpha(ColorUtils.getThemeColor(), (int) (f3 * 255.0f)));
        }
    }

    @Override
    public boolean containsPoint(double d, double d2) {
        float[] floatArray2950 = getMeasuredSize();
        return d >= ((double) getX()) && d <= ((double) (getX() + Math.max(floatArray2950[0], 4.0f))) && d2 >= ((double) getY()) && d2 <= ((double) (getY() + Math.max(floatArray2950[1], 4.0f)));
    }

    public void updateVisibilityAnim() {
        this.zoomHelper2.animateTo(isVisible() ? 1.0f : 0.0f, 250L);
    }

    public abstract float[] getSize();

    public float getX() {
        return Math.min(this.val * minecraftClient.getWindow().getGuiScaledWidth(), minecraftClient.getWindow().getGuiScaledWidth() - getMeasuredSize()[0]);
    }

    public float getRelativeX() {
        return this.val;
    }

    public void setRelativeX(float f, boolean z) {
        if (z) {
            this.val = f / minecraftClient.getWindow().getGuiScaledWidth();
        } else {
            this.val = Mth.clamp(f, 0.0f, 1.0f);
        }
    }

    public float getY() {
        return Math.min(this.val2 * minecraftClient.getWindow().getGuiScaledHeight(), minecraftClient.getWindow().getGuiScaledHeight() - getMeasuredSize()[1]);
    }

    public float getRelativeY() {
        return this.val2;
    }

    public void setRelativeY(float f, boolean z) {
        if (z) {
            this.val2 = f / minecraftClient.getWindow().getGuiScaledHeight();
        } else {
            this.val2 = Mth.clamp(f, 0.0f, 1.0f);
        }
    }

    @Override
    public int getFontHeight() {
        int i = ClientTextRenderer.INSTANCE.getFontHeight();
        if (Fonts.fonts.isToggled()) {
            i--;
        }
        return i;
    }

    public void setDragging(boolean z) {
        if (this.flag2 == z) {
            return;
        }
        this.zoomHelper.animateTo(z ? 1.0f : 0.0f, 250L);
        this.flag2 = z;
    }

    public boolean isDragging() {
        return this.flag2;
    }

    public float[] getMeasuredSize() {
        return this.floatArr;
    }

    public void setMeasuredSize(float[] fArr) {
        this.floatArr = fArr;
    }

    public ModuleListMode getAnchor() {
        return this.moduleListMode;
    }

    public void setAnchor(ModuleListMode moduleListMode) {
        this.moduleListMode = moduleListMode;
    }

    public boolean isVisible() {
        return this.moduleList.isToggled();
    }

    public float getAlignedX(float f) {
        return this.moduleListMode == ModuleListMode.TOP_CENTER ? (getX() + (this.floatArr[0] / 2.0f)) - (f / 2.0f) : getHorizontalSide() == -1 ? (getX() + this.floatArr[0]) - f : getX();
    }

    public float getAlignedY(float f) {
        return getVerticalSide() == -1 ? (getY() + this.floatArr[1]) - f : getY();
    }

    public int getHorizontalSide() {
        if (this.num == 0) {
            computeSides();
        }
        return this.num;
    }

    public int getVerticalSide() {
        if (this.num2 == 0) {
            computeSides();
        }
        return this.num2;
    }

    public void computeSides() {
        this.num = getX() + (this.floatArr[0] / 2.0f) > ((float) minecraftClient.getWindow().getGuiScaledWidth()) / 2.0f ? -1 : 1;
        this.num2 = getY() + (this.floatArr[1] / 2.0f) > ((float) minecraftClient.getWindow().getGuiScaledHeight()) / 2.0f ? -1 : 1;
    }

    public Rectangle getData_32936() {
        return Rectangle.fromPositionAndSize(this.val * minecraftClient.getWindow().getGuiScaledWidth(), this.val2 * minecraftClient.getWindow().getGuiScaledHeight(), this.floatArr[0], this.floatArr[1]);
    }

    public HudModule getHudModule() {
        return this.moduleList;
    }

    public float get2142() {
        return this.zoomHelper2.getValue();
    }
}
