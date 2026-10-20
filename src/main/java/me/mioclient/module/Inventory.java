package me.mioclient.module;

import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.util.anim.ValueAnimator;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.render.RenderUtil;

import java.awt.Color;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

public class Inventory extends me.mioclient.module.HudModule {
    public Setting<Color> setting;
    public Setting<Color> setting2;
    public Setting<Boolean> setting3;
    public final ValueAnimator zoomHelper;

    public Inventory() {
        super("Inventory", new String[0]);
        this.setting = add(new ColorSetting("Background", new Color(10, 10, 10, 50)));
        this.setting2 = add(new ColorSetting("Outline", new Color(10, 10, 10, 100)));
        this.setting3 = add(new BooleanSetting("HideEmpty", true));
        this.zoomHelper = new ValueAnimator();
        ResizableHudElement size = new ResizableHudElement(this);
        size.enableScaling(this);
        setElement(size);
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        this.zoomHelper.toggle(!isEmpty(), 200L);
        float f = this.zoomHelper.getValue();
        if (!this.setting3.getValue().booleanValue()) {
            f = 1.0f;
        }
        Color color817 = ColorUtils.withAlphaPercent(this.setting.getValue(), (this.setting.getValue().getAlpha() / 255.0f) * f);
        Color color8172 = ColorUtils.withAlphaPercent(this.setting2.getValue(), (this.setting2.getValue().getAlpha() / 255.0f) * f);
        drawContext.fill(0, 0, (int) 162.0f, (int) 54.0f, color817.hashCode());
        drawContext.fill((int) (-1.0f), (int) (-1.0f), (int) 162.0f, (int) 54.0f, color8172.hashCode());
        for (int i = 0; i < 27; i++) {
            int i2 = (i % 9) * 18;
            int i3 = (i / 9) * 18;
            ItemStack itemStack = minecraftClient.player.getInventory().getItem(i + 9);
            drawContext.item(itemStack, i2, i3);
            drawContext.itemDecorations(minecraftClient.font, itemStack, i2, i3);
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < 27; i++) {
            if (!minecraftClient.player.getInventory().getItem(i + 9).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{162.0f, 54.0f};
    }
}