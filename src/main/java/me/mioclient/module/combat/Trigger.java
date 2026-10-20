package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import java.util.concurrent.TimeUnit;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.mixin.ducks.DuckMinecraftClient;
import me.mioclient.module.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

public class Trigger extends Module {
    public Setting<Float> setting;
    public Setting<Boolean> setting2;
    public Setting<Double> setting3;
    public Setting<Integer> setting4;
    public Setting<Boolean> setting5;
    public Setting<Boolean> setting6;
    public Setting<Boolean> setting7;
    public Setting<Boolean> setting8;
    public Setting<Boolean> setting9;
    public Setting<Boolean> setting10;
    public final Stopwatch stopwatch;

    public Trigger() {
        super("Trigger", "Attacks entities under your crosshair.", Category.COMBAT, new String[0]);
        this.stopwatch = new Stopwatch();
    }

    
    @Listen
    
    public void handleMotion(MotionEvent motionEvent) {
        boolean z;
        boolean z2;
        if (this.setting == null || this.setting2 == null || this.setting3 == null || this.setting4 == null || this.setting5 == null || this.setting6 == null || this.setting7 == null || this.setting8 == null || this.setting9 == null || this.setting10 == null) {
            return;
        }
        if (motionEvent.getPhase() == KeyPearlMode.Post || isNotInGame()) {
            return;
        }
        if ((!(minecraftClient.player.getMainHandItem().getItem() instanceof AxeItem) && !minecraftClient.player.getMainHandItem().is(ItemTags.SWORDS) && this.setting2.getValue().booleanValue()) || !(minecraftClient.hitResult instanceof EntityHitResult)) {
            return;
        }
        Entity entity = ((EntityHitResult) minecraftClient.hitResult).getEntity();
        if (entity instanceof Player) {
            if (!ClientServices.socialManager.isFriend(entity.getName().getString()) && this.setting6.getValue().booleanValue()) {
                z = true;
                z2 = z;
                if (z2 && this.setting7.getValue().booleanValue() && !PlayerUtils.hasArmor((Player) entity)) {
                    z2 = false;
                }
                if (entity.isAlive()) {
                    return;
                }
                if (z2 || (((entity instanceof AgeableMob) && this.setting8.getValue().booleanValue()) || (((entity instanceof Monster) && this.setting9.getValue().booleanValue()) || ((entity instanceof EndCrystal) && this.setting10.getValue().booleanValue())))) {
                    if (minecraftClient.player.getAttackStrengthScale(0.5f) >= 1.0f) {
                        if (!this.stopwatch.hasElapsed(this.setting3.getValue().doubleValue() * Math.random(), TimeUnit.SECONDS) || minecraftClient.player.distanceTo(entity) > this.setting.getValue().floatValue()) {
                            return;
                        }
                        performAttack(MathUtils.isChanceHit(this.setting4.getValue().intValue()));
                        this.stopwatch.reset();
                        return;
                    }
                    return;
                }
                return;
            }
        }
        z = false;
        z2 = z;
        if (z2) {
            z2 = false;
        }
        if (entity.isAlive()) {
        }
    }

    public void performAttack(boolean z) {
        if (!z) {
            ((DuckMinecraftClient)(Object) minecraftClient).attack();
            return;
        }
        minecraftClient.player.swing(InteractionHand.MAIN_HAND);
        minecraftClient.player.resetAttackStrengthTicker();
    }
}