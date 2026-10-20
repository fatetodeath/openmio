package me.mioclient.setting;

import me.mioclient.gui.widget.PanelComponent;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.font.ClientTextRenderer;

import java.awt.Color;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;

public class PresetPresetSettingButton extends PanelComponent {
    public final Supplier<String> supplier;
    public boolean flag;

    public PresetPresetSettingButton(ClickGuiPanel presetEnumSettingHelper, Supplier<String> supplier) {
        super(presetEnumSettingHelper, 0);
        this.supplier = supplier;
    }

    public PresetPresetSettingButton(ClickGuiPanel presetEnumSettingHelper, String str) {
        super(presetEnumSettingHelper, 0);
        this.supplier = () -> {
            return str;
        };
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        String[] split = this.supplier.get().split("\n");
        for (int i = 0; i < split.length; i++) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, split[i], this.presetEnumSettingHelper.getX() + getUI().modulePadding.getValue().intValue(), ((((this.presetEnumSettingHelper.getY() + this.num) + getTextOffsetY()) + (i * ClientTextRenderer.INSTANCE.getFontHeight())) + 1.0f) - ((this.flag && containsPoint(d, d2)) ? 1.0f : 0.0f), Color.white);
        }
        super.render(drawContext, matrixStack, d, d2);
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        return super.getFontHeight() + (ClientTextRenderer.INSTANCE.getFontHeight() * (this.supplier.get().split("\n").length - 1)) + 1;
    }

    public void setHoverHighlight(boolean z) {
        this.flag = z;
    }
}
