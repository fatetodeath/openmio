package me.mioclient.module.combat.antiphase;

import me.mioclient.module.combat.antiphase.LadderShapeEntry;
import me.mioclient.module.combat.antiphase.SolidBlockTargetResolver;

import java.util.List;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

public class FacePlateTargetResolver extends SolidBlockTargetResolver {
    public static final List<LadderShapeEntry> list = List.of(LadderShapeEntry.fromShape(Direction.EAST, Shapes.box(0.0d, 2.0d, 2.0d, 1.0d, 14.0d, 14.0d)), LadderShapeEntry.fromShape(Direction.WEST, Shapes.box(15.0d, 2.0d, 2.0d, 16.0d, 14.0d, 14.0d)), LadderShapeEntry.fromShape(Direction.NORTH, Shapes.box(2.0d, 2.0d, 0.0d, 14.0d, 14.0d, 1.0d)), LadderShapeEntry.fromShape(Direction.SOUTH, Shapes.box(2.0d, 2.0d, 15.0d, 14.0d, 14.0d, 16.0d)), LadderShapeEntry.fromShape(Direction.DOWN, Shapes.box(2.0d, 0.0d, 2.0d, 14.0d, 1.0d, 14.0d)), LadderShapeEntry.fromShape(Direction.UP, Shapes.box(2.0d, 15.0d, 2.0d, 14.0d, 16.0d, 14.0d)));

    @Override // me.mioclient.module.combat.antiphase.SolidBlockTargetResolver
    public AABB getTargetBox(BlockPos blockPos, Direction direction) {
        for (LadderShapeEntry antiPhaseHelperData : list) {
            if (antiPhaseHelperData.getDirection() == direction.getOpposite()) {
                return antiPhaseHelperData.getBox().move(blockPos);
            }
        }
        return new AABB(blockPos);
    }
}