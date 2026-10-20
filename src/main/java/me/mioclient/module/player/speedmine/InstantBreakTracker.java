package me.mioclient.module.player.speedmine;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.player.SpeedMine;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.core.BlockPos;

public final class InstantBreakTracker {
    public static final long num = 750;
    public final SpeedMine speedMine;
    public final Stopwatch stopwatch = new Stopwatch();
    public boolean flag;
    public BlockPos blockPos;

    public InstantBreakTracker(SpeedMine speedMine) {
        this.speedMine = speedMine;
    }

    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundBlockUpdatePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundBlockUpdatePacket ? (ClientboundBlockUpdatePacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundBlockUpdatePacket) {
            ClientboundBlockUpdatePacket blockUpdateS2CPacket = packet904;
            if (blockUpdateS2CPacket.getBlockState().isAir() && blockUpdateS2CPacket.getPos().equals(this.blockPos)) {
                reset();
            }
        }
        ClientboundSectionBlocksUpdatePacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundSectionBlocksUpdatePacket ? (ClientboundSectionBlocksUpdatePacket) (channelRead0Event.getPacket()) : null;
        if (packet9042 instanceof ClientboundSectionBlocksUpdatePacket) {
            packet9042.runUpdates((blockPos, blockState) -> {
                if (blockPos.equals(this.blockPos) && blockState.isAir()) {
                    reset();
                }
            });
        }
    }

    public void beginTracking(BlockPos blockPos) {
        if (this.flag) {
            return;
        }
        this.flag = true;
        this.blockPos = blockPos;
        this.stopwatch.reset();
    }

    public void recalculateLayout() {
        if (this.flag) {
            if (!WorldUtils.isBreakable(this.blockPos) || WorldUtils.isAir(this.blockPos)) {
                reset();
            }
            if (this.blockPos == null || this.blockPos.equals(this.speedMine.getMineTargetPos())) {
                return;
            }
            reset();
        }
    }

    public boolean isInstantBreakReady() {
        if (!this.flag || this.blockPos == null) {
            return false;
        }
        long max = Math.max((long) (Math.ceil((ClientServices.holeSnapRenderHelper.getPing() * 15.0f) / 50.0f) * 50.0d), 750L);
        if (this.speedMine.tPSSync.getValue().booleanValue()) {
            max = (long) (((float) max) / ClientServices.holeSnapRenderHelper.getTickRateFactor());
        }
        return this.stopwatch.hasElapsedMs((long) (((float) max) + this.speedMine.instantDelay.getValue().floatValue()));
    }

    public void reset() {
        this.flag = false;
        this.blockPos = null;
    }
}