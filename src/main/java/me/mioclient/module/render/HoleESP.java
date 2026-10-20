package me.mioclient.module.render;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.HoleData;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class HoleESP extends Module {
    public Setting<HoleESPMode> fill = add(new EnumSetting<>("Fill", HoleESPMode.GRADIENT));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    public Setting<Float> height = add(new NumberSetting<>("Height", 0.0f, -1.0f, 1.0f).setSuffix("m"));
    public Setting<Integer> radius = add(new NumberSetting<>("Radius", 10, 1, 32).setSuffix("m"));
    public Setting<Boolean> hideOwn = add(new BooleanSetting("HideOwn", false));
    public Setting<Boolean> fade = add(new BooleanSetting("Fade", false).setVisible());
    public Setting<Float> fadeRadius = add(new NumberSetting<>("FadeRadius", 7.0f, 1.0f, 16.0f, v -> this.fade.isShown()).setSuffix("m"));
    public Setting<Boolean> safe = add(new BooleanSetting("Safe", true).setDependent());
    public Setting<Color> safeFill = add(new ColorSetting("SafeFill", new Color(540373280, true), v -> this.trapped.isShown()).setThemeSync(true));
    public Setting<Color> safeOutline = add(new ColorSetting("SafeOutline", new Color(-1037662665, true), v -> this.safe.isShown()).setThemeSync(true));
    public Setting<Boolean> unsafe = add(new BooleanSetting("Unsafe", true).setDependent());
    public Setting<Color> unsafeFill = add(new ColorSetting("UnsafeFill", new Color(634257408, true), v -> this.unsafe.isShown()));
    public Setting<Color> unsafeOutline = add(new ColorSetting("UnsafeOutline", new Color(-1077018624, true), v -> this.unsafe.isShown()));
    public Setting<Boolean> trapped = add(new BooleanSetting("Trapped", false).setVisible());
    public Setting<Color> trappedFill = add(new ColorSetting("TrappedFill", new Color(233622784, true), v -> this.trapped.isShown()).setThemeSync(true));
    public Setting<Color> trappedOutline = add(new ColorSetting("TrappedOutline", new Color(-1073745408, true), v -> this.safe.isShown()).setThemeSync(true));

    
    public enum HoleESPMode implements NamedValue {
        NONE("None"),
        SOLID("Solid"),
        GRADIENT("Gradient");

        public final String name;

        HoleESPMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public HoleESP() {
        super("HoleESP", "Highlights the spots that are safe from end crystals.", Category.RENDER, new String[0]);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return String.valueOf(ClientServices.holeSnapOverlayHelper.getHoles().size());
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        double lerp = Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.yo, minecraftClient.player.getY());
        Vec3 pos = minecraftClient.gameRenderer.mainCamera().position();
        AABB box = new AABB(PlayerUtils.getRoundedBlockPos());
        for (HoleData holeSnapData : ClientServices.holeSnapOverlayHelper.getHoles()) {
            if (RotationManager.isInFrustum(holeSnapData.getBox()) && (this.trapped.getValue().booleanValue() || !holeSnapData.isTrapped())) {
                if (holeSnapData.getBlockPos().closerToCenterThan(pos, this.radius.getValue().intValue())) {
                    if (!minecraftClient.player.getBoundingBox().intersects(holeSnapData.getBox()) || !this.hideOwn.getValue().booleanValue() || this.fade.getValue().booleanValue()) {
                        Color value = this.safeFill.getValue();
                        Color value2 = this.safeOutline.getValue();
                        if (holeSnapData.isTrapped()) {
                            value = this.trappedFill.getValue();
                            value2 = this.trappedOutline.getValue();
                        } else if (holeSnapData.getHoleSnapDataMode() == HoleData.HoleSnapDataMode.UNSAFE) {
                            value = this.unsafeFill.getValue();
                            value2 = this.unsafeOutline.getValue();
                        }
                        double distanceTo = pos.distanceTo(Vec3.atCenterOf(holeSnapData.getBlockPos()));
                        boolean z = holeSnapData.getBox().intersects(box) && this.hideOwn.getValue().booleanValue();
                        if (this.fade.getValue().booleanValue() && (distanceTo >= this.fadeRadius.getValue().floatValue() || z)) {
                            float intBitsToFloat = 1.0f - ((float) Mth.clamp((distanceTo - this.fadeRadius.getValue().floatValue()) / (this.radius.getValue().intValue() - this.fadeRadius.getValue().floatValue()), 0.0d, 1.0d));
                            if (z) {
                                intBitsToFloat = (float) (lerp - Math.floor(lerp));
                            }
                            value = ColorUtils.withAlpha(value, (int) (intBitsToFloat * value.getAlpha()));
                            value2 = ColorUtils.withAlpha(value2, (int) (intBitsToFloat * value2.getAlpha()));
                        }
                        switch (this.fill.getValue().ordinal()) {
                            case 1:
                                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), holeSnapData.getBox().setMaxY(holeSnapData.getBlockPos().getY() + this.height.getValue().floatValue()), value);
                                break;
                            case 2:
                                BoxRenderer.drawFadingBox(inner_3.getMatrixStack(), holeSnapData.getBox().setMaxY(holeSnapData.getBlockPos().getY() + 1), value);
                                break;
                        }
                        BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), holeSnapData.getBox().setMaxY(holeSnapData.getBlockPos().getY() + this.height.getValue().floatValue()), value2, this.lineWidth.getValue().floatValue());
                    }
                }
            }
        }
    }
}