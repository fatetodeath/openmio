package me.mioclient.module.client.hud;

import me.mioclient.api.ClientServices;

import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.ValueAnimator;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.api.ClientAccessor;

import java.awt.Color;
import me.mioclient.module.client.HUD;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class HudEdgeDragHandler implements ClientAccessor {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving hud null. */
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
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public boolean flag4;
    public final ValueAnimator zoomHelper = new ValueAnimator();
    public final ValueAnimator zoomHelper2 = new ValueAnimator();

    public void do1010(GuiGraphicsExtractor drawContext, int i, int i2, float f) {
        HUD hud_lcl = getHud();
        if (hud_lcl == null) {
            return;
        }
        int scaledWindowWidth = drawContext.guiWidth();
        int scaledWindowHeight = drawContext.guiHeight();
        boolean z = isNearHorizontalEdge((double) i) || this.flag3;
        boolean z2 = isNearVerticalEdge((double) i2) || this.flag4;
        this.zoomHelper.animateTo(z ? 140.0f : 0.0f, 250L);
        this.zoomHelper2.animateTo((z2 && this.zoomHelper.getValue() == 0.0f) ? 140.0f : 0.0f, 250L);
        if (this.zoomHelper.getValue() > 0.0f) {
            if (this.flag3) {
                hud_lcl.setSafeAreaX(this.flag ? Math.abs(i - scaledWindowWidth) : i);
            }
            int i3 = ColorUtils.toIntWithAlpha(Color.black, (int) this.zoomHelper.getValue());
            drawContext.fill(0, 0, hud_lcl.getSafeAreaX(), scaledWindowHeight, i3);
            drawContext.fill(scaledWindowWidth - hud_lcl.getSafeAreaX(), 0, scaledWindowWidth, scaledWindowHeight, i3);
            return;
        }
        if (this.zoomHelper2.getValue() > 0.0f) {
            if (this.flag4) {
                hud_lcl.setSafeAreaY(this.flag2 ? Math.abs(i2 - scaledWindowHeight) : i2);
            }
            int i4 = ColorUtils.toIntWithAlpha(Color.black, (int) this.zoomHelper2.getValue());
            drawContext.fill(0, 0, scaledWindowWidth, hud_lcl.getSafeAreaY(), i4);
            drawContext.fill(0, scaledWindowHeight - hud_lcl.getSafeAreaY(), scaledWindowWidth, scaledWindowHeight, i4);
        }
    }

    public void mouseClicked(double d, double d2, int i) {
        if (i != 0) {
            return;
        }
        this.flag3 = isNearHorizontalEdge(d);
        if (!this.flag3) {
            this.flag4 = isNearVerticalEdge(d2);
        }
        this.flag = d > ((double) (((float) minecraftClient.getWindow().getGuiScaledWidth()) / 2.0f));
        this.flag2 = d2 > ((double) (((float) minecraftClient.getWindow().getGuiScaledHeight()) / 2.0f));
    }

    public void mouseReleased(double d, double d2, int i) {
        if (i != 0) {
            return;
        }
        this.flag3 = false;
        this.flag4 = false;
    }

    public boolean isNearHorizontalEdge(double d) {
        HUD hud_lcl = getHud();
        if (hud_lcl == null) {
            return false;
        }
        return d < ((double) (hud_lcl.getSafeAreaX() + 1)) || d > ((double) ((minecraftClient.getWindow().getGuiScaledWidth() - hud_lcl.getSafeAreaX()) - 1));
    }

    public boolean isNearVerticalEdge(double d) {
        HUD hud_lcl = getHud();
        if (hud_lcl == null) {
            return false;
        }
        return d < ((double) (hud_lcl.getSafeAreaY() + 1)) || d > ((double) ((minecraftClient.getWindow().getGuiScaledHeight() - hud_lcl.getSafeAreaY()) - 1));
    }
}