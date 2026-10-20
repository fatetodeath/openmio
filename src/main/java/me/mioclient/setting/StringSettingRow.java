package me.mioclient.setting;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.mode.client.CursorType;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.gui.ClosableElement;

import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import me.mioclient.setting.SettingPresetSettingButton;
import me.mioclient.setting.StringSetting;

public class StringSettingRow extends SettingPresetSettingButton<String> {
    public final Stopwatch stopwatch;
    public boolean flag;
    public String string;
    public int num;
    /* 26.2 port: PanelComponent.num is the row's vertical offset (set by setOffsetY).
       The text cursor needs its own field — sharing num teleported the row to the top. */
    public int cursorPos;

    public StringSettingRow(ClickGuiPanel presetEnumSettingHelper, ClosableElement presetHelper_2, Setting<?> setting) {
        super(presetEnumSettingHelper, presetHelper_2, (StringSetting) setting);
        this.stopwatch = new Stopwatch();
        this.flag = false;
        this.num = 0;
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (isHidden()) {
            this.flag2 = false;
            return;
        }
        super.mouseClicked(d, d2, i);
        if (containsPoint(d, d2) && i == 0) {
            this.flag2 = !this.flag2;
            if (this.flag2) {
                if (getClickGuiScreen().getActiveSettingButton() != null && getClickGuiScreen().getActiveSettingButton() != this) {
                    getClickGuiScreen().getActiveSettingButton().flag2 = false;
                }
                getClickGuiScreen().setActiveSetting(this);
            } else if (getClickGuiScreen().getActiveSettingButton() == this) {
                getClickGuiScreen().setActiveSetting(null);
            }
            this.string = (String) this.setting.getValue();
            if (this.string == null || this.string.isEmpty()) {
                return;
            }
            this.cursorPos = this.string.length();
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        if (isHidden()) {
            if (this.flag2 && getClickGuiScreen().getActiveSettingButton() == this) {
                getClickGuiScreen().setActiveSetting(null);
            }
            this.flag2 = false;
            return;
        }
        super.render(drawContext, matrixStack, d, d2);
        if (this.flag) {
            AbstractClickGuiScreen.cursorType = CursorType.INPUT;
        }
        Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + getPadding(), this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 0.5f, getUI().color.getValue());
        if (this.string != null) {
            this.cursorPos = Mth.clamp(this.cursorPos, 0, this.string.length());
        }
        if (!this.flag2) {
            renderScrolling(matrixStack, new StringTemplate().arg((String) this.setting.getValue()).arg(this.setting.getName()).format("\u0001: \u0001"), () -> {
                ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, new StringTemplate().arg((String) this.setting.getValue()).arg(this.setting.getName()).format("\u0001: \u0001"), this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
            });
            return;
        }
        matrixStack.pushPose();
        String string95 = getBlinkingCursor();
        float f = ClientTextRenderer.INSTANCE.getStringWidth(new StringTemplate().arg(this.string.substring(0, this.cursorPos)).format("_"));
        if (f > getPanel().getTotalWidth() - (getPadding() * 2)) {
            matrixStack.translate((getPanel().getTotalWidth() - (getPadding() * 2)) - f, 0.0f, 0.0f);
        }
        if (!this.string.isEmpty()) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, this.string, this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        }
        if (!string95.isBlank()) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string95, this.presetEnumSettingHelper.getX() + 4 + ClientTextRenderer.INSTANCE.getStringWidth(this.string.substring(0, this.cursorPos)), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        }
        matrixStack.popPose();
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        if (!isHidden() && this.flag2) {
            ClientServices.getClickGui().suppressEscapeClose();
            switch (i) {
                case 86:
                    if (GLFW.glfwGetKey(minecraftClient.getWindow().handle(), 341) == 1) {
                        this.string = new StringBuilder(this.string).insert(this.cursorPos, minecraftClient.keyboardHandler.getClipboard()).toString();
                        return;
                    }
                    return;
                case 256:
                    this.flag2 = false;
                    return;
                case 257:
                    this.flag2 = false;
                    this.setting.setValue(this.string);
                    this.cursorPos = this.string.length();
                    return;
                case 259:
                    if (this.string.length() == 0 || this.cursorPos == 0) {
                        return;
                    }
                    int i2 = this.cursorPos - 1;
                    if (this.cursorPos == this.string.length()) {
                        this.string = this.string.substring(0, this.string.length() - 1);
                    } else if (i2 >= 0 && i2 <= this.string.length()) {
                        this.string = new StringBuilder(this.string).deleteCharAt(i2).toString();
                    }
                    this.cursorPos--;
                    return;
                case 262:
                    if (this.cursorPos < this.string.length()) {
                        this.cursorPos++;
                        return;
                    }
                    return;
                case 263:
                    if (this.cursorPos > 0) {
                        this.cursorPos--;
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyTyped(char c) {
        if (!isHidden() && this.flag2) {
            this.string = new StringBuilder(this.string).insert(this.cursorPos, c).toString();
            this.cursorPos++;
        }
    }

    public String getBlinkingCursor() {
        if (this.stopwatch.hasElapsedMs(500L)) {
            this.flag = !this.flag;
            this.stopwatch.reset();
        }
        return this.flag ? "_" : "";
    }
}
