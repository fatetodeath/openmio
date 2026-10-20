package me.mioclient.util.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class CrystalThreatDetector implements ClientAccessor {
    public final Stopwatch stopwatch = new Stopwatch();

    public abstract boolean isEnabled();

    public void recalculateLayout() {
        if (isLethalCrystalNearby()) {
            this.stopwatch.reset();
        }
    }

    public boolean isThreatActive() {
        return isEnabled() && !this.stopwatch.hasElapsedMs(750L);
    }

    public boolean isLethalCrystalNearby() {
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if (isNearbyCrystal(entity)) {
                if (isLethalExplosionAt(entity.position())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isLethalExplosionAt(Vec3 vec3d) {
        if (vec3d.distanceToSqr(minecraftClient.player.position()) > 144.0d) {
            return false;
        }
        for (int i = 4; i >= 0; i--) {
            AABB box1109 = ClientServices.mainhandHelper.getPredictedBox(minecraftClient.player, i);
            if (ExplosionDamageCalculator.getExplosionDamage(vec3d, minecraftClient.player, box1109, 6.0d, true, (BlockPos) null, (BlockPos) null) >= EntityUtil.getPlayerHealth()) {
                return true;
            }
        }
        return false;
    }

    public boolean isNearbyCrystal(Entity entity) {
        return (entity instanceof EndCrystal) && minecraftClient.player.distanceToSqr(entity) <= 144.0d;
    }
}
