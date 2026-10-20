package me.mioclient.gui.panel;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.registry.CollectionRegistry;

import me.mioclient.gui.GuiComponent;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;

import me.mioclient.api.ClientAccessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import me.mioclient.module.client.UI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.manager.ScissorStackManager;

public class ClickGuiPanel implements NamedValue, GuiComponent, CollectionRegistry<GuiComponent, List<GuiComponent>> {
    public final String name;
    public int panelHeight = 14;
    public int dragOffsetX;
    public int totalHeight;
    public final ArrayList<GuiComponent> registry = new ArrayList<>();
    public AnimationProgress progress = new AnimationProgress((Supplier<Float>) () -> {
        return Float.valueOf(getUI().animSpeed.getValue().floatValue() * 2.0f);
    }, true);
    public final int panelWidth = 92;
    public int dragOffsetY = 0;
    public boolean flag = true;
    public int y = 5;
    public int x = 5;
    public boolean flag2 = false;

    public ClickGuiPanel(String str) {
        this.panelHeight = 14;
        this.panelHeight = 14;
        this.name = str;
        this.progress.setToggledImmediate(true);
    }

    @Override // me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        drawContext.fill(this.x, this.y, this.x + getTotalWidth(), this.y + 14, UI.uI.color.getValue().getRGB());
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, this.name, this.x + 2, (this.y + 7.0f) - (ClientTextRenderer.INSTANCE.getFontHeight() / 2.0f), Color.white);
        this.progress.animateToggle(this.flag);
        float f = this.progress.getValue();
        if (f != 1.0f && this.totalHeight != 0) {
            Shape2DRenderer.resetShadowOffset();
            ScissorStackManager.pushClipRegion(this.x - 1, this.y - 1, getTotalWidth() + 2, ((int) (14.0f + (this.totalHeight * f))) + 3);
        }
        if (f > 0.0f && !this.registry.isEmpty()) {
            drawContext.fill(this.x, this.y + 14, this.x + getTotalWidth(), (int) (this.y + this.totalHeight + 0.5f), UI.uI.bgColor.getValue().getRGB());
            this.registry.forEach(presetHelper_5 -> {
                if (shouldShowChild(presetHelper_5)) {
                    presetHelper_5.update(d, d2);
                    presetHelper_5.render(drawContext, matrixStack, d, d2);
                }
            });
            if (UI.uI.windowShadow.getValue().booleanValue()) {
                int shadow = (int) UI.uI.shadowSize.getValue().floatValue();
                int shadowBottom = (int) (this.y + this.totalHeight + 0.5f);
                drawContext.fill(this.x + shadow, shadowBottom, this.x + getTotalWidth(), shadowBottom + shadow, UI.uI.shadowColor.getValue().getRGB());
                drawContext.fill(this.x + getTotalWidth(), this.y + shadow, this.x + getTotalWidth() + shadow, shadowBottom + shadow, UI.uI.shadowColor.getValue().getRGB());
            }
        }
        if (UI.uI.line.getValue().booleanValue()) {
            int bottom = this.y + (f == 0.0f ? 14 : (int) (this.totalHeight + 0.5f));
            drawContext.outline(this.x - 1, this.y - 1, getTotalWidth() + 2, bottom - (this.y - 1), UI.uI.color.getValue().getRGB());
        }
        if (f != 1.0f && this.totalHeight != 0) {
            Shape2DRenderer.resetShadowOffset();
            ScissorStackManager.popClipRegion();
        }
        Objects.requireNonNull(this);
        int i = 15;
        Iterator<GuiComponent> it = this.registry.iterator();
        while (it.hasNext()) {
            GuiComponent next = it.next();
            if (shouldShowChild(next)) {
                next.setOffsetY(i);
                i += next.getFontHeight();
            } else {
                next.setOffsetY(0);
            }
        }
        this.totalHeight = f == 0.0f ? 15 : i;
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (isTitleBarClicked(d, d2) && i == 1) {
            setExpanded(!this.flag);
        }
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().mouseClicked(d, d2, i);
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseReleased(double d, double d2, int i) {
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().mouseReleased(d, d2, i);
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseScrolled(double d, double d2, double d3) {
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().mouseScrolled(d, d2, d3);
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().keyPressed(i);
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyTyped(char c) {
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().keyTyped(c);
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void init() {
        if (this.flag) {
            Iterator<GuiComponent> it = this.registry.iterator();
            while (it.hasNext()) {
                it.next().init();
            }
        }
    }

    public ArrayList<GuiComponent> getChildren() {
        return this.registry;
    }

    public void setDragging(boolean z) {
        if (z && !ClientServices.getClickGui().isSearchActive()) {
            ClientServices.getClickGui().setSearchActive(true);
            this.flag2 = true;
        } else {
            if (ClientServices.getClickGui().isSearchActive()) {
                ClientServices.getClickGui().setSearchActive(false);
            }
            this.flag2 = false;
        }
    }

    public boolean shouldShowChild(GuiComponent presetHelper_5) {
        return true;
    }

    public boolean isShown() {
        return this.flag && this.progress.val == 2.0f;
    }

    public float getAnimationProgress() {
        return this.progress.getRawValue() - 1.0f;
    }

    public void setExpanded(boolean z) {
        this.flag = z;
    }

    public void recalculateLayout() {
        Objects.requireNonNull(this);
        int i = 15;
        Iterator<GuiComponent> it = this.registry.iterator();
        while (it.hasNext()) {
            GuiComponent next = it.next();
            next.setOffsetY(i);
            i += next.getFontHeight();
        }
        this.totalHeight = i;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int i) {
        this.x = i;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int i) {
        this.y = i;
    }

    public int getTotalWidth() {
        return this.panelWidth + UI.uI.frameWidth.getValue().intValue();
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        return this.totalHeight;
    }

    public void handleDrag(int i, int i2) {
        if (this.flag2) {
            setX(Math.max(0, Math.min(ClientAccessor.minecraftClient.getWindow().getGuiScaledWidth() - getTotalWidth(), i - this.dragOffsetY)));
            setY(Math.max(0, Math.min(ClientAccessor.minecraftClient.getWindow().getGuiScaledHeight() - getFontHeight(), i2 - this.dragOffsetX)));
        }
    }

    public boolean isTitleBarClicked(double d, double d2) {
        if (d >= this.x && d <= this.x + getTotalWidth() && d2 >= this.y) {
            int i = this.y;
            Objects.requireNonNull(this);
            if (d2 <= i + 14) {
                return true;
            }
        }
        return false;
    }

    @Override // me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return d >= ((double) this.x) && d <= ((double) (this.x + getTotalWidth())) && d2 >= ((double) this.y) && d2 <= ((double) (this.y + this.totalHeight));
    }

    public boolean isDragging() {
        return this.flag2;
    }

    @Override // me.mioclient.api.NamedValue
    public String getName() {
        return this.name;
    }

    @Override // me.mioclient.util.registry.CollectionRegistry
    public List<GuiComponent> getRegistry() {
        return this.registry;
    }

    @Override // me.mioclient.util.registry.CollectionRegistry
    /* renamed from: is1975, reason: merged with bridge method [inline-methods] */
    public boolean register(GuiComponent presetHelper_5) {
        return getRegistry().add(presetHelper_5);
    }

    @Override // me.mioclient.util.registry.CollectionRegistry
    /* renamed from: is1976, reason: merged with bridge method [inline-methods] */
    public boolean unregister(GuiComponent presetHelper_5) {
        return getRegistry().add(presetHelper_5);
    }
}
