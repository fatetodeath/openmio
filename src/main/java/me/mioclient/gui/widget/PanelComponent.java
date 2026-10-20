package me.mioclient.gui.widget;

import me.mioclient.api.ClientServices;
import me.mioclient.module.KeybindModule;
import me.mioclient.util.client.SoundPlayer;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.gui.widget.ModuleButton;
import me.mioclient.mode.client.CursorType;
import me.mioclient.util.anim.InterpolatedFloat;

import me.mioclient.api.ClientAccessor;
import me.mioclient.gui.GuiComponent;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;

public abstract class PanelComponent implements ClientAccessor, GuiComponent {
    public final ClickGuiPanel presetEnumSettingHelper;
    public int num;
    public int num2;
    public final InterpolatedFloat hUDHelper_2 = new InterpolatedFloat(4.0f);
    public boolean flag = false;

    public PanelComponent(ClickGuiPanel presetEnumSettingHelper, int i) {
        this.presetEnumSettingHelper = presetEnumSettingHelper;
        this.num = i;
    }

    public ClickGuiPanel getPanel() {
        return this.presetEnumSettingHelper;
    }

    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        SoundPlayer searchHelper4_42970;
        if (containsPoint(d, d2)) {
            if (!this.flag && getUI().hover.getValue().booleanValue() && !hasFlag(PanelComponentFlag.SILENT) && (searchHelper4_42970 = ClientServices.killstreakSoundManager.findSoundPlayer(getUI().sound2.getValue())) != null) {
                searchHelper4_42970.play(getUI().volume.getValue().floatValue());
            }
            this.flag = true;
            ClientServices.getClickGui().markTooltipActive();
            AbstractClickGuiScreen.cursorType = CursorType.POINTER;
        } else {
            this.flag = false;
        }
        if (getUI().bounce.getValue().booleanValue()) {
            this.hUDHelper_2.animate(containsPoint(d, d2) ? 1.0f : 0.0f);
        } else {
            this.hUDHelper_2.animate(0.0f);
        }
        GuiComponent.super.render(drawContext, matrixStack, d, d2);
    }

    public void setOffsetY(int i) {
        this.num = i;
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getOffsetY() {
        return this.num;
    }

    public float getAnimationOffset() {
        return this.hUDHelper_2.getValue();
    }

    public void mouseClicked(double d, double d2, int i) {
        SoundPlayer searchHelper4_42970;
        SoundPlayer searchHelper4_429702;
        if (!containsPoint(d, d2) || hasFlag(PanelComponentFlag.SILENT)) {
            return;
        }
        if (i == 0 && shouldPlayClickSound()) {
            if (!getUI().leftClick.getValue().booleanValue() || (searchHelper4_429702 = ClientServices.killstreakSoundManager.findSoundPlayer(getUI().sound3.getValue())) == null) {
                return;
            }
            searchHelper4_429702.play(getUI().volume3.getValue().floatValue());
            return;
        }
        if (i == 1 && getUI().rightClick.getValue().booleanValue() && (searchHelper4_42970 = ClientServices.killstreakSoundManager.findSoundPlayer(getUI().sound.getValue())) != null) {
            searchHelper4_42970.play(getUI().volume2.getValue().floatValue());
        }
    }

    public boolean containsPoint(double d, double d2) {
        return d > ((double) this.presetEnumSettingHelper.getX()) && d < ((double) (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth())) && d2 > ((double) (this.presetEnumSettingHelper.getY() + this.num)) && d2 < ((double) ((this.presetEnumSettingHelper.getY() + getFontHeight()) + this.num));
    }

    public int getPadding() {
        return hasFlag(PanelComponentFlag.PADDING_SHIFT) ? 2 : 1;
    }

    public boolean shouldPlayClickSound() {
        return ((this instanceof ModuleButton) && (((ModuleButton) this).getModule() instanceof KeybindModule)) ? false : true;
    }

    public void addFlags(PanelComponentFlag... mode_13Arr) {
        for (PanelComponentFlag mode_13 : mode_13Arr) {
            this.num2 |= mode_13.getBitmask();
        }
    }

    public boolean hasFlag(PanelComponentFlag mode_13) {
        return (this.num2 & mode_13.getBitmask()) == mode_13.getBitmask();
    }
}
