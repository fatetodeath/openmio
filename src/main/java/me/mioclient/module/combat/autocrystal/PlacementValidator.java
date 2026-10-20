package me.mioclient.module.combat.autocrystal;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.util.math.BoxMath;

import me.mioclient.ducks.DuckPlayerEntity;

import java.util.Iterator;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class PlacementValidator implements ClientAccessor {
    public boolean flag;
    public boolean flag3;
    public boolean flag5;
    public boolean flag6;
    public boolean flag7;
    public boolean flag8;
    public int num;
    public boolean flag2 = true;
    public boolean flag4 = true;
    public float val = 6.0f;

    public boolean isPositionValid(BlockPos blockPos) {
        if (!this.flag7 && !WorldUtils.isBedrockOrObsidian(blockPos)) {
            return false;
        }
        BlockPos add = blockPos.offset(0, 1, 0);
        Block block = minecraftClient.level.getBlockState(add).getBlock();
        boolean z = (block instanceof AirBlock) || add.equals(ClientServices.stashFinderHelper.getUnconfirmedBreakPos());
        if (this.flag4) {
            if (!z && block != Blocks.FIRE) {
                return false;
            }
        } else if (!z) {
            return false;
        }
        if (this.flag) {
            if (!(minecraftClient.level.getBlockState(blockPos.offset(0, 2, 0)).getBlock() instanceof AirBlock)) {
                return false;
            }
        }
        AABB withMaxY = new AABB(add).setMaxY(add.getY() + (this.flag5 ? 1 : 2));
        int i = 0;
        Iterator it = minecraftClient.level.entitiesForRendering().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            // the world was cast and any non-player (e.g. a zombie) threw ClassCastException,
            // killing the placement search thread. Keep the loop local as Entity and only
            // duck-cast inside the Player branch.
            Entity entity = (Entity) it.next();
            if (entity != null && !entity.isRemoved() && entity.isAlive()) {
                AABB boundingBox = entity.getBoundingBox();
                if (!(entity instanceof Player) || entity == minecraftClient.player) {
                    if ((entity instanceof EndCrystal) && !this.flag3) {
                        if (minecraftClient.player.getEyePosition().distanceToSqr(entity.position()) <= this.val) {
                            continue;
                        }
                    }
                    // uses `continue`, and an empty body makes dropped items fall through to the
                    // intersects() check below, where they count as obstructions and block placement.
                    if ((entity instanceof ItemEntity) && this.flag8) {
                        continue;
                    }
                } else {
                    Player playerEntity = (Player) entity;
                    DuckPlayerEntity duckPlayer = entity instanceof DuckPlayerEntity ? (DuckPlayerEntity) entity : null;
                    boundingBox = BoxMath.getServerBox((LivingEntity) entity);
                    if (this.flag6 && duckPlayer != null && !duckPlayer.mio$isNextToWall()) {
                        boundingBox = boundingBox.inflate(1.0E-4d, 0.0d, 1.0E-4d);
                    }
                    for (int i2 = 1; i2 <= this.num; i2++) {
                        if (ClientServices.mainhandHelper.getPredictedBox(playerEntity, i2).setMaxY(boundingBox.maxY).setMinY(boundingBox.minY).intersects(withMaxY)) {
                            i++;
                        }
                    }
                }
                if (boundingBox.intersects(withMaxY)) {
                    i++;
                    break;
                }
            }
        }
        if (this.flag2 && i != 0) {
            return false;
        }
        return true;
    }

    public PlacementValidator getAutoCrystalSearchHelper42071(boolean z) {
        this.flag = z;
        return this;
    }

    public PlacementValidator withEntityCheck(boolean z) {
        this.flag2 = z;
        return this;
    }

    public PlacementValidator withIgnoreExistingCrystals(boolean z) {
        this.flag3 = z;
        return this;
    }

    public PlacementValidator withAllowFire(boolean z) {
        this.flag4 = z;
        return this;
    }

    public PlacementValidator getAutoCrystalSearchHelper42075(boolean z) {
        this.flag5 = z;
        return this;
    }

    public PlacementValidator withHitboxFix(boolean z) {
        this.flag6 = z;
        return this;
    }

    public PlacementValidator withBasePlace(boolean z) {
        this.flag7 = z;
        return this;
    }

    public PlacementValidator getAutoCrystalSearchHelper42078(boolean z) {
        this.flag8 = z;
        return this;
    }

    public PlacementValidator getAutoCrystalSearchHelper42079(int i) {
        this.num = i;
        return this;
    }

    public PlacementValidator getAutoCrystalSearchHelper42080(float f) {
        this.val = f * f;
        return this;
    }
}