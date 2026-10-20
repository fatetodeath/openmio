package me.mioclient.module;

import me.mioclient.render.hud.ResizableHudElement;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

public class PlayerModel extends me.mioclient.module.HudModule {
    public static boolean flag = false;

    public PlayerModel() {
        super("PlayerModel", new String[0]);
        ResizableHudElement size = new ResizableHudElement(this);
        size.enableScaling(this);
        setElement(size);
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        LivingEntity livingEntity = minecraftClient.player;
        if (livingEntity == null) {
            return;
        }
        flag = true;
        InventoryScreen.extractEntityInInventoryFollowsMouse(drawContext, 5, 5, 45, 78, 30, 0.0625F, 0.0F, 0.0F, livingEntity);
        flag = false;
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{50.0f, 80.0f};
    }
}