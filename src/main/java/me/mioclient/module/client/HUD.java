package me.mioclient.module.client;

import me.mioclient.event.events.render.HudRenderEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.gui.screen.HudEditorScreen;
import me.mioclient.mode.client.MixinTitleScreenMode;
import me.mioclient.mode.client.ModuleListMode;
import me.mioclient.render.hud.DraggableHudElement;
import me.mioclient.util.anim.InterpolatedFloat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.exploit.AntiLevitation;
import me.mioclient.module.render.NoRender;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;

public class HUD extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving antiLevitation/norender null. */
    private static AntiLevitation antiLevitation;
    private static NoRender norender;

    public static AntiLevitation getAntiLevitation() {
        if (antiLevitation == null && ClientServices.moduleInjector != null) {
            try {
                antiLevitation = (AntiLevitation) ClientServices.moduleInjector.getModule(AntiLevitation.class);
            } catch (Exception e) {
            }
        }
        return antiLevitation;
    }

    public static NoRender getNorender() {
        if (norender == null && ClientServices.moduleInjector != null) {
            try {
                norender = (NoRender) ClientServices.moduleInjector.getModule(NoRender.class);
            } catch (Exception e) {
            }
        }
        return norender;
    }
    public final Predicate<Boolean> predicate;
    public Setting<Boolean> setting;
    public Setting<HUDMode> setting2;
    public Setting<Boolean> setting3;
    public Setting<Integer> setting4;
    public Setting<Integer> setting5;
    public Setting<Boolean> setting6;
    public Setting<Color> setting7;
    public Setting<MixinTitleScreenMode> setting8;
    public Setting<Integer> setting9;
    public Setting<Color> setting10;
    public Setting<Integer> setting11;
    public Setting<Integer> setting12;
    public Setting<Boolean> setting13;
    public final List<Setting<Color>> list;
    public final InterpolatedFloat hUDHelper_2;
    public final InterpolatedFloat hUDHelper_22;

    
    public enum HUDMode implements NamedValue {
        KEEP("Keep"),
        MOVE("Move"),
        HIDE("Hide");

        public final String name;

        HUDMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum ModuleListMode implements NamedValue {
        ALPHABET("Alphabet"),
        LENGTH("Length");

        public final String name;

        ModuleListMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public HUD() {
        super("HUD", "Draws useful HUD elements on your screen.", Category.CLIENT, new String[0]);
        this.predicate = bool -> {
            Screen screen = minecraftClient.gui.screen();
            return !(screen instanceof AbstractClickGuiScreen) || ((AbstractClickGuiScreen) screen).getParentScreen() == null;
        };
        this.setting = add(new BooleanSetting("HudEditor", false, this.predicate));
        this.setting2 = add(new EnumSetting("Icons", HUDMode.MOVE));
        this.setting3 = add(new BooleanSetting("SmoothWidth", false));
        this.setting4 = add(new NumberSetting("SafeX", 1, 0, 20));
        this.setting5 = add(new NumberSetting("SafeY", 1, 0, 10));
        this.setting6 = add(new BooleanSetting("Colors", true).setDependent());
        this.setting7 = add(new ColorSetting("Color", new Color(189, 153, 255, 255)).setDisabled().requiresSettings(this.setting6));
        this.setting8 = add(new EnumSetting("Fade", MixinTitleScreenMode.NONE, mixinTitleScreenMode -> {
            return this.setting6.isShown();
        }));
        this.setting9 = add(new NumberSetting("FadeOffset", 5, 1, 30, num -> {
            return (this.setting8.getValue() != MixinTitleScreenMode.NONE || ((ColorSetting) this.setting7).isRainbow()) && this.setting6.isShown();
        }));
        this.setting10 = add(new ColorSetting("SecondColor", new Color(105, 86, 143, 255), color -> {
            return this.setting8.getValue() == MixinTitleScreenMode.PULSE && this.setting6.isShown();
        }).setDisabled());
        this.setting11 = add(new NumberSetting("ColorAmount", 5, 2, 9, num2 -> {
            return this.setting8.getValue() == MixinTitleScreenMode.RAW && this.setting6.isShown();
        }));
        this.setting12 = add(new NumberSetting("FadeSpeed", 50, 20, 100, num3 -> {
            return this.setting8.getValue() == MixinTitleScreenMode.RAW && this.setting6.isShown();
        }));
        this.setting13 = add(new BooleanSetting("MixColors", true, bool2 -> {
            return this.setting8.getValue() == MixinTitleScreenMode.RAW && this.setting6.isShown();
        }));
        this.list = new ArrayList(this.setting11.getMaxValue().intValue());
        this.hUDHelper_2 = new InterpolatedFloat(2.0f);
        this.hUDHelper_22 = new InterpolatedFloat(1.5f);
        this.setting.setChangeCallback(() -> {
            if (this.setting.getValue().booleanValue()) {
                this.setting.setValue(false);
                if (isNotInGame()) {
                    return;
                }
                minecraftClient.gui.setScreen(ClientServices.getHud());
            }
        });
        setDrawn(false);
        setToggled(true);
        for (int i = 1; i <= this.setting11.getMaxValue().intValue(); i++) {
            int i2 = i;
            this.list.add(add(new ColorSetting(new StringTemplate().arg(i).format("Color\u0001"), Color.white, color2 -> {
                return this.setting8.getValue() == MixinTitleScreenMode.RAW && this.setting6.isShown() && i2 <= this.setting11.getValue().intValue();
            }).setDisabled()));
        }
    }

    @Listen
    public void onHudRender(HudRenderEvent matrixStackEvent_2) {
        int i = 0;
        if (this.setting2.getValue() == HUDMode.MOVE && !(minecraftClient.gui.screen() instanceof InventoryScreen)) {
            AntiLevitation antiLevitation_lcl = getAntiLevitation();
            NoRender norender_lcl = getNorender();
            for (MobEffectInstance statusEffectInstance : minecraftClient.player.getActiveEffects()) {
                Holder effectType = statusEffectInstance.getEffect();
                if (antiLevitation_lcl == null || !antiLevitation_lcl.isToggled() || (effectType != MobEffects.SLOW_FALLING && effectType != MobEffects.LEVITATION)) {
                    if (norender_lcl == null || !norender_lcl.isToggled() || !norender_lcl.darkness.getValue().booleanValue() || effectType != MobEffects.DARKNESS) {
                        if (i == 0) {
                            i++;
                        }
                        if (!((MobEffect) statusEffectInstance.getEffect().value()).isBeneficial()) {
                            i = 2;
                        }
                    }
                }
            }
        }
        if (minecraftClient.gui.screen() instanceof InventoryScreen) {
            i = 0;
        }
        this.hUDHelper_2.animate((24 * i) + 4);
        this.hUDHelper_2.getValue();
        float f = this.hUDHelper_22.getValue();
        float scaledWidth = minecraftClient.getWindow().getGuiScaledWidth();
        float scaledHeight = minecraftClient.getWindow().getGuiScaledHeight();
        if (f > 1.0f && !(minecraftClient.gui.screen() instanceof ChatScreen)) {
            matrixStackEvent_2.getDrawContext().fill(2, (int) ((scaledHeight - 2.0f) - f), (int) (scaledWidth - 2.0f), (int) (scaledHeight - 2.0f), (int) (minecraftClient.options.textBackgroundOpacity().get() * 255) << 24);
        }
        DraggableHudElement.flag = true;
        if (!(minecraftClient.gui.screen() instanceof HudEditorScreen)) {
            ClientServices.getHud().renderAll(matrixStackEvent_2.getDrawContext(), 0.0d, 0.0d);
        }
        DraggableHudElement.flag = false;
    }

    public float getBottomOffsetTarget() {
        return this.hUDHelper_22.val;
    }

    public float getStatusEffectOffset() {
        return this.hUDHelper_2.val == 4.0f ? 1.0f : this.hUDHelper_2.val;
    }

    public void animateBottomOffset(float f) {
        this.hUDHelper_22.animate(f);
    }

    public int getSafeAreaX() {
        return this.setting4.getValue().intValue();
    }

    public int getSafeAreaY() {
        return this.setting5.getValue().intValue();
    }

    public void setSafeAreaX(int i) {
        this.setting4.setValue(Integer.valueOf(i));
    }

    public void setSafeAreaY(int i) {
        this.setting5.setValue(Integer.valueOf(i));
    }
}