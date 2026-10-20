package me.mioclient.setting;

import me.mioclient.api.ClientServices;
import me.mioclient.gui.widget.PanelComponent;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.mode.client.CursorType;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.gui.ClosableElement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;

public class StopwatchSettingPresetSettingButton extends SettingPresetSettingButton<Number> {
    public static final NumberFormat numberFormat = NumberFormat.getNumberInstance();
    public final InterpolatedFloat hUDHelper_2;
    public final Stopwatch stopwatch;
    public int barX;
    public int barY;
    public boolean flag;
    public boolean flag2;
    public float val;
    public String string;

    public StopwatchSettingPresetSettingButton(ClickGuiPanel presetEnumSettingHelper, ClosableElement presetHelper_2, Setting<?> setting) {
        super(presetEnumSettingHelper, presetHelper_2, (Setting) setting);
        this.hUDHelper_2 = new InterpolatedFloat(6.0f, false);
        this.stopwatch = new Stopwatch();
        this.flag2 = false;
        this.string = "";
    }

    public static double roundCoord(double d) {
        return new BigDecimal(d).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (isHidden()) {
            this.flag2 = false;
            return;
        }
        super.mouseClicked(d, d2, i);
        if (containsPoint(d, d2) && i == 0 && this.presetHelper_2.isShown()) {
            this.flag = true;
        }
        if (containsPoint(d, d2) && i == 1 && this.presetHelper_2.isShown()) {
            this.flag2 = !this.flag2;
            if (!this.flag2) {
                if (getClickGuiScreen().getActiveSettingButton() == this) {
                    getClickGuiScreen().setActiveSetting(null);
                }
            } else {
                if (getClickGuiScreen().getActiveSettingButton() != null && getClickGuiScreen().getActiveSettingButton() != this) {
                    getClickGuiScreen().getActiveSettingButton().flag2 = false;
                }
                getClickGuiScreen().setActiveSetting(this);
                this.string = ((Number) this.setting.getValue()).toString();
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseReleased(double d, double d2, int i) {
        this.flag = false;
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        if (!isHidden() && this.flag2 && this.presetHelper_2.isShown()) {
            switch (i) {
                case 256:
                    this.flag2 = false;
                    ClientServices.getClickGui().suppressEscapeClose();
                    return;
                case 257:
                case 335:
                    this.flag2 = false;
                    boolean z = this.string == null || this.string.isBlank();
                    try {
                        String simpleName = ((Number) this.setting.getValue()).getClass().getSimpleName();
                        int z2 = -1;
                        switch (simpleName.hashCode()) {
                            case -672261858:
                                if (simpleName.equals("Integer")) {
                                    z2 = 0;
                                    break;
                                }
                                break;
                            case 67973692:
                                if (simpleName.equals("Float")) {
                                    z2 = 1;
                                    break;
                                }
                                break;
                            case 2052876273:
                                if (simpleName.equals("Double")) {
                                    z2 = 2;
                                    break;
                                }
                                break;
                        }
                        switch (z2) {
                            case 0:
                                this.setting.setValue(z ? (Number) this.setting.getValue() : Long.valueOf(Math.round(Double.parseDouble(this.string))));
                                break;
                            case 1:
                                this.setting.setValue(z ? (Number) this.setting.getValue() : Float.valueOf(Float.parseFloat(new StringTemplate().arg(this.string).format("\u0001f"))));
                                break;
                            case 2:
                                this.setting.setValue(z ? (Number) this.setting.getValue() : Double.valueOf(Double.parseDouble(new StringTemplate().arg(this.string).format("\u0001d"))));
                                break;
                        }
                        return;
                    } catch (Exception e) {
                        return;
                    }
                case 259:
                    if (this.string.length() > 0) {
                        this.string = this.string.substring(0, this.string.length() - 1);
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
        if (!isHidden() && this.flag2 && this.presetHelper_2.isShown() && ClientTextRenderer.containsChar("1234567890.-", c)) {
            if (c == '.' && ClientTextRenderer.containsChar(this.string, c)) {
                return;
            }
            this.string = new StringTemplate().arg(c).arg(this.string).format("\u0001\u0001");
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void update(double d, double d2) {
        if (isHidden()) {
            this.hUDHelper_2.setImmediate(0.0f);
        }
        this.barY = this.presetEnumSettingHelper.getY() + this.num;
        this.barX = this.presetEnumSettingHelper.getX();
        double d3 = getPanel().getTotalWidth() - 2;
        double min = Math.min(d3, Math.max(0.0d, d - this.barX));
        double doubleValue = ((Number) this.setting.getMinValue()).doubleValue();
        double doubleValue2 = ((Number) this.setting.getMaxValue()).doubleValue();
        double longBitsToDouble = ((Number) this.setting.getValue()).getClass().getSimpleName().equalsIgnoreCase("Integer") ? 1.0d : 0.1d;
        this.val = (float) ((d3 * ((longBitsToDouble == 1.0d ? ((Number) this.setting.getValue()).intValue() : ((Number) this.setting.getValue()).floatValue()) - doubleValue)) / (doubleValue2 - doubleValue));
        if (this.flag) {
            if (min != 0.0d) {
                float f = (float) (((min / d3) * (doubleValue2 - doubleValue)) + doubleValue);
                float longBitsToDouble2 = (float) (1.0d / longBitsToDouble);
                double max = Math.max(doubleValue, Math.min(doubleValue2, f));
                if (longBitsToDouble == 0.1d) {
                    this.setting.setValue(Double.valueOf(roundCoord(((float) Math.round(max * longBitsToDouble2)) / longBitsToDouble2)));
                } else {
                    this.setting.setValue(Double.valueOf(roundCoord(max)));
                }
            } else if (longBitsToDouble == 1.0d) {
                this.setting.setValue(Integer.valueOf((int) ((Number) this.setting.getMinValue()).floatValue()));
            } else {
                this.setting.setValue(Float.valueOf(((Number) this.setting.getMinValue()).floatValue()));
            }
        }
        if (((Number) this.setting.getValue()).floatValue() >= ((Number) this.setting.getMaxValue()).floatValue()) {
            this.val = getPanel().getTotalWidth() - 2;
        }
        this.hUDHelper_2.animate(this.val);
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
        if (this.flag && this.flag2) {
            AbstractClickGuiScreen.cursorType = CursorType.INPUT;
        }
        Shape2DRenderer.drawRect(matrixStack, this.barX + getPadding(), this.barY + 0.5f, (int) (this.barX + Mth.clamp(this.hUDHelper_2.getValue(), 0.0f, this.presetEnumSettingHelper.getTotalWidth() - 3) + 2.0f), (this.barY + getFontHeight()) - 0.5f, getUI().color.getValue());
        String string2921 = this.flag2 ? this.string == null ? "" : this.string : this.setting.isHoleSnapConfigured() ? new StringTemplate().arg(this.setting.getHoleSnapString()).arg(this.setting.getName()).format("\u0001: \u0001") : new StringTemplate().arg(((NumberSetting) this.setting).getSuffix()).arg(String.format("%s: %s", this.setting.getName(), numberFormat.format(((Number) this.setting.getValue()).doubleValue())).replace(",", ".")).format("\u0001\u0001");
        renderScrolling(matrixStack, formatDisplayText(string2921), () -> {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, new StringTemplate().arg(getBlinkingCursor()).arg(string2921).format("\u0001\u0001"), this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        });
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return super.containsPoint(d, d2) || this.flag;
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        if (this.setting.isVisible()) {
            return getRowHeight();
        }
        return 0;
    }

    public String formatDisplayText(String str) {
        return new StringTemplate().arg(this.flag2 ? "_" : "").arg(str).format("\u0001\u0001");
    }

    public boolean cursorBlink;
    public String getBlinkingCursor() {
        // å…‰æ ‡é—ªçƒ: æ¯ 500ms åˆ‡æ¢ç‹¬ç«‹çš„ cursorBlink, ä¸å†åˆ‡æ¢ flag2(ç¼–è¾‘æ¨¡å¼æ ‡å¿—)ã€‚
        // åç¼–è¯‘æŠŠ obf çš„å…‰æ ‡é—ªçƒå­—æ®µå’Œç¼–è¾‘æ ‡å¿—éƒ½å¹¶æˆäº† flag2, å¯¼è‡´æ¯ä¸ªæ»‘å—æ¯ 500ms
        // è¿›å‡º"ç¼–è¾‘æ¨¡å¼"â†’æ˜¾ç¤ºåœ¨å€¼å’Œç©ºç¼“å†²é—´é—ªçƒã€‚ä»…åœ¨çœŸæ­£ç¼–è¾‘(flag2)ä¸”é—ªçƒå¯è§æ—¶æ˜¾ç¤º "_"ã€‚
        if (this.stopwatch.hasElapsedMs(500L)) {
            this.cursorBlink = !this.cursorBlink;
            this.stopwatch.reset();
        }
        return (this.cursorBlink && this.flag2) ? "_" : "";
    }

    static {
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(4);
        numberFormat.setGroupingUsed(false);
    }
}
