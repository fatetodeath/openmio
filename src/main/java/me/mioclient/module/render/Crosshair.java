package me.mioclient.module.render;

import me.mioclient.event.events.render.HudRenderEvent;

import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;

import java.awt.Color;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Crosshair extends Module {
    public Setting<Boolean> thirdPerson = add(new BooleanSetting("ThirdPerson", true));
    public Setting<Boolean> dot = add(new BooleanSetting("Dot", true));
    public Setting<Boolean> shadow = add(new BooleanSetting("Shadow", false).setVisible());
    public Setting<Integer> distance = add(new NumberSetting<>("Distance", 1, 0, 3, v -> this.shadow.isShown()));
    public Setting<Boolean> dynamic = add(new BooleanSetting("Dynamic", true).setVisible());
    public Setting<Boolean> smooth = add(new BooleanSetting("Smooth", true, v -> this.dynamic.isShown()));
    public Setting<Float> amplitude = add(new NumberSetting<>("Amplitude", 2.0f, 0.1f, 8.0f, v -> this.dynamic.isShown()));
    public Setting<Integer> gap = add(new NumberSetting<>("Gap", 2, 0, 16));
    public Setting<Integer> length = add(new NumberSetting<>("Length", 2, 0, 16));
    public Setting<Integer> width = add(new NumberSetting<>("Width", 1, 1, 5));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-2828, true)).setThemeSync(true));
    public final AnimationProgress progress;

    public Crosshair() {
        super("Crosshair", "Allows you to customize your crosshair.", Category.RENDER, new String[0]);
        this.progress = new AnimationProgress(2.0f, true);
        setDrawn(false);
    }

    @Listen
    public void onHudRender(HudRenderEvent matrixStackEvent_2) {
        if (isNotInGame()) {
            return;
        }
        if (minecraftClient.options.getCameraType() != CameraType.FIRST_PERSON || this.thirdPerson.getValue().booleanValue()) {
            float intValue = this.gap.getValue().intValue();
            if (this.dynamic.getValue().booleanValue()) {
                net.minecraft.world.entity.player.Input presses = minecraftClient.player.input.keyPresses;
                boolean moving = presses.forward() || presses.backward() || presses.left() || presses.right() || presses.jump();
                if (this.smooth.getValue().booleanValue()) {
                    this.progress.animateToggle(moving);
                } else {
                    this.progress.setToggledImmediate(moving);
                }
                intValue += this.amplitude.getValue().floatValue() * this.progress.getValue();
            }
            float scaleFactor = (float) minecraftClient.getWindow().getGuiScale();
            matrixStackEvent_2.getMatrixStack().pushPose();
            matrixStackEvent_2.getMatrixStack().scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f / scaleFactor);
            if (this.shadow.getValue().booleanValue()) {
                PoseStack matrixStack472 = matrixStackEvent_2.getMatrixStack();
                matrixStack472.pushPose();
                matrixStack472.translate(this.distance.getValue().intValue(), this.distance.getValue().intValue(), 0.0f);
                drawCrosshairLines(matrixStackEvent_2.getMatrixStack(), this.length.getValue().intValue(), this.width.getValue().intValue() / 2.0f, intValue, Color.BLACK);
                matrixStack472.popPose();
            }
            drawCrosshairLines(matrixStackEvent_2.getMatrixStack(), this.length.getValue().intValue(), this.width.getValue().intValue() / 2.0f, intValue, this.color.getValue());
            matrixStackEvent_2.getMatrixStack().popPose();
        }
    }

    public void drawCrosshairLines(PoseStack matrixStack, float f, float f2, float f3, Color color) {
        int width = minecraftClient.getWindow().getWidth() / 2;
        int height = minecraftClient.getWindow().getHeight() / 2;
        if (Math.ceil(f2) - f2 != 0.0d) {
            f3 += 0.5f;
        } else {
            width--;
            height++;
        }
        if (this.dot.getValue().booleanValue()) {
            Shape2DRenderer.drawRect(matrixStack, width - f2, height - f2, width + f2, height + f2, color);
        }
        if (f == 0.0f) {
            return;
        }
        Shape2DRenderer.drawRect(matrixStack, width - f2, (height - f3) - f, width + f2, height - f3, color);
        Shape2DRenderer.drawRect(matrixStack, width + f3, height - f2, width + f3 + f, height + f2, color);
        Shape2DRenderer.drawRect(matrixStack, width - f2, height + f3, width + f2, height + f3 + f, color);
        Shape2DRenderer.drawRect(matrixStack, (width - f3) - f, height - f2, width - f3, height + f2, color);
    }
}