package me.mioclient.module.combat.automine;
import net.minecraft.world.phys.Vec3;

import me.mioclient.util.math.BoxMath;

import java.util.Comparator;
import java.util.List;
import me.mioclient.module.combat.AutoMine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

public final class BurrowTargetStrategy extends MineStrategyBase {
    public BurrowTargetStrategy(AutoMine autoMine) {
        super(autoMine);
    }

    @Override // me.mioclient.module.combat.automine.MineTargetStrategy
    public void findTargets(MineTargetState autoMineHelper) {
        boolean isExtraBreakEnabled = this.autoMine.isExtraBreakEnabled();
        Player playerEntity886 = this.autoMine.autoMineSearchHelper4.getTargetPlayer();
        if (playerEntity886 == null) {
            return;
        }
        List<BlockPos> list = BlockPos.betweenClosedStream(playerEntity886.getBoundingBox().deflate(BoxMath.val, 0.0d, BoxMath.val).setMaxY(playerEntity886.getY())).map(v0 -> v0.immutable()).filter(blockPos -> {
            return this.isValidMineTarget(blockPos);
        }).filter(blockPos2 -> {
            Block block = minecraftClient.level.getBlockState(blockPos2).getBlock();
            return (block.getExplosionResistance() < 600.0f || block == Blocks.COBWEB || block == Blocks.ANVIL) ? false : true;
        }).sorted(Comparator.comparing(blockPos3 -> {
            BlockPos pos3 = (BlockPos) blockPos3;
            return isSpeedMineTarget(pos3) ? Double.valueOf((-1.0d)) : Double.valueOf(playerEntity886.distanceToSqr(Vec3.atCenterOf(pos3)));
        })).toList();
        if (list.isEmpty()) {
            return;
        }
        int i = 700;
        if (isExtraBreakEnabled && list.size() == 1) {
            i = 500;
        }
        autoMineHelper.tryUpdatePriority(i, autoMineHelper2 -> {
            if (!isExtraBreakEnabled) {
                autoMineHelper2.setBlockPos((BlockPos) list.getFirst());
            } else if (list.size() == 1) {
                autoMineHelper2.setSecondaryBlockPos((BlockPos) list.getFirst());
            } else {
                autoMineHelper2.setBlockPos((BlockPos) list.getFirst());
                autoMineHelper2.setSecondaryBlockPos((BlockPos) list.get(1));
            }
        });
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase, me.mioclient.module.combat.automine.MineTargetStrategy
    public boolean isEnabled() {
        return this.autoMine.burrow.getValue().booleanValue();
    }
}