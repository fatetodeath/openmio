package me.mioclient.setting;

import me.mioclient.gui.widget.PanelComponent;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.registry.EnumSettingRegistry;

import me.mioclient.gui.ClosableElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Supplier;
import me.mioclient.api.Setting;
import me.mioclient.module.client.UI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;

public class PresetSettingPresetSettingButton extends SettingPresetSettingButton<Enum<?>> {
    public final ArrayList<PresetSettingButton> arrayList;
    public final InterpolatedFloat hUDHelper_2;
    public final AnimationProgress progress;
    public boolean flag;
    public int num;
    public int x;
    public int y;
    public int num2;
    public final int num3;

    public PresetSettingPresetSettingButton(ClickGuiPanel presetEnumSettingHelper, ClosableElement presetHelper_2, Setting<?> setting) {
        super(presetEnumSettingHelper, presetHelper_2, (Setting) setting);
        this.arrayList = new ArrayList<>();
        this.hUDHelper_2 = new InterpolatedFloat((Supplier<Float>) () -> {
            return Float.valueOf(2.0f * getUI().animSpeed.getValue().floatValue());
        }, true);
        this.progress = new AnimationProgress((Supplier<Float>) () -> {
            return Float.valueOf(2.0f * UI.uI.animSpeed.getValue().floatValue());
        }, true);
        this.flag = false;
        int i = getRowHeight();
        int i2 = 0;
        for (Enum r0 : (Enum[]) ((Enum) this.setting.getDefaultValue()).getDeclaringClass().getEnumConstants()) {
            if (!EnumSettingRegistry.isExperimentalEnumValue(r0)) {
                this.arrayList.add(new PresetSettingButton(this, EnumJsonConverter.getEnumDisplayName(r0), i, i2));
                i += getRowHeight();
                i2++;
            }
        }
        this.num3 = i2;
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void setOffsetY(int i) {
        this.num = i;
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        if (isHidden()) {
            return;
        }
        super.mouseClicked(d, d2, i);
        this.arrayList.forEach(PresetSettingButton -> {
            PresetSettingButton.mouseClicked(d, d2, i);
        });
        if (containsPoint(d, d2)) {
            if (i == 0) {
                this.setting.setValue(EnumJsonConverter.getNextEnumValue((Enum) this.setting.getValue()));
                this.num2 = (this.num2 + 1) % this.num3;
            }
            if (i == 1) {
                int i2 = 0;
                int i3 = 0;
                while (true) {
                    if (i3 >= ((Enum[]) ((Enum) this.setting.getValue()).getDeclaringClass().getEnumConstants()).length) {
                        break;
                    }
                    Enum r0 = ((Enum[]) ((Enum) this.setting.getValue()).getDeclaringClass().getEnumConstants())[i3];
                    if (r0 == this.setting.getValue()) {
                        this.num2 = i2;
                        break;
                    } else {
                        if (!EnumSettingRegistry.isExperimentalEnumValue(r0)) {
                            i2++;
                        }
                        i3++;
                    }
                }
                this.flag = !this.flag;
                getPanel().recalculateLayout();
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void update(double d, double d2) {
        if (isHidden()) {
            return;
        }
        this.y = getPanel().getY() + this.num;
        this.x = getPanel().getX();
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        if (isHidden()) {
            this.progress.reset();
            return;
        }
        super.render(drawContext, matrixStack, d, d2);
        this.progress.animateToggle(true);
        if (this.flag) {
            this.hUDHelper_2.animate(this.num2 * getRowHeight());
            float f = this.hUDHelper_2.getValue();
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + getPadding() + 1, (((this.presetEnumSettingHelper.getY() + this.num) + f) + getRowHeight()) - 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 2, (((this.presetEnumSettingHelper.getY() + this.num) + (getRowHeight() * 2)) + f) - 0.5f, getUI().color.getValue());
        } else {
            this.hUDHelper_2.animate(-getRowHeight());
        }
        int i = this.flag ? 1 : 0;
        Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + getPadding() + i, this.presetEnumSettingHelper.getY() + this.num + 0.5f + i, (this.presetEnumSettingHelper.getX() + Math.max((this.presetEnumSettingHelper.getTotalWidth() * this.progress.getValue()) - 1.0f, 2.0f)) - i, ((this.presetEnumSettingHelper.getY() + this.num) + getRowHeight()) - 0.5f, getUI().color.getValue());
        if (this.flag) {
            this.arrayList.forEach(PresetSettingButton -> {
                PresetSettingButton.render(drawContext, matrixStack, d, d2);
            });
            if (UI.uI.line.getValue().booleanValue()) {
                Shape2DRenderer.drawOutline(matrixStack, this.presetEnumSettingHelper.getX() + getPadding(), this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 2, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 1.5f, getUI().color.getValue());
            }
        }
        String string2921 = new StringTemplate().arg(EnumJsonConverter.getEnumDisplayName((Enum) this.setting.getValue())).arg(this.setting.getName()).format("\u0001: \u0001");
        renderScrolling(matrixStack, string2921, () -> {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string2921, this.presetEnumSettingHelper.getX() + 4, ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - getAnimationOffset()) + this.num, getUI().textColor.getValue());
        });
        int i2 = getRowHeight();
        Iterator<PresetSettingButton> it = this.arrayList.iterator();
        while (it.hasNext()) {
            it.next().setOffsetY(i2);
            i2 += getRowHeight();
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return d > ((double) this.x) && d < ((double) (this.x + getPanel().getTotalWidth())) && d2 > ((double) this.y) && d2 < ((double) (this.y + 11));
    }

    @Override // me.mioclient.setting.SettingPresetSettingButton, me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        if (!isHidden()) {
            return this.flag ? getRowHeight() + (this.num3 * getRowHeight()) + 1 : super.getFontHeight();
        }
        this.hUDHelper_2.setImmediate(-getRowHeight());
        return 0;
    }

    @Override // me.mioclient.gui.GuiComponent
    public void init() {
        setSelectedIndex(((Enum) this.setting.getValue()).ordinal());
    }

    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.x;
    }

    public boolean isShown() {
        return this.flag;
    }

    public int get1296() {
        return this.num2;
    }

    public void setSelectedIndex(int i) {
        this.num2 = i;
    }
}
