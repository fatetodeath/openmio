package me.mioclient.module.combat.pusher;

import me.mioclient.api.ClientServices;

import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.api.ClientAccessor;

import java.util.Comparator;
import java.util.List;
import me.mioclient.module.combat.Pusher;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import me.mioclient.manager.RotationManager;

public final class PusherTargetSelector implements ClientAccessor {
    public static final List<Block> list = List.of(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.REINFORCED_DEEPSLATE, Blocks.RESPAWN_ANCHOR);
    public final Pusher pusher;

    public PusherTargetSelector(Pusher pusher) {
        this.pusher = pusher;
    }

    public Player getTargetPlayer() {
        return (Player) minecraftClient.level.players().stream().filter(abstractClientPlayerEntity -> {
            return abstractClientPlayerEntity.isAlive() && abstractClientPlayerEntity != minecraftClient.player && EntityUtil.isOnGround((LivingEntity) abstractClientPlayerEntity) && minecraftClient.player.distanceTo((Entity) abstractClientPlayerEntity) <= this.pusher.getRange() && !ClientServices.socialManager.isFriendEntity((Player) abstractClientPlayerEntity);
        }).filter(abstractClientPlayerEntity2 -> {
            return !this.pusher.ignoreNaked.getValue().booleanValue() || PlayerUtils.hasArmor((LivingEntity) abstractClientPlayerEntity2);
        }).filter((v1) -> {
            return isValidPushTarget(v1);
        }).min(Comparator.comparing(abstractClientPlayerEntity3 -> {
            return Float.valueOf(Mth.degreesDifferenceAbs(minecraftClient.player.getYRot(), RotationManager.getRotationsTo((Entity) abstractClientPlayerEntity3)[0]));
        })).orElse(null);
    }

    public boolean isValidPushTarget(Player playerEntity) {
        BlockPos blockPos = playerEntity.blockPosition();
        BlockState blockState = minecraftClient.level.getBlockState(blockPos.above());
        if (list.contains(blockState.getBlock())) {
            return false;
        }
        if (blockState.getDestroySpeed(minecraftClient.level, blockPos.above()) == (-1.0f)) {
            return false;
        }
        if (ClientServices.stashFinderHelper.isBlockSolid(blockPos.above(2))) {
            return false;
        }
        if (this.pusher.onlyHole.getValue().booleanValue() && !ClientServices.holeSnapOverlayHelper.isHoleAt(blockPos)) {
            return false;
        }
        if (this.pusher.onlySafe.getValue().booleanValue() && !ClientServices.holeSnapOverlayHelper.isHoleAt(blockPos)) {
            if (minecraftClient.level.getBlockState(playerEntity.blockPosition()).getBlock().getExplosionResistance() < 600.0f) {
                return false;
            }
        }
        return PlayerUtils.getSurroundPositions((LivingEntity) playerEntity).size() <= 6;
    }
}