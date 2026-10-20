package me.mioclient.module.render;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.client.SearchMode;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.render.RenderUtil;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.client.Colors;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL32C;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Tracers extends Module {
    /* Resolved lazily -- Colors registers after this module, so an eager static
       lookup ran before it was registered and left the ref null (NPE on render). */
    private static Colors colors;

    public static Colors getColors() {
        if (colors == null && ClientServices.moduleInjector != null) {
            try {
                colors = (Colors) ClientServices.moduleInjector.getModule(Colors.class);
            } catch (Exception e) {
            }
        }
        return colors;
    }
    public Setting<TracersMode> hitbox = add(new EnumSetting<>("Hitbox", TracersMode.HEAD));
    public Setting<Float> maxDistance = add(new NumberSetting<>("MaxDistance", 256.0f, 8.0f, 256.0f).setSuffix("m"));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-65536, true)).setThemeSync(true));
    public Setting<Boolean> stem = add(new BooleanSetting("Stem", true));
    public Setting<Boolean> distanceColor = add(new BooleanSetting("DistanceColor", false));
    public Setting<Boolean> enemyColor = add(new BooleanSetting("EnemyColor", true));
    public Setting<Boolean> ignoreFriends = add(new BooleanSetting("IgnoreFriends", false));
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", false));

    
    private enum TracersMode implements NamedValue {
        HEAD("Head", 0.85f),
        BODY("Body", 0.5f),
        LEGS("Legs", 0.0f);

        public final String name;
        public final float val;

        TracersMode(String str, float f) {
            this.name = str;
            this.val = f;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public float getHeightFraction() {
            return this.val;
        }
    }

    public Tracers() {
        super("Tracers", "Draws lines from your crosshair to the players nearby.", Category.RENDER, new String[0]);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        Colors colors_lcl = getColors();
        if (colors_lcl == null) {
            return;
        }
        Camera camera = minecraftClient.gameRenderer.mainCamera();
        Vec3 add = new Vec3(0.0d, 0.0d, 100.0d).xRot(-((float) Math.toRadians(camera.xRot()))).yRot(-((float) Math.toRadians(camera.yRot()))).add(minecraftClient.getEntityRenderDispatcher().camera.position());
        GL32C.glLineWidth(this.lineWidth.getValue().floatValue());
        for (Player playerEntity : minecraftClient.level.players()) {
            if (minecraftClient.player != playerEntity) {
                if (minecraftClient.gameRenderer.mainCamera().position().distanceTo(((AbstractClientPlayer) playerEntity).position()) <= this.maxDistance.getValue().floatValue() && (!this.ignoreFriends.getValue().booleanValue() || !ClientServices.socialManager.isFriendEntity(playerEntity))) {
                    if (!this.ignoreNakeds.getValue().booleanValue() || PlayerUtils.hasArmor((LivingEntity) playerEntity)) {
                        float partial = RenderUtil.getPartialTick();
                        Vec3 lerped = new Vec3(Mth.lerp(partial, playerEntity.xo, playerEntity.getX()), Mth.lerp(partial, playerEntity.yo, playerEntity.getY()), Mth.lerp(partial, playerEntity.zo, playerEntity.getZ()));
                        Vec3 add2 = lerped.add(0.0d, ((LivingEntity) playerEntity).getBbHeight() * this.hitbox.getValue().getHeightFraction(), 0.0d);
                        Color value = this.color.getValue();
                        boolean booleanValue = this.distanceColor.getValue().booleanValue();
                        if (booleanValue) {
                            value = ColorUtils.withAlpha(ColorUtils.lerpColor(Color.red, Color.green, 1.0f - ((float) ((Mth.clamp(minecraftClient.player.getEyePosition().distanceTo(add2), 8.0d, 64.0d) - 8.0d) / 56.0d))), this.color.getValue().getAlpha());
                        }
                        if (ClientServices.socialManager.isFriendEntity(playerEntity)) {
                            value = ColorUtils.withAlpha(ClientServices.socialManager.getFriendColor(), this.color.getValue().getAlpha());
                        } else if (ClientServices.socialManager.isEnemyEntity(playerEntity) && this.enemyColor.getValue().booleanValue()) {
                            value = ColorUtils.withAlpha(ClientServices.socialManager.getEnemyColor(), this.color.getValue().getAlpha());
                        }
                        SearchMode value2 = booleanValue ? colors_lcl.scheme.getValue() : SearchMode.NORMAL;
                        Color color = value;
                        value2.runWithShaderColor(() -> {
                            RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), add, add2, color);
                            if (this.stem.getValue().booleanValue()) {
                                float partial2 = RenderUtil.getPartialTick();
                                Vec3 feet = new Vec3(Mth.lerp(partial2, playerEntity.xo, playerEntity.getX()), Mth.lerp(partial2, playerEntity.yo, playerEntity.getY()), Mth.lerp(partial2, playerEntity.zo, playerEntity.getZ()));
                                Vec3 head = feet.add(0.0d, ((LivingEntity) playerEntity).getEyeHeight(playerEntity.getPose()), 0.0d);
                                RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), feet, head, color);
                            }
                        });
                    }
                }
            }
        }
        GL32C.glLineWidth(1.0f);
    }
}