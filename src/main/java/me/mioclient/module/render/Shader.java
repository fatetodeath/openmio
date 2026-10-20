package me.mioclient.module.render;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shader.OutlineShader;
import me.mioclient.render.shader.ShaderFramebufferSwapper;
import me.mioclient.render.shader.ShaderRegistry;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.player.EntityUtil;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.awt.Color;
import java.util.Comparator;
import java.util.function.Supplier;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.player.Freecam;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.CameraType;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Shader extends Module {
    public static Freecam freecam = (Freecam) ClientServices.moduleInjector.getModule(Freecam.class);
    public static NoRender norender = (NoRender) ClientServices.moduleInjector.getModule(NoRender.class);
    public Setting<MixinHeldItemRendererMode> shader = add(new EnumSetting<>("Shader", MixinHeldItemRendererMode.SOLID));
    public Setting<Integer> range2 = add(new NumberSetting<>("Range", 60, 1, 300).setSuffix("m"));
    public Setting<Integer> lineWidth = add(new NumberSetting<>("LineWidth", 1, 0, 10));
    public Setting<Boolean> fastLines = add(new BooleanSetting("FastLines", false, v -> this.lineWidth.getValue().intValue() > 2));
    public Setting<Float> glow = add(new NumberSetting<>("Glow", 1.0f, 0.0f, 5.0f));
    public Setting<Integer> quality = add(new NumberSetting<>("Quality", 1, 1, 6, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.BLOOM));
    public Setting<Float> step = add(new NumberSetting<>("Step", 0.5f, 0.1f, 2.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.GRADIENT));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.1f, 5.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.GRADIENT));
    public Setting<Float> fillSpeed = add(new NumberSetting<>("FillSpeed", 1.0f, 0.0f, 5.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.RAINBOW));
    public Setting<Float> outlineSpeed = add(new NumberSetting<>("OutlineSpeed", 1.0f, 0.0f, 5.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.RAINBOW));
    public Setting<Float> fillStrength = add(new NumberSetting<>("FillStrength", 1.0f, 0.0f, 5.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.RAINBOW));
    public Setting<Float> outlineStrength = add(new NumberSetting<>("OutlineStrength", 1.0f, 0.0f, 5.0f, v -> this.shader.getValue() == Shader.MixinHeldItemRendererMode.RAINBOW));
    public Setting<Boolean> image = add(new BooleanSetting("Image", false).setVisible());
    public Setting<Float> overlayAlpha = add(new NumberSetting<>("OverlayAlpha", 1.0f, 0.0f, 1.0f).requiresSettings(this.image));
    public Setting<Boolean> decorator = add(new BooleanSetting("Decorator", false).setDependent());
    public Setting<Mode> type = add(new EnumSetting<>("Type", Mode.NONE).requiresSettings(this.decorator));
    public Setting<Integer> radius = add(new NumberSetting<>("Radius", 5, 1, 20).requiresSettings(this.decorator));
    public Setting<Float> alpha = add(new NumberSetting<>("Alpha", 1.0f, 0.0f, 1.0f).requiresSettings(this.decorator));
    public Setting<Boolean> colors = add(new BooleanSetting("Colors", false).setDependent());
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(2025691647, true)).requiresSettings(this.colors));
    public Setting<Color> fillSecond = add(new ColorSetting("FillSecond", new Color(1936677274, true)));
    public Setting<Color> outline2 = add(new ColorSetting("Outline", new Color(-4351489, true)).requiresSettings(this.colors));
    public Setting<Color> outlineSecond = add(new ColorSetting("OutlineSecond", new Color(-8625746, true)));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", true).setDependent());
    public Setting<Boolean> hands = add(new BooleanSetting("Hands", true).setVisible().requiresSettings(this.targets));
    public Setting<Boolean> fill2 = add(new BooleanSetting("Fill", true).requiresSettings(this.targets, this.hands));
    public Setting<Boolean> outline = add(new BooleanSetting("Outline", true).requiresSettings(this.targets, this.hands));
    public Setting<Boolean> crystals = add(new BooleanSetting("Crystals", false).setVisible().requiresSettings(this.targets));
    public Setting<Integer> range = add(new NumberSetting<>("Range", 60, 1, 300).setSuffix("m").requiresSettings(this.targets, this.crystals));
    public Setting<Boolean> items = add(new BooleanSetting("Items", false).setVisible().requiresSettings(this.targets));
    public Setting<Integer> range3 = add(new NumberSetting<>("Range", 60, 1, 300).setSuffix("m").requiresSettings(this.targets, this.items));
    public Setting<Boolean> animals = add(new BooleanSetting("Animals", false).requiresSettings(this.targets));
    public Setting<Boolean> hostiles = add(new BooleanSetting("Hostiles", false).requiresSettings(this.targets));
    public Setting<Boolean> players = add(new BooleanSetting("Players", false).requiresSettings(this.targets));
    public Setting<Boolean> self = add(new BooleanSetting("Self", false).requiresSettings(this.targets, this.players));
    public Setting<Boolean> exp = add(new BooleanSetting("Exp", false).requiresSettings(this.targets));
    public Setting<Boolean> pearls = add(new BooleanSetting("Pearls", false).requiresSettings(this.targets));
    public Setting<Boolean> minecarts = add(new BooleanSetting("Minecarts", false).requiresSettings(this.targets));
    public Setting<Boolean> dead = add(new BooleanSetting("Dead", false).requiresSettings(this.targets));
    public final ObjectArrayList<Entity> objectArrayList;
    public float val;

    
    public enum MixinHeldItemRendererMode implements NamedValue {
        SOLID("Solid", () -> {
            return ShaderRegistry.shaderFramebufferHelper;
        }),
        RAINBOW("Rainbow", () -> {
            return ShaderRegistry.shaderFramebufferHelper2;
        }),
        GRADIENT("Gradient", () -> {
            return ShaderRegistry.shaderFramebufferHelper3;
        }),
        BLOOM("Bloom", () -> {
            return ShaderRegistry.shaderFramebufferHelper4;
        });

        public final String name;
        public final Supplier<OutlineShader> supplier;

        MixinHeldItemRendererMode(String str, Supplier supplier) {
            this.name = str;
            this.supplier = supplier;
        }

        public OutlineShader getOutlineShader() {
            return this.supplier.get();
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum Mode implements NamedValue {
        NONE("None"),
        DOTS("Dots"),
        GRID("Grid");

        public final String name;

        Mode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Shader() {
        super("Shader", "Advanced ESP using shaders.", Category.RENDER, new String[0]);
        this.objectArrayList = new ObjectArrayList<>();
        this.val = 1.0f;
        this.radius.setConfigName("DecoratorRadius");
        this.alpha.setConfigName("DecoratorAlpha");
        this.fill2.setConfigName("HandsFill");
        this.outline.setConfigName("HandsOutline");
        this.range3.setConfigName("ItemRange");
        this.range.setConfigName("CrystalRange");
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return ClientTextRenderer.capitalize(this.shader.getValue());
    }

    @Listen(get219= -99999999)
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        ShaderRegistry.init();
        OutlineShader outlineShader = this.shader.getValue().getOutlineShader();
        if (outlineShader == null) {
            return;
        }
        this.objectArrayList.clear();
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if (shouldRenderEntity(entity)) {
                this.objectArrayList.add(entity);
            }
        }
        this.objectArrayList.sort(Comparator.comparing(entity2 -> {
            return Double.valueOf(minecraftClient.gameRenderer.mainCamera().position().distanceToSqr(entity2.position()));
        }));
        ShaderFramebufferSwapper.swapAndRender(outlineShader, false, () -> {
            inner_3.getMatrixStack().pushPose();
            this.objectArrayList.forEach(entity3 -> {
                renderEntityToOutline(inner_3, entity3, outlineShader);
            });
            inner_3.getMatrixStack().popPose();
        });
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.val = getClusterAlpha((Entity) this.objectArrayList.stream().min(Comparator.comparing(this::getClusterAlpha)).orElse(null));
    }

    public void renderEntityToOutline(MatrixStackEvent matrixStackEvent, Entity entity, OutlineShader shaderFramebufferHelper) {
        if (entity.isAlive() || this.dead.getValue().booleanValue()) {
            me.mioclient.render.RenderUtil.INSTANCE.renderEntity(entity, matrixStackEvent.getTickDelta(), matrixStackEvent.getMatrixStack(), null);
        }
    }

    public float getClusterAlpha(Entity entity) {
        if (entity != null && norender.isToggled() && norender.noCluster.getValue().booleanValue()) {
            double distanceTo = minecraftClient.player.position().distanceTo(entity.position());
            float width = entity.getBbWidth();
            if (distanceTo <= width && entity != minecraftClient.player) {
                float f = (float) (distanceTo / width);
                float intValue = norender.clusterAlpha.getValue().intValue() / 255.0f;
                return intValue + (f * (1.0f - intValue));
            }
        }
        return 1.0f;
    }

    /* Picks the outline post chain for the current mode. SOLID uses the crisp
       outline/fill/both variants, BLOOM the bloom chain, GRADIENT/RAINBOW the
       animated-effect variants (static uniforms ship in the JSONs; per-frame UBO
       animation is a follow-up). Null means vanilla compositing only. */
    public net.minecraft.resources.Identifier pickActiveOutlineChain() {
        if (!isToggled()) {
            return null;
        }
        if (this.shader.getValue() == MixinHeldItemRendererMode.BLOOM) {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_bloom");
        }
        boolean fill = this.fill.getValue().getAlpha() != 0;
        boolean outline = this.outline2.getValue().getAlpha() != 0;
        if (!fill && !outline) {
            return null;
        }
        if (this.shader.getValue() == MixinHeldItemRendererMode.RAINBOW) {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_rainbow_" + alphaBucket());
        }
        if (this.shader.getValue() == MixinHeldItemRendererMode.GRADIENT) {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_gradient_" + alphaBucket());
        }
        if (fill && outline) {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_both_" + alphaBucket());
        }
        if (fill) {
            return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_fill_" + alphaBucket());
        }
        return net.minecraft.resources.Identifier.fromNamespaceAndPath("mio", "outline_outline");
    }

    public int alphaBucket() {
        return Math.max(20, Math.min(100, Math.round((this.fill.getValue().getAlpha() / 255.0f) * 5.0f) * 20));
    }

    public Color applyClusterAlpha(Color color) {
        float f = this.val;
        if (ShaderRegistry.flag2) {
            f = 1.0f;
        }
        return ColorUtils.withAlpha(color, (int) (f * color.getAlpha()));
    }

    public float getOverlayAlpha() {
        return ShaderRegistry.flag2 ? this.overlayAlpha.getValue().floatValue() : this.overlayAlpha.getValue().floatValue() * this.val;
    }

    public boolean shouldRenderEntity(Entity entity) {
        float intValue = this.range2.getValue().intValue();
        if (entity instanceof EndCrystal) {
            intValue = this.range.getValue().intValue();
        }
        if (entity instanceof ItemEntity) {
            intValue = this.range3.getValue().intValue();
        }
        boolean z = !RotationManager.isInFrustum(entity.getBoundingBox()) || minecraftClient.player.distanceTo(entity) > intValue;
        if (((entity instanceof Player) && ((Player) entity).isRemoved()) || z || EntityUtil.isFirstPersonView(entity)) {
            return false;
        }
        boolean z2 = minecraftClient.options.getCameraType() != CameraType.FIRST_PERSON || freecam.isToggled();
        if (!(entity instanceof Player)) {
            return ((entity instanceof ItemEntity) && this.items.getValue().booleanValue()) || ((entity instanceof ThrownExperienceBottle) && this.exp.getValue().booleanValue()) || ((((entity instanceof Animal) || (entity instanceof AbstractFish) || (entity instanceof Squid)) && this.animals.getValue().booleanValue()) || (((entity instanceof Monster) && this.hostiles.getValue().booleanValue()) || (((entity instanceof ThrownEnderpearl) && this.pearls.getValue().booleanValue()) || (((entity instanceof EndCrystal) && this.crystals.getValue().booleanValue()) || ((entity instanceof AbstractMinecart) && this.minecarts.getValue().booleanValue())))));
        }
        if (z2 && this.self.getValue().booleanValue() && entity == minecraftClient.player) {
            return true;
        }
        return this.players.getValue().booleanValue() && entity != minecraftClient.player;
    }

    public boolean isHandFillDisabled() {
        return ShaderRegistry.flag2 && !this.fill2.getValue().booleanValue();
    }

    public boolean isHandOutlineDisabled() {
        return ShaderRegistry.flag2 && !this.outline.getValue().booleanValue();
    }
}