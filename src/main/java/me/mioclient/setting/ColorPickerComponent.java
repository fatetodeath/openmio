package me.mioclient.setting;

import me.mioclient.gui.widget.PanelComponent;
import me.mioclient.gui.widget.PanelComponentFlag;
import me.mioclient.gui.widget.PanelTextButton;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.mode.client.CursorType;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.gui.ClosableElement;
import me.mioclient.gui.GuiComponent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.ProgressSettingPresetSettingButton;
import me.mioclient.setting.SettingPresetSettingButton;

public class ColorPickerComponent extends SettingPresetSettingButton<Color> implements ClosableElement {
    public final List<GuiComponent> list;
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public final Stopwatch stopwatch3;
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public boolean flag4;
    public boolean flag5;
    public float[] floatArr;
    public final InterpolatedFloat hUDHelper_2;
    public final InterpolatedFloat hUDHelper_22;
    public final InterpolatedFloat hUDHelper_23;
    public final InterpolatedFloat hUDHelper_24;
    public final AnimationProgress progress;

    
    private static class Inner extends ProgressSettingPresetSettingButton {
        public Inner(ColorPickerComponent stopwatchPresetHelper2) {
            super(stopwatchPresetHelper2.presetEnumSettingHelper, stopwatchPresetHelper2, new BooleanSetting("", Boolean.valueOf(((ColorSetting) stopwatchPresetHelper2.setting).isRainbow())));
            this.setting.setChangeCallback(() -> {
                ((ColorSetting) stopwatchPresetHelper2.setting).setRainbowEnabled(((Boolean) this.setting.getValue()).booleanValue());
            });
            addFlags(PanelComponentFlag.PADDING_SHIFT);
        }

        @Override // me.mioclient.setting.ProgressSettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
            super.render(drawContext, matrixStack, d, d2);
            String str = "Rainbow";
            renderScrolling(matrixStack, "Rainbow", () -> {
                ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, str, (this.presetEnumSettingHelper.getX() + (getPanel().getTotalWidth() * 0.5f)) - (ClientTextRenderer.INSTANCE.getStringWidth(str) * 0.5f), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
            });
        }
    }

    
    private static class Inner_2 extends PanelTextButton {
        public Inner_2(int i, ColorPickerComponent stopwatchPresetHelper2) {
            super(i, stopwatchPresetHelper2, "Paste", () -> {
                try {
                    Color color828 = ColorUtils.colorFromHex(minecraftClient.keyboardHandler.getClipboard().trim());
                    stopwatchPresetHelper2.syncHsbFromColor(color828);
                    stopwatchPresetHelper2.getSetting().setValue(color828);
                    stopwatchPresetHelper2.stopwatch2.reset();
                } catch (Exception e) {
                    stopwatchPresetHelper2.stopwatch.reset();
                }
            });
        }

        @Override // me.mioclient.gui.widget.PanelTextButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
            if (this.stopwatchPresetHelper2.isHidden()) {
                return;
            }
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + (this.presetEnumSettingHelper.getTotalWidth() / 2.0f) + 0.5f, (int) (this.presetEnumSettingHelper.getY() + this.num + getY()), (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 0.5f, (((this.presetEnumSettingHelper.getY() + this.num) + getY()) + getFontHeight()) - 0.5f, getUI().color.getValue());
            super.render(drawContext, matrixStack, d, d2);
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public float getX() {
            return ((getPanel().getTotalWidth() - (getPanel().getTotalWidth() / 4.0f)) - ClientTextRenderer.INSTANCE.getStringWidth(getName())) + (ClientTextRenderer.INSTANCE.getStringWidth(getName()) * 0.5f);
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public float getY() {
            return 0.0f;
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public String getName() {
            return !this.stopwatchPresetHelper2.stopwatch.hasElapsedMs(1000L) ? "Invalid" : !this.stopwatchPresetHelper2.stopwatch2.hasElapsedMs(1000L) ? "Pasted" : super.getName();
        }
    }

    
    private static class Inner_3 extends ProgressSettingPresetSettingButton {
        public Inner_3(ColorPickerComponent stopwatchPresetHelper2) {
            super(stopwatchPresetHelper2.presetEnumSettingHelper, stopwatchPresetHelper2, new BooleanSetting("", Boolean.valueOf(((ColorSetting) stopwatchPresetHelper2.setting).isSyncToTheme())));
            this.setting.setChangeCallback(() -> {
                ((ColorSetting) stopwatchPresetHelper2.setting).setSyncEnabled(((Boolean) this.setting.getValue()).booleanValue());
            });
            addFlags(PanelComponentFlag.PADDING_SHIFT);
        }

        @Override // me.mioclient.setting.ProgressSettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
            super.render(drawContext, matrixStack, d, d2);
            String str = "Sync";
            renderScrolling(matrixStack, "Sync", () -> {
                ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, str, (this.presetEnumSettingHelper.getX() + (getPanel().getTotalWidth() * 0.5f)) - (ClientTextRenderer.INSTANCE.getStringWidth(str) * 0.5f), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
            });
        }

        @Override // me.mioclient.gui.GuiComponent
        public void init() {
            this.setting.setValue(Boolean.valueOf(((ColorSetting) ((ColorPickerComponent) this.presetHelper_2).getSetting()).isSyncToTheme()));
        }
    }

    
    private static class Inner_4 extends PanelTextButton {
        public Inner_4(int i, ColorPickerComponent stopwatchPresetHelper2) {
            super(i, stopwatchPresetHelper2, "Copy", () -> {
                minecraftClient.keyboardHandler.setClipboard(ColorUtils.colorToHex(stopwatchPresetHelper2.getColor(), true));
                stopwatchPresetHelper2.stopwatch3.reset();
            });
        }

        @Override // me.mioclient.gui.widget.PanelTextButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
            if (this.stopwatchPresetHelper2.isHidden()) {
                return;
            }
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 2, (int) (this.presetEnumSettingHelper.getY() + this.num + getY()), (this.presetEnumSettingHelper.getX() + (this.presetEnumSettingHelper.getTotalWidth() / 2.0f)) - 0.5f, (((this.presetEnumSettingHelper.getY() + this.num) + getY()) + getFontHeight()) - 0.5f, getUI().color.getValue());
            super.render(drawContext, matrixStack, d, d2);
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public float getX() {
            return (getPanel().getTotalWidth() / 4.0f) - (ClientTextRenderer.INSTANCE.getStringWidth(getName()) / 2.0f);
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public float getY() {
            return 0.0f;
        }

        @Override // me.mioclient.gui.widget.PanelTextButton
        public String getName() {
            return !this.stopwatchPresetHelper2.stopwatch3.hasElapsedMs(1000L) ? "Copied" : super.getName();
        }
    }

    public ColorPickerComponent(ClickGuiPanel presetEnumSettingHelper, ClosableElement presetHelper_2, Setting<?> setting) {
        super(presetEnumSettingHelper, presetHelper_2, (ColorSetting) setting);
        this.list = new ArrayList(3);
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.stopwatch3 = new Stopwatch();
        this.floatArr = new float[3];
        this.hUDHelper_2 = new InterpolatedFloat(5.0f, true);
        this.hUDHelper_22 = new InterpolatedFloat(5.0f, true);
        this.hUDHelper_23 = new InterpolatedFloat(5.0f, true);
        this.hUDHelper_24 = new InterpolatedFloat(5.0f, true);
        this.progress = new AnimationProgress(4.0f, true);
        int i = (int) (this.num + getRowHeight() + getPanel().getTotalWidth() + 6.5d + getRowSpacing());
        this.list.add(new Inner_4(i, this));
        this.list.add(new Inner_2(i, this));
        if (!((ColorSetting) setting).isThemeColor()) {
            this.list.add(new Inner_3(this));
        }
        this.list.add(new Inner(this));
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        super.render(drawContext, matrixStack, d, d2);
        if (isHidden() || !this.flag) {
            this.hUDHelper_2.setImmediate(0.0f);
            this.hUDHelper_22.setImmediate(0.0f);
            this.hUDHelper_23.setImmediate(0.0f);
            this.hUDHelper_24.setImmediate(0.0f);
        }
        if (isHidden()) {
            return;
        }
        if (this.flag && this.flag) {
            AbstractClickGuiScreen.cursorType = CursorType.STANDARD;
        }
        renderScrolling(matrixStack, this.setting.getName(), () -> {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, this.setting.getName(), this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        });
        Shape2DRenderer.drawRect(matrixStack, (getX() + this.presetEnumSettingHelper.getTotalWidth()) - 12.5f, getY() + 1.0f, (getX() + this.presetEnumSettingHelper.getTotalWidth()) - 3.5f, getY() + 10.0f, getUI().color.getValue());
        Shape2DRenderer.drawRect(matrixStack, (getX() + this.presetEnumSettingHelper.getTotalWidth()) - 12.0f, getY() + 1.5f, (getX() + this.presetEnumSettingHelper.getTotalWidth()) - 4.0f, getY() + 9.5f, ColorUtils.withAlpha((Color) this.setting.getValue(), 255));
        if (this.flag) {
            int alpha = ((Color) this.setting.getValue()).getAlpha();
            int i = getRowHeight();
            if (this.flag3) {
                AbstractClickGuiScreen.cursorType = CursorType.POINTER;
                this.floatArr[1] = (float) Mth.clamp(((d - getX()) - 5.0d) / (getPanel().getTotalWidth() - 8), 0.0d, 1.0d);
                this.floatArr[2] = 1.0f - ((float) Mth.clamp((((d2 - getY()) - i) - 3.0d) / (getPanel().getTotalWidth() - 8), 0.0d, 1.0d));
            } else if (this.flag4) {
                AbstractClickGuiScreen.cursorType = CursorType.POINTER;
                this.floatArr[0] = Math.min((((float) Math.min(Math.max(getX(), d - 1.0d), getX() + (getPanel().getTotalWidth() - 4))) - getX()) / (getPanel().getTotalWidth() - 4), 1.0f);
            } else if (this.flag5 && !this.setting.flag3) {
                AbstractClickGuiScreen.cursorType = CursorType.POINTER;
                alpha = (int) (Math.min(1.0f - ((((float) Math.min(Math.max(getX(), d - 3.0d), getX() + (getPanel().getTotalWidth() - 4))) - getX()) / (getPanel().getTotalWidth() - 4)), 1.0f) * 255.0f);
            }
            int HSBtoRGB = Color.HSBtoRGB(this.floatArr[0], 1.0f, 1.0f);
            float intBitsToFloat = ((HSBtoRGB >> 16) & 255) / 255.0f;
            float intBitsToFloat2 = ((HSBtoRGB >> 8) & 255) / 255.0f;
            float intBitsToFloat3 = (HSBtoRGB & 255) / 255.0f;
            int hashCode = this.flag3 ? -1 : getUI().color.getValue().hashCode();
            Shape2DRenderer.drawGradientRect(matrixStack.last().pose(), getX() + 2.5f, getY() + i + 0.5f, getPanel().getTotalWidth() - 3, getPanel().getTotalWidth() - 3, hashCode, hashCode, hashCode, hashCode);
            Shape2DRenderer.drawGradientRect(matrixStack.last().pose(), getX() + 3.0f, getY() + i + 1.0f, getPanel().getTotalWidth() - 4, getPanel().getTotalWidth() - 4, -1, HSBtoRGB, -16777216, -16777216);
            Matrix4f positionMatrix = matrixStack.last().pose();
            float f = getX() + 2.5f;
            float f2 = ((getY() + getPanel().getTotalWidth()) + i) - 0.5f;
            float f3 = getPanel().getTotalWidth() - 3;
            float intBitsToFloat4 = 7.0f;
            int[] iArr = new int[2];
            iArr[0] = this.flag4 ? -1 : getAccentColor().hashCode();
            iArr[1] = this.flag4 ? -1 : getAccentColor().hashCode();
            Shape2DRenderer.drawMultiGradientRect(positionMatrix, f, f2, f3, intBitsToFloat4, iArr);
            Shape2DRenderer.drawMultiGradientRect(matrixStack.last().pose(), getX() + 3.0f, getY() + getPanel().getTotalWidth() + i, getPanel().getTotalWidth() - 4, 6.0f, -65536, -256, -16711936, -16711681, -16776961, -65281, -65536);
            applyAnimatedValue(this.hUDHelper_23, this.floatArr[0] * (getPanel().getTotalWidth() - 2));
            float clamp = Mth.clamp(this.hUDHelper_23.getValue(), 3.0f, getPanel().getTotalWidth() - 3);
            Shape2DRenderer.drawRect(matrixStack, ((int) (getX() + clamp)) - 0.5f, ((int) ((getY() + getPanel().getTotalWidth()) + i)) - 0.5f, ((int) (getX() + clamp + 2.0f)) + 0.5f, (int) (getY() + getPanel().getTotalWidth() + i + 6.5f), this.flag3 ? -1 : getAccentColor().hashCode());
            Shape2DRenderer.drawRect(matrixStack, (int) (getX() + clamp), (int) (getY() + getPanel().getTotalWidth() + i), (int) (getX() + clamp + 2.0f), (int) (getY() + getPanel().getTotalWidth() + i + 6.5f), -1);
            if (!this.setting.flag3) {
                Matrix4f positionMatrix2 = matrixStack.last().pose();
                float f4 = getX() + 2.5f;
                float f5 = ((int) getY()) + getPanel().getTotalWidth() + i + 7.5f;
                float f6 = getPanel().getTotalWidth() - 3;
                float intBitsToFloat5 = 7.0f;
                int[] iArr2 = new int[2];
                iArr2[0] = this.flag5 ? -1 : getAccentColor().hashCode();
                iArr2[1] = this.flag5 ? -1 : getAccentColor().hashCode();
                Shape2DRenderer.drawMultiGradientRect(positionMatrix2, f4, f5, f6, intBitsToFloat5, iArr2);
                drawAlphaSlider(matrixStack, ((int) getX()) + 3, ((int) getY()) + getPanel().getTotalWidth() + i + 8, getPanel().getTotalWidth() - 4, 6, intBitsToFloat, intBitsToFloat2, intBitsToFloat3, alpha / 255.0f);
            }
            applyAnimatedValue(this.hUDHelper_2, Mth.clamp(this.floatArr[1], 0.0f, 1.0f) * (getPanel().getTotalWidth() - 8));
            applyAnimatedValue(this.hUDHelper_22, (1.0f - Mth.clamp(this.floatArr[2], 0.0f, 1.0f)) * (getPanel().getTotalWidth() - 8));
            float f7 = getX() + 5.0f + this.hUDHelper_2.getValue();
            float f8 = getY() + i + 3.0f + this.hUDHelper_22.getValue();
            this.progress.animateToggle(this.flag3);
            float intBitsToFloat6 = 2.0f * 1.0f * this.progress.getValue();
            Color hSBColor = Color.getHSBColor(this.floatArr[0], this.floatArr[1], this.floatArr[2]);
            Shape2DRenderer.drawRect(matrixStack, (f7 - 1.5f) - intBitsToFloat6, (f8 - 1.5f) - intBitsToFloat6, f7 + 1.5f + intBitsToFloat6, f8 + 1.5f + intBitsToFloat6, this.flag3 ? Color.white : getAccentColor());
            Shape2DRenderer.drawRect(matrixStack, (f7 - 1.0f) - intBitsToFloat6, (f8 - 1.0f) - intBitsToFloat6, f7 + 1.0f + intBitsToFloat6, f8 + 1.0f + intBitsToFloat6, this.flag3 ? hSBColor : hSBColor.darker());
            this.list.forEach(presetHelper_5 -> {
                presetHelper_5.render(drawContext, matrixStack, d, d2);
            });
            this.setting.setValue(ColorUtils.withAlpha(new Color(Color.HSBtoRGB(this.floatArr[0], this.floatArr[1], this.floatArr[2])), Mth.clamp(alpha, 0, 255)));
            if (((ColorSetting) this.setting).isRainbow()) {
                this.floatArr[0] = Color.RGBtoHSB(((Color) this.setting.getValue()).getRed(), ((Color) this.setting.getValue()).getGreen(), ((Color) this.setting.getValue()).getBlue(), (float[]) null)[0];
            }
        }
        int i2 = (int) (this.num + getRowHeight() + getPanel().getTotalWidth() + 6 + getRowSpacing());
        float intBitsToFloat7 = 1.0f;
        for (GuiComponent presetHelper_52 : this.list) {
            presetHelper_52.update(d, d2);
            presetHelper_52.setOffsetY(i2);
            if (!(presetHelper_52 instanceof PanelTextButton)) {
                i2 += (int) (presetHelper_52.getFontHeight() + intBitsToFloat7);
                presetHelper_52.setOffsetY(i2);
                intBitsToFloat7 = 0.5f;
            }
        }
    }

    public void drawAlphaSlider(PoseStack matrixStack, int i, int i2, int i3, int i4, float f, float f2, float f3, float f4) {
        boolean z = true;
        int i5 = i4 / 2;
        int i6 = -i5;
        while (true) {
            int i7 = i6;
            if (i7 >= i3) {
                break;
            }
            if (!z) {
                Shape2DRenderer.drawRect(matrixStack, i + i7, i2, i + i7 + i5, i2 + i4, -1);
                Shape2DRenderer.drawRect(matrixStack, i + i7, i2 + i5, i + i7 + i5, i2 + i4, -7303024);
                if (i7 < i3 - i5) {
                    int i8 = i + i7 + i5;
                    int min = Math.min(i + i3, i + i7 + (i5 * 2));
                    Shape2DRenderer.drawRect(matrixStack, i8, i2, min, i2 + i4, -7303024);
                    Shape2DRenderer.drawRect(matrixStack, i8, i2 + i5, min, i2 + i4, -1);
                }
            }
            z = !z;
            i6 = i7 + i5;
        }
        Shape2DRenderer.drawMultiGradientRect(matrixStack.last().pose(), i, i2, i3, i4, new Color(f, f2, f3, 1.0f).hashCode(), 0);
        applyAnimatedValue(this.hUDHelper_24, i3 - ((i3 - 2) * f4));
        float f5 = (i + this.hUDHelper_24.getValue()) - 1.0f;
        Shape2DRenderer.drawRect(matrixStack, f5 - 1.5f, i2 - 0.5f, f5 + 1.5f, i2 + i4 + 0.5f, this.flag5 ? Color.white : getAccentColor());
        Shape2DRenderer.drawRect(matrixStack, f5 - 1.0f, i2, f5 + 1.0f, i2 + i4, -1);
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (isHidden()) {
            return;
        }
        int i2 = getRowHeight();
        if (isMouseOverRow(d, d2) && i == 1) {
            this.flag = !this.flag;
        }
        if (this.flag) {
            this.list.forEach(presetHelper_5 -> {
                presetHelper_5.mouseClicked(d, d2, i);
            });
        }
        if (this.flag && i == 0) {
            if (d > getX() + 2.0f && d < (getX() + getPanel().getTotalWidth()) - 2.0f && d2 > getY() + i2 && d2 < ((getY() + i2) + getPanel().getTotalWidth()) - 2.0f) {
                this.flag3 = true;
            }
            if (d > getX() + 2.0f && d < (getX() + getPanel().getTotalWidth()) - 2.0f && d2 > getY() + i2 + getPanel().getTotalWidth() && d2 < getY() + i2 + getPanel().getTotalWidth() + 6.0f) {
                this.flag4 = true;
            }
            if (d > getX() + 2.0f && d < (getX() + getPanel().getTotalWidth()) - 2.0f && d2 > getY() + i2 + getPanel().getTotalWidth() + 8.0f && d2 < getY() + i2 + getPanel().getTotalWidth() + 14.0f && !this.setting.flag3) {
                this.flag5 = true;
            }
            super.mouseClicked(d, d2, i);
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseReleased(double d, double d2, int i) {
        if (isHidden()) {
            return;
        }
        this.list.forEach(presetHelper_5 -> {
            presetHelper_5.mouseReleased(d, d2, i);
        });
        super.mouseReleased(d, d2, i);
        this.flag5 = false;
        this.flag4 = false;
        this.flag3 = false;
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyTyped(char c) {
        if (isHidden()) {
            return;
        }
        this.list.forEach(presetHelper_5 -> {
            presetHelper_5.keyTyped(c);
        });
        super.keyTyped(c);
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        if (isHidden()) {
            return;
        }
        this.list.forEach(presetHelper_5 -> {
            presetHelper_5.keyPressed(i);
        });
        super.keyPressed(i);
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        if (!this.flag || isHidden()) {
            return super.getFontHeight();
        }
        int i = (int) (getPanel().getTotalWidth() + 7.0f + (getRowHeight() * 2) + getRowSpacing());
        if (this.list.size() > 2) {
            i = (int) (i + (getRowHeight() * (this.list.size() - 2)) + 0.5f);
        }
        return i;
    }

    @Override // me.mioclient.gui.GuiComponent
    public void init() {
        syncHsbFromColor((Color) this.setting.getValue());
        this.list.forEach((v0) -> {
            v0.init();
        });
    }

    @Override // me.mioclient.gui.ClosableElement
    public boolean isClosed() {
        return super.presetHelper_2.isClosed() || !this.flag;
    }

    public boolean isMouseOverRow(double d, double d2) {
        return d > ((double) this.presetEnumSettingHelper.getX()) && d < ((double) (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth())) && d2 > ((double) (this.presetEnumSettingHelper.getY() + this.num)) && d2 < ((double) ((this.presetEnumSettingHelper.getY() + getRowHeight()) + this.num));
    }

    public float getX() {
        return this.presetEnumSettingHelper.getX();
    }

    public float getY() {
        return this.presetEnumSettingHelper.getY() + this.num;
    }

    public Color getAccentColor() {
        return ColorUtils.withAlphaPercent(getUI().color.getValue(), 1.0f);
    }

    public float getRowSpacing() {
        return this.setting.flag3 ? 2.0f : 10.0f;
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return super.containsPoint(d, d2) || this.flag4 || this.flag3 || this.flag5;
    }

    public void syncHsbFromColor(Color color) {
        float[] RGBtoHSB = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), (float[]) null);
        this.floatArr[0] = RGBtoHSB[0];
        this.floatArr[1] = RGBtoHSB[1];
        this.floatArr[2] = RGBtoHSB[2];
    }

    public Color getColor() {
        return ColorUtils.withAlpha(Color.getHSBColor(this.floatArr[0], this.floatArr[1], this.floatArr[2]), ((Color) this.setting.getValue()).getAlpha());
    }

    public void applyAnimatedValue(InterpolatedFloat hUDHelper_2, float f) {
        if (this.flag2) {
            hUDHelper_2.setImmediate(f);
        } else {
            hUDHelper_2.animate(f);
        }
    }
}
