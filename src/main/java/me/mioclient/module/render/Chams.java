package me.mioclient.module.render;

import me.mioclient.event.events.entity.EntityModelRenderEvent;
import me.mioclient.event.events.entity.TotemPopEvent;

import me.mioclient.event.events.entity.EntityRenderEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.player.ClonedPlayerEntity;

import me.mioclient.mode.render.ChamsMode;
import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.module.render.chams.ChamsMode_2;
import me.mioclient.render.chams.ChamsRenderDispatcher;
import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import me.mioclient.render.shader.ShaderRegistry;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.player.EntityUtil;

import me.mioclient.module.render.chams.ChamsColorApplier;
import me.mioclient.module.render.esp.EspBoxRenderer;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import me.mioclient.render.RenderConstants;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.ducks.DuckEndCrystalEntity;
import me.mioclient.ducks.DuckSimpleOption;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.combat.AutoCrystal;
import me.mioclient.module.player.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Chams extends Module {
    public Setting<Integer> range = add(new NumberSetting<>("Range", 60, 1, 300).setSuffix("m"));
    public Setting<Boolean> fade = add(new BooleanSetting("Fade", false).setVisible());
    public Setting<Integer> fadeRadius = add(new NumberSetting<>("FadeRadius", 75, 1, 100).setSuffix("%").requiresSettings(this.fade));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    public Setting<Boolean> extraLayer = add(new BooleanSetting("ExtraLayer", true));
    public Setting<Boolean> model = add(new BooleanSetting("Model", true).setVisible());
    public Setting<Boolean> xqz = add(new BooleanSetting("XQZ", false).requiresSettings(this.model));
    public Setting<Integer> opacity = add(new NumberSetting<>("Opacity", 100, 0, 100).setSuffix("%").requiresSettings(this.model, this.xqz));
    public Setting<Boolean> shine2 = add(new BooleanSetting("Shine", false).setVisible().requiresSettings(this.model));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 0.5f, 0.0f, 2.0f).setHoleSnapConfig("Static", HoleSnapMode.MIN).requiresSettings(this.model, this.shine2));
    public Setting<Float> strength = add(new NumberSetting<>("Strength", 1.0f, 0.0f, 1.0f).requiresSettings(this.model, this.shine2));
    public Setting<Integer> progress = add(new NumberSetting<>("Progress", 0, 0, 100).setSuffix("%"));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", true).setDependent());
    public Setting<ChamsMode> animals = add(new EnumSetting<>("Animals", ChamsMode.OFF).requiresSettings(this.targets));
    public Setting<ChamsMode> hostiles = add(new EnumSetting<>("Hostiles", ChamsMode.OFF).requiresSettings(this.targets));
    public Setting<ChamsMode> players = add(new EnumSetting<>("Players", ChamsMode.OFF).requiresSettings(this.targets));
    public Setting<ChamsMode> self = add(new EnumSetting<>("Self", ChamsMode.OFF).requiresSettings(this.targets));
    public Setting<ChamsMode> crystals = add(new EnumSetting<>("Crystals", ChamsMode.OFF).requiresSettings(this.targets));
    public Setting<Boolean> pop = add(new BooleanSetting("Pop", false).setVisible().requiresSettings(this.targets));
    public Setting<Boolean> death = add(new BooleanSetting("Death", false).requiresSettings(this.targets, this.pop));
    public Setting<ChamsMode_2> wireframe = add(new EnumSetting<>("Wireframe", ChamsMode_2.BOTH).requiresSettings(this.targets, this.pop));
    public Setting<Boolean> animate = add(new BooleanSetting("Animate", true).requiresSettings(this.targets, this.pop));
    public Setting<Boolean> boost = add(new BooleanSetting("Boost", false).requiresSettings(this.targets, this.pop, this.animate));
    public Setting<Float> time = add(new NumberSetting<>("Time", 1.0f, 0.0f, 5.0f).requiresSettings(this.targets, this.pop));
    public Setting<Float> motion = add(new NumberSetting<>("Motion", 0.0f, -8.0f, 8.0f).requiresSettings(this.targets, this.pop));
    public Setting<Boolean> colors = add(new BooleanSetting("Colors", false).setDependent());
    public Setting<Boolean> friends = add(new BooleanSetting("Friends", false).requiresSettings(this.colors));
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(935172607, true)).setThemeSync(true).requiresSettings(this.colors));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(-4351489, true)).setThemeSync(true).requiresSettings(this.colors));
    public Setting<Color> shine = add(new ColorSetting("Shine", new Color(2076023295, true)).requiresSettings(this.colors));
    public Setting<Color> popFill = add(new ColorSetting("PopFill", new Color(935172607, true)).setThemeSync(true).requiresSettings(this.colors));
    public Setting<Color> popLine = add(new ColorSetting("PopLine", new Color(12425727, true)).setThemeSync(true).requiresSettings(this.colors));
    public final TrajectoriesVertexConsumer trajectoriesVertexConsumer;
    public final Map<ClonedPlayerEntity, Long> map;
    public boolean flag3;
    public static final Identifier identifier = Identifier.fromNamespaceAndPath("mio-mount", "textures/shine.png");
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static AutoCrystal autoCrystal;
    private static NoRender norender;
    private static Animations animations;
    private static Freecam freecam;

    public static AutoCrystal getAutoCrystal() {
        if (autoCrystal == null && ClientServices.moduleInjector != null) {
            try {
                autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
            } catch (Exception e) {
            }
        }
        return autoCrystal;
    }

    public static NoRender getNorender() {
        if (norender == null && ClientServices.moduleInjector != null) {
            try {
                norender = (NoRender) ClientServices.moduleInjector.getModule(NoRender.class);
            } catch (Exception e) {
            }
        }
        return norender;
    }

    public static Animations getAnimations() {
        if (animations == null && ClientServices.moduleInjector != null) {
            try {
                animations = (Animations) ClientServices.moduleInjector.getModule(Animations.class);
            } catch (Exception e) {
            }
        }
        return animations;
    }

    public static Freecam getFreecam() {
        if (freecam == null && ClientServices.moduleInjector != null) {
            try {
                freecam = (Freecam) ClientServices.moduleInjector.getModule(Freecam.class);
            } catch (Exception e) {
            }
        }
        return freecam;
    }
    public static boolean flag = false;
    public static boolean flag2 = false;

    public Chams() {
        super("Chams", "Wallhack on entities.", Category.RENDER, new String[0]);
        this.trajectoriesVertexConsumer = TrajectoriesVertexConsumer.createDefault();
        this.map = new HashMap();
        this.wireframe.setConfigName("PopMode");
        this.shine.setConfigName("ShineColor");
        this.speed.setHoleSnapConfig("Static", HoleSnapMode.MIN);
    }

    @Listen(get219= RenderConstants.num)
    public void onRenderWorldPre(MatrixStackEvent.Inner_3 inner) {
        if (this.shine2.getValue().booleanValue()) {
            this.trajectoriesVertexConsumer.beginBuffer(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        float intBitsToFloat = is2047() ? 2.3f : 1.0f;
        EspBoxRenderer.disableEntityCulling();
        org.lwjgl.opengl.GL32C.glEnable(32823);
        org.lwjgl.opengl.GL32C.glPolygonOffset(1.0f, (-3500000.0f) * intBitsToFloat);
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if (shouldRenderEntity(entity)) {
                NoRender norender_lcl = getNorender();
                if (this.model.getValue().booleanValue() && this.xqz.getValue().booleanValue() && (norender_lcl == null || !norender_lcl.isEntityHidden(entity)) && this.opacity.getValue().intValue() > 0) {
                    this.flag3 = true;
                    RenderUtil.INSTANCE.renderEntity(entity, inner.getTickDelta(), inner.getMatrixStack(), null);
                    this.flag3 = false;
                }
                if (this.shine2.getValue().booleanValue()) {
                    flag = true;
                    inner.getMatrixStack().pushPose();
                    RenderUtil.applyCameraRotation(inner.getMatrixStack());
                    RenderUtil.INSTANCE.renderEntity(entity, inner.getTickDelta(), inner.getMatrixStack(), null);
                    inner.getMatrixStack().popPose();
                    flag = false;
                }
            }
        }
        org.lwjgl.opengl.GL32C.glDisable(32823);
        org.lwjgl.opengl.GL32C.glPolygonOffset(1.0f, 3500000.0f * intBitsToFloat);
        EspBoxRenderer.restoreEntityCulling();
    }

    @Listen(get219= -1000)
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        ChamsRenderDispatcher.setOutlineWidth(this.lineWidth.getValue().floatValue());
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if (shouldRenderEntity(entity) && getChamsModeForEntity(entity).getValue().is594()) {
                getChamsModeForEntity(entity).getValue().applyChamsRender(this, entity, inner_3.getMatrixStack());
            }
        }
        if (this.pop.getValue().booleanValue()) {
            synchronized (this.map) {
                this.map.entrySet().removeIf(entry -> {
                    return ((float) ((Long) entry.getValue()).longValue()) + (this.time.getValue().floatValue() * 1000.0f) < ((float) System.currentTimeMillis());
                });
                this.map.forEach((otherClientPlayerEntity, l) -> {
                    float intBitsToFloat = 1.0f - Mth.clamp(((float) (System.currentTimeMillis() - l.longValue())) / (this.time.getValue().floatValue() * 1000.0f), 0.0f, 1.0f);
                    otherClientPlayerEntity.setPos(otherClientPlayerEntity.getX(), otherClientPlayerEntity.getLastPosition().y + ((1.0f - intBitsToFloat) * this.motion.getValue().floatValue()), otherClientPlayerEntity.getZ());
                    Color[] colorArray593 = this.wireframe.getValue().getChamsColors(this);
                    ChamsRenderDispatcher.setChamsColors(ColorUtils.withAlphaPercent(colorArray593[0], (colorArray593[0].getAlpha() / 255.0f) * intBitsToFloat), ColorUtils.withAlphaPercent(colorArray593[1], (colorArray593[1].getAlpha() / 255.0f) * intBitsToFloat));
                    ChamsRenderDispatcher.renderEntityChams(inner_3.getMatrixStack(), otherClientPlayerEntity);
                });
            }
        }
        if (this.shine2.getValue().booleanValue()) {
            RenderType glintType = RenderTypes.armorEntityGlint();
            flag2 = true;
            RenderUtil.INSTANCE.setupRenderState();
            this.trajectoriesVertexConsumer.drawBuffer();
            RenderUtil.INSTANCE.restoreRenderState();
            flag2 = false;
        }
    }

    @Listen
    public void onEntityRenderPre(EntityRenderEvent.Inner inner) {
        if (ShaderRegistry.flag || this.model.getValue().booleanValue() || !shouldRenderEntity(inner.getEntity()) || this.flag3) {
            return;
        }
        inner.cancel();
    }

    @Listen
    public void onEntityModelRender(EntityModelRenderEvent entityEvent) {
        if (shouldRenderEntity(entityEvent.getEntity())) {
            entityEvent.setLight(15728880);
        }
    }

    @Listen
    public void onTotemPop(TotemPopEvent playerEntityEvent) {
        Player playerEntity1890;
        if (this.pop.getValue().booleanValue()) {
            if ((playerEntityEvent.getType() != TotemPopEvent.LogoutSpotsHelperMode.DEATH || this.death.getValue().booleanValue()) && (playerEntity1890 = playerEntityEvent.getPlayer()) != minecraftClient.player) {
                ClonedPlayerEntity otherClientPlayerEntity = new ClonedPlayerEntity(minecraftClient.level);
                otherClientPlayerEntity.copyFromPlayer(playerEntity1890);
                otherClientPlayerEntity.walkAnimation.setSpeed(otherClientPlayerEntity.walkAnimation.speed() * 2.0f);
                if (!this.animate.getValue().booleanValue()) {
                    otherClientPlayerEntity.walkAnimation.stop();
                } else if (this.boost.getValue().booleanValue()) {
                    otherClientPlayerEntity.setChamsVisible(true);
                    otherClientPlayerEntity.attackAnim = 0.0f;
                    otherClientPlayerEntity.oAttackAnim = 0.0f;
                    otherClientPlayerEntity.walkAnimation.stop();
                    otherClientPlayerEntity.walkAnimation.setSpeed((float) ((Math.random() * 0.800000011920929d) + 0.20000000298023224d));
                }
                synchronized (this.map) {
                    this.map.putIfAbsent(otherClientPlayerEntity, Long.valueOf(System.currentTimeMillis()));
                }
            }
        }
    }

    public boolean shouldRenderEntity(Entity entity) {
        if (LogoutSpots.is1291()) {
            return false;
        }
        try {
            boolean z = !RotationManager.isInFrustum(entity.getBoundingBox()) || minecraftClient.gameRenderer.mainCamera().position().distanceTo(entity.position()) > this.range.getValue().intValue();
            if (z || EntityUtil.isFirstPersonView(entity)) {
                return false;
            }
            if (!(entity instanceof EndCrystal)) {
                if (entity instanceof Player) {
                    Freecam freecam_lcl = getFreecam();
                    return entity == minecraftClient.player ? this.self.getValue().is594() && ((freecam_lcl != null && freecam_lcl.isToggled()) || minecraftClient.options.getCameraType() != CameraType.FIRST_PERSON) : this.players.getValue().is594();
                }
                return ((entity instanceof Animal) && this.animals.getValue().is594()) || ((entity instanceof Monster) && this.hostiles.getValue().is594());
            }
            DuckEndCrystalEntity spawnTimeHelper_2 = (DuckEndCrystalEntity)((EndCrystal) entity);
            AutoCrystal autoCrystal_lcl = getAutoCrystal();
            if (System.currentTimeMillis() - spawnTimeHelper_2.getSpawnTime() > Math.max(ClientServices.holeSnapRenderHelper.getPing(), 50) && ((EndCrystal) spawnTimeHelper_2).tickCount < 10 && autoCrystal_lcl != null && autoCrystal_lcl.isToggled() && autoCrystal_lcl.inhibit.getValue().booleanValue() && spawnTimeHelper_2.isMioAttacked()) {
                return false;
            }
            return this.crystals.getValue().is594();
        } catch (Exception e) {
            return false;
        }
    }

    public float get2044(Entity entity) {
        return (float) (1.0d - Mth.clamp(((minecraftClient.gameRenderer.mainCamera().position().distanceTo(entity.position()) / this.range.getValue().intValue()) - (this.fadeRadius.getValue().intValue() / 100.0f)) / (1.0f - (this.fadeRadius.getValue().intValue() / 100.0f)), 0.0d, 1.0d));
    }

    public boolean shouldRenderChams(Entity entity) {
        if (isToggled() && this.xqz.getValue().booleanValue()) {
            return shouldRenderEntity(entity);
        }
        return false;
    }

    public boolean shouldShineEntity(Entity entity) {
        return isToggled() && (getChamsModeForEntity(entity).getValue().is594() || entity == minecraftClient.player) && this.shine2.getValue().booleanValue() && flag;
    }

    public boolean is2047() {
        float intBitsToFloat = 0.3f;
        Vec3 eyePos = minecraftClient.player.getEyePosition();
        BlockPos ofFloored = BlockPos.containing(eyePos.x, eyePos.y, eyePos.z);
        AABB eyeBox = new AABB(eyePos.x - intBitsToFloat, eyePos.y - intBitsToFloat, eyePos.z - intBitsToFloat, eyePos.x + intBitsToFloat, eyePos.y + intBitsToFloat, eyePos.z + intBitsToFloat);
        if (!minecraftClient.level.noCollision(minecraftClient.player, eyeBox)) {
            if (minecraftClient.level.getBlockState(ofFloored).getCollisionShape(minecraftClient.level, ofFloored).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean is2048() {
        Animations animations_lcl = getAnimations();
        return animations_lcl != null && animations_lcl.isStaticAnimationEnabled();
    }

    public TrajectoriesVertexConsumer getShineVertexConsumer() {
        return this.trajectoriesVertexConsumer;
    }

    public Setting<? extends ChamsColorApplier> getChamsModeForEntity(Entity entity) {
        return entity == minecraftClient.player ? this.self : entity instanceof EndCrystal ? this.crystals : entity instanceof Player ? this.players : entity instanceof Animal ? this.animals : this.hostiles;
    }
}