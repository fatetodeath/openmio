package me.mioclient.module.render;

import me.mioclient.event.events.render.HudRenderEvent;

import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.render.shader.BlurFramebuffer;

import java.awt.Color;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import me.mioclient.setting.NumberSetting;

public class Blur extends Module {
    public Setting<Integer> radius = add(new NumberSetting<>("Radius", 6, 1, 8));

    public Blur() {
        super("Blur", "Blurs the background in GUI's.", Category.RENDER, new String[0]);
    }

    @Listen(get219= RenderConstants.num5)
    public void onHudRender(HudRenderEvent matrixStackEvent_2) {
        do991(matrixStackEvent_2.getDrawContext());
    }

    public void do991(GuiGraphicsExtractor drawContext) {
        Screen screen = minecraftClient.gui.screen();
        if (screen instanceof AbstractClickGuiScreen) {
            return;
        }
        if ((minecraftClient.gui.screen() == null || (minecraftClient.gui.screen() instanceof ChatScreen)) ? false : true) {
            BlurFramebuffer.renderWithBlur(() -> {
                drawContext.fill(0, 0, minecraftClient.getWindow().getGuiScaledWidth(), minecraftClient.getWindow().getGuiScaledHeight(), new Color(0.0f, 0.0f, 0.0f, 1.0f).hashCode());
            }, getBlurRadius());
        }
    }

    public int getBlurRadius() {
        return this.radius.getValue().intValue();
    }
}