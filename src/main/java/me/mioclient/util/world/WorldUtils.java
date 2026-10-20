package me.mioclient.util.world;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.mode.world.StashFinderMode;
import me.mioclient.util.inventory.EnchantmentHelper;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;
import me.mioclient.util.world.BlockRaycastFilter;
import me.mioclient.util.world.CustomRaycastContext;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.api.ClientAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import me.mioclient.module.combat.AutoCrystal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

public class WorldUtils implements ClientAccessor {
    public static final AutoCrystal autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
    public static final List<Block> list = List.of(Blocks.BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.GILDED_BLACKSTONE);
    public static final List<Block> list2 = List.of(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, Blocks.DEEPSLATE, Blocks.CHISELED_DEEPSLATE, Blocks.SCULK);
    public static final List<Block> list3 = List.of(Blocks.EMERALD_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.GOLD_BLOCK, Blocks.RAW_COPPER_BLOCK);

    public static List<LevelChunk> getLoadedChunks() {
        ArrayList<LevelChunk> arrayList = new ArrayList<>();
        int intValue = minecraftClient.options.renderDistance().get();
        int playerChunkX = (int) Math.floor(minecraftClient.player.getX()) >> 4;
        int playerChunkZ = (int) Math.floor(minecraftClient.player.getZ()) >> 4;
        for (int i = -intValue; i <= intValue; i++) {
            for (int i2 = -intValue; i2 <= intValue; i2++) {
                try {
                    LevelChunk chunk = minecraftClient.level.getChunk(playerChunkX + i, playerChunkZ + i2);
                    if (chunk != null) {
                        arrayList.add(chunk);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return arrayList;
    }

    public static List<BlockEntity> getLoadedBlockEntities() {
        ArrayList<BlockEntity> arrayList = new ArrayList<>();
        for (LevelChunk worldChunk : getLoadedChunks()) {
            if (!worldChunk.getBlockEntities().isEmpty()) {
                try {
                    arrayList.addAll(new ArrayList<>(worldChunk.getBlockEntities().values()));
                } catch (Throwable th) {
                    autoCrystal.cancel();
                }
            }
        }
        return arrayList;
    }

    

    public static boolean isStashChest(ChunkAccess chunk, BlockPos blockPos, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
        BlockState blockState = chunk.getBlockState(blockPos);
        if (!blockState.is(Blocks.CHEST)) {
            return false;
        }
        BlockState blockState2 = chunk.getBlockState(blockPos.below());
        boolean z6 = blockState.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE;
        if (getDimension() == StashFinderMode.THE_NETHER) {
            if (z && z6 && blockState2.is(Blocks.NETHER_BRICKS)) {
                return true;
            }
            if (z2) {
                if (list.contains(blockState2.getBlock())) {
                    return true;
                }
            }
            return false;
        }
        if (getDimension() != StashFinderMode.OVERWORLD || !z6) {
            return false;
        }
        if (z5 && blockPos.getY() <= 34) {
            boolean isOf = blockState2.is(Blocks.CHISELED_TUFF_BRICKS);
            if (list3.contains(blockState2.getBlock()) || isOf) {
                boolean z7 = false;
                Direction[] values = Direction.values();
                int length = values.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Direction direction = values[i];
                    if (direction.getAxis().isHorizontal()) {
                        BlockState blockState3 = chunk.getBlockState(blockPos.relative(direction));
                        if (!isOf) {
                            if (list3.contains(blockState3.getBlock())) {
                                return true;
                            }
                        } else if (!blockState3.is(Blocks.AIR)) {
                            z7 = true;
                            break;
                        }
                    }
                    i++;
                }
            }
        }
        if (z3 && blockPos.getY() <= -32 && blockPos.getY() >= -51) {
            if (list2.contains(blockState2.getBlock())) {
                return true;
            }
        }
        return z4 && (blockState2.is(Blocks.MOSSY_COBBLESTONE) || blockState2.is(Blocks.COBBLESTONE));
    }

    public static List<BlockPos> getBlocksInRadius(Vec3 vec3d, float f, boolean z) {
        ArrayList<BlockPos> arrayList = new ArrayList<>();
        BlockPos ofFloored = BlockPos.containing(vec3d.x, vec3d.y, vec3d.z);
        float f2 = -f;
        while (true) {
            float f3 = f2;
            if (f3 >= f) {
                return arrayList;
            }
            float f4 = -f;
            while (true) {
                float f5 = f4;
                if (f5 < f) {
                    float f6 = -f;
                    while (true) {
                        float f7 = f6;
                        if (f7 < f) {
                            BlockPos add = ofFloored.offset((int) f3, (int) f5, (int) f7);
                            if (ofFloored.closerThan(add, f) && (!z || !minecraftClient.level.getBlockState(add).isAir())) {
                                arrayList.add(add);
                            }
                            f6 = f7 + 1.0f;
                        } else {
                            break;
                        }
                    }
                    f4 = f5 + 1.0f;
                } else {
                    break;
                }
            }
            f2 = f3 + 1.0f;
        }
    }

    public static Vec3 findVisiblePoint(BlockPos blockPos, AutoCraftMode autoCraftMode) {
        for (Vec3 vec3d : autoCraftMode.getWorldCorners(blockPos)) {
            if (isPositionVisible(vec3d)) {
                return vec3d;
            }
        }
        return null;
    }

    public static boolean isPositionVisible(Vec3i vec3i) {
        return isPositionVisible(Vec3.atCenterOf(vec3i));
    }

    public static boolean isAnyPositionVisible(Collection<Vec3> collection) {
        Iterator<Vec3> it = collection.iterator();
        while (it.hasNext()) {
            if (isPositionVisible(it.next())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPositionVisible(Vec3 vec3d) {
        Vec3 eyePos = minecraftClient.player.getEyePosition();
        BlockPos ofFloored = BlockPos.containing(vec3d.x, vec3d.y, vec3d.z);
        if (vec3d.distanceTo(eyePos) > 64.0) {
            return false;
        }
        BlockHitResult blockHitResult2784 = RaycastUtils.performCustomBlockRaycast(new CustomRaycastContext.Inner(minecraftClient.player.getEyePosition(), vec3d).setFilter(BlockRaycastFilter.getHoleSnapHelper1674(ofFloored)).build());
        return ((HitResult) blockHitResult2784).getType() == HitResult.Type.MISS || ((HitResult) blockHitResult2784).getLocation().equals(vec3d) || ((blockHitResult2784 instanceof BlockHitResult) && blockHitResult2784.getBlockPos().equals(ofFloored));
    }

    public static boolean isPositionVisibleCollider(Vec3 vec3d, Vec3 vec3d2) {
        Vec3 eyePos = minecraftClient.player.getEyePosition();
        if (vec3d.distanceTo(eyePos) > 64.0) {
            return false;
        }
        if (minecraftClient.level.clip(new ClipContext(eyePos, vec3d, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS) {
            return true;
        }
        if (minecraftClient.player.getY() >= vec3d.y) {
            return minecraftClient.level.clip(new ClipContext(eyePos, vec3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS;
        }
        return false;
    }

    public static boolean isBreakable(BlockPos blockPos) {
        if (blockPos == null || minecraftClient.level == null || minecraftClient.player == null) {
            return false;
        }
        BlockState blockState = minecraftClient.level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return false;
        }
        float destroySpeed = blockState.getDestroySpeed(minecraftClient.level, blockPos);
        if (destroySpeed < 0.0f) {
            return false;
        }
        return true;
    }

    public static double getBlockBreakingDelta(ItemStack itemStack, BlockState blockState, boolean z) {
        float destroySpeed = blockState.getDestroySpeed(minecraftClient.level, BlockPos.ZERO);
        if (destroySpeed == -1.0f) {
            return 0.0d;
        }
        return (getMiningSpeed(itemStack, blockState, z) / destroySpeed) / ((!blockState.requiresCorrectToolForDrops() || itemStack.isCorrectToolForDrops(blockState)) ? 30 : 100);
    }

    public static double getMiningSpeed(ItemStack itemStack, BlockState blockState, boolean z) {
        float intBitsToFloat;
        int i;
        double miningSpeedMultiplier = itemStack.getDestroySpeed(blockState);
        if (miningSpeedMultiplier > 1.0d && (i = EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, itemStack)) > 0 && !itemStack.isEmpty()) {
            miningSpeedMultiplier += (i * i) + 1;
        }
        if (MobEffectUtil.hasDigSpeed(minecraftClient.player)) {
            miningSpeedMultiplier *= 1.0f + ((MobEffectUtil.getDigSpeedAmplification(minecraftClient.player) + 1) * 0.2f);
        }
        if (minecraftClient.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            switch (minecraftClient.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    intBitsToFloat = 0.3f;
                    break;
                case 1:
                    intBitsToFloat = 0.09f;
                    break;
                case 2:
                    intBitsToFloat = 0.0027f;
                    break;
                default:
                    intBitsToFloat = 8.1e-4f;
                    break;
            }
            miningSpeedMultiplier *= intBitsToFloat;
        }
        if (minecraftClient.player.isInWater()) {
            if (!EnchantmentHelper.hasPlayerEnchantment(Enchantments.AQUA_AFFINITY, EquipmentSlot.HEAD)) {
                miningSpeedMultiplier /= 5.0d;
            }
        }
        if (!z) {
            miningSpeedMultiplier /= 5.0d;
        }
        return miningSpeedMultiplier;
    }

    public static StashFinderMode getDimension() {
        if (minecraftClient.level == null) {
            return StashFinderMode.OVERWORLD;
        }
        String path = minecraftClient.level.dimension().identifier().getPath();
        int z = -1;
        switch (path.hashCode()) {
            case -1350117363:
                if (path.equals("the_end")) {
                    z = 1;
                    break;
                }
                break;
            case 1272296422:
                if (path.equals("the_nether")) {
                    z = 0;
                    break;
                }
                break;
        }
        switch (z) {
            case 0:
                return StashFinderMode.THE_NETHER;
            case 1:
                return StashFinderMode.THE_END;
            default:
                return StashFinderMode.OVERWORLD;
        }
    }

    public static Entity getNearestEntity(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, float f, float f2) {
        return getNearestEntity(z, z2, z3, z4, z5, z6, f, f2, false, false);
    }

    public static Entity getNearestEntity(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, float f, float f2, boolean z7, boolean z8) {
        LocalPlayer clientPlayerEntity = minecraftClient.player;
        Objects.requireNonNull(clientPlayerEntity);
        return getNearestEntity(z, z2, z3, z4, z5, z6, f, f2, false, false, Comparator.comparingDouble(clientPlayerEntity::distanceToSqr), (java.util.function.Predicate<Entity>) null);
    }

    public static Entity getNearestEntity(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, float f, float f2, boolean z7, boolean z8, Comparator<Entity> comparator, java.util.function.Predicate<Entity> predicate) {
        Entity entity = StreamSupport.stream(minecraftClient.level.entitiesForRendering().spliterator(), false).filter(entity2 -> {
            return !(entity2 instanceof ThrownExperienceBottle) && isValidTarget(entity2) && (((entity2 instanceof Player) && z && (!z8 || PlayerUtils.hasArmor((Player) entity2))) || ((EntityUtil.isPassive(entity2) && z3) || ((EntityUtil.isHostile(entity2) && z4) || (((entity2 instanceof Piglin) && z2) || (((entity2 instanceof ZombifiedPiglin) && z2) || (((entity2 instanceof EndCrystal) && z5) || ((entity2 instanceof Projectile) && z6 && entity2.isAttackable())))))));
        }).filter(entity3 -> {
            return getEntityDistance(entity3) <= ((double) (RaycastUtils.hasLineOfSight(entity3) ? f2 : f));
        }).filter(entity4 -> {
            return predicate == null || predicate.test(entity4);
        }).min(comparator).orElse(null);
        if (entity != null && !(entity instanceof Player) && entity.hasCustomName() && z7) {
            entity = null;
        }
        return entity;
    }

    public static boolean isValidTarget(Entity entity) {
        if (entity == minecraftClient.player) {
            return false;
        }
        if ((entity instanceof Player) && ClientServices.socialManager.isFriendEntity((Player) entity)) {
            return false;
        }
        return entity.isAlive();
    }

    public static double getEntityDistance(Entity entity) {
        AABB boundingBox = entity.getBoundingBox();
        Vec3 add = BoxMath.getServerBox(minecraftClient.player).getBottomCenter().add(0.0d, minecraftClient.player.getEyeHeight(), 0.0d);
        if (!(entity instanceof EnderDragon)) {
            return getDistanceToBox(add, boundingBox);
        }
        double best = 1.0E9d;
        for (EnderDragonPart enderDragonPart : ((EnderDragon) entity).getSubEntities()) {
            best = Math.min(best, getDistanceToBox(add, enderDragonPart.getBoundingBox()));
        }
        return best;
    }

    public static double getDistanceToBox(Vec3 vec3d, AABB box) {
        return vec3d.distanceTo(BoxMath.getClosestPointInBox(vec3d, box));
    }

    public static boolean isBedrockOrObsidian(BlockPos blockPos) {
        return minecraftClient.level.getBlockState(blockPos).getBlock() == Blocks.BEDROCK || minecraftClient.level.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN;
    }

    public static boolean isAir(BlockPos blockPos) {
        return minecraftClient.level.getBlockState(blockPos).isAir();
    }

    public static boolean hasFluid(BlockPos blockPos) {
        return !minecraftClient.level.getFluidState(blockPos).isEmpty();
    }

    public static boolean isStashBlock(BlockPos blockPos) {
        return ClientServices.stashFinderHelper.isBlockSolid(blockPos);
    }

    public static Block getBlock(BlockPos blockPos) {
        return minecraftClient.level.getBlockState(blockPos).getBlock();
    }

    public static boolean isInteractableHit(HitResult hitResult) {
        if (hitResult == null) {
            return false;
        }
        if (hitResult instanceof BlockHitResult) {
            BlockState blockState = minecraftClient.level.getBlockState(((BlockHitResult) hitResult).getBlockPos());
            if (blockState.hasBlockEntity() || BlockPlacer.isBlockInteractive(blockState.getBlock())) {
                return true;
            }
        }
        if (!(hitResult instanceof EntityHitResult)) {
            return false;
        }
        Entity entity = ((EntityHitResult) hitResult).getEntity();
        return (entity instanceof VehicleEntity) || (entity instanceof Villager);
    }
}
