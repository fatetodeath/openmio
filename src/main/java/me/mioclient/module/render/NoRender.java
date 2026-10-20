package me.mioclient.module.render;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.entity.EntityModelRenderEvent;

import me.mioclient.event.events.entity.EntityRenderEvent;

import me.mioclient.api.NamedValue;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.player.ScaffoldMode_2;
import me.mioclient.mode.world.StashFinderMode;
import me.mioclient.render.shader.ShaderRegistry;

import java.util.Set;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.render.AddParticleEvent;
import me.mioclient.event.events.player.GetStatusEffectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.render.RenderLabelEvent;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.HugeExplosionSeedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.BlockPos;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class NoRender extends Module {
    public Setting<Boolean> self = add(new BooleanSetting("Self", false).setDependent());
    public Setting<Boolean> hurtCam = add(new BooleanSetting("HurtCam", true).requiresSettings(this.self));
    public Setting<Boolean> potions = add(new BooleanSetting("Potions", false).requiresSettings(this.self));
    public Setting<Boolean> darkness = add(new BooleanSetting("Darkness", true).requiresSettings(this.self));
    public Setting<Boolean> blindness = add(new BooleanSetting("Blindness", true).requiresSettings(this.self));
    public Setting<Boolean> newSneaking = add(new BooleanSetting("NewSneaking", false).requiresSettings(this.self));
    public Setting<Boolean> totemOverlay = add(new BooleanSetting("TotemOverlay", false).requiresSettings(this.self));
    public Setting<Boolean> hands = add(new BooleanSetting("Hands", false).setVisible().requiresSettings(this.self));
    public Setting<Float> opacity2 = add(new NumberSetting<>("Opacity", 0.0f, 0.0f, 1.0f).requiresSettings(this.self, this.hands));
    public Setting<Boolean> ui = add(new BooleanSetting("UI", false).setDependent());
    public Setting<Boolean> messageIndicator = add(new BooleanSetting("MessageIndicator", true).requiresSettings(this.ui));
    public Setting<Boolean> unsecureServer = add(new BooleanSetting("UnsecureServer", true).requiresSettings(this.ui));
    public Setting<Boolean> tutorialToast = add(new BooleanSetting("TutorialToast", true).requiresSettings(this.ui));
    public Setting<Boolean> textShadow = add(new BooleanSetting("TextShadow", false).requiresSettings(this.ui));
    public Setting<Boolean> bossBars = add(new BooleanSetting("BossBars", false).requiresSettings(this.ui));
    public Setting<Boolean> scoreBoard = add(new BooleanSetting("ScoreBoard", false).requiresSettings(this.ui));
    public Setting<Boolean> tabIcons = add(new BooleanSetting("TabIcons", false).requiresSettings(this.ui));
    public Setting<Boolean> heldTooltips = add(new BooleanSetting("HeldTooltips", false).requiresSettings(this.ui));
    public Setting<Boolean> tint = add(new BooleanSetting("Tint", false).requiresSettings(this.ui));
    public Setting<Boolean> world = add(new BooleanSetting("World", false).setDependent());
    public Setting<Boolean> explosions = add(new BooleanSetting("Explosions", true).requiresSettings(this.world));
    public Setting<Boolean> worldBorder = add(new BooleanSetting("WorldBorder", true).requiresSettings(this.world));
    public Setting<Boolean> skyLight = add(new BooleanSetting("SkyLight", false).requiresSettings(this.world));
    public Setting<Boolean> fog = add(new BooleanSetting("Fog", false).setVisible().requiresSettings(this.world));
    public Setting<Float> range = add(new NumberSetting<>("Range", 3.0f, 0.1f, 10.0f).requiresSettings(this.fog, this.world));
    public Setting<Boolean> sky = add(new BooleanSetting("Sky", false).requiresSettings(this.fog, this.world));
    public Setting<Boolean> particles = add(new BooleanSetting("Particles", false).setVisible().requiresSettings(this.world));
    public Setting<Set<ParticleType<?>>> particleTypes = add(new EntityListObjectSetting<>("ParticleTypes", BuiltInRegistries.PARTICLE_TYPE).requiresSettings(this.particles, this.world));
    public Setting<ScaffoldMode_2> selection2 = add(new EnumSetting<>("Selection", ScaffoldMode_2.BLACKLIST).requiresSettings(this.particles, this.world));
    public Setting<Boolean> noBlockBreaking = add(new BooleanSetting("NoBlockBreaking", false).requiresSettings(this.particles, this.world));
    public Setting<Boolean> entities = add(new BooleanSetting("Entities", false).setDependent());
    public Setting<Boolean> hurt = add(new BooleanSetting("Hurt", true).requiresSettings(this.entities));
    public Setting<Boolean> nameTags = add(new BooleanSetting("NameTags", false).requiresSettings(this.entities));
    public Setting<Boolean> interpolation = add(new BooleanSetting("Interpolation", false).requiresSettings(this.entities));
    public Setting<Boolean> fire = add(new BooleanSetting("Fire", true).setVisible().requiresSettings(this.entities));
    public Setting<Boolean> others = add(new BooleanSetting("Others", false).requiresSettings(this.entities, this.fire));
    public Setting<Boolean> armor = add(new BooleanSetting("Armor", false).setVisible().requiresSettings(this.entities));
    public Setting<Float> opacity = add(new NumberSetting<>("Opacity", 0.0f, 0.0f, 1.0f).requiresSettings(this.entities, this.armor));
    public Setting<Boolean> noCluster = add(new BooleanSetting("NoCluster", false).setVisible().requiresSettings(this.entities));
    public Setting<Integer> clusterAlpha = add(new NumberSetting<>("ClusterAlpha", 120, 0, 255).requiresSettings(this.noCluster, this.entities));
    public Setting<Boolean> tileEntities = add(new BooleanSetting("TileEntities", false).setVisible().requiresSettings(this.entities));
    public Setting<Integer> tileDistance = add(new NumberSetting<>("TileDistance", 64, 1, 256).setSuffix("m").requiresSettings(this.entities, this.tileEntities));
    public Setting<Boolean> ignoreESP = add(new BooleanSetting("IgnoreESP", false).requiresSettings(this.tileEntities));
    public Setting<Boolean> wardens = add(new BooleanSetting("Wardens", false).setVisible().requiresSettings(this.entities));
    public Setting<Integer> wardenDistance = add(new NumberSetting<>("WardenDistance", 32, 1, 128).setSuffix("m").requiresSettings(this.entities, this.wardens));
    public Setting<Boolean> entities2 = add(new BooleanSetting("Entities", false).setVisible().requiresSettings(this.entities));
    public Setting<Set<EntityType<?>>> entityTypes = add(new EntityListObjectSetting<>("EntityTypes", BuiltInRegistries.ENTITY_TYPE).requiresSettings(this.entities2, this.entities));
    public Setting<NoRenderMode> removal = add(new EnumSetting<>("Removal", NoRenderMode.PARTIAL).requiresSettings(this.entities, this.entities2));
    public Setting<ScaffoldMode_2> selection = add(new EnumSetting<>("Selection", ScaffoldMode_2.BLACKLIST).requiresSettings(this.entities, this.entities2));
    public float val;
    public BlockPos blockPos;

    
    public enum NoRenderMode implements NamedValue {
        PARTIAL("Partial"),
        FULL("Full");

        public final String name;

        NoRenderMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public NoRender() {
        super("NoRender", "Removes the annoying stuff from your screen.", Category.RENDER, new String[0]);
        setDrawn(false);
        this.entities.setConfigName("EntitiesPage");
        this.selection2.setConfigName("ParticleSelection");
        this.selection.setConfigName("EntitySelection");
        this.opacity2.setConfigName("HandsOpacity");
    }

    @Listen(get219= RenderConstants.num4)
    public void onEntityModelRender(EntityModelRenderEvent entityEvent) {
        if (!this.noCluster.getValue().booleanValue()) {
            this.val = 1.0f;
            return;
        }
        double distanceTo = minecraftClient.player.position().distanceTo(entityEvent.getEntity().position());
        float width = entityEvent.getEntity().getBbWidth();
        if (distanceTo > width || entityEvent.getEntity() == minecraftClient.player) {
            this.val = 1.0f;
            return;
        }
        float f = (float) (distanceTo / width);
        float intValue = this.clusterAlpha.getValue().intValue() / 255.0f;
        entityEvent.setAlpha(intValue + (f * (1.0f - intValue)));
        this.val = entityEvent.getAlpha();
    }

    @Listen
    public void onEntityRenderPre(EntityRenderEvent.Inner inner) {
        if (isEntityHidden(inner.getEntity()) && inner.getEntity() != minecraftClient.player && this.removal.getValue() == NoRenderMode.FULL) {
            inner.cancel();
        }
    }

    @Listen
    public void onRenderLabel(RenderLabelEvent renderLabelEvent) {
        if (this.nameTags.getValue().booleanValue()) {
            renderLabelEvent.cancel();
        }
    }

    @Listen
    public void onAddParticle(AddParticleEvent addParticleEvent) {
        Particle particle982 = addParticleEvent.getParticle();
        if (((particle982 instanceof HugeExplosionParticle) || (particle982 instanceof HugeExplosionSeedParticle)) && this.explosions.getValue().booleanValue()) {
            addParticleEvent.cancel();
        }
        if (this.potions.getValue().booleanValue() && (particle982 instanceof SpellParticle)) {
            addParticleEvent.cancel();
        }
    }

    @Listen
    public void onGetStatusEffect(GetStatusEffectEvent getStatusEffectEvent) {
        if (this.blindness.getValue().booleanValue() && getStatusEffectEvent.getEffect() == MobEffects.BLINDNESS) {
            getStatusEffectEvent.cancel();
        }
        if (this.darkness.getValue().booleanValue() && getStatusEffectEvent.getEffect() == MobEffects.DARKNESS) {
            getStatusEffectEvent.cancel();
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (this.fire.getValue().booleanValue() && WorldUtils.getDimension() == StashFinderMode.THE_END) {
            ClientboundBlockUpdatePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundBlockUpdatePacket ? (ClientboundBlockUpdatePacket) (channelRead0Event.getPacket()) : null;
            if (packet904 instanceof ClientboundBlockUpdatePacket) {
                ClientboundBlockUpdatePacket blockUpdateS2CPacket = packet904;
                if (this.blockPos != null) {
                    if (!blockUpdateS2CPacket.getPos().equals(this.blockPos)) {
                        return;
                    }
                    if (blockUpdateS2CPacket.getBlockState().getBlock() != Blocks.FIRE) {
                        return;
                    }
                    this.blockPos = null;
                    channelRead0Event.cancel();
                }
            }
            ClientboundAddEntityPacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundAddEntityPacket ? (ClientboundAddEntityPacket) (channelRead0Event.getPacket()) : null;
            if (packet9042 instanceof ClientboundAddEntityPacket) {
                ClientboundAddEntityPacket entitySpawnS2CPacket = packet9042;
                if (entitySpawnS2CPacket.getType() == EntityTypes.END_CRYSTAL) {
                    this.blockPos = BlockPos.containing(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ());
                }
            }
        }
    }

    public boolean is179() {
        return isToggled() && this.hands.getValue().booleanValue() && !ShaderRegistry.flag2 && !ShaderRegistry.flag && this.opacity2.getValue().floatValue() == 0.0f;
    }

    public boolean shouldHideTabIcons() {
        return isToggled() && this.tabIcons.getValue().booleanValue();
    }

    public boolean isParticleBlocked(ParticleOptions particleEffect) {
        if (isToggled() && this.particles.getValue().booleanValue()) {
            return !this.selection2.getValue().isAllowed(particleEffect.getType(), this.particleTypes.getValue());
        }
        return false;
    }

    public boolean isEntityHidden(Entity entity) {
        return isEntityTypeHidden(entity.getType());
    }

    public boolean isEntityTypeHidden(EntityType<?> entityType) {
        if (isToggled() && this.entities2.getValue().booleanValue()) {
            return !this.selection.getValue().isAllowed(entityType, this.entityTypes.getValue());
        }
        return false;
    }

    public boolean shouldBlockBlockBreaking() {
        return isToggled() && this.noBlockBreaking.getValue().booleanValue();
    }

    public float getHandOpacity() {
        if (!isToggled() || !this.hands.getValue().booleanValue() || ShaderRegistry.flag || ShaderRegistry.flag2) {
            return 1.0f;
        }
        float floatValue = this.opacity2.getValue().floatValue();
        if (floatValue == 0.0f) {
            return 0.0f;
        }
        return Math.max(floatValue, 0.11f);
    }

    public float getArmorOpacity() {
        if (!isToggled() || !this.armor.getValue().booleanValue()) {
            return 1.0f;
        }
        float floatValue = this.opacity.getValue().floatValue();
        if (floatValue == 0.0f) {
            return 0.0f;
        }
        return Math.max(floatValue, 0.11f);
    }

    public float getGlobalOpacity() {
        return !isToggled() ? 1.0f : this.val;
    }

    public boolean isTintEnabled() {
        return isToggled() && this.tint.getValue().booleanValue();
    }
}