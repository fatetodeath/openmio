package me.mioclient.render.hud;

import me.mioclient.api.ClientServices;
import me.mioclient.module.HudModule;

import me.mioclient.mode.client.ModuleListMode;

import me.mioclient.api.ClientAccessor;
import me.mioclient.gui.GuiComponent;

import me.mioclient.module.client.HUD;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

public class ModuleListRenderer implements ClientAccessor, GuiComponent {
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
    public final ArrayList<DraggableHudElement> arrayList = new ArrayList<>();
    public final ModuleListMode moduleListMode;

    public static final class ArrayList<T> extends java.util.ArrayList<T> {
        public T getLast() {
            return get(size() - 1);
        }
    }

    public ModuleListRenderer(ModuleListMode moduleListMode) {
        this.moduleListMode = moduleListMode;
    }

    @Override
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        for (int i = 0; i < this.arrayList.size(); i++) {
            DraggableHudElement moduleListSearchHelper4 = this.arrayList.get(i);
            moduleListSearchHelper4.updateVisibilityAnim();
            boolean z = moduleListSearchHelper4.isVisible() || moduleListSearchHelper4.get2142() > 0.0f;
            float[] floatArray365 = moduleListSearchHelper4.getSize();
            if (!z) {
                floatArray365 = new float[]{0.0f, 0.0f};
            }
            moduleListSearchHelper4.setMeasuredSize(floatArray365);
            if (this.moduleListMode != ModuleListMode.NONE) {
                moduleListSearchHelper4.setRelativeX(getAnchorX(floatArray365[0], this.moduleListMode), true);
                if (i == 0) {
                    moduleListSearchHelper4.setRelativeY(getAnchorY(floatArray365[1], this.moduleListMode), true);
                } else if (this.moduleListMode == ModuleListMode.BOTTOM_LEFT || this.moduleListMode == ModuleListMode.BOTTOM_RIGHT) {
                    moduleListSearchHelper4.setRelativeY(this.arrayList.get(i - 1).getY() - floatArray365[1], true);
                }
            }
            if (z) {
                moduleListSearchHelper4.render(drawContext, matrixStack, d, d2);
            }
            if (i < this.arrayList.size() - 1 && this.moduleListMode != ModuleListMode.NONE && getVerticalDirection(this.moduleListMode) == 1) {
                this.arrayList.get(i + 1).setRelativeY(moduleListSearchHelper4.getY() + floatArray365[1], true);
            }
        }
    }

    public ModuleListMode getAnchor() {
        return this.moduleListMode;
    }

    public ArrayList<DraggableHudElement> getElements() {
        return this.arrayList;
    }

    public int getVerticalDirection(ModuleListMode moduleListMode) {
        switch (moduleListMode) {
            case TOP_LEFT:
            case TOP_RIGHT:
            case TOP_CENTER:
            case NONE:
                return 1;
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return -1;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public float getAnchorX(float f, ModuleListMode moduleListMode) {
        HUD hud_lcl = getHud();
        switch (moduleListMode) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                return hud_lcl != null ? hud_lcl.getSafeAreaX() : 0.0f;
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return (minecraftClient.getWindow().getGuiScaledWidth() - f) - (hud_lcl != null ? hud_lcl.getSafeAreaX() : 0.0f);
            case TOP_CENTER:
                return (minecraftClient.getWindow().getGuiScaledWidth() / 2.0f) - (f / 2.0f);
            case NONE:
                throw new IllegalArgumentException("dumb");
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public float getAnchorY(float f, ModuleListMode moduleListMode) {
        HUD hud_lcl = getHud();
        switch (moduleListMode) {
            case TOP_LEFT:
            case TOP_CENTER:
                return hud_lcl != null ? hud_lcl.getSafeAreaY() : 0.0f;
            case TOP_RIGHT:
                return (hud_lcl != null ? hud_lcl.getStatusEffectOffset() + hud_lcl.getSafeAreaY() : 0.0f);
            case NONE:
                throw new IllegalArgumentException("dumb");
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return (minecraftClient.getWindow().getGuiScaledHeight() - f) - getBottomPadding();
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public float getBottomPadding() {
        HUD hud_lcl = getHud();
        if (hud_lcl == null) {
            return 0.0f;
        }
        return Mth.clamp(hud_lcl.getSafeAreaY() + hud_lcl.getBottomOffsetTarget(), 0.0f, 14.0f);
    }
}
