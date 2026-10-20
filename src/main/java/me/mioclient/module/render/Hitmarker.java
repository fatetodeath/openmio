package me.mioclient.module.render;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.render.HudRenderEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.setting.SoundIdentifierSetting;
import me.mioclient.util.client.SoundIdentifier;

import me.mioclient.util.client.InteractPacketReader;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.render.shape.Shape2DRenderer;

import java.awt.Color;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Hitmarker extends Module {
    public Setting<Boolean> draw = add(new BooleanSetting("Draw", true).setVisible());
    public Setting<Float> length = add(new NumberSetting<>("Length", 3.0f, 0.1f, 10.0f, v -> this.draw.isShown()));
    public Setting<Float> time = add(new NumberSetting<>("Time", 1.0f, 0.5f, 3.0f, v -> this.draw.isShown()).setSuffix("s"));
    public Setting<Float> fadeTime = add(new NumberSetting<>("FadeTime", 0.5f, 0.5f, 3.0f, v -> this.draw.isShown()).setSuffix("s"));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-1581409793, true), v -> this.draw.isShown()));
    public Setting<Color> outlineColor = add(new ColorSetting("OutlineColor", new Color(-16777216, true), v -> this.draw.isShown()));
    public Setting<Boolean> sound2 = add(new BooleanSetting("Sound", false).setVisible());
    public Setting<SoundIdentifier> sound = add(new SoundIdentifierSetting("Sound", new SoundIdentifier("system", "neverlose")).requiresSettings(this.sound2));
    public Setting<Float> volume = add(new NumberSetting<>("Volume", 0.5f, 0.0f, 1.0f, v -> this.sound2.isShown()));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", false).setDependent());
    public Setting<Boolean> animals = add(new BooleanSetting("Animals", false, v -> this.targets.isShown()));
    public Setting<Boolean> hostiles = add(new BooleanSetting("Hostiles", false, v -> this.targets.isShown()));
    public Setting<Boolean> players = add(new BooleanSetting("Players", true, v -> this.targets.isShown()));
    public Setting<Boolean> crystals = add(new BooleanSetting("Crystals", false, v -> this.targets.isShown()));
    public static Crosshair crosshair = (Crosshair) ClientServices.moduleInjector.getModule(Crosshair.class);
    public long num;
    public int num2;
    public boolean flag;

    public Hitmarker() {
        super("Hitmarker", "Marks your screen when you hit entities.", Category.RENDER, new String[0]);
        this.num = -1L;
        this.num2 = 255;
        this.flag = false;
        setDrawn(false);
        this.sound.setConfigName("Type");
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.num = -1L;
        this.num2 = 255;
        this.flag = false;
    }

    @Listen
    public void onHudRender(HudRenderEvent matrixStackEvent_2) {
        if (isNotInGame() || this.num == -1) {
            return;
        }
        if (this.flag) {
            this.flag = false;
            if (this.sound2.getValue().booleanValue()) {
                ClientServices.killstreakSoundManager.playSound(this.sound.getValue(), this.volume.getValue().floatValue());
            }
        }
        long currentTimeMillis = System.currentTimeMillis() - this.num;
        if (currentTimeMillis > this.time.getValue().floatValue() * 1000.0f) {
            this.num = -1L;
            return;
        }
        updateFadeAlpha(currentTimeMillis);
        if (this.num2 <= 0 || !this.draw.getValue().booleanValue()) {
            return;
        }
        float scaledWidth = minecraftClient.getWindow().getGuiScaledWidth() / 2.0f;
        float scaledHeight = (minecraftClient.getWindow().getGuiScaledHeight() / 2.0f) - 0.5f;
        if (crosshair.isToggled()) {
            scaledHeight = (float) (scaledHeight + 0.5d);
        } else {
            scaledWidth = (float) (scaledWidth - 0.5d);
        }
        Color color816 = ColorUtils.withAlpha(this.outlineColor.getValue(), this.num2);
        Color color8162 = ColorUtils.withAlpha(this.color.getValue(), this.num2);
        float intBitsToFloat = 2.0f + this.length.getValue().floatValue();
        int i = 0;
        while (i < 2) {
            Color color = i == 0 ? color816 : color8162;
            matrixStackEvent_2.getMatrixStack().pushPose();
            if (crosshair.isToggled()) {
                matrixStackEvent_2.getMatrixStack().translate((-0.6d), 0.0d, 0.0d);
            }
            drawHitmarkerLine(matrixStackEvent_2.getMatrixStack(), color, scaledWidth - intBitsToFloat, scaledHeight - intBitsToFloat, scaledWidth - 2.0f, scaledHeight - 2.0f);
            drawHitmarkerLine(matrixStackEvent_2.getMatrixStack(), color, scaledWidth - intBitsToFloat, scaledHeight + intBitsToFloat, scaledWidth - 2.0f, scaledHeight + 2.0f);
            drawHitmarkerLine(matrixStackEvent_2.getMatrixStack(), color, scaledWidth + 2.0f, scaledHeight - 2.0f, scaledWidth + intBitsToFloat, scaledHeight - intBitsToFloat);
            drawHitmarkerLine(matrixStackEvent_2.getMatrixStack(), color, scaledWidth + 2.0f, scaledHeight + 2.0f, scaledWidth + intBitsToFloat, scaledHeight + intBitsToFloat);
            matrixStackEvent_2.getMatrixStack().popPose();
            i++;
        }
    }

    @Listen(get219= RenderConstants.num4)
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (isNotInGame() || sendImmediatelyEvent.isCancelled()) {
            return;
        }
        if (!(sendImmediatelyEvent.getPacket() instanceof ServerboundInteractPacket interactPacket)) {
            return;
        }
        Entity entityById = InteractPacketReader.resolvePacketEntity(interactPacket);
        if (entityById == null || entityById.level() == null || entityById == minecraftClient.player || !entityById.level().isClientSide() || !shouldTrackEntity(entityById)) {
            return;
        }
        this.num = System.currentTimeMillis();
        this.num2 = 255;
        this.flag = true;
    }

    public void updateFadeAlpha(long j) {
        if (j < (this.time.getValue().floatValue() * 1000.0f) - (this.fadeTime.getValue().floatValue() * 1000.0f)) {
            this.num2 = 255;
            return;
        }
        this.num2 = (int) (this.num2 - ((255.0f / this.fadeTime.getValue().floatValue()) * ClientServices.hitmarkerHelper.getFrameDelta(1.0f)));
        this.num2 = Mth.clamp(this.num2, 0, 255);
    }

    public boolean shouldTrackEntity(Entity entity) {
        return (this.animals.getValue().booleanValue() && (entity instanceof Animal)) || (this.hostiles.getValue().booleanValue() && (entity instanceof Monster)) || ((this.players.getValue().booleanValue() && (entity instanceof Player)) || (this.crystals.getValue().booleanValue() && (entity instanceof EndCrystal)));
    }

    public static void drawHitmarkerLine(PoseStack matrixStack, Color color, float f, float f2, float f3, float f4) {
        float dx = f3 - f;
        float dy = f4 - f2;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)));
        if (steps <= 0) {
            Shape2DRenderer.drawRect(matrixStack, f, f2, f + 1.0f, f2 + 1.0f, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / (float) steps;
            float x = f + dx * t;
            float y = f2 + dy * t;
            Shape2DRenderer.drawRect(matrixStack, x, y, x + 1.0f, y + 1.0f, color);
        }
    }
}