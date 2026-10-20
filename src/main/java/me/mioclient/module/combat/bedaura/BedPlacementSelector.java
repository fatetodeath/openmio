package me.mioclient.module.combat.bedaura;

import me.mioclient.api.ClientFunction;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.world.BlockPlacer;

import java.util.Iterator;
import java.util.List;
import me.mioclient.module.combat.BedAura;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;

public class BedPlacementSelector implements ClientFunction<LivingEntity, BedPlacement> {
    public static final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    public final BedAura bedAura;
    public Direction direction;
    public Vec3 vec3d;
    public float val;

    public BedPlacementSelector(BedAura bedAura) {
        this.bedAura = bedAura;
    }

    @Override // me.mioclient.api.ClientFunction
    /* renamed from: getBedAuraData964, reason: merged with bridge method [inline-methods] */
    public BedPlacement selectTarget(LivingEntity livingEntity) {
        this.val = this.bedAura.range2.getValue().floatValue();
        Vec3 pos = livingEntity.position();
        float intBitsToFloat = 2.0f;
        BedPlacement bedAuraData = null;
        float f = -intBitsToFloat;
        while (true) {
            float f2 = f;
            if (f2 >= intBitsToFloat) {
                return bedAuraData;
            }
            float f3 = -intBitsToFloat;
            while (true) {
                float f4 = f3;
                if (f4 < 3.0f) {
                    float f5 = -intBitsToFloat;
                    while (true) {
                        float f6 = f5;
                        if (f6 < intBitsToFloat) {
                            this.vec3d = null;
                            mutable.set(pos.x + f2, pos.y + f4, pos.z + f6);
                            BedPlacement bedAuraData965 = getBedAuraData965(livingEntity, mutable);
                            if (bedAuraData965 != null && (bedAuraData == null || bedAuraData965.getTargetDamage() >= bedAuraData.getTargetDamage())) {
                                bedAuraData = bedAuraData965;
                            }
                            f5 = f6 + 1.0f;
                        } else {
                            break;
                        }
                    }
                    f3 = f4 + 1.0f;
                } else {
                    break;
                }
            }
            f = f2 + 1.0f;
        }
    }

    public BedPlacement getBedAuraData965(LivingEntity livingEntity, BlockPos blockPos) {
        if (!minecraftClient.level.getBlockState(blockPos).canBeReplaced()) {
            if (!this.bedAura.isBedPartAt(blockPos, BedPart.HEAD)) {
                return null;
            }
        }
        BlockPos blockPos2 = null;
        Direction direction = null;
        Iterator it = Direction.Plane.HORIZONTAL.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Direction direction2 = (Direction) it.next();
            BlockPos offset = blockPos.relative(direction2);
            if (!BlockPlacer.isSpaceFree(offset, true, false)) {
                if (!this.bedAura.isBedPartAt(offset, BedPart.FOOT)) {
                    continue;
                }
            }
            direction = getDirection966(direction2.getOpposite(), offset, this.bedAura.strictDirection.getValue().booleanValue());
            if (direction != null) {
                this.direction = direction2.getOpposite();
                blockPos2 = offset;
                break;
            }
        }
        if (blockPos2 == null) {
            return null;
        }
        float f = ExplosionDamageCalculator.getBedDamage(Vec3.atCenterOf(blockPos), livingEntity, this.bedAura.getPredictedBox(livingEntity));
        if (f < this.bedAura.minDamage.getValue().floatValue()) {
            return null;
        }
        float f2 = ExplosionDamageCalculator.getBedDamage(Vec3.atCenterOf(blockPos), minecraftClient.player);
        if (f2 > this.bedAura.maxSelfDamage.getValue().floatValue()) {
            return null;
        }
        if (this.vec3d == null && !this.bedAura.rotate.getValue().booleanValue()) {
            this.vec3d = Vec3.atCenterOf(blockPos2).relative(direction, 0.5d);
        }
        return new BedPlacement(blockPos2, blockPos.immutable(), f, f2, getBlockHitResult969(direction, blockPos2));
    }

    public Direction getDirection966(Direction direction, BlockPos blockPos, boolean z) {
        boolean booleanValue = this.bedAura.airPlace.getValue().booleanValue();
        for (Direction direction2 : Direction.values()) {
            BlockPos offset = blockPos.relative(direction2);
            if (offset.getY() < minecraftClient.level.getMaxY() && (WorldUtils.isStashBlock(offset) || booleanValue)) {
                if (z) {
                    if (!BlockPlacer.getInteractableSides(offset).contains(direction2.getOpposite())) {
                        continue;
                    }
                }
                if (this.bedAura.rotate.getValue().booleanValue()) {
                    this.vec3d = getBedPlacementEyePosition(direction2, direction, blockPos);
                    if (this.vec3d == null) {
                    }
                }
                return direction2;
            }
        }
        return null;
    }

    public Vec3 getBedPlacementEyePosition(Direction direction, Direction direction2, BlockPos blockPos) {
        for (Vec3 vec3d : getList968(blockPos, direction)) {
            if (vec3d.distanceTo(minecraftClient.player.getEyePosition()) <= this.val && direction2 == Direction.fromYRot(RotationManager.getRotationsTo(vec3d)[0])) {
                return vec3d;
            }
        }
        return null;
    }

    public List<Vec3> getList968(BlockPos blockPos, Direction direction) {
        return direction == null ? AutoCraftMode.X8.getWorldCorners(blockPos) : AutoCraftMode.X8.getVisibleFaceCorners(blockPos, direction);
    }

    public BlockHitResult getBlockHitResult969(Direction direction, BlockPos blockPos) {
        if ((!this.bedAura.airPlace.getValue().booleanValue() || WorldUtils.isStashBlock(blockPos.relative(direction))) && !(this.bedAura.liquidPlace.getValue().booleanValue() && WorldUtils.hasFluid(blockPos))) {
            return new BlockHitResult(this.vec3d, direction.getOpposite(), blockPos.relative(direction), false);
        }
        this.vec3d = getBedPlacementEyePosition((Direction) null, this.direction, blockPos);
        return new BlockHitResult(this.vec3d, direction, blockPos, false);
    }
}