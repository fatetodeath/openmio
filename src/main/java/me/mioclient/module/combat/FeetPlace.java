package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.ChunkDeltaUpdateEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.Delay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.mioclient.module.Module;
import me.mioclient.setting.BooleanSetting;

public class FeetPlace extends Delay {
    public Setting<Boolean> sequential = add(new BooleanSetting("Sequential", false));
    public Setting<Boolean> center = add(new BooleanSetting("Center", true));
    public Setting<Boolean> flatten = add(new BooleanSetting("Flatten", true));
    public Setting<Boolean> onlyObby = add(new BooleanSetting("OnlyObby", false));
    public final Stopwatch stopwatch;
    public boolean flag;

    public FeetPlace() {
        super("FeetPlace", "Surrounds your feet with obby.", Category.COMBAT, "surround");
        this.stopwatch = new Stopwatch();
        setPlacementEnabled(true);
    }

    @Override // me.mioclient.module.Delay, me.mioclient.module.Module
    public void onEnable() {
        if (!isNotInGame()) {
            centerPlayerInBlock();
        }
        super.onEnable();
        this.stopwatch.reset();
    }

    @Override // me.mioclient.module.Delay
    public List<BlockPos> getPlacePositions() {
        List<BlockPos> list877 = collectSurroundPositions(new ArrayList(), getRoundedBlockPos(), 0);
        list877.sort(getPlacementComparator());
        return list877;
    }

    @Override // me.mioclient.module.Delay
    public int getPriority() {
        return 999;
    }

    @Override // me.mioclient.module.Delay
    public int getSlotIndex() {
        return this.onlyObby.getValue().booleanValue() ? InventoryUtils.findInHotbar(Items.OBSIDIAN) : super.getSlotIndex();
    }

    @Override // me.mioclient.module.Delay
    public void do1323() {
        if (minecraftClient.player.isPassenger()) {
            return;
        }
        this.flag = !ClientServices.holeSnapRenderHelper.getTeleportStopwatch().hasElapsedMs(100L);
        if (this.flag) {
            this.val = minecraftClient.player.getY();
        }
        try {
            super.do1323();
        } catch (Throwable th) {
        }
    }

    @Override // me.mioclient.module.Delay
    public void sortPositionsBySupport(List<BlockPos> list) {
    }

    @Override // me.mioclient.module.Delay
    public boolean isPlayerFellTooFar() {
        if (this.flag) {
            return false;
        }
        return super.isPlayerFellTooFar();
    }

    @Listen
    public void onChunkDeltaUpdate(ChunkDeltaUpdateEvent chunkDeltaUpdateEvent) {
        if (this.sequential.getValue().booleanValue()) {
            do1323();
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundTeleportEntityPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundTeleportEntityPacket ? (ClientboundTeleportEntityPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundTeleportEntityPacket) {
            ClientboundTeleportEntityPacket playerPositionLookS2CPacket = packet904;
            if (minecraftClient.player.position().distanceToSqr(playerPositionLookS2CPacket.change().position()) > 9.0d) {
                this.val = playerPositionLookS2CPacket.change().position().y;
                ClientServices.holeSnapRenderHelper.getTeleportStopwatch().reset();
            }
        }
    }

    public List<BlockPos> collectSurroundPositions(List<Integer> list, BlockPos blockPos, int i) {
        int i2 = getSlotIndex();
        BlockState defaultState = Blocks.OBSIDIAN.defaultBlockState();
        if (i2 != -1) {
            BlockItem item = (minecraftClient.player.getInventory().getItem(i2).getItem()) instanceof BlockItem ? (BlockItem) (minecraftClient.player.getInventory().getItem(i2).getItem()) : null;
            if (item instanceof BlockItem) {
                defaultState = item.getBlock().defaultBlockState();
            }
        }
        ArrayList arrayList = new ArrayList();
        list.add(Integer.valueOf(blockPos.hashCode()));
        Iterator<Vec3> it = getNeighborOffsets(i).iterator();
        while (it.hasNext()) {
            Position position = (Vec3) it.next();
            BlockPos add = blockPos.offset(BlockPos.containing(position));
            if (((Vec3) position).y == (-1.0d)) {
                if (!ClientServices.holeSnapOverlayHelper.isHoleAt(add) || minecraftClient.player.onGround()) {
                    if (this.flatten.getValue().booleanValue() && !minecraftClient.player.isSwimming() && !add.equals(ClientServices.stashFinderHelper.getUnconfirmedBreakPos())) {
                    }
                }
            }
            VoxelShape collisionShape = defaultState.getCollisionShape(minecraftClient.level, BlockPos.ZERO);
            AABB box = collisionShape.isEmpty() ? new AABB(add) : BoxMath.getBoxAroundBlock(collisionShape.bounds(), add);
            do1328(add);
            if (!minecraftClient.level.getBlockState(add).canBeReplaced()) {
                if (!BoxMath.hasSignificantIntersection(minecraftClient.player.getBoundingBox(), new AABB(add))) {
                }
            }
            if (!minecraftClient.level.getEntitiesOfClass(LivingEntity.class, box, livingEntity -> {
                return livingEntity == minecraftClient.player || BoxMath.hasSignificantIntersection(box, livingEntity.getBoundingBox());
            }).isEmpty()) {
                if (!list.contains(Integer.valueOf(add.hashCode())) && ((Vec3) position).y == i) {
                    arrayList.addAll(collectSurroundPositions(list, add, i));
                }
            }
            if (minecraftClient.level.getBlockState(add).canBeReplaced()) {
                if (BlockPlacer.findSupportSide(add, this.setting4.getValue().booleanValue()) == null) {
                    BlockPos blockPos3032 = BlockPlacer.findNearbyPlaceablePos(add, 2, true, this.setting4.getValue().booleanValue());
                    if (blockPos3032 != null) {
                        arrayList.add(blockPos3032);
                    }
                }
                arrayList.add(add);
            }
        }
        return arrayList;
    }

    public List<Vec3> getNeighborOffsets(int i) {
        return Arrays.asList(new Vec3((-1.0d), i, 0.0d), new Vec3(1.0d, i, 0.0d), new Vec3(0.0d, i, 1.0d), new Vec3(0.0d, i, (-1.0d)), new Vec3(0.0d, i - 1, 0.0d));
    }

    public Comparator<BlockPos> getPlacementComparator() {
        return ClientServices.feetPlaceHelper.getInstantSpeed() <= 10.0d ? Comparator.comparing(blockPos -> {
            return Float.valueOf(getPlacementScore(minecraftClient.player.getYRot(), blockPos));
        }) : Comparator.comparing(blockPos2 -> {
            return Float.valueOf(getAngularPlacementScore(MovementUtils.getMovementYaw(), blockPos2));
        });
    }

    public float getPlacementScore(float f, BlockPos blockPos) {
        if (blockPos.getY() != PlayerUtils.getRoundedBlockPos().getY() || !isOnPartialBlock(minecraftClient.player)) {
            return getAngularPlacementScore(f, blockPos);
        }
        return (float) PlayerUtils.getRoundedBlockPos().distSqr(blockPos);
    }

    public float getAngularPlacementScore(float f, BlockPos blockPos) {
        return Mth.degreesDifferenceAbs(f, RotationManager.getRotationsTo(Vec3.atCenterOf(blockPos))[0]) + (blockPos.getY() * 0.001f);
    }

    public void centerPlayerInBlock() {
        if (isNotInGame() || !this.center.getValue().booleanValue() || !minecraftClient.player.onGround() || ClientServices.holeSnapOverlayHelper.isInHole() || PlayerUtils.getSurroundPositions(minecraftClient.player).size() <= 4) {
            return;
        }
        Vec3 vec3d = new Vec3(Math.floor(minecraftClient.player.getX()) + 0.5d, minecraftClient.player.getY(), Math.floor(minecraftClient.player.getZ()) + 0.5d);
        minecraftClient.player.setDeltaMovement((vec3d.x - minecraftClient.player.getX()) * 0.5d, minecraftClient.player.getDeltaMovement().y, (vec3d.z - minecraftClient.player.getZ()) * 0.5d);
        minecraftClient.player.setPos(vec3d);
    }

    public boolean isOnPartialBlock(LivingEntity livingEntity) {
        Iterator<BlockPos> it = PlayerUtils.getFeetPositions(livingEntity).iterator();
        while (it.hasNext()) {
            VoxelShape collisionShape = minecraftClient.level.getBlockState(it.next()).getCollisionShape(minecraftClient.level, BlockPos.ZERO);
            if (!collisionShape.isEmpty() && !BoxMath.isFullBlockBox(collisionShape.bounds())) {
                return true;
            }
        }
        return false;
    }
}