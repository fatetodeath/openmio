package me.mioclient.module;

import me.mioclient.event.events.render.HudRenderEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.math.MathConstants;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.client.Fonts;
import me.mioclient.module.client.HUD;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Armor extends me.mioclient.module.HudModule {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving hud null (NPE). */
    private static HUD hud;

    public static HUD getHud() {
        if (hud == null && ClientServices.moduleInjector != null) {
            try {
                hud = (HUD) ClientServices.moduleInjector.getModule(HUD.class);
            } catch (Exception e) {
            }
        }
        return hud;
    }
    public Setting<Boolean> setting;
    public Setting<Boolean> setting2;
    public Setting<Boolean> setting3;

    
    class Inner extends ResizableHudElement {
        public Inner(Armor armor, me.mioclient.module.HudModule moduleList) {
            super(moduleList);
        }

        @Override // me.mioclient.render.hud.DraggableHudElement
        public boolean isVisible() {
            return false;
        }
    }

    public Armor() {
        super("Armor", new String[0]);
        this.setting = add(new BooleanSetting("Durability", true));
        this.setting2 = add(new BooleanSetting("Percentage", true));
        this.setting3 = add(new BooleanSetting("BarColor", true));
        setElement(new Inner(this, this));
    }

    @Listen(get219= 1)
    public void onHudRender(HudRenderEvent matrixStackEvent_2) {
        HUD hud_lcl = getHud();
        if ((hud_lcl != null && hud_lcl.isToggled()) || isInHudEditor()) {
            int scaledWindowWidth = matrixStackEvent_2.getDrawContext().guiWidth();
            int scaledWindowHeight = matrixStackEvent_2.getDrawContext().guiHeight();
            int i = 15;
            net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
            for (int i2 = 3; i2 >= 0; i2--) {
                ItemStack itemStack = minecraftClient.player.getItemBySlot(slots[i2]);
                if (!itemStack.isEmpty()) {
                    drawArmorItem(matrixStackEvent_2.getDrawContext(), itemStack, (scaledWindowWidth / 2) + i, scaledWindowHeight - getHudOffsetY());
                    if (itemStack.isDamageableItem() && this.setting.getValue().booleanValue()) {
                        String format = String.format("%d%s", Integer.valueOf(ExplosionDamageCalculator.getClampedDurabilityPercent(itemStack)), "%");
                        float intBitsToFloat = Fonts.fonts.isToggled() ? 0.7f : 0.65f;
                        if (!this.setting2.getValue().booleanValue()) {
                            format = format.substring(0, format.length() - 1);
                            intBitsToFloat = 1.0f;
                        }
                        float intBitsToFloat2 = ((scaledWindowHeight - getHudOffsetY()) - 3.8f) - (this.setting2.getValue().booleanValue() ? 0 : 2);
                        ClientTextRenderer.INSTANCE.drawScaledTextShadow(matrixStackEvent_2.getDrawContext(), format, ((((scaledWindowWidth / 2.0f) + i) + 9.0f) - ((ClientTextRenderer.INSTANCE.getStringWidth(format) * 0.5f) * intBitsToFloat)) - intBitsToFloat, intBitsToFloat2, intBitsToFloat, this.setting3.getValue().booleanValue() ? new Color(itemStack.getBarColor(), false) : getHudColor(intBitsToFloat2));
                    }
                    i += 18;
                }
            }
        }
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        super.renderContent(drawContext);
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{0.0f, 0.0f};
    }

    public void drawArmorItem(GuiGraphicsExtractor drawContext, ItemStack itemStack, int i, int i2) {
        drawContext.item(itemStack, i, i2);
        drawContext.itemDecorations(minecraftClient.font, itemStack, i, i2);
    }

    public int getHudOffsetY() {
        int i;
        if ((minecraftClient.player.isUnderWater() || minecraftClient.player.getAirSupply() != minecraftClient.player.getMaxAirSupply()) && minecraftClient.player.getAirSupply() > 0 && !minecraftClient.player.isCreative()) {
            i = 65;
        } else {
            if (minecraftClient.player.isPassenger()) {
                if (minecraftClient.player.getVehicle() instanceof LivingEntity) {
                    i = (int) (MathConstants.num + (Math.ceil((((LivingEntity) minecraftClient.player.getVehicle()).getMaxHealth() - 1.0f) / 20.0f) * 10.0d));
                }
            }
            i = 55 - (minecraftClient.player.isCreative() ? 17 : 0);
        }
        return i;
    }
}