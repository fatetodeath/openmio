package me.mioclient.util.math;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;

public class MathUtils implements ClientAccessor {
    public static double floorToDecimals(double d, int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return BigDecimal.valueOf(d).setScale(i, RoundingMode.FLOOR).doubleValue();
    }

    public static float roundFloat(float f, int i) {
        if (i < 0) {
            throw new IllegalArgumentException();
        }
        return BigDecimal.valueOf(f).setScale(i, RoundingMode.FLOOR).floatValue();
    }

    public static float randomFloat(float f, float f2) {
        return f + (new Random().nextFloat() * (f2 - f));
    }

    public static int randomInt(int i, int i2) {
        return i + (new Random().nextInt() * (i2 - i));
    }

    public static double getLerpedValue(double d, double d2, double d3) {
        return d + ((d2 - d) * d3);
    }

    public static float getRadians(float f) {
        return f * MathConstants.val4;
    }

    public static float getDegrees(float f) {
        return f * MathConstants.val5;
    }

    public static Vec3 getRotationVector(float f, float f2) {
        float f3 = getRadians(f);
        float f4 = getRadians(-f2);
        float cos = Mth.cos(f4);
        float sin = Mth.sin(f4);
        float cos2 = Mth.cos(f3);
        return new Vec3(sin * cos2, -Mth.sin(f3), cos * cos2);
    }

    public static double getCameraDistanceScale(Vec3 vec3d, double d) {
        return getDistanceScale(minecraftClient.gameRenderer.mainCamera().position(), vec3d, d);
    }

    public static double getDistanceScale(Vec3 vec3d, Vec3 vec3d2, double d) {
        double distanceTo = vec3d.distanceTo(vec3d2);
        double intValue = ((Integer) minecraftClient.options.fov().get()).intValue() / 110.0d;
        if (intValue < 1.0d) {
            intValue = 1.0d;
        }
        return distanceTo <= 10.0d / d ? Math.min(1.0d, d) * intValue : distanceTo * d * 0.1d * intValue;
    }

    public static float getYawDifference(Player playerEntity) {
        return Mth.degreesDifferenceAbs(minecraftClient.player.getYRot(), RotationManager.getRotationsTo(playerEntity.position())[0]);
    }

    public static double snapToNearestBound(double d, double d2, double d3) {
        return d3 - d > d - d2 ? d2 : d3;
    }

    public static boolean isChanceHit(int i) {
        return i != 0 && new Random().nextInt(100) <= i;
    }

    public static float getWrappedDegrees(float f) {
        return (f < 0.0f ? f + 360.0f : f) % 360.0f;
    }

    public static float interpolateWithTick(float f, float f2) {
        return Mth.lerp(RenderUtil.getPartialTick(), f, f2);
    }

    public static double interpolateWithTick(double d, double d2) {
        return Mth.lerp(RenderUtil.getPartialTick(), d, d2);
    }

    public static double getCameraDistanceToBlock(BlockPos blockPos) {
        return getCameraDistance(Vec3.atCenterOf(blockPos));
    }

    public static double getCameraDistance(Vec3 vec3d) {
        return minecraftClient.gameRenderer.mainCamera().position().distanceTo(vec3d);
    }
}
