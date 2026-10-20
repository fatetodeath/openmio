package me.mioclient.module.render;

import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.render.RenderUtil;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import me.mioclient.render.RenderConstants;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import me.mioclient.render.shape.BoxRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL32C;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Trails extends Module {
    public final HashMap<Integer, List<Inner>> hashMap;
    public Setting<Boolean> render = add(new BooleanSetting("Render", true));
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 0.0f, 0.0f, 5.0f).setSuffix("s"));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", false).setDependent());
    public Setting<Boolean> self = add(new BooleanSetting("Self", true, v -> this.targets.isShown()));
    public Setting<Boolean> players = add(new BooleanSetting("Players", false, v -> this.targets.isShown()));
    public Setting<Boolean> pearls = add(new BooleanSetting("Pearls", false, v -> this.targets.isShown()));
    public Setting<Boolean> arrows = add(new BooleanSetting("Arrows", false, v -> this.targets.isShown()));
    public Setting<Boolean> exp = add(new BooleanSetting("Exp", false, v -> this.targets.isShown()));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    public Setting<Boolean> fade = add(new BooleanSetting("Fade", false).setVisible());
    public Setting<Float> fadeDelay = add(new NumberSetting<>("FadeDelay", 1.0f, 0.0f, 10.0f, v -> this.fade.isShown()).setSuffix("s"));
    public Setting<Float> fadeDuration = add(new NumberSetting<>("FadeDuration", 0.5f, 0.0f, 10.0f, v -> this.fade.isShown()).setSuffix("s"));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-1312974337, true)).setThemeSync(true));
    public Setting<Boolean> rainbow = add(new BooleanSetting("Rainbow", false).setVisible());
    public Setting<Double> threshold = add(new NumberSetting<>("Threshold", 1.0, 0.1, 3.0).requiresSettings(this.rainbow));
    public final Stopwatch stopwatch;

    
    
    public static class Inner {
        public final long num = System.currentTimeMillis();
        public final Vec3 vec3d;

        public Inner(Vec3 vec3d) {
            this.vec3d = vec3d;
        }

        public long getCreatedAt() {
            return this.num;
        }

        public Vec3 getPosition() {
            return this.vec3d;
        }
    }

    public Trails() {
        super("Trails", "Draws trails behind certain entities.", Category.RENDER, new String[0]);
        this.hashMap = new HashMap<>();
        this.stopwatch = new Stopwatch();
        setDrawn(false);
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.hashMap.clear();
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (isNotInGame() || !this.render.getValue().booleanValue()) {
            return;
        }
        for (Map.Entry<Integer, List<Inner>> entry : this.hashMap.entrySet()) {
            if (entry.getValue().size() > 2) {
                do3148(inner_3.getMatrixStack(), entry.getValue());
            }
        }
    }

    @Listen(get219= RenderConstants.num4)
    public void onRenderWorldPre(MatrixStackEvent.Inner_3 inner) {
        if (isNotInGame()) {
            return;
        }
        if (this.delay.getValue().floatValue() != 0.0f) {
            if (!this.stopwatch.hasElapsed(this.delay.getValue().floatValue(), TimeUnit.SECONDS)) {
                return;
            }
        }
        this.stopwatch.reset();
        synchronized (this.hashMap) {
            if (minecraftClient.player.tickCount % 4 == 0) {
                int i = 0;
                Iterator<Integer> it = this.hashMap.keySet().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    Integer next = it.next();
                    List<Inner> list = this.hashMap.get(next);
                    list.removeIf(inner2 -> {
                        return this.fade.getValue().booleanValue() && System.currentTimeMillis() - inner2.num > getFadeDelayMs() + getFadeDurationMs();
                    });
                    if (list.size() == 0) {
                        i = next.intValue();
                        break;
                    }
                }
                if (i != 0) {
                    this.hashMap.remove(Integer.valueOf(i));
                }
            }
            for (Entity entity : minecraftClient.level.entitiesForRendering()) {
                if (shouldRenderEntity(entity)) {
                    this.hashMap.compute(Integer.valueOf(entity.getId()), (num, list2) -> {
                        float partial = RenderUtil.getPartialTick();
                        Inner inner3 = new Inner(new Vec3(Mth.lerp(partial, entity.xOld, entity.getX()), Mth.lerp(partial, entity.yOld, entity.getY()), Mth.lerp(partial, entity.zOld, entity.getZ())));
                        if (list2 == null) {
                            return new ArrayList(Collections.singleton(inner3));
                        }
                        list2.add(inner3);
                        return list2;
                    });
                }
            }
        }
    }

    public void do3148(PoseStack matrixStack, List<Inner> list) {
        float[] RGBtoHSB = Color.RGBtoHSB(this.color.getValue().getRed(), this.color.getValue().getGreen(), this.color.getValue().getBlue(), (float[]) null);
        for (int i = 0; i < list.size() - 2; i++) {
            RenderUtil.INSTANCE.addLineVertex(matrixStack, null, list.get(i).getPosition(), list.get(i + 1).getPosition(), calculateTrailColor(RGBtoHSB, list.get(i)).hashCode(), calculateTrailColor(RGBtoHSB, list.get(i + 1)).hashCode());
        }
    }

    public Color calculateTrailColor(float[] fArr, Inner inner) {
        Color hSBColor = this.rainbow.getValue().booleanValue() ? Color.getHSBColor((float) ((Math.ceil(inner.getCreatedAt() / (20.0d * this.threshold.getValue().doubleValue())) % MathConstants.num3) / MathConstants.num3), fArr[1], fArr[2]) : this.color.getValue();
        return (!this.fade.getValue().booleanValue() || System.currentTimeMillis() - inner.getCreatedAt() < getFadeDelayMs()) ? ColorUtils.withAlpha(hSBColor, this.color.getValue().getAlpha()) : ColorUtils.withAlpha(hSBColor, (int) Mth.clamp((1.0f - (((float) ((System.currentTimeMillis() - inner.getCreatedAt()) - getFadeDelayMs())) / ((float) getFadeDurationMs()))) * this.color.getValue().getAlpha(), 0.0f, 255.0f));
    }

    public boolean shouldRenderEntity(Entity entity) {
        return (entity == minecraftClient.player && this.self.getValue().booleanValue()) || ((entity instanceof Player) && this.players.getValue().booleanValue() && entity != minecraftClient.player) || (((entity instanceof ThrownEnderpearl) && this.pearls.getValue().booleanValue()) || (((entity instanceof ThrownExperienceBottle) && this.exp.getValue().booleanValue()) || ((entity instanceof Arrow) && this.arrows.getValue().booleanValue())));
    }

    public long getFadeDelayMs() {
        return (long) (this.fadeDelay.getValue().floatValue() * 1000.0f);
    }

    public long getFadeDurationMs() {
        return (long) (this.fadeDuration.getValue().floatValue() * 1000.0f);
    }
}