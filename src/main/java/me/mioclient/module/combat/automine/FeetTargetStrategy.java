package me.mioclient.module.combat.automine;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.module.combat.automine.AutoMineLogic;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.module.combat.automine.MineStrategyBase;
import me.mioclient.module.combat.automine.MineTargetState;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import me.mioclient.module.combat.AutoCrystal;
import me.mioclient.module.combat.AutoMine;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Vec3i;

public final class FeetTargetStrategy extends MineStrategyBase {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving autoCrystal/speedmine null. */
    private static AutoCrystal autoCrystal;
    private static SpeedMine speedmine;

    public static AutoCrystal getAutoCrystal() {
        if (autoCrystal == null && ClientServices.moduleInjector != null) {
            try {
                autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
            } catch (Exception e) {
            }
        }
        return autoCrystal;
    }

    public static SpeedMine getSpeedmine() {
        if (speedmine == null && ClientServices.moduleInjector != null) {
            try {
                speedmine = (SpeedMine) ClientServices.moduleInjector.getModule(SpeedMine.class);
            } catch (Exception e) {
            }
        }
        return speedmine;
    }
    public static final List<Vec3i> list = List.of(new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 1, 0), new Vec3i(0, 0, -1), new Vec3i(-1, 0, 0));

    
    
    public static final class Record {
        public final BlockPos blockPos;
        public final BlockPos blockPos2;

        public Record(BlockPos blockPos, BlockPos blockPos2) {
            this.blockPos = blockPos;
            this.blockPos2 = blockPos2;
        }

        public BlockPos getTargetBlockPos() {
            return this.blockPos;
        }

        public BlockPos getSecondaryBlockPos() {
            return this.blockPos2;
        }
    }

    public FeetTargetStrategy(AutoMine autoMine) {
        super(autoMine);
    }

    @Override // me.mioclient.module.combat.automine.MineTargetStrategy
    public void findTargets(MineTargetState autoMineHelper) {
        LivingEntity playerEntity886 = this.autoMine.autoMineSearchHelper4.getTargetPlayer();
        boolean z = this.autoMine.fish.getValue().booleanValue() && autoMineHelper.getPriority() == 400 && autoMineHelper.getSecondaryBlockPos() != null;
        if (playerEntity886 == null || isSpaceBelowEmpty((Entity) playerEntity886)) {
            return;
        }
        if (!is1210((Player) playerEntity886) || z) {
            BlockPos ofFloored = BlockPos.containing(((Player) playerEntity886).position());
            List<BlockPos> list2010 = PlayerUtils.getSurroundPositions(playerEntity886);
            Block block = minecraftClient.level.getBlockState(ofFloored).getBlock();
            boolean z2 = block.getExplosionResistance() >= 600.0f && block != Blocks.AIR;
            if (!this.autoMine.speedMineHelper.is1228(500L) || ofFloored.equals(this.autoMine.speedMineHelper.getBlockPos())) {
                z2 = false;
            }
            Record record1207 = getRecord1207(list2010, z2, z);
            if (record1207.blockPos == null) {
                return;
            }
            autoMineHelper.tryUpdatePriority(600, autoMineHelper2 -> {
                if (autoMineHelper2.getSecondaryBlockPos() == null) {
                    autoMineHelper2.setSecondaryBlockPos(record1207.blockPos2);
                }
                autoMineHelper2.setBlockPos(record1207.blockPos);
            });
        }
    }

    public Record getRecord1207(List<BlockPos> list2, boolean z, boolean z2) {
        BlockPos blockPos = null;
        BlockPos blockPos2 = null;
        boolean z3 = false;
        list2.sort(Comparator.comparing(blockPos3 -> {
            if (minecraftClient.player.getBoundingBox().intersects(new AABB(blockPos3))) {
                return Double.valueOf((-9999.0d));
            }
            if (PlayerUtils.getSurroundPositions(minecraftClient.player).contains(blockPos3)) {
                return Double.valueOf((-999.0d));
            }
            boolean z4 = false;
            Iterator<Vec3i> it = list.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Vec3i next = it.next();
                if (next.getY() == 0) {
                    if (isCrystalPlaceableAt(blockPos3.offset(next).below())) {
                        z4 = true;
                        break;
                    }
                }
            }
            double squaredDistance = net.minecraft.world.phys.Vec3.atCenterOf(blockPos3).distanceToSqr(minecraftClient.player.position());
            if (!z4) {
                SpeedMine speedmine_lcl = getSpeedmine();
                float rangeMax = speedmine_lcl != null ? speedmine_lcl.range.getMaxValue().floatValue() : 6.0f;
                squaredDistance += Math.pow(rangeMax, 2.0d);
            }
            return Double.valueOf(-squaredDistance);
        }));
        Iterator<BlockPos> it = list2.iterator();
        while (it.hasNext()) {
            BlockPos next = it.next();
            if (z2) {
                next = next.below();
            }
            SpeedMine speedmine_lcl = getSpeedmine();
            boolean z4 = WorldUtils.isAir(next) && speedmine_lcl != null && next.equals(speedmine_lcl.getMineTargetPos());
            boolean z5 = this.autoMine.logic.getValue() == AutoMineLogic.GRIM || this.autoMine.logic.getValue() == AutoMineLogic.GRIMV3;
            if (z) {
                z4 = false;
            }
            if (speedmine_lcl != null && speedmine_lcl.hasCooldownElapsed(400L) && z5) {
                z4 = false;
            }
            if (is1208(next) || z2 || z5) {
                if (isValidMineTarget(next) || z4) {
                    if (isSpeedMineTarget(next)) {
                        blockPos = next;
                        z3 = true;
                    } else if (z3) {
                        blockPos2 = next;
                    } else {
                        blockPos2 = blockPos;
                        blockPos = next;
                    }
                }
            }
        }
        return new Record(blockPos, blockPos2);
    }

    public boolean is1208(BlockPos blockPos) {
        Iterator<Vec3i> it = list.iterator();
        while (it.hasNext()) {
            if (isCrystalPlaceableAt(blockPos.offset(it.next()).below())) {
                return true;
            }
        }
        return false;
    }

    public boolean is1209(BlockPos blockPos) {
        if (this.autoMine.safe.getValue().booleanValue()) {
            Iterator<BlockPos> it = PlayerUtils.getSurroundPositions(minecraftClient.player).iterator();
            while (it.hasNext()) {
                if (it.next().equals(blockPos)) {
                    return true;
                }
            }
        }
        List list2 = BlockPos.betweenClosedStream(minecraftClient.player.getBoundingBox().setMaxY(minecraftClient.player.getY())).map((v0) -> {
            return v0.immutable();
        }).filter(blockPos2 -> {
            return minecraftClient.level.getBlockState(blockPos2).getBlock().getExplosionResistance() >= 600.0f;
        }).toList();
        return list2.size() == 1 && list2.contains(blockPos);
    }

    public boolean is1210(Player playerEntity) {
        return BlockPos.betweenClosedStream(playerEntity.getBoundingBox().setMaxY(playerEntity.getY())).map((v0) -> {
            return v0.immutable();
        }).allMatch(blockPos -> {
            return minecraftClient.level.getBlockState(blockPos).is(Blocks.BEDROCK);
        });
    }

    public boolean isCrystalPlaceableAt(BlockPos blockPos) {
        AutoCrystal autoCrystal_lcl = getAutoCrystal();
        if (autoCrystal_lcl == null) {
            return false;
        }
        return BlockPlacer.isCrystalPlaceable(blockPos, autoCrystal_lcl.f112.getValue().booleanValue(), true, false, false, autoCrystal_lcl.lowerHitBox.getValue().booleanValue(), autoCrystal_lcl.hBFix.getValue().booleanValue());
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase, me.mioclient.module.combat.automine.MineTargetStrategy
    public boolean isEnabled() {
        return this.autoMine.feet.getValue().booleanValue();
    }

    @Override // me.mioclient.module.combat.automine.MineStrategyBase
    public boolean isValidMineTarget(BlockPos blockPos) {
        if (is1209(blockPos) || WorldUtils.getBlock(blockPos) == Blocks.COBWEB) {
            return false;
        }
        return super.isValidMineTarget(blockPos);
    }
}