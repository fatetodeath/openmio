package me.mioclient.setting;

import me.mioclient.util.client.SoundIdentifier;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.gui.screen.SoundSelectScreen;
import me.mioclient.mode.client.CursorType;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;

import me.mioclient.gui.ClosableElement;

import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.setting.SettingPresetSettingButton;

public class SoundSettingRow extends SettingPresetSettingButton<SoundIdentifier> {
    public SoundSettingRow(ClickGuiPanel presetEnumSettingHelper, ClosableElement presetHelper_2, Setting<?> setting) {
        super(presetEnumSettingHelper, presetHelper_2, (Setting<SoundIdentifier>) setting);
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (!isHidden() && containsPoint(d, d2) && i == 0) {
            minecraftClient.setScreenAndShow(new SoundSelectScreen(this.setting, getClickGuiScreen()));
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        if (isHidden()) {
            return;
        }
        super.render(drawContext, matrixStack, d, d2);
        if (containsPoint(d, d2)) {
            AbstractClickGuiScreen.cursorType = CursorType.POINTER;
        }
        Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + getPadding(), this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 0.5f, getUI().color.getValue());
        String string2921 = new StringTemplate().arg(((SoundIdentifier) this.setting.getValue()).getName()).arg(this.setting.getName()).format("\u0001: \u0001");
        renderScrolling(matrixStack, string2921, () -> {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string2921, this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        });
    }
}
