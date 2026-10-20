package me.mioclient.gui.widget;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.shape.Shape2DRenderer;

import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.setting.PresetPresetSettingButton;

public class ActionButton extends PresetPresetSettingButton {
    public final java.lang.Runnable runnable;

    public ActionButton(ClickGuiPanel presetEnumSettingHelper, String str, java.lang.Runnable runnable) {
        this(presetEnumSettingHelper, (Supplier<String>) () -> {
            return str;
        }, runnable);
    }

    public ActionButton(ClickGuiPanel presetEnumSettingHelper, Supplier<String> supplier, java.lang.Runnable runnable) {
        super(presetEnumSettingHelper, supplier);
        this.runnable = runnable;
        setHoverHighlight(true);
    }

    @Override // me.mioclient.setting.PresetPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        if (isHighlighted()) {
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 1, this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 0.5f, getUI().color.getValue());
        }
        super.render(drawContext, matrixStack, d, d2);
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        super.mouseClicked(d, d2, i);
        if (containsPoint(d, d2) && i == 0) {
            this.runnable.run();
        }
    }

    public boolean isHighlighted() {
        return true;
    }
}
