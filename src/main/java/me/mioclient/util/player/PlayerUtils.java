package me.mioclient.util.player;

import me.mioclient.mode.combat.BlockerOffsetMode;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.api.ClientAccessor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import me.mioclient.module.movement.ElytraFly;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

public class PlayerUtils implements ClientAccessor {
    public static boolean isInCobweb(Player playerEntity) {
        return minecraftClient.level.getBlockStates(playerEntity.getBoundingBox()).anyMatch(blockState -> {
            return blockState.getBlock() == Blocks.COBWEB;
        });
    }

    public static boolean isInWater(Player playerEntity) {
        return BlockPlacer.getBlockAt(BlockPos.containing(playerEntity.getX(), playerEntity.getBoundingBox().minY, playerEntity.getZ())) == Blocks.WATER || playerEntity.isInWater();
    }

    public static boolean isInLiquid(Player playerEntity) {
        return isInWater(playerEntity) || playerEntity.isInLava();
    }

    public static BlockPos getRoundedBlockPos() {
        return getRoundedBlockPos(minecraftClient.player);
    }

    public static BlockPos getRoundedBlockPos(LivingEntity livingEntity) {
        return BlockPos.containing(livingEntity.getX(), Math.round(livingEntity.getY()), livingEntity.getZ());
    }

    public static Direction getMovementDirection() {
        double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, 1.0d);
        return Direction.fromYRot(Math.toDegrees(Math.atan2(doubleArray2508[1], doubleArray2508[0])) - MathConstants.num2);
    }

    public static List<BlockPos> getSurroundPositions(LivingEntity livingEntity) {
        ArrayList arrayList = new ArrayList();
        Set<BlockPos> set2011 = getFeetPositions(livingEntity);
        for (BlockPos blockPos : set2011) {
            for (Vec3i vec3i : BlockerOffsetMode.SURROUND.getOffsets()) {
                BlockPos add = blockPos.offset(vec3i);
                if (!set2011.contains(add)) {
                    arrayList.add(add);
                }
            }
        }
        return arrayList;
    }

    public static Set<BlockPos> getFeetPositions(LivingEntity livingEntity) {
        BlockPos blockPos2008 = getRoundedBlockPos(livingEntity);
        HashSet hashSet = new HashSet();
        AABB expand = livingEntity.getBoundingBox().inflate(-BoxMath.val);
        hashSet.add(BlockPos.containing(expand.minX, blockPos2008.getY(), expand.minZ));
        hashSet.add(BlockPos.containing(expand.minX, blockPos2008.getY(), expand.maxZ));
        hashSet.add(BlockPos.containing(expand.maxX, blockPos2008.getY(), expand.minZ));
        hashSet.add(BlockPos.containing(expand.maxX, blockPos2008.getY(), expand.maxZ));
        return hashSet;
    }

    public static boolean isHungry() {
        return minecraftClient.player.getFoodData().getFoodLevel() < 6;
    }

    public static boolean hasArmor(LivingEntity livingEntity) {
        // 26.2 port: LivingEntity.getArmorItems removed; iterate armor EquipmentSlots.
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            if (!livingEntity.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasElytraEquipped(Player playerEntity) {
        return playerEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    public static boolean isReallyFallFlying() {
        if (ElytraFly.flag) {
            return minecraftClient.player.isFallFlying();
        }
        ElytraFly.flag = true;
        boolean isFallFlying = minecraftClient.player.isFallFlying();
        ElytraFly.flag = false;
        return isFallFlying;
    }

    public static InteractionHand getOppositeHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
