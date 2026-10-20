package me.mioclient.module.render;

import me.mioclient.util.math.ColorUtils;

import java.awt.Color;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import me.mioclient.ducks.DuckParticle;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.render.AddParticleEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.client.particle.BaseAshSmokeParticle;
import net.minecraft.client.particle.AshParticle;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.particle.WhiteAshParticle;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Particles extends Module {
    public final Random random;
    public Setting<Boolean> totems = add(new BooleanSetting("Totems", false).setVisible());
    public Setting<Float> totemsScale = add(new NumberSetting<>("TotemsScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.totems));
    public Setting<Float> totemsVelocity = add(new NumberSetting<>("TotemsVelocity", 1.0f, 0.0f, 5.0f).requiresSettings(this.totems));
    public Setting<Color> totemsColor1 = add(new ColorSetting("TotemsColor1", new Color(-4351489, true)).setThemeSync(true).requiresSettings(this.totems));
    public Setting<Color> totemsColor2 = add(new ColorSetting("TotemsColor2", new Color(-9286761, true)).setThemeSync(true).requiresSettings(this.totems));
    public Setting<Boolean> ignoreSelf = add(new BooleanSetting("IgnoreSelf", false).requiresSettings(this.totems));
    public Setting<Boolean> rockets = add(new BooleanSetting("Rockets", false).setVisible());
    public Setting<Float> rocketScale = add(new NumberSetting<>("RocketScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.rockets));
    public Setting<Color> rocketColor = add(new ColorSetting("RocketColor", new Color(-4351489, true)).setThemeSync(true).requiresSettings(this.rockets));
    public Setting<Boolean> damage = add(new BooleanSetting("Damage", false).setVisible());
    public Setting<Float> damageScale = add(new NumberSetting<>("DamageScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.damage));
    public Setting<Float> damageVelocity = add(new NumberSetting<>("DamageVelocity", 1.0f, 0.0f, 5.0f).requiresSettings(this.damage));
    public Setting<Color> damageColor = add(new ColorSetting("DamageColor", new Color(-4351489, true)).setThemeSync(true).requiresSettings(this.damage));
    public Setting<Boolean> portal = add(new BooleanSetting("Portal", false).setVisible());
    public Setting<Float> portalScale = add(new NumberSetting<>("PortalScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.portal));
    public Setting<Color> portalColor = add(new ColorSetting("PortalColor", new Color(-4351489, true)).setThemeSync(true).requiresSettings(this.portal));
    public Setting<Boolean> dust = add(new BooleanSetting("Dust", false).setVisible());
    public Setting<Color> from = add(new ColorSetting("From", new Color(-4351489, true)).requiresSettings(this.dust));
    public Setting<Color> to = add(new ColorSetting("To", new Color(-4351489, true)).requiresSettings(this.dust));

    public Particles() {
        super("Particles", "Allows you to adjust certain particles.", Category.RENDER, new String[0]);
        this.random = new Random();
        setDrawn(false);
    }

    @Listen
    public void onAddParticle(AddParticleEvent addParticleEvent) {
        TotemParticle particle982 = (addParticleEvent.getParticle()) instanceof TotemParticle ? (TotemParticle) (addParticleEvent.getParticle()) : null;
        if (particle982 instanceof TotemParticle) {
            TotemParticle totemParticle = particle982;
            if (this.ignoreSelf.getValue().booleanValue()) {
                if (minecraftClient.player.getBoundingBox().intersects(totemParticle.getBoundingBox())) {
                    addParticleEvent.cancel();
                }
            }
            if (!this.totems.getValue().booleanValue()) {
                return;
            }
            if (this.random.nextInt(4) == 0) {
                applyColor(totemParticle, this.totemsColor1.getValue().hashCode());
            } else {
                applyColor(totemParticle, this.totemsColor2.getValue().hashCode());
            }
            totemParticle.scale(this.totemsScale.getValue().floatValue());
            totemParticle.setPower(this.totemsVelocity.getValue().floatValue());
        }
        if (this.rockets.getValue().booleanValue()) {
            // 26.2: FireworksSparkParticle removed; sparks are now private FireworkParticles.SparkParticle.
            // SimpleAnimatedParticle (minus Totem) covers firework trail sparks with real color+scale behavior.
            Particle p = addParticleEvent.getParticle();
            if (p instanceof SimpleAnimatedParticle && !(p instanceof TotemParticle)) {
                SimpleAnimatedParticle spark = (SimpleAnimatedParticle) p;
                applyColor(spark, this.rocketColor.getValue().hashCode());
                spark.scale(this.rocketScale.getValue().floatValue());
            }
        }
        if (this.rockets.getValue().booleanValue()) {
            // 26.2: FireworksSparkParticle.Explosion removed; Starter is the public burst controller.
            Particle p2 = addParticleEvent.getParticle();
            if (p2 instanceof FireworkParticles.Starter) {
                FireworkParticles.Starter starter = (FireworkParticles.Starter) p2;
                starter.scale(this.rocketScale.getValue().floatValue());
            }
        }
        if (this.damage.getValue().booleanValue()) {
            // 26.2: DamageParticle removed; CritParticle (DamageIndicatorProvider) is the damage indicator.
            Particle p3 = addParticleEvent.getParticle();
            if (p3 instanceof CritParticle) {
                CritParticle damageParticle = (CritParticle) p3;
                applyColor(damageParticle, this.damageColor.getValue().hashCode());
                damageParticle.scale(this.damageScale.getValue().floatValue());
                damageParticle.setPower(this.damageVelocity.getValue().floatValue());
            }
        }
        if (this.portal.getValue().booleanValue()) {
            PortalParticle particle9825 = (addParticleEvent.getParticle()) instanceof PortalParticle ? (PortalParticle) (addParticleEvent.getParticle()) : null;
            if (particle9825 instanceof PortalParticle) {
                PortalParticle portalParticle = particle9825;
                applyColor(portalParticle, this.portalColor.getValue().hashCode());
                portalParticle.scale(this.portalScale.getValue().floatValue());
            }
        }
        if (this.dust.getValue().booleanValue()) {
            Particle particle9826 = addParticleEvent.getParticle();
            // 26.2: AscendingParticle removed; BaseAshSmokeParticle is the surviving base of Ash/WhiteAsh.
            if (particle9826 instanceof BaseAshSmokeParticle) {
                Particle particle = particle9826;
                if (particle instanceof WhiteAshParticle) {
                    applyColor((SingleQuadParticle) particle, this.to.getValue().brighter().hashCode());
                }
                if (particle instanceof AshParticle) {
                    applyColor((SingleQuadParticle) particle, ColorUtils.lerpColor(this.from.getValue(), this.to.getValue(), ThreadLocalRandom.current().nextFloat()).hashCode());
                }
            }
        }
    }

    public void applyColor(SingleQuadParticle particle, int i) {
        float intBitsToFloat = (i >> 24) / 255.0f;
        particle.setColor(((i & 16711680) >> 16) / 255.0f, ((i & 65280) >> 8) / 255.0f, (i & 255) / 255.0f);
        ((DuckParticle) particle).mio$setInitialAlpha(intBitsToFloat);
    }
}