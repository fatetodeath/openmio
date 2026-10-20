package me.mioclient.module.combat.antiphase;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class AntiPhasePlacement {
    public final BlockPos blockPos;
    public final Direction direction;
    public final AABB box;

    public AntiPhasePlacement(BlockPos blockPos, Direction direction, AABB box) {
        this.blockPos = blockPos;
        this.direction = direction;
        this.box = box;
    }

    public Vec3 getFaceCenter() {
        Vec3 centerPos = Vec3.atCenterOf(this.blockPos);
        return centerPos.relative(this.direction, 0.5d);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public AABB getBox() {
        return this.box;
    }
}