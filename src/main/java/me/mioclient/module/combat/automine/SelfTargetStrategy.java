package me.mioclient.module.combat.automine;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.module.combat.AutoMine;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class SelfTargetStrategy extends MineStrategyBase {
    public SelfTargetStrategy(AutoMine autoMine) {
        super(autoMine);
    }

    @Override // me.mioclient.module.combat.automine.MineTargetStrategy
    public void findTargets(MineTargetState autoMineHelper) {
        BlockPos blockPos2156 = getEyeLevelSolidBlock();
        if ((this.autoMine.autoMineSearchHelper4.getTargetPlayer() == null && blockPos2156 == null) || minecraftClient.player.isFallFlying()) {
            return;
        }
        AABB boundingBox = minecraftClient.player.getBoundingBox();
        if (this.autoMine.onlyCrawl.getValue().booleanValue() && !isSmallBoundingBox() && blockPos2156 == null) {
            return;
        }
        int i = (isSmallBoundingBox() || blockPos2156 != null) ? 1500 : 100;
        BlockPos blockPos2155 = findMineTargetPos(boundingBox);
        if (blockPos2155 == null) {
            return;
        }
        autoMineHelper.tryUpdatePriority(i, autoMineHelper2 -> {
            autoMineHelper2.setBlockPos(blockPos2155);
        });
    }

    public BlockPos findMineTargetPos(AABB box) {
        BlockPos down = BlockPos.containing(minecraftClient.player.position()).below();
        BlockPos blockPos2156 = getEyeLevelSolidBlock();
        if (isValidMineTarget(down) && ((isSpeedMineTarget(down) || isSmallBoundingBox()) && hasSolidGroundBelow())) {
            return down;
        }
        BlockPos up = BlockPos.containing(minecraftClient.player.position().add(0.0d, box.getYsize() - BoxMath.val, 0.0d)).above();
        if (blockPos2156 != null) {
            up = blockPos2156;
        }
        if (isValidMineTarget(up)) {
            return up;
        }
        return null;
    }

    public BlockPos getEyeLevelSolidBlock() {
        if (!this.autoMine.face.getValue().booleanValue()) {
            return null;
        }
        BlockPos ofFloored = BlockPos.containing(minecraftClient.player.getEyePosition());
        BlockState blockState = minecraftClient.level.getBlockState(ofFloored);
        if (blockState.isAir() || blockState.getBlock().getExplosionResistance() < 600.0f || isSmallBoundingBox() || !isValidMineTarget(ofFloored)) {
            return null;
        }
        return ofFloored;
    }

    public boolean isSmallBoundingBox() {
        return minecraftClient.player.getBoundingBox().getYsize() <= 1.0d;
    }

    public boolean hasSolidGroundBelow() {
        if (this.autoMine.downPriority.getValue().booleanValue()) {
            return true;
        }
        if (minecraftClient.level.isEmptyBlock(PlayerUtils.getRoundedBlockPos().below(2))) {
            return false;
        }
        for (BlockPos blockPos : PlayerUtils.getSurroundPositions(minecraftClient.player)) {
            BlockState blockState = minecraftClient.level.getBlockState(blockPos.below());
            if (blockState.isAir() || blockState.getBlock().getExplosionResistance() < 600.0f) {
                return false;
            }
        }
        return true;
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase, me.mioclient.module.combat.automine.MineTargetStrategy
    public boolean isEnabled() {
        return this.autoMine.self.getValue().booleanValue();
    }
}