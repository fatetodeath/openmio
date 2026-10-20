package me.mioclient.util.player;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.world.TimedBlockEntry;

import me.mioclient.api.ClientAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class PlayerMotionPredictor implements ClientAccessor {
    public static final float val = (-0.0784f);
    public static final float val2 = MovementUtils.val;
    public static final float val3 = val2 * 0.3f;
    public static final float val4 = (val2 * 1.4f) + 0.2f;
    public final List<AABB> list = new ArrayList();
    public final Player playerEntity;

    public PlayerMotionPredictor(Player playerEntity) {
        this.playerEntity = playerEntity;
    }

    public void recalculateLayout() {
        synchronized (this.list) {
            this.list.clear();
            double x = this.playerEntity.getX() - this.playerEntity.xo;
            double y = this.playerEntity.getY() - this.playerEntity.yo;
            double z = this.playerEntity.getZ() - this.playerEntity.zo;
            double hypot = Math.hypot(x, z);
            double clamp = Mth.clamp(hypot, this.playerEntity.isCrouching() ? val3 : val2, val4);
            if (hypot != 0.0d) {
                x = (x / hypot) * clamp;
                z = (z / hypot) * clamp;
            }
            if (EntityUtil.isOnGround(this.playerEntity) && y > 0.0d) {
                y = (-0.07840000092983246d);
            }
            AABB box234 = BoxMath.getServerBox(this.playerEntity);
            if (y > 0.0d) {
                y = applyGravity(y);
            }
            if (hypot < MovementUtils.val2) {
                x = 0.0d;
                z = 0.0d;
            }
            if (hypot < MovementUtils.val2 && Math.abs(y) < 0.1d) {
                this.list.addAll(Collections.nCopies(20, box234));
                return;
            }
            for (int i = 0; i <= 20; i++) {
                y = !is2711(this.playerEntity, box234.move(0.0d, (-0.01d), 0.0d)) ? applyGravity(y) : (-0.07840000092983246d);
                List entityCollisions = minecraftClient.level.getEntityCollisions(this.playerEntity, box234.expandTowards(x, y, z));
                ArrayList arrayList = new ArrayList();
                synchronized (ClientServices.stashFinderHelper.getTimedBlockMap()) {
                    for (Map.Entry<BlockPos, TimedBlockEntry> entry : ClientServices.stashFinderHelper.getTimedBlockMap().entrySet()) {
                        arrayList.add(entry.getValue().getBlockState().defaultBlockState().getCollisionShape(minecraftClient.level, entry.getKey()));
                    }
                }
                AABB stretched2711 = box234.expandTowards(x, y, z);
                arrayList.addAll(entityCollisions);
                for (VoxelShape voxelShape : minecraftClient.level.getBlockCollisions(this.playerEntity, stretched2711)) {
                    arrayList.add(voxelShape);
                }
                box234 = calculateMovementBox(box234, arrayList, x, y, z);
                this.list.add(box234);
            }
        }
    }

    public static double applyGravity(double d) {
        return (d + 0.08d) * 0.98d;
    }

    public static AABB calculateMovementBox(AABB box, List<VoxelShape> list, double d, double d2, double d3) {
        if (list.isEmpty()) {
            return box.move(d, d2, d3);
        }
        if (d2 != 0.0d) {
            d2 = collideList(list, Direction.Axis.Y, box, d2);
            if (d2 != 0.0d) {
                box = box.move(0.0d, d2, 0.0d);
            }
        }
        boolean z = Math.abs(d) < Math.abs(d3);
        if (z && d3 != 0.0d) {
            d3 = collideList(list, Direction.Axis.Z, box, d3);
            if (d3 != 0.0d) {
                box = box.move(0.0d, 0.0d, d3);
            }
        }
        if (d != 0.0d) {
            d = collideList(list, Direction.Axis.X, box, d);
            if (!z && d != 0.0d) {
                box = box.move(d, 0.0d, 0.0d);
            }
        }
        if (!z && d3 != 0.0d) {
            d3 = collideList(list, Direction.Axis.Z, box, d3);
        }
        return box.move(d, d2, d3);
    }

    // 26.2 port: Shapes.calculateMaxOffset removed; VoxelShape.collide per shape.
    private static double collideList(List<VoxelShape> list, Direction.Axis axis, AABB box, double d) {
        for (VoxelShape shape : list) {
            d = shape.collide(axis, box, d);
        }
        return d;
    }

    public boolean is2711(Player playerEntity, AABB box) {
        return minecraftClient.level.getBlockCollisions((Entity) playerEntity, box).iterator().hasNext();
    }

    public synchronized List<AABB> getList2712() {
        return this.list;
    }
}
