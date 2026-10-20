package me.mioclient.module.combat.automine;
import net.minecraft.world.phys.Vec3;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.api.ClientAccessor;

import me.mioclient.module.combat.AutoMine;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;

public abstract class MineStrategyBase implements ClientAccessor, MineTargetStrategy {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving speedmine null. */
    private static SpeedMine speedmine;

    public static SpeedMine getSpeedmine() {
        if (speedmine == null && ClientServices.moduleInjector != null) {
            try {
                speedmine = (SpeedMine) ClientServices.moduleInjector.getModule(SpeedMine.class);
            } catch (Exception e) {
            }
        }
        return speedmine;
    }
    public final AutoMine autoMine;

    public MineStrategyBase(AutoMine autoMine) {
        this.autoMine = autoMine;
    }

    public boolean isValidMineTarget(BlockPos blockPos) {
        if (blockPos.equals(this.autoMine.speedMineHelper.getRecentBlockPos())) {
            return false;
        }
        SpeedMine speedmine_lcl = getSpeedmine();
        if (minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(blockPos)) > this.autoMine.getRange()) {
            return false;
        }
        if (speedmine_lcl != null && !speedmine_lcl.isBlockAllowed(blockPos)) {
            return false;
        }
        if (this.autoMine.strictDirection.getValue().booleanValue() && BlockPlacer.getInteractableSides(blockPos).isEmpty()) {
            return false;
        }
        if (!this.autoMine.raytrace.getValue().booleanValue() || WorldUtils.isAnyPositionVisible(AutoCraftMode.X8.getWorldCorners(blockPos))) {
            return WorldUtils.isBreakable(blockPos);
        }
        return false;
    }

    public boolean isSpeedMineTarget(BlockPos blockPos) {
        if (blockPos == null) {
            return false;
        }
        SpeedMine speedmine_lcl = getSpeedmine();
        if (speedmine_lcl == null) {
            return false;
        }
        return blockPos.equals(speedmine_lcl.getMineTargetPos()) || blockPos.equals(speedmine_lcl.getRebreakTargetPos());
    }

    public boolean isSpaceBelowEmpty(Entity entity) {
        return minecraftClient.level.noCollision(entity.getBoundingBox().expandTowards(0.0d, (-0.1d), 0.0d));
    }

    public boolean isEnabled() {
        return false;
    }
}