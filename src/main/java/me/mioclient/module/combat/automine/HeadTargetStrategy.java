package me.mioclient.module.combat.automine;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import me.mioclient.module.combat.AutoMine;
import me.mioclient.module.combat.Range;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class HeadTargetStrategy extends MineStrategyBase {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving range null. */
    private static Range range;

    public static Range getRange() {
        if (range == null && ClientServices.moduleInjector != null) {
            try {
                range = (Range) ClientServices.moduleInjector.getModule(Range.class);
            } catch (Exception e) {
            }
        }
        return range;
    }

    public HeadTargetStrategy(AutoMine autoMine) {
        super(autoMine);
    }

    @Override // me.mioclient.module.combat.automine.MineTargetStrategy
    public void findTargets(MineTargetState autoMineHelper) {
        int i;
        LivingEntity playerEntity886 = this.autoMine.autoMineSearchHelper4.getTargetPlayer();
        if (playerEntity886 == null || ((Player) playerEntity886).isVisuallyCrawling() || isSpaceBelowEmpty((Entity) playerEntity886)) {
            return;
        }
        AABB boundingBox = ((Player) playerEntity886).getBoundingBox();
        boolean isExtraBreakEnabled = this.autoMine.isExtraBreakEnabled();
        boolean anyMatch = BlockPos.betweenClosedStream(boundingBox.setMaxY(boundingBox.minY)).anyMatch(blockPos -> {
            return BlockPlacer.getBlockAt(blockPos) == Blocks.BEDROCK;
        });
        if (BlockPos.betweenClosedStream(boundingBox.deflate(0.05d, 0.05d, 0.05d)).filter(blockPos2 -> {
            return (BlockPlacer.getBlockAt(blockPos2).getExplosionResistance() < 600.0f || is724() || BlockPlacer.getBlockAt(blockPos2) == Blocks.AIR) ? false : true;
        }).toList().isEmpty()) {
            AtomicReference<BlockPos> atomicReference = new AtomicReference<>(BlockPos.betweenClosedStream(boundingBox.setMaxY(boundingBox.maxY + 0.5d)).map(v0 -> v0.immutable()).filter(this::isValidHeadTarget).max(Comparator.comparing((BlockPos blockPos3) -> Boolean.valueOf(this.isSpeedMineTarget(blockPos3)))).orElse(null));
            AtomicReference<BlockPos> atomicReference2 = new AtomicReference<>(PlayerUtils.getSurroundPositions(playerEntity886).stream().map(v0 -> v0.above()).map(v0 -> v0.immutable()).filter(this::isValidHeadTarget).max(Comparator.comparing((BlockPos blockPos4) -> Boolean.valueOf(this.isSpeedMineTarget(blockPos4)))).orElse(null));
            if (atomicReference.get() == null && atomicReference2.get() != null && !is724()) {
                atomicReference.set(atomicReference2.get());
                atomicReference2.set(null);
            }
            if (atomicReference.get() == null) {
                return;
            }
            int i2 = 599;
            if (is724()) {
                if (anyMatch) {
                    i = isExtraBreakEnabled ? 900 : 1000;
                } else {
                    i = isExtraBreakEnabled ? 499 : 599;
                }
                autoMineHelper.tryUpdatePriority(i, autoMineHelper2 -> {
                    if (!isExtraBreakEnabled) {
                        autoMineHelper2.setBlockPos((BlockPos) atomicReference.get());
                    } else if (atomicReference2.get() == null) {
                        autoMineHelper2.setBlockPos((BlockPos) atomicReference.get());
                    } else {
                        autoMineHelper2.setSecondaryBlockPos((BlockPos) atomicReference.get());
                        autoMineHelper2.setBlockPos((BlockPos) atomicReference2.get());
                    }
                });
                return;
            }
            if (isSpeedMineTarget((BlockPos) atomicReference.get())) {
                i2 = 650;
            }
            Range range_lcl = getRange();
            if (this.autoMine.trapOverride.getValue().booleanValue() && range_lcl != null && range_lcl.isToggled()) {
                i2 = 650;
            }
            autoMineHelper.tryUpdatePriority(i2, autoMineHelper3 -> {
                autoMineHelper3.setBlockPos((BlockPos) atomicReference.get());
            });
        }
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase, me.mioclient.module.combat.automine.MineTargetStrategy
    public boolean isEnabled() {
        return this.autoMine.head.getValue().booleanValue();
    }

    public boolean isValidHeadTarget(BlockPos blockPos) {
        return ((WorldUtils.isBreakable(blockPos) && (BlockPlacer.getBlockAt(blockPos) == Blocks.OBSIDIAN || is724())) || isSpeedMineTarget(blockPos)) && (minecraftClient.level.isEmptyBlock(blockPos.above()) || is724());
    }

    public boolean is724() {
        return this.autoMine.logic.getValue() == AutoMineLogic.GRIMV3;
    }
}