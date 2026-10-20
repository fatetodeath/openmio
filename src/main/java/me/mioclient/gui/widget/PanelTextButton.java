package me.mioclient.gui.widget;

import me.mioclient.render.font.ClientTextRenderer;

import me.mioclient.setting.ColorPickerComponent;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class PanelTextButton extends PanelComponent {
    public final ColorPickerComponent stopwatchPresetHelper2;
    public final String name;
    public java.lang.Runnable runnable;

    public PanelTextButton(int i, ColorPickerComponent stopwatchPresetHelper2, String str, java.lang.Runnable runnable) {
        super(stopwatchPresetHelper2.getPanel(), i);
        this.stopwatchPresetHelper2 = stopwatchPresetHelper2;
        this.name = str;
        this.runnable = runnable;
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        if (this.stopwatchPresetHelper2.isHidden()) {
            return;
        }
        super.render(drawContext, matrixStack, d, d2);
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, getName(), this.presetEnumSettingHelper.getX() + getX(), (((this.presetEnumSettingHelper.getY() + this.num) + getY()) + getTextOffsetY()) - getAnimationOffset(), getUI().textColor.getValue());
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (this.stopwatchPresetHelper2.isHidden()) {
            return;
        }
        super.mouseClicked(d, d2, i);
        if (containsPoint(d, d2) && i == 0) {
            this.runnable.run();
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return d > ((double) (((float) this.presetEnumSettingHelper.getX()) + getX())) && d < ((double) ((((float) this.presetEnumSettingHelper.getX()) + getX()) + ClientTextRenderer.INSTANCE.getStringWidth(getName()))) && d2 > ((double) (((float) (this.presetEnumSettingHelper.getY() + this.num)) + getY())) && d2 < ((double) (((((float) (this.presetEnumSettingHelper.getY() + this.num)) + getY()) + ((float) ClientTextRenderer.INSTANCE.getFontHeight())) + (getTextOffsetY() * 2.0f)));
    }

    public abstract float getX();

    public abstract float getY();

    public String getName() {
        return this.name;
    }
}
