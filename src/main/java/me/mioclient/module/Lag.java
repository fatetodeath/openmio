package me.mioclient.module;

import me.mioclient.api.ClientServices;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.util.anim.AnimationProgress;

import java.awt.Color;
import me.mioclient.setting.ColorSetting;
import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class Lag extends me.mioclient.module.HudModule {
    public Setting<Color> setting;
    public final AnimationProgress progress;

    public Lag() {
        super("Lag'O'Meter", "lag", "lagometer");
        this.setting = add(new ColorSetting("Color", Color.gray));
        this.progress = new AnimationProgress(1.0f);
        setElement(new ResizableHudElement(this));
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        String string773 = getBindString();
        this.progress.animateToggle(string773 != null);
        if (string773 == null) {
            return;
        }
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string773, 0.0f, (1.0f - this.progress.getValue()) * (-ClientTextRenderer.INSTANCE.getFontHeight()), this.setting.getValue());
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        String string773 = getBindString();
        return string773 == null ? new float[]{0.0f, 0.0f} : new float[]{ClientTextRenderer.INSTANCE.getStringWidth(string773), ClientTextRenderer.INSTANCE.getFontHeight() + 1};
    }

    public String getBindString() {
        if ((Math.max(System.currentTimeMillis() - ClientServices.holeSnapRenderHelper.getLastPacketTime(), 0L) <= 1000 || minecraftClient.hasSingleplayerServer()) && !isInHudEditor()) {
            return null;
        }
        return "The server is not responding for %.1fs".formatted(Float.valueOf(((float) (System.currentTimeMillis() - ClientServices.holeSnapRenderHelper.getLastPacketTime())) / 1000.0f));
    }
}