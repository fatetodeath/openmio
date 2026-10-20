package me.mioclient.module.combat.antiphase;
import net.minecraft.world.phys.Vec3;

import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.api.ClientAccessor;

import java.util.List;
import me.mioclient.module.combat.AntiPhase;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

public class LadderTargetResolver implements ClientAccessor, AntiPhaseTargetResolver {
    public static final List<LadderShapeEntry> list = List.of(LadderShapeEntry.fromShape(Direction.EAST, Shapes.box(0.0d, 0.0d, 2.0d, 3.0d, 16.0d, 14.0d)), LadderShapeEntry.fromShape(Direction.WEST, Shapes.box(13.0d, 0.0d, 2.0d, 16.0d, 16.0d, 14.0d)), LadderShapeEntry.fromShape(Direction.NORTH, Shapes.box(2.0d, 0.0d, 0.0d, 14.0d, 16.0d, 3.0d)), LadderShapeEntry.fromShape(Direction.SOUTH, Shapes.box(2.0d, 0.0d, 13.0d, 14.0d, 16.0d, 16.0d)));

    @Override // me.mioclient.module.combat.antiphase.AntiPhaseTargetResolver
    public AntiPhasePlacement resolveTarget(AntiPhase antiPhase, Player playerEntity) {
        BlockPos blockPos2008 = PlayerUtils.getRoundedBlockPos((LivingEntity) playerEntity);
        Direction direction = null;
        AABB box = null;
        float intBitsToFloat = 999.0f;
        if (!WorldUtils.isAir(blockPos2008)) {
            return null;
        }
        for (LadderShapeEntry antiPhaseHelperData : list) {
            Direction direction842 = antiPhaseHelperData.getDirection();
            BlockPos offset = blockPos2008.relative(direction842);
            if (!isPositionBlocked(blockPos2008, playerEntity, antiPhaseHelperData) && (!antiPhase.strictDirection.getValue().booleanValue() || !isSideBlocked(offset, direction842))) {
                float squaredDistanceTo = (float) playerEntity.position().distanceToSqr(Vec3.atCenterOf(offset));
                if (squaredDistanceTo < intBitsToFloat) {
                    box = antiPhaseHelperData.getBox();
                    direction = direction842;
                    intBitsToFloat = squaredDistanceTo;
                }
            }
        }
        if (direction == null) {
            return null;
        }
        return new AntiPhasePlacement(blockPos2008.relative(direction), direction.getOpposite(), box.move(blockPos2008));
    }

    public boolean isPositionBlocked(BlockPos blockPos, Player playerEntity, LadderShapeEntry antiPhaseHelperData) {
        return BoxMath.getServerBox((LivingEntity) playerEntity).intersects(antiPhaseHelperData.getBox().move(blockPos)) || minecraftClient.level.getBlockState(blockPos.relative(antiPhaseHelperData.getDirection())).canBeReplaced();
    }

    public boolean isSideBlocked(BlockPos blockPos, Direction direction) {
        return !BlockPlacer.getInteractableSides(blockPos).contains(direction.getOpposite());
    }
}