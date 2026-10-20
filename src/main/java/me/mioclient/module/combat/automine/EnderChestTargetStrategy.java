package me.mioclient.module.combat.automine;
import net.minecraft.world.phys.Vec3;

import me.mioclient.api.ClientServices;

import me.mioclient.module.combat.AutoMine;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.core.BlockPos;

public final class EnderChestTargetStrategy extends MineStrategyBase {
    public EnderChestTargetStrategy(AutoMine autoMine) {
        super(autoMine);
    }

    @Override // me.mioclient.module.combat.automine.MineTargetStrategy
    public void findTargets(MineTargetState autoMineHelper) {
        BlockPos blockPos = null;
        for (BlockEntity blockEntity : ClientServices.stashFinderHelper.getLoadedBlockEntities()) {
            if (minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(blockEntity.getBlockPos())) <= 2.2d && (blockEntity instanceof EnderChestBlockEntity)) {
                if (isValidMineTarget(blockEntity.getBlockPos())) {
                    if (blockPos != null) {
                        if (isSpeedMineTarget(blockEntity.getBlockPos())) {
                        }
                    }
                    blockPos = blockEntity.getBlockPos();
                }
            }
        }
        BlockPos blockPos2 = blockPos;
        if (blockPos2 == null) {
            return;
        }
        autoMineHelper.tryUpdatePriority(5, autoMineHelper2 -> {
            autoMineHelper2.setBlockPos(blockPos2);
        });
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase, me.mioclient.module.combat.automine.MineTargetStrategy
    public boolean isEnabled() {
        return this.autoMine.enderChests.getValue().booleanValue();
    }
}