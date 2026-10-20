package me.mioclient.manager;

import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.HoleData;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

public final class HoleTracker implements ClientAccessor {
    public List<HoleData> list = new ArrayList();
    public boolean flag;
    public Future<?> future;

    public HoleTracker() {
        eventBus.register(this);
        this.future = executorService.submit(new HoleScanTask(this));
    }

    public boolean isHoleAt(BlockPos blockPos) {
        return this.list.stream().anyMatch(holeSnapData -> {
            return holeSnapData.getBox().intersects(new AABB(blockPos));
        });
    }

    public static HoleData detectHole(BlockPos blockPos, Direction direction) {
        HoleData holeSnapData2724;
        if (isSolidBlock(blockPos) || !isSolidBlock(blockPos.below()) || isSolidBlock(blockPos.above())) {
            return null;
        }
        if (minecraftClient.level.getBlockState(blockPos.below()).getBlock() == Blocks.END_PORTAL) {
            return null;
        }
        boolean isSolidBlock = isSolidBlock(blockPos.above(2));
        HoleData.HoleSnapDataMode holeSnapDataMode = HoleData.HoleSnapDataMode.SAFE;
        Direction direction2 = null;
        for (Direction direction3 : Direction.values()) {
            if (direction != direction3.getOpposite() && direction3 != Direction.UP) {
                BlockPos offset = blockPos.relative(direction3);
                Block block = minecraftClient.level.getBlockState(offset).getBlock();
                if (isSolidBlock(offset)) {
                    if (block == Blocks.RESPAWN_ANCHOR) {
                        return null;
                    }
                    if ((block.getExplosionResistance() < 600.0f && block.getExplosionResistance() >= 0.0f) || block.defaultDestroyTime() == 0.0f) {
                        return null;
                    }
                    if (block.getExplosionResistance() >= 600.0f && block.defaultDestroyTime() >= 0.0f) {
                        holeSnapDataMode = HoleData.HoleSnapDataMode.UNSAFE;
                    }
                } else {
                    if (direction2 != null || direction != null || (holeSnapData2724 = detectHole(blockPos.relative(direction3), direction3)) == null) {
                        return null;
                    }
                    if (!holeSnapData2724.isTrapped()) {
                        isSolidBlock = false;
                    }
                    if (holeSnapDataMode == HoleData.HoleSnapDataMode.SAFE) {
                        holeSnapDataMode = holeSnapData2724.getHoleSnapDataMode();
                    }
                    direction2 = direction3;
                }
            }
        }
        return new HoleData(holeSnapDataMode, blockPos.immutable(), direction2 == null ? new AABB(blockPos) : new AABB(blockPos).expandTowards(direction2.getUnitVec3i().getX(), 0.0d, direction2.getUnitVec3i().getZ()), isSolidBlock);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.future.isDone() || this.future.isCancelled()) {
            this.future = executorService.submit(new HoleScanTask(this));
        }
        this.flag = isHoleAt(PlayerUtils.getRoundedBlockPos());
    }

    public static boolean isSolidBlock(BlockPos blockPos) {
        BlockState blockState = minecraftClient.level.getBlockState(blockPos);
        return (blockState.isAir() || blockState.getBlock() == Blocks.FIRE || blockState.getBlock() == Blocks.SOUL_FIRE || (blockState.getBlock() instanceof ButtonBlock) || (blockState.getBlock() instanceof TorchBlock) || (blockState.getBlock() instanceof RailBlock) || blockState.getBlock() == Blocks.LIGHT) ? false : true;
    }

    public List<HoleData> getHoles() {
        return this.list;
    }

    public void setHoles(List<HoleData> list) {
        this.list = list;
    }

    public boolean isInHole() {
        return this.flag;
    }
}
