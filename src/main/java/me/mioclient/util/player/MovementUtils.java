package me.mioclient.util.player;

import me.mioclient.util.math.MathConstants;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.util.Iterator;
import me.mioclient.event.events.player.MoveEvent;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MovementUtils implements ClientAccessor {
    public static float val = 0.2873f;
    public static float val2 = val * 0.2f;

    public static double[] applyVelocity(MoveEvent moveEvent, double d) {
        if (!is2181()) {
            moveEvent.setHorizontal(0.0d, 0.0d);
            return new double[]{0.0d, 0.0d};
        }
        double[] doubleArray2508 = computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, d);
        moveEvent.setHorizontal(doubleArray2508[0], doubleArray2508[1]);
        return doubleArray2508;
    }

    public static double[] computeVelocity(float f, ClientInput input, double d) {
        // 26.2 port: Yarn Input.movementForward/movementSideways -> MojMap ClientInput.keyPresses (Input record).
        net.minecraft.world.entity.player.Input presses = input.keyPresses;
        float f2 = (presses.forward() ? 1.0f : 0.0f) - (presses.backward() ? 1.0f : 0.0f);
        float f3 = (presses.left() ? 1.0f : 0.0f) - (presses.right() ? 1.0f : 0.0f);
        float f4 = f;
        if (f2 != 0.0f) {
            if (f3 > 0.0f) {
                f4 += f2 > 0.0f ? -MathConstants.num : MathConstants.num;
            } else if (f3 < 0.0f) {
                f4 += f2 > 0.0f ? MathConstants.num : -MathConstants.num;
            }
            f3 = 0.0f;
            if (f2 > 0.0f) {
                f2 = 1.0f;
            } else if (f2 < 0.0f) {
                f2 = (-1.0f);
            }
        }
        return new double[]{(f2 * d * (-Math.sin(Math.toRadians(f4)))) + (f3 * d * Math.cos(Math.toRadians(f4))), ((f2 * d) * Math.cos(Math.toRadians(f4))) - ((f3 * d) * (-Math.sin(Math.toRadians(f4))))};
    }

    public static double computeAcceleratedSpeed(double d, double d2, double d3, long j) {
        if (d2 >= d) {
            return d;
        }
        return Math.min(d2 + ((d - d2) * Mth.clamp((System.currentTimeMillis() - j) / (d3 * 1000.0d), 0.0d, 1.0d)), d);
    }

    public static boolean isPlayerMoving(Player playerEntity) {
        if (playerEntity == null) {
            return false;
        }
        // 26.2 port: Yarn Player.forwardSpeed/sidewaysSpeed removed; use horizontal delta movement.
        return playerEntity.getDeltaMovement().horizontalDistanceSqr() > 0.01d;
    }

    public static boolean is2181() {
        return isPlayerMoving(minecraftClient.player);
    }

    public static double getBaseMoveSpeed(boolean z) {
        return computeMoveSpeed(z, val);
    }

    public static double computeMoveSpeed(boolean z, double d) {
        double d2 = d;
        if (minecraftClient.player.hasEffect(MobEffects.SPEED)) {
            d2 *= 1.0d + (0.2d * (minecraftClient.player.getEffect(MobEffects.SPEED).getAmplifier() + 1));
        }
        if (z) {
            if (minecraftClient.player.hasEffect(MobEffects.SLOWNESS)) {
                d2 /= 1.0d + (0.2d * (minecraftClient.player.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1));
            }
        }
        return d2;
    }

    public static double getJumpBoostBonus() {
        double d = 0.0d;
        if (minecraftClient.player.hasEffect(MobEffects.JUMP_BOOST)) {
            d = 0.0d + ((minecraftClient.player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1d);
        }
        return d;
    }

    public static double computeStepUpHeight(double d) {
        if (!(minecraftClient.player.onGround() && minecraftClient.player.horizontalCollision)) {
            return 0.0d;
        }
        double longBitsToDouble = (-1.0d);
        AABB expand = minecraftClient.player.getBoundingBox().move(0.0d, 0.05d, 0.0d).inflate(0.05d);
        Iterator it = minecraftClient.level.getCollisions(minecraftClient.player, expand.setMaxY(expand.maxY + d)).iterator();
        while (it.hasNext()) {
            for (AABB box : ((VoxelShape) it.next()).toAabbs()) {
                if (box.maxY > longBitsToDouble) {
                    longBitsToDouble = box.maxY;
                }
            }
        }
        double y = longBitsToDouble - minecraftClient.player.getY();
        if (y <= 0.0d || y > d) {
            return 0.0d;
        }
        return y;
    }

    public static boolean isEntityMovingToward(Player playerEntity, Vec3 vec3d) {
        Vec3 velocity = playerEntity.getDeltaMovement();
        if (isPlayerMoving(playerEntity)) {
            return playerEntity.position().add(velocity.x * 10000.0d, 0.0d, velocity.z * 10000.0d).distanceToSqr(vec3d) < playerEntity.position().add(velocity.x * (-10000.0d), 0.0d, velocity.z * (-10000.0d)).distanceToSqr(vec3d);
        }
        return false;
    }

    public static boolean isLocalPlayerMovingToward(Vec3 vec3d) {
        return isEntityMovingToward(minecraftClient.player, vec3d);
    }

    public static float getMovementYaw() {
        if (minecraftClient.player.isFallFlying()) {
            double[] doubleArray2508 = computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, 1.0d);
            return (float) (Math.toDegrees(Math.atan2(doubleArray2508[1], doubleArray2508[0])) - MathConstants.num2);
        }
        float yaw = minecraftClient.player.getYRot();
        // 26.2 port: ClientInput has keyPresses (Input record); forward/strafe derived from it.
        net.minecraft.world.entity.player.Input presses = minecraftClient.player.input.keyPresses;
        boolean forward = presses.forward();
        boolean back = presses.backward();
        boolean left = presses.left();
        boolean right = presses.right();
        boolean z = forward != back;
        if (back) {
            yaw += 180.0f;
        }
        if (right && !z) {
            yaw += MathConstants.num2 * (back ? -1 : 1);
        }
        if (left && !z) {
            yaw -= MathConstants.num2 * (back ? -1 : 1);
        }
        return yaw;
    }
}
