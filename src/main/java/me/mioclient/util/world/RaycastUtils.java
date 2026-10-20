package me.mioclient.util.world;

import me.mioclient.mode.world.ReachMode;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;

import me.mioclient.api.ClientAccessor;

import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;

public class RaycastUtils implements ClientAccessor {
    // 26.2 port: Yarn BlockGetter.raycast with per-block shape factories removed;
    // approximate with vanilla ClipContext.clip + ReachMode filter on the hit block.
    public static BlockHitResult performCustomBlockRaycast(CustomRaycastContext ctx) {
        BlockHitResult hit = minecraftClient.level.clip(new ClipContext(ctx.getStart(), ctx.getEnd(), ctx.getShapeType(), ctx.getFluidHandling(), ctx.getShapeContext()));
        if (hit.getType() == HitResult.Type.MISS) {
            return hit;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = minecraftClient.level.getBlockState(pos);
        ReachMode test = ctx.getBlockFilter().test(pos, state);
        if (test == ReachMode.IGNORE) {
            Vec3 diff = ctx.getStart().subtract(ctx.getEnd());
            return BlockHitResult.miss(ctx.getStart(), Direction.getApproximateNearest(diff.x, diff.y, diff.z), BlockPos.containing(ctx.getEnd()));
        }
        return hit;
    }

    public static HitResult performPlayerAimRaycast(Vec3 vec3d, float f, float f2, float f3) {
        float f4 = f2 * MathConstants.val4;
        float f5 = (-f) * MathConstants.val4;
        float cos = Mth.cos(f5);
        float sin = Mth.sin(f5);
        float cos2 = Mth.cos(f4);
        Vec3 vec3d2 = new Vec3(sin * cos2, -Mth.sin(f4), cos * cos2);
        return minecraftClient.level.clip(new ClipContext(vec3d, vec3d.add(vec3d2.x * f3, vec3d2.y * f3, vec3d2.z * f3), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, minecraftClient.player));
    }

    public static int getDistanceToFloor() {
        return (int) Math.round(minecraftClient.player.getY() - minecraftClient.level.clip(new ClipContext(minecraftClient.player.position(), new Vec3(minecraftClient.player.getX(), minecraftClient.level.dimensionType().minY(), minecraftClient.player.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getLocation().y);
    }

    public static int getDistanceToCeiling() {
        return (minecraftClient.level.clip(new ClipContext(minecraftClient.player.position(), new Vec3(minecraftClient.player.getX(), minecraftClient.level.dimensionType().minY() + minecraftClient.level.dimensionType().height(), minecraftClient.player.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getBlockPos().getY() - (((int) Math.round(minecraftClient.player.getY())) + 1)) - 1;
    }

    public static boolean hasLineOfSight(Entity entity) {
        AABB boundingBox = entity.getBoundingBox();
        float f = 0.0f;
        while (true) {
            float f2 = f;
            if (f2 > 1.0f) {
                return false;
            }
            List<Vec3> list228 = BoxMath.getCornersSortedByDistance(boundingBox, boundingBox.minY + ((boundingBox.maxY - boundingBox.minY) * f2));
            for (int i = 0; i < list228.size() - 1; i++) {
                if (isLineUnobstructed(minecraftClient.player.getEyePosition(), list228.get(i))) {
                    return true;
                }
            }
            f = f2 + 0.5f;
        }
    }

    public static boolean isLineUnobstructed(Vec3 vec3d, Vec3 vec3d2) {
        return minecraftClient.level.clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS;
    }
}
