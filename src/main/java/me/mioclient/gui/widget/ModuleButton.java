package me.mioclient.gui.widget;

import me.mioclient.api.ClientServices;
import me.mioclient.module.HudModule;
import me.mioclient.module.KeybindModule;
import me.mioclient.setting.SoundSettingRow;
import me.mioclient.setting.StringSettingRow;
import me.mioclient.util.client.SoundIdentifier;

import me.mioclient.gui.ClosableElement;
import me.mioclient.gui.GuiComponent;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.anim.ValueAnimator;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.setting.ColorPickerComponent;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import me.mioclient.api.Setting;
import me.mioclient.module.Module;
import me.mioclient.module.client.UI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import me.mioclient.setting.KeybindPresetSettingButton;
import me.mioclient.setting.PresetSettingPresetSettingButton;
import me.mioclient.setting.ProgressSettingPresetSettingButton;
import me.mioclient.manager.ScissorStackManager;
import me.mioclient.setting.SettingPresetSettingButton;
import me.mioclient.setting.StopwatchSettingPresetSettingButton;

public class ModuleButton extends PanelComponent implements ClosableElement {
    public final ArrayList<GuiComponent> arrayList;
    public final InterpolatedFloat hUDHelper_2;
    public final AnimationProgress progress;
    public final ValueAnimator zoomHelper;
    public final Module module;
    public int flashNum;
    public boolean flag;
    public float val;
    public static long num2 = 0;
    public static ModuleButton arrayListPresetHelper2 = null;
    public static boolean flag2 = false;

    public ModuleButton(Module module, ClickGuiPanel presetEnumSettingHelper, int i) {
        super(presetEnumSettingHelper, i);
        this.arrayList = new ArrayList<>();
        this.hUDHelper_2 = new InterpolatedFloat((Supplier<Float>) () -> {
            return Float.valueOf(getUI().animSpeed.getValue().floatValue() * 2.0f);
        }, true);
        this.progress = new AnimationProgress((Supplier<Float>) () -> {
            return Float.valueOf(2.0f * UI.uI.animSpeed.getValue().floatValue());
        }, true);
        this.zoomHelper = new ValueAnimator();
        this.flashNum = 0;
        this.module = module;
        int i2 = this.num + getRowHeight();
        Iterator<Setting<?>> it = module.getRegistry().iterator();
        while (it.hasNext()) {
            SettingPresetSettingButton<?> settingPresetSettingButton651 = getSettingPresetSettingButton651(presetEnumSettingHelper, it.next());
            if (settingPresetSettingButton651 != null) {
                settingPresetSettingButton651.setOffsetY(i2);
                settingPresetSettingButton651.addFlags(PanelComponentFlag.PADDING_SHIFT);
                this.arrayList.add(settingPresetSettingButton651);
            }
            i2 += 11;
        }
        if (!(module instanceof HudModule) && !(module instanceof KeybindModule)) {
            this.arrayList.add(new KeybindPresetSettingButton(i2, this));
        }
        this.hUDHelper_2.setImmediate(calculateExpandedHeight());
    }

    @Nullable
    public SettingPresetSettingButton<?> getSettingPresetSettingButton651(ClickGuiPanel presetEnumSettingHelper, Setting<?> setting) {
        Object object2324 = setting.getDefaultValue();
        Objects.requireNonNull(object2324);
        int typeSwitchIndex = object2324 instanceof Boolean ? 0
                : object2324 instanceof Number ? 1
                : object2324 instanceof Color ? 2
                : object2324 instanceof String ? 3
                : object2324 instanceof Enum ? 4
                : object2324 instanceof SoundIdentifier ? 5
                : -1;
        switch (typeSwitchIndex) {
            case 0:
                return new ProgressSettingPresetSettingButton(presetEnumSettingHelper, this, setting);
            case 1:
                return new StopwatchSettingPresetSettingButton(presetEnumSettingHelper, this, setting);
            case 2:
                return new ColorPickerComponent(presetEnumSettingHelper, this, setting);
            case 3:
                return new StringSettingRow(presetEnumSettingHelper, this, setting);
            case 4:
                return new PresetSettingPresetSettingButton(presetEnumSettingHelper, this, setting);
            case 5:
                return new SoundSettingRow(presetEnumSettingHelper, this, setting);
            default:
                return null;
        }
    }

    public Module getModule() {
        return this.module;
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        return (int) this.val;
    }

    public int calculateExpandedHeight() {
        int i = getRowHeight();
        if (this.flag) {
            Iterator<GuiComponent> it = this.arrayList.iterator();
            while (it.hasNext()) {
                i += it.next().getFontHeight();
            }
        }
        return i;
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void setOffsetY(int i) {
        this.num = i;
        int i2 = this.num + getRowHeight();
        Iterator<GuiComponent> it = this.arrayList.iterator();
        while (it.hasNext()) {
            GuiComponent next = it.next();
            next.setOffsetY(i2);
            i2 += next.getFontHeight();
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void update(double d, double d2) {
        this.hUDHelper_2.animate(calculateExpandedHeight());
        this.val = this.hUDHelper_2.getValue();
        this.arrayList.forEach(presetHelper_5 -> {
            presetHelper_5.update(d, d2);
        });
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void mouseClicked(double d, double d2, int i) {
        super.mouseClicked(d, d2, i);
        if (containsPoint(d, d2) && i == 0 && shouldPlayClickSound()) {
            this.module.toggle();
        }
        if (containsPoint(d, d2) && i == 1) {
            this.flag = !this.flag;
            if (this.flag && !ClientServices.getClickGui().list.contains(this)) {
                ClientServices.getClickGui().list.add(this);
            }
            this.presetEnumSettingHelper.recalculateLayout();
            if (!ClientServices.getClickGui().getSearchField().getValue().isEmpty()) {
                flashHighlight();
            }
        }
        this.arrayList.forEach(presetHelper_5 -> {
            presetHelper_5.mouseClicked(d, d2, i);
        });
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseReleased(double d, double d2, int i) {
        this.arrayList.forEach(presetHelper_5 -> {
            presetHelper_5.mouseReleased(d, d2, i);
        });
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        this.arrayList.forEach(presetHelper_5 -> {
            presetHelper_5.keyPressed(i);
        });
    }

    @Override // me.mioclient.gui.GuiComponent
    public void mouseScrolled(double d, double d2, double d3) {
        Iterator<GuiComponent> it = this.arrayList.iterator();
        while (it.hasNext()) {
            it.next().mouseScrolled(d, d2, d3);
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        super.render(drawContext, matrixStack, d, d2);
        this.progress.animateToggle(this.module.isToggled());
        float f = this.progress.getValue();
        float f2 = getAnimationOffset();
        if (this.module.isToggled() || f > 0.01d) {
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 1.0f + ((this.presetEnumSettingHelper.getTotalWidth() - 1.0f) * f), this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1.0f, ((this.presetEnumSettingHelper.getY() + this.num) + getRowHeight()) - 0.5f, getUI().bgButton.getValue());
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 1.0f, this.presetEnumSettingHelper.getY() + this.num + 0.5f, this.presetEnumSettingHelper.getX() + ((this.presetEnumSettingHelper.getTotalWidth() - 1.0f) * f), ((this.presetEnumSettingHelper.getY() + this.num) + getRowHeight()) - 0.5f, getUI().bgEnabled.getValue());
        } else {
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 1.0f, this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1.0f, ((this.presetEnumSettingHelper.getY() + this.num) + getRowHeight()) - 0.5f, getUI().bgButton.getValue());
        }
        if (this.flashNum > 0) {
            float f3 = this.zoomHelper.getValue();
            Shape2DRenderer.drawRect(matrixStack, this.presetEnumSettingHelper.getX() + 1.0f, this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1.0f, ((this.presetEnumSettingHelper.getY() + this.num) + getRowHeight()) - 0.5f, ColorUtils.argbFloatsToInt(1.0f, 0.0f, 0.0f, f3));
            if (f3 == 1.0f || f3 == 0.0f) {
                this.zoomHelper.animateTo((f3 + 1.0f) % 2.0f, 150L);
            }
            if (f3 == 0.0f) {
                this.flashNum = (this.flashNum + 1) % 4;
            }
        }
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, this.module.getName(), this.presetEnumSettingHelper.getX() + UI.uI.modulePadding.getValue().intValue(), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - f2) + this.num, this.module.isToggled() ? getUI().enabledColor.getValue() : getUI().textColor.getValue());
        if (getUI().gear.getValue().booleanValue() && !this.arrayList.isEmpty()) {
            String str = this.flag ? "-" : "+";
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, str, ((this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 4) - ClientTextRenderer.INSTANCE.getStringWidth(str), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - f2) + this.num, getUI().textColor.getValue());
        }
        net.minecraft.network.chat.MutableComponent empty = Component.empty();
        if (this.module.isWip()) {
            empty.append("beta ");
        }
        if (getUI().binds.getValue().booleanValue() && this.module.getKeybind().getKey() != -1) {
            empty.append("[").append(this.module.getKeybind().getBindString().toUpperCase(Locale.ROOT)).append("]");
        }
        if (!empty.getString().isEmpty()) {
            ClientTextRenderer.INSTANCE.drawScaledTextShadowObj(drawContext, empty, this.presetEnumSettingHelper.getX() + ClientTextRenderer.INSTANCE.getStringWidth(this.module.getName()) + getUI().modulePadding.getValue().intValue(), ((this.presetEnumSettingHelper.getY() + getTextOffsetY()) - f2) + this.num + 1.0f, 0.5f, getUI().textColor.getValue());
        }
        matrixStack.pushPose();
        if (this.val > getRowHeight() || this.flag) {
            int clamp = (int) Mth.clamp(this.val, getRowHeight(), (this.presetEnumSettingHelper.getFontHeight() * this.presetEnumSettingHelper.getAnimationProgress()) - this.num);
            Shape2DRenderer.resetShadowOffset();
            ScissorStackManager.pushClipRegion(this.presetEnumSettingHelper.getX(), this.presetEnumSettingHelper.getY() + this.num, this.presetEnumSettingHelper.getTotalWidth(), clamp);
            Iterator<GuiComponent> it = this.arrayList.iterator();
            while (it.hasNext()) {
                it.next().render(drawContext, matrixStack, d, d2);
            }
            Shape2DRenderer.resetShadowOffset();
            ScissorStackManager.popClipRegion();
        }
        matrixStack.popPose();
        if (getUI().descriptions.getValue().booleanValue() && containsPoint(d, d2)) {
            if (getUI().delay.getValue().intValue() == 0) {
                ClientServices.getClickGui().setTooltipText(this.module.getDescription());
                return;
            }
            if (getUI().constantReset.getValue().booleanValue()) {
                if (arrayListPresetHelper2 != this) {
                    arrayListPresetHelper2 = this;
                    flag2 = false;
                    num2 = System.currentTimeMillis();
                }
            } else if (arrayListPresetHelper2 == null) {
                arrayListPresetHelper2 = this;
                num2 = System.currentTimeMillis();
                flag2 = false;
            } else if (arrayListPresetHelper2 != this) {
                arrayListPresetHelper2 = this;
                if (!flag2) {
                    num2 = System.currentTimeMillis();
                }
            }
            if (flag2) {
                ClientServices.getClickGui().setTooltipText(this.module.getDescription());
            } else if (System.currentTimeMillis() >= num2 + getUI().delay.getValue().intValue()) {
                flag2 = true;
                ClientServices.getClickGui().setTooltipText(this.module.getDescription());
            }
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyTyped(char c) {
        this.arrayList.forEach(presetHelper_5 -> {
            presetHelper_5.keyTyped(c);
        });
    }

    @Override // me.mioclient.gui.GuiComponent
    public void init() {
        Iterator<GuiComponent> it = this.arrayList.iterator();
        while (it.hasNext()) {
            it.next().init();
        }
    }

    @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public boolean containsPoint(double d, double d2) {
        return d > ((double) this.presetEnumSettingHelper.getX()) && d < ((double) (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth())) && d2 > ((double) (this.presetEnumSettingHelper.getY() + this.num)) && d2 < ((double) ((this.presetEnumSettingHelper.getY() + getRowHeight()) + this.num));
    }

    public static void resetTooltipState() {
        num2 = 0L;
        arrayListPresetHelper2 = null;
        flag2 = false;
    }

    @Override // me.mioclient.gui.ClosableElement
    public boolean isClosed() {
        return this.val <= ((float) (getRowHeight() + 1)) && !this.flag;
    }

    public void flashHighlight() {
        this.zoomHelper.setImmediate(0.0f);
        this.zoomHelper.animateTo(1.0f, 150L);
        this.flashNum = 1;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Objects.equals(this.module, ((ModuleButton) obj).module);
    }

    public int hashCode() {
        return Objects.hashCode(this.module);
    }
}
