package me.mioclient.module;

import me.mioclient.render.hud.ResizableHudElement;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;

public class Chat extends me.mioclient.module.HudModule {
    public Chat() {
        super("Chat", new String[0]);
        setElement(new ResizableHudElement(this));
    }

    public float getChatYOffset() {
        if (this.moduleListSearchHelper4.getMeasuredSize() == null) {
            return 0.0f;
        }
        return ((this.moduleListSearchHelper4.getY() - (minecraftClient.getWindow().getGuiScaledHeight() - 40.0f)) / getScale()) + getSpeed();
    }

    public float getChatXOffset() {
        if (this.moduleListSearchHelper4.getMeasuredSize() == null) {
            return 0.0f;
        }
        return this.moduleListSearchHelper4.getX() / getScale();
    }

    public float getScale() {
        return minecraftClient.options.chatScale().get().floatValue();
    }

    public float getSpeed() {
        return ChatComponent.getHeight(minecraftClient.options.chatHeightUnfocused().get().floatValue());
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        super.renderContent(drawContext);
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{(minecraftClient.options.chatWidth().get().floatValue() * 280.0f) + 40.0f + (12.0f * getScale()), getSpeed() * getScale()};
    }
}