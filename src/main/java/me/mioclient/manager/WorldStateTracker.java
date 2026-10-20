package me.mioclient.manager;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.interaction.SlotChangeEvent;
import me.mioclient.event.events.world.BlockCollisionShapeEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockStateSnapshot;
import me.mioclient.util.world.TimedBlockEntry;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.mixin.ducks.DuckAbstractBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import me.mioclient.render.RenderConstants;
import me.mioclient.manager.RotationManager;

public class WorldStateTracker implements ClientAccessor {
    public ItemStack itemStack;
    public ItemStack itemStack2;
    public int num;
    public final List<BlockEntity> list = Collections.synchronizedList(new ArrayList());
    public final Map<BlockPos, TimedBlockEntry> map = Collections.synchronizedMap(new HashMap());
    public final Set<BlockPos> set = Collections.synchronizedSet(new HashSet());
    public final Set<BlockPos> set2 = Collections.synchronizedSet(new HashSet());
    public final Set<BlockStateSnapshot> set3 = Collections.synchronizedSet(new HashSet());

    @Deprecated
    public BlockPos unconfirmedBreak = null;
    public final Stopwatch stopwatch = new Stopwatch();

    public WorldStateTracker() {
        eventBus.register(this);
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        this.map.clear();
        this.set.clear();
        this.list.clear();
        this.set3.clear();
    }

    @Listen(get219= RenderConstants.num)
    public void onTickPost(TickPostEvent tickPostEvent) {
        syncHandStacks();
    }

    @Listen(get219= RenderConstants.num5)
    public void onTick(TickEvent tickEvent) {
        this.set2.clear();
        if (this.stopwatch.hasElapsedMs(150L)) {
            this.unconfirmedBreak = null;
        }
        this.set.removeIf(blockPos -> {
            return !blockPos.closerThan(minecraftClient.player.blockPosition(), 128.0d);
        });
        synchronized (this.map) {
            ArrayList arrayList = new ArrayList();
            for (Map.Entry<BlockPos, TimedBlockEntry> entry : this.map.entrySet()) {
                if (System.currentTimeMillis() > entry.getValue().getTimestamp() + 250) {
                    arrayList.add(entry.getKey());
                }
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                this.map.remove((BlockPos) it.next());
            }
        }
        synchronized (getLoadedBlockEntities()) {
            this.list.clear();
            this.list.addAll(WorldUtils.getLoadedBlockEntities());
            this.list.removeIf((v0) -> {
                return Objects.isNull(v0);
            });
            this.list.sort(Comparator.comparing(blockEntity -> {
                return Double.valueOf(minecraftClient.player.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(blockEntity.getBlockPos())));
            }));
        }
        synchronized (this.set3) {
            for (BlockStateSnapshot blockStateSearchHelper4 : this.set3) {
                if (blockStateSearchHelper4.isInvalid()) {
                    minecraftClient.level.setBlock(blockStateSearchHelper4.getBlockPos(), blockStateSearchHelper4.getBlockState(), 3);
                }
            }
        }
        this.set3.removeIf((v0) -> {
            return v0.isInvalid();
        });
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        ServerboundUseItemOnPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundUseItemOnPacket ? (ServerboundUseItemOnPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundUseItemOnPacket) {
            ServerboundUseItemOnPacket playerInteractBlockC2SPacket = packet904;
            BlockItem item = (getHandItemStack(playerInteractBlockC2SPacket.getHand()).getItem()) instanceof BlockItem ? (BlockItem) (getHandItemStack(playerInteractBlockC2SPacket.getHand()).getItem()) : null;
            if (item instanceof BlockItem) {
                BlockItem blockItem = item;
                BlockHitResult blockHitResult = playerInteractBlockC2SPacket.getHitResult();
                BlockPos offset = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
                this.map.put(offset, TimedBlockEntry.getBlockData2589(blockItem.getBlock()));
                this.set2.add(offset);
                syncHandStacks();
            }
        }
    }

    @Listen
    public void onSlotChange(SlotChangeEvent speedMineEvent) {
        syncHandStacks();
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundBlockUpdatePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundBlockUpdatePacket ? (ClientboundBlockUpdatePacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundBlockUpdatePacket) {
            ClientboundBlockUpdatePacket blockUpdateS2CPacket = packet904;
            this.set3.removeIf(blockStateSearchHelper4 -> {
                return blockStateSearchHelper4.getBlockPos().equals(blockUpdateS2CPacket.getPos());
            });
            if (blockUpdateS2CPacket.getBlockState().isAir()) {
                this.set.remove(blockUpdateS2CPacket.getPos());
                this.map.remove(blockUpdateS2CPacket.getPos());
            } else {
                if (this.map.remove(blockUpdateS2CPacket.getPos()) != null) {
                    this.set.add(blockUpdateS2CPacket.getPos());
                }
            }
        }
        ClientboundSectionBlocksUpdatePacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundSectionBlocksUpdatePacket ? (ClientboundSectionBlocksUpdatePacket) (channelRead0Event.getPacket()) : null;
        if (packet9042 instanceof ClientboundSectionBlocksUpdatePacket) {
            packet9042.runUpdates((blockPos, blockState) -> {
                this.set3.removeIf(blockStateLlamaRenderHelper -> {
                    return blockStateLlamaRenderHelper.getBlockPos().equals(blockPos);
                });
            });
        }
        ClientboundLoginPacket packet9043 = (channelRead0Event.getPacket()) instanceof ClientboundLoginPacket ? (ClientboundLoginPacket) (channelRead0Event.getPacket()) : null;
        if (packet9043 instanceof ClientboundLoginPacket) {
            this.num = packet9043.chunkRadius();
        }
        ClientboundSetChunkCacheRadiusPacket packet9044 = (channelRead0Event.getPacket()) instanceof ClientboundSetChunkCacheRadiusPacket ? (ClientboundSetChunkCacheRadiusPacket) (channelRead0Event.getPacket()) : null;
        if (packet9044 instanceof ClientboundSetChunkCacheRadiusPacket) {
            this.num = packet9044.getRadius();
        }
    }

    @Listen
    public void onBlockCollisionShape(BlockCollisionShapeEvent voxelShapeEvent) {
        if (voxelShapeEvent.getBlockPos() == null || minecraftClient.player == null) {
            return;
        }
        if (this.map.containsKey(voxelShapeEvent.getBlockPos())) {
            TimedBlockEntry blockData = this.map.get(voxelShapeEvent.getBlockPos());
            if (blockData == null || blockData.getBlockState() == null) {
                return;
            }
            AABB box = new AABB(voxelShapeEvent.getBlockPos());
            if (minecraftClient.player.getBoundingBox().intersects(box) || !((DuckAbstractBlock)(Object) blockData.getBlockState()).isCollidable()) {
                return;
            }
            if (!minecraftClient.level.getEntities(EntityTypeTest.forClass(Entity.class), box, entity -> {
                return ((entity instanceof ThrownExperienceBottle) || (entity instanceof ItemEntity) || (entity instanceof ExperienceOrb) || (entity instanceof Arrow) || (entity instanceof ThrownEnderpearl)) ? false : true;
            }).isEmpty()) {
                return;
            }
            voxelShapeEvent.setShape(blockData.getBlockState().defaultBlockState().getShape(minecraftClient.level, voxelShapeEvent.getBlockPos()));
        }
        if (RotationManager.is724() && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(300L) && voxelShapeEvent.getBlockPos().equals(this.unconfirmedBreak)) {
            voxelShapeEvent.setShape(Shapes.empty());
        }
    }

    public void breakBlockWithSnapshot(BlockPos blockPos) {
        this.set3.add(BlockStateSnapshot.getBlockStateSearchHelper42779(blockPos));
        minecraftClient.gameMode.destroyBlock(blockPos);
    }

    public void setUnconfirmedBreak(BlockPos blockPos) {
        this.unconfirmedBreak = blockPos;
        this.stopwatch.reset();
    }

    public BlockPos getUnconfirmedBreakPos() {
        return this.unconfirmedBreak;
    }

    public Map<BlockPos, TimedBlockEntry> getTimedBlockMap() {
        return this.map;
    }

    public ItemStack getHandItemStack(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? this.itemStack : this.itemStack2;
    }

    public void syncHandStacks() {
        this.itemStack = minecraftClient.player.getMainHandItem().copy();
        this.itemStack2 = minecraftClient.player.getOffhandItem().copy();
    }

    public List<BlockEntity> getLoadedBlockEntities() {
        return this.list;
    }

    public boolean isBlockConfirmed(BlockPos blockPos) {
        return this.set.contains(blockPos) || this.map.containsKey(blockPos);
    }

    public boolean isBlockSolid(BlockPos blockPos) {
        return !minecraftClient.level.getBlockState(blockPos).canBeReplaced() || this.set2.contains(blockPos);
    }

    public int getViewDistance() {
        return this.num;
    }
}
