package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.mode.player.ScaffoldMode_2;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.concurrent.LockedTaskRunner;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.entity.EntityTypeTest;
import me.mioclient.mixin.ducks.DuckAbstractBlock;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.setting.EnumSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class SelfFill extends Module {
    public Setting<ScaffoldMode_2> mode = add(new EnumSetting<>("Mode", ScaffoldMode_2.WHITELIST));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", true));
    public Setting<Boolean> persistent = add(new BooleanSetting("Persistent", false).setVisible());
    public Setting<Boolean> jumpDisable = add(new BooleanSetting("JumpDisable", true).requiresSettings(this.persistent));
    public Setting<Set<Block>> whitelist = add(new EntityListObjectSetting<>("Whitelist", BuiltInRegistries.BLOCK));
    public Setting<Boolean> attack = add(new BooleanSetting("Attack", true));
    public Setting<Boolean> noLagBack = add(new BooleanSetting("NoLagBack", false));
    public Entity entity;
    public boolean flag;
    public double val;
    public final List<Double> list;

    public SelfFill() {
        super("SelfFill", "Places solid blocks inside you blocking your lower hitbox.", Category.COMBAT, "burrow");
        this.list = List.of(Double.valueOf(0.4d), Double.valueOf(0.75d), Double.valueOf(1.01d), Double.valueOf(1.15d));
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        if (isNotInGame()) {
            disable();
        } else {
            this.val = minecraftClient.player.getY();
            this.flag = false;
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.player.getY() > this.val && this.persistent.getValue().booleanValue() && this.jumpDisable.getValue().booleanValue()) {
            toggle();
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundPlayerPositionPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundPlayerPositionPacket ? (ClientboundPlayerPositionPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundPlayerPositionPacket) {
            ClientboundPlayerPositionPacket playerPositionLookS2CPacket = packet904;
            if (minecraftClient.player.position().distanceToSqr(playerPositionLookS2CPacket.change().position()) > 9.0d) {
                this.val = playerPositionLookS2CPacket.change().position().y;
            }
        }
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post || minecraftClient.player.isPassenger()) {
            return;
        }
        int i = 0;
        int i2 = -8;
        while (true) {
            if (i2 >= 8) {
                break;
            }
            if (i2 != 0 && i2 != 1) {
                BlockPos add = BlockPos.containing(minecraftClient.player.position()).offset(0, i2, 0);
                if (minecraftClient.level.getBlockState(add).getCollisionShape(minecraftClient.level, add).isEmpty()) {
                    i = i2;
                    break;
                }
            }
            i2++;
        }
        BlockPos blockPos1333 = PlayerUtils.getRoundedBlockPos();
        if (minecraftClient.level.getEntities(EntityTypeTest.forClass(Player.class), new AABB(blockPos1333), playerEntity -> {
            return playerEntity != minecraftClient.player;
        }).isEmpty()) {
            BlockState blockState = minecraftClient.level.getBlockState(blockPos1333);
            AABB expand = minecraftClient.player.getBoundingBox().inflate(-BoxMath.val);
            if (((BlockPos.betweenClosedStream(expand.setMaxY(expand.minY)).count() != 1) && MovementUtils.is2181()) || !blockState.canBeReplaced() || blockState.liquid() || !minecraftClient.player.onGround() || i == 0) {
                return;
            }
            if (this.attack.getValue().booleanValue()) {
                this.entity = BlockPlacer.findAndAttackCrystal(blockPos1333, 0);
            }
            if (this.entity != null) {
                return;
            }
            int i3 = InventoryUtils.findInHotbar(itemStack -> {
                BlockItem item = (itemStack.getItem()) instanceof BlockItem ? (BlockItem) (itemStack.getItem()) : null;
                if (item instanceof BlockItem) {
                    BlockItem blockItem = item;
                    if (this.mode.getValue().isAllowedBySetting(blockItem.getBlock(), this.whitelist)) {
                        return true;
                    }
                }
                return false;
            });
            int i4 = minecraftClient.player.getInventory().getSelectedSlot();
            if (i3 == -1) {
                ChatUtils.sendPrefixedMessage(Component.literal(getName()).append(" is out of blocks!"), ChatUtils.createSignature(-2), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode);
                disable();
                return;
            }
            Direction direction3028 = BlockPlacer.findSupportSide(blockPos1333);
            if (direction3028 == null) {
                return;
            }
            float[] fArr = {motionEvent.getYaw(), MathConstants.num2};
            if (this.entity != null && this.rotate.getValue().booleanValue()) {
                fArr = RotationManager.getRotationsTo(this.entity);
            }
            int i5 = i;
            LockedTaskRunner.executeLocked(() -> {
                placeSelfFillBlocks(blockPos1333, direction3028, i4, i3, i5);
            });
            if (this.rotate.getValue().booleanValue()) {
                ClientServices.RotationManager.requestRotations(fArr, 101);
            }
            if (!this.flag || this.persistent.getValue().booleanValue()) {
                return;
            }
            this.flag = false;
            disable();
        }
    }

    public void placeSelfFillBlocks(BlockPos blockPos, Direction direction, int i, int i2, int i3) {
        boolean z = true;
        int i4 = 1;
        while (true) {
            if (i4 >= 3) {
                break;
            }
            if (!minecraftClient.level.getBlockState(blockPos.above(i4)).canBeReplaced()) {
                z = false;
                break;
            }
            i4++;
        }
        if (z) {
            minecraftClient.player.setShiftKeyDown(false);
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), true, _cur.sprint()))); };
            InventoryUtils.selectSlot(i2);
            boolean isCollidable = ((me.mioclient.mixin.ducks.DuckAbstractBlock)(Object) ((BlockItem) minecraftClient.player.getMainHandItem().getItem()).getBlock()).isCollidable();
            if (isCollidable) {
                Iterator<Double> it = this.list.iterator();
                while (it.hasNext()) {
                    minecraftClient.player.connection.send(new ServerboundMovePlayerPacket.PosRot(minecraftClient.player.getX(), minecraftClient.player.getY() + it.next().doubleValue(), minecraftClient.player.getZ(), minecraftClient.player.getYRot(), MathConstants.num2, false, minecraftClient.player.horizontalCollision));
                }
            }
            AABB boundingBox = minecraftClient.player.getBoundingBox();
            minecraftClient.player.setBoundingBox(new AABB(0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d));
            if (this.noLagBack.getValue().booleanValue()) {
                BlockPlacer.placeBlock(blockPos, (Vec3) null, direction, false, InteractionHand.MAIN_HAND);
            } else {
                BlockPlacer.placeBlockPacket(blockPos, direction, false, InteractionHand.MAIN_HAND);
            }
            minecraftClient.player.setBoundingBox(boundingBox);
            if (isCollidable) {
                sendPositionAdjustment(i3);
            }
            InventoryUtils.selectSlot(i);
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), false, _cur.sprint()))); };
            this.flag = true;
        }
    }

    public void sendPositionAdjustment(double d) {
        if (!this.noLagBack.getValue().booleanValue()) {
            minecraftClient.player.connection.send(new ServerboundMovePlayerPacket.PosRot(minecraftClient.player.getX(), minecraftClient.player.getY() + d, minecraftClient.player.getZ(), minecraftClient.player.getYRot(), MathConstants.num2, false, minecraftClient.player.horizontalCollision));
        } else {
            minecraftClient.player.connection.send(new ServerboundMovePlayerPacket.PosRot(minecraftClient.player.getX(), minecraftClient.player.getY() + 1.0d, minecraftClient.player.getZ(), minecraftClient.player.getYRot(), MathConstants.num2, true, minecraftClient.player.horizontalCollision));
            ClientServices.tooltipsHelper.schedule(() -> {
                minecraftClient.player.setPos(minecraftClient.player.getX(), minecraftClient.player.getY() + 1.0d, minecraftClient.player.getZ());
            }, 1);
        }
    }
}