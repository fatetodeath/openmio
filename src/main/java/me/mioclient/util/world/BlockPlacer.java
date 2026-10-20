package me.mioclient.util.world;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.client.AntiCheatMode;
import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.mioclient.mixin.ducks.DuckAbstractBlock;
import me.mioclient.ducks.DuckLivingEntity;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.misc.KillEffects;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.Level;

public class BlockPlacer implements ClientAccessor {
    public static final Stopwatch stopwatch = new Stopwatch();
    public static boolean flag = false;
    public static final Direction[] directionArr = {Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
    public static AntiCheat antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
    public static KillEffects killEffects = (KillEffects) ClientServices.moduleInjector.getModule(KillEffects.class);

    public static Direction findSupportSide(BlockPos blockPos) {
        return findSupportSide(blockPos, false);
    }

    public static Direction findSupportSide(BlockPos blockPos, boolean z) {
        return findSupportSide(blockPos, z, false);
    }

    public static Direction findSupportSide(BlockPos blockPos, boolean z, boolean z2) {
        for (Direction direction : directionArr) {
            if (blockPos.offset(direction.getUnitVec3i()).getY() < (minecraftClient.level.dimensionType().minY() + minecraftClient.level.dimensionType().height())) {
                BlockPos offset = blockPos.relative(direction);
                if (z2) {
                    if (!WorldUtils.isAnyPositionVisible(AutoCraftMode.X8.getVisibleFaceCorners(offset, direction.getOpposite()))) {
                        continue;
                    }
                }
                if (ClientServices.stashFinderHelper.isBlockSolid(offset)) {
                    if (z) {
                        if (!getInteractableSides(offset).contains(direction.getOpposite())) {
                        }
                    }
                    return direction;
                }
                continue;
            }
        }
        return null;
    }

    public static List<Direction> getInteractableSides(BlockPos blockPos) {
        ArrayList arrayList = new ArrayList(List.of((Object[]) Direction.values()));
        for (Direction direction : Direction.values()) {
            BlockPos offset = blockPos.relative(direction);
            BlockState blockState = minecraftClient.level.getBlockState(offset);
            if (!blockState.canBeReplaced() && blockState.isCollisionShapeFullBlock(minecraftClient.level, offset)) {
                arrayList.remove(direction);
            }
        }
        if (blockPos.getY() + 1 >= (minecraftClient.level.dimensionType().minY() + minecraftClient.level.dimensionType().height())) {
            arrayList.remove(Direction.UP);
        }
        double d = minecraftClient.player.getEyePosition().x - Vec3.atCenterOf(blockPos).x;
        double d2 = minecraftClient.player.getEyePosition().z - Vec3.atCenterOf(blockPos).z;
        if (d > MathConstants.val2) {
            arrayList.remove(Direction.WEST);
        } else if (d < (-MathConstants.val2)) {
            arrayList.remove(Direction.EAST);
        } else {
            arrayList.remove(Direction.WEST);
            arrayList.remove(Direction.EAST);
        }
        if (d2 > MathConstants.val2) {
            arrayList.remove(Direction.NORTH);
        } else if (d2 < (-MathConstants.val2)) {
            arrayList.remove(Direction.SOUTH);
        } else {
            arrayList.remove(Direction.NORTH);
            arrayList.remove(Direction.SOUTH);
        }
        if (minecraftClient.player.getEyePosition().y < blockPos.getY() + (antiCheat.strictDirection.getValue() == AntiCheatMode.GRIM ? 1 : 0)) {
            arrayList.remove(Direction.UP);
        }
        if (minecraftClient.player.getEyePosition().y > blockPos.getY()) {
            arrayList.remove(Direction.DOWN);
        }
        return arrayList;
    }

    public static BlockPos findNearbyPlaceablePos(BlockPos blockPos, int i, boolean z, boolean z2) {
        return findNearbyPlaceablePos(blockPos, i, z, z2, blockPos2 -> {
            return true;
        });
    }

    public static BlockPos findNearbyPlaceablePos(BlockPos blockPos, int i, boolean z, boolean z2, java.util.function.Predicate<BlockPos> predicate) {
        for (int i2 = 1; i2 <= i; i2++) {
            for (Direction direction : directionArr) {
                BlockPos offset = blockPos.relative(direction, i2);
                if (predicate.test(offset) && findSupportSide(offset, z2) != null) {
                    if (minecraftClient.level.getEntitiesOfClass(Player.class, new AABB(offset), playerEntity -> {
                        return z;
                    }).isEmpty()) {
                        return offset;
                    }
                }
            }
        }
        return null;
    }

    public static boolean placeBlock(BlockPos blockPos, boolean z) {
        return placeBlock(blockPos, z, InteractionHand.MAIN_HAND);
    }

    public static boolean placeBlock(BlockPos blockPos, boolean z, InteractionHand hand) {
        return placeBlock(blockPos, (Vec3) null, findSupportSide(blockPos), z, hand);
    }

    public static boolean placeBlock(BlockPos blockPos, Vec3 vec3d, Direction direction, boolean z, InteractionHand hand) {
        if (minecraftClient.level == null || minecraftClient.player == null || minecraftClient.gameMode == null) {
            return false;
        }
        BlockItem item = (minecraftClient.player.getItemInHand(hand).getItem()) instanceof BlockItem ? (BlockItem) (minecraftClient.player.getItemInHand(hand).getItem()) : null;
        if (!canPlaceAt(blockPos, item instanceof BlockItem ? item.getBlock() : null, false)) {
            return false;
        }
        boolean z2 = z && direction == null;
        if (direction == null) {
            if (!z) {
                return false;
            }
            BlockHitResult blockHitResult = (minecraftClient.hitResult) instanceof BlockHitResult ? (BlockHitResult) (minecraftClient.hitResult) : null;
            direction = blockHitResult instanceof BlockHitResult ? blockHitResult.getDirection() : Direction.DOWN;
        }
        BlockPos offset = z2 ? blockPos : blockPos.relative(direction);
        boolean needsSneakPlace = needsSneakPlace(offset);
        if (vec3d == null) {
            vec3d = Vec3.atCenterOf(blockPos).add(new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()).scale(MathConstants.val2));
        }
        if (needsSneakPlace) {
            // 26.2 port: ServerboundPlayerCommandPacket PRESS/RELEASE_SHIFT_KEY removed;
            // use ServerboundPlayerInputPacket with shift=true (preserve current inputs).
            net.minecraft.world.entity.player.Input presses = minecraftClient.player.input.keyPresses;
            minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(presses.forward(), presses.backward(), presses.left(), presses.right(), presses.jump(), true, presses.sprint())));
        }
        flag = true;
        InteractionResult interactBlock = minecraftClient.gameMode.useItemOn(minecraftClient.player, hand, new BlockHitResult(vec3d, z2 ? direction : direction.getOpposite(), offset, false));
        flag = false;
        if (interactBlock.consumesAction()) {
            minecraftClient.player.swing(hand);
        }
        if (!needsSneakPlace) {
            return true;
        }
        net.minecraft.world.entity.player.Input presses2 = minecraftClient.player.input.keyPresses;
        minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(presses2.forward(), presses2.backward(), presses2.left(), presses2.right(), presses2.jump(), false, presses2.sprint())));
        return true;
    }

    public static boolean placeBlockPacket(BlockPos blockPos, Direction direction, boolean z, InteractionHand hand) {
        return placeBlockPacket(blockPos, (Vec3) null, direction, z, hand);
    }

    public static boolean placeBlockPacket(BlockPos blockPos, Vec3 vec3d, Direction direction, boolean z, InteractionHand hand) {
        if (minecraftClient.level == null || minecraftClient.player == null || minecraftClient.gameMode == null) {
            return false;
        }
        BlockItem item = (minecraftClient.player.getItemInHand(hand).getItem()) instanceof BlockItem ? (BlockItem) (minecraftClient.player.getItemInHand(hand).getItem()) : null;
        if (!canPlaceAt(blockPos, item instanceof BlockItem ? item.getBlock() : null, false)) {
            return false;
        }
        boolean z2 = z && direction == null;
        if (direction == null) {
            List<Direction> list3031 = getInteractableSides(blockPos);
            if (!z) {
                return false;
            }
            direction = list3031.isEmpty() ? Direction.DOWN : (Direction) list3031.getFirst();
        }
        BlockPos offset = z2 ? blockPos : blockPos.relative(direction);
        boolean needsSneakPlace = needsSneakPlace(offset);
        if (needsSneakPlace) {
            net.minecraft.world.entity.player.Input pressesP = minecraftClient.player.input.keyPresses;
            minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(pressesP.forward(), pressesP.backward(), pressesP.left(), pressesP.right(), pressesP.jump(), true, pressesP.sprint())));
        }
        if (vec3d == null) {
            vec3d = Vec3.atCenterOf(blockPos).add(new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ()).scale(MathConstants.val2));
        }
        PacketSender.sendInteractBlock(hand, new BlockHitResult(vec3d, z2 ? direction : direction.getOpposite(), offset, false));
        if (!antiCheat.isF2b2t() || !antiCheat.movementSync.getValue().booleanValue()) {
            minecraftClient.player.swing(hand);
        } else if (!minecraftClient.player.swinging || minecraftClient.player.swingTime >= 3 || minecraftClient.player.swingTime < 0) {
            minecraftClient.player.swingTime = -1;
            minecraftClient.player.swinging = true;
            minecraftClient.player.swingingArm = hand;
        }
        if (!needsSneakPlace) {
            return true;
        }
        net.minecraft.world.entity.player.Input pressesR = minecraftClient.player.input.keyPresses;
            minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(pressesR.forward(), pressesR.backward(), pressesR.left(), pressesR.right(), pressesR.jump(), false, pressesR.sprint())));
        return true;
    }

    public static boolean canPlaceAt(BlockPos blockPos, Block block, boolean z) {
        return canPlaceAt(blockPos, block, z, false);
    }

    public static boolean canPlaceAt(BlockPos blockPos, Block block, boolean z, boolean z2) {
        if (!minecraftClient.level.getBlockState(blockPos).canBeReplaced() || !minecraftClient.level.getWorldBorder().isWithinBounds(blockPos) || !minecraftClient.level.isInWorldBounds(blockPos)) {
            return false;
        }
        VoxelShape empty = block == null ? Shapes.empty() : block.defaultBlockState().getCollisionShape(minecraftClient.level, BlockPos.ZERO);
        AABB box = empty.isEmpty() ? new AABB(blockPos) : BoxMath.getBoxAroundBlock(empty.bounds(), blockPos);
        if (block != null && !((DuckAbstractBlock)(Object) block).isCollidable()) {
            return true;
        }
        for (Entity clientPlayerEntity : minecraftClient.level.getEntitiesOfClass(Entity.class, box, entity -> {
            return ((entity instanceof ThrownExperienceBottle) || (entity instanceof ItemEntity) || (entity instanceof ItemFrame) || (entity instanceof ExperienceOrb)) ? false : true;
        })) {
            if (BoxMath.hasSignificantIntersection(box, ((Entity) clientPlayerEntity).getBoundingBox()) || clientPlayerEntity == minecraftClient.player) {
                if (clientPlayerEntity instanceof Arrow) {
                    continue;
                } else if (clientPlayerEntity instanceof Player) {
                    if (((Entity) clientPlayerEntity).getBoundingBox().intersects(box)) {
                        return false;
                    }
                } else if (!(clientPlayerEntity instanceof EndCrystal) || !z2) {
                    return !z;
                }
            }
        }
        return true;
    }

    public static boolean isSpaceFree(BlockPos blockPos, boolean z) {
        return isSpaceFree(blockPos, z, false);
    }

    public static boolean isSpaceFree(BlockPos blockPos, boolean z, boolean z2) {
        if (!minecraftClient.level.getBlockState(blockPos).canBeReplaced() || !minecraftClient.level.getWorldBorder().isWithinBounds(blockPos)) {
            return false;
        }
        for (Entity clientPlayerEntity : minecraftClient.level.getEntitiesOfClass(Entity.class, new AABB(blockPos), entity -> {
            return ((entity instanceof ThrownExperienceBottle) || (entity instanceof ItemEntity) || (entity instanceof ExperienceOrb)) ? false : true;
        })) {
            if (BoxMath.hasSignificantIntersection(new AABB(blockPos), ((Entity) clientPlayerEntity).getBoundingBox()) || clientPlayerEntity == minecraftClient.player) {
                if (!(clientPlayerEntity instanceof EndCrystal) || !z2) {
                    if (!(clientPlayerEntity instanceof Arrow) && ((clientPlayerEntity instanceof Player) || z)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean isCrystalPlaceable(BlockPos blockPos, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6) {
        if (!WorldUtils.isBedrockOrObsidian(blockPos)) {
            return false;
        }
        BlockPos blockPos2 = blockPos.offset(0, 1, 0);
        Block block = minecraftClient.level.getBlockState(blockPos2).getBlock();
        if (z4) {
            if (block != Blocks.AIR && block != Blocks.FIRE) {
                return false;
            }
        } else if (block != Blocks.AIR) {
            return false;
        }
        if (z) {
            if (minecraftClient.level.getBlockState(blockPos.offset(0, 2, 0)).getBlock() != Blocks.AIR) {
                return false;
            }
        }
        AABB box = new AABB(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ(), blockPos2.getX() + 1, blockPos2.getY() + (z5 ? 0.9d : 2.0d), blockPos2.getZ() + 1);
        int i = 0;
        Iterator it = minecraftClient.level.getEntitiesOfClass(Entity.class, box, e -> true).iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Entity clientPlayerEntity = (Entity) it.next();
            if (!((Entity) clientPlayerEntity).isRemoved() && ((Entity) clientPlayerEntity).isAlive()) {
                AABB boundingBox = ((Entity) clientPlayerEntity).getBoundingBox();
                if ((clientPlayerEntity instanceof Player) && clientPlayerEntity != minecraftClient.player && z6) {
                    boundingBox = boundingBox.inflate(0.01d);
                } else if ((clientPlayerEntity instanceof EndCrystal) && !z3) {
                }
                if (boundingBox.intersects(box)) {
                    i = 0 + 1;
                    break;
                }
            }
        }
        return !z2 || i == 0;
    }

    public static Block getBlockAt(BlockPos blockPos) {
        if (blockPos == null) {
            return null;
        }
        return minecraftClient.level.getBlockState(blockPos).getBlock();
    }

    public static boolean isBlockCollidable(BlockPos blockPos) {
        return ((DuckAbstractBlock)(Object) getBlockAt(blockPos)).isCollidable();
    }

    public static Entity findAndAttackCrystal(BlockPos blockPos, int i) {
        for (Entity entity : minecraftClient.level.getEntitiesOfClass(Entity.class, new AABB(blockPos).inflate(8.0), e -> true)) {
            if (entity instanceof EndCrystal) {
                if (entity.getBoundingBox().intersects(new AABB(blockPos)) && entity.tickCount >= i) {
                    attackEntity(entity);
                    return entity;
                }
            }
        }
        return null;
    }

    public static void attackEntity(Entity entity) {
        attackEntity(entity, false);
    }

    public static void attackEntity(Entity entity, boolean z) {
        if (ClientServices.scaffoldHelper.tryAttackCrystal(entity.getId())) {
            if (killEffects.isToggled() && (entity instanceof Player)) {
                killEffects.registerAttackTarget((Player) entity);
            }
            minecraftClient.player.resetAttackStrengthTicker();
        }
    }

    public static void interactWithEntity(Entity entity, InteractionHand hand) {
        // 26.2 port: interactEntityAtLocation/interactEntity -> interact(player, entity, hitResult, hand).
        minecraftClient.gameMode.interact(minecraftClient.player, entity, new EntityHitResult(entity), hand);
        minecraftClient.player.swing(hand);
    }

    public static boolean isBlockInteractive(Block block) {
        return (block instanceof CraftingTableBlock) || (block instanceof AnvilBlock) || (block instanceof ButtonBlock) || (block instanceof BasePressurePlateBlock) || (block instanceof BaseEntityBlock) || (block instanceof BedBlock) || (block instanceof FenceGateBlock) || (block instanceof DoorBlock) || (block instanceof NoteBlock) || (block instanceof TrapDoorBlock);
    }

    public static boolean needsSneakPlace(BlockPos blockPos) {
        BlockState blockState = minecraftClient.level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        return block == Blocks.RESPAWN_ANCHOR || blockState.hasBlockEntity() || (block instanceof BedBlock) || (block instanceof AbstractChestBlock) || (block instanceof DoorBlock) || (block instanceof TrapDoorBlock) || (block instanceof ButtonBlock) || (block instanceof ComposterBlock) || (block instanceof NoteBlock) || (block instanceof AnvilBlock) || (block instanceof AbstractFurnaceBlock) || (block instanceof CraftingTableBlock) || (block instanceof EnchantingTableBlock) || (block instanceof CakeBlock);
    }

    public Direction getRotatedDirection(Direction direction, int i) {
        return Direction.values()[2 + ((direction.get3DDataValue() + i) % Direction.values().length)];
    }

    public static boolean isThrowableItem(Item item) {
        return (item instanceof EggItem) || (item instanceof SnowballItem) || (item instanceof EnderpearlItem) || (item instanceof ExperienceBottleItem) || (item instanceof ThrowablePotionItem) || (item instanceof TridentItem);
    }

    public static void resetPlaceCooldown() {
        stopwatch.reset();
    }

    public static boolean isPlaceOnCooldown() {
        return !stopwatch.hasElapsedMs(50L);
    }
}
