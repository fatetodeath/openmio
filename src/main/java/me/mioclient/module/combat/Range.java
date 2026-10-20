package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.command.FakePlayerCommand;
import me.mioclient.setting.NumberSetting;
import me.mioclient.manager.RotationManager;
import me.mioclient.ducks.DuckEndCrystalEntity;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.ChunkDeltaUpdateEvent;
import me.mioclient.event.Listen;
import me.mioclient.ducks.DuckLivingEntity;
import me.mioclient.module.Delay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import me.mioclient.module.Module;

public class Range extends Delay {
    public Setting<Float> setting;
    public Setting<Boolean> setting2;
    public Setting<Boolean> setting3;
    public Setting<Boolean> setting4;
    public Setting<Boolean> setting5;
    public Setting<Boolean> setting6;
    public Setting<Boolean> setting7;
    public Setting<Boolean> setting8;
    public Setting<Boolean> setting9;
    public Player playerEntity;

    
    private static final class Record {
        public final BlockPos blockPos;
        public final BlockState blockState;

        public Record(BlockPos blockPos, BlockState blockState) {
            this.blockPos = blockPos;
            this.blockState = blockState;
        }

        public BlockPos getBlockPos() {
            return this.blockPos;
        }

        public BlockState getBlockState() {
            return this.blockState;
        }
    }

    public Range(String str, String str2, Category category) {
        super(str, str2, category, new String[0]);
        this.setting = add(new NumberSetting("Range", Float.valueOf(5.0f), Float.valueOf(1.0f), Float.valueOf(8.0f)).setSuffix("m"));
        this.setting2 = add(new BooleanSetting("Sequential", false));
        this.setting3 = add(new BooleanSetting("Targets", true).setDependent());
        this.setting4 = add(new BooleanSetting("Head", true).requiresSettings(this.setting3));
        this.setting5 = add(new BooleanSetting("Face", true).requiresSettings(this.setting3));
        this.setting6 = add(new BooleanSetting("Legs", false).requiresSettings(this.setting3));
        this.setting7 = add(new BooleanSetting("AntiStep", false));
        this.setting8 = add(new BooleanSetting("IgnoreNakeds", false));
        this.setting9 = add(new BooleanSetting("OnlyObby", false), this.setting5);
    }

    public Range() {
        super("AutoTrap", "Traps your enemies with obby.", Category.COMBAT, new String[0]);
        this.setting = add(new NumberSetting("Range", Float.valueOf(5.0f), Float.valueOf(1.0f), Float.valueOf(8.0f)).setSuffix("m"));
        this.setting2 = add(new BooleanSetting("Sequential", false));
        this.setting3 = add(new BooleanSetting("Targets", true).setDependent());
        this.setting4 = add(new BooleanSetting("Head", true).requiresSettings(this.setting3));
        this.setting5 = add(new BooleanSetting("Face", true).requiresSettings(this.setting3));
        this.setting6 = add(new BooleanSetting("Legs", false).requiresSettings(this.setting3));
        this.setting7 = add(new BooleanSetting("AntiStep", false));
        this.setting8 = add(new BooleanSetting("IgnoreNakeds", false));
        this.setting9 = add(new BooleanSetting("OnlyObby", false), this.setting5);
        setPlacementEnabled(true);
        do1335();
        this.setting9.setDefaultValue(false);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        if (this.playerEntity != null) {
            return this.playerEntity.getGameProfile().name();
        }
        return null;
    }

    @Override // me.mioclient.module.Delay
    public List<BlockPos> getPlacePositions() {
        this.playerEntity = getTargetPlayer();
        if (this.playerEntity == null || Math.hypot(this.playerEntity.getX() - this.playerEntity.xo, this.playerEntity.getZ() - this.playerEntity.zo) * 20.0d * 3.6d > 20.0d) {
            return Collections.emptyList();
        }
        if (Math.abs(this.playerEntity.yo - this.playerEntity.getY()) > 0.6d) {
            return Collections.emptyList();
        }
        Position vec3d885 = getTargetPos(this.playerEntity);
        List<BlockPos> list877 = collectSurroundPositions(new ArrayList(), BlockPos.containing(vec3d885), 0);
        list877.removeIf(this::isPositionInvalid);
        list877.sort(Comparator.comparing(blockPos -> {
            return Integer.valueOf(-blockPos.getY());
        }));
        if (this.setting5.getValue().booleanValue()) {
            list877.sort(Comparator.comparing(blockPos2 -> {
                return Double.valueOf(-minecraftClient.player.position().distanceTo(Vec3.atCenterOf(blockPos2)));
            }));
        }
        return list877;
    }

    @Override // me.mioclient.module.Delay
    public int getSlotIndex() {
        return this.setting9.getValue().booleanValue() ? InventoryUtils.findInHotbar(Items.OBSIDIAN) : super.getSlotIndex();
    }

    @Listen
    public void onChunkDeltaUpdate(ChunkDeltaUpdateEvent chunkDeltaUpdateEvent) {
        if (this.setting2.getValue().booleanValue()) {
            do1323();
        }
    }

    public List<BlockPos> collectSurroundPositions(List<Integer> list, BlockPos blockPos, int i) {
        BlockPos blockPos878;
        ArrayList arrayList = new ArrayList();
        list.add(Integer.valueOf(blockPos.hashCode()));
        for (Vec3 vec3d : getList882(blockPos, i, this.playerEntity.getBbHeight())) {
            BlockPos add = blockPos.offset((int) vec3d.x, (int) vec3d.y, (int) vec3d.z);
            if (minecraftClient.level.getBlockState(add).canBeReplaced()) {
                if (!minecraftClient.level.getEntitiesOfClass(Player.class, new AABB(add), playerEntity -> {
                    return true;
                }).isEmpty()) {
                    if (!list.contains(Integer.valueOf(add.hashCode())) && vec3d.y == i) {
                        arrayList.addAll(collectSurroundPositions(list, add, i));
                    }
                }
                if (findSupportDirection(add, this.setting4.getValue().booleanValue(), arrayList) == null && !this.setting3.getValue().booleanValue() && (blockPos878 = getBlockPos878(add, arrayList)) != null) {
                    arrayList.add(blockPos878);
                }
                arrayList.add(add);
            }
        }
        return arrayList;
    }

    public BlockPos getBlockPos878(BlockPos blockPos, List<BlockPos> list) {
        Stream<Direction> stream = Arrays.stream(Direction.values());
        Objects.requireNonNull(blockPos);
        return (BlockPos) stream.map(d -> blockPos.relative(d)).sorted(Comparator.comparingDouble(this::getPlacementSortKey)).filter(blockPos2 -> {
            if (findSupportDirection(blockPos2, this.setting4.getValue().booleanValue(), list) != null) {
                if (BlockPlacer.isSpaceFree(blockPos2, true, this.setting7.getValue().booleanValue())) {
                    return true;
                }
            }
            return false;
        }).findFirst().orElse(null);
    }

    public double getPlacementSortKey(BlockPos blockPos) {
        if (!this.setting5.getValue().booleanValue()) {
            return blockPos.getY();
        }
        return -minecraftClient.player.position().distanceToSqr(Vec3.atCenterOf(blockPos));
    }

    public boolean isPositionInvalid(BlockPos blockPos) {
        if (this.playerEntity == null) {
            return true;
        }
        AABB box = new AABB(blockPos.above());
        for (Entity entity : minecraftClient.level.getEntitiesOfClass(Entity.class, box, e -> true)) {
            if ((entity instanceof DuckEndCrystalEntity) && !((DuckEndCrystalEntity) entity).isMioAttacked() && entity.getBoundingBox().intersects(box) && BlockPos.containing(entity.position()).equals(blockPos.above())) {
                return true;
            }
        }
        if (minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(blockPos)) > this.setting.getValue().floatValue()) {
            return true;
        }
        if (!this.setting6.getValue().booleanValue() && blockPos.getY() == this.playerEntity.blockPosition().getY()) {
            if (!minecraftClient.level.getBlockState(blockPos.above()).canBeReplaced()) {
                return true;
            }
        }
        return false;
    }

    public Direction findSupportDirection(BlockPos blockPos, boolean z, List<BlockPos> list) {
        for (Direction direction : Direction.values()) {
            BlockPos offset = blockPos.relative(direction);
            if (!minecraftClient.level.getBlockState(offset).canBeReplaced() || list.contains(offset)) {
                if (z) {
                    if (!BlockPlacer.getInteractableSides(offset).contains(direction.getOpposite())) {
                    }
                }
                return direction;
            }
        }
        return null;
    }

    public List<Vec3> getList882(BlockPos blockPos, int i, float f) {
        ArrayList arrayList = new ArrayList();
        if (is887()) {
            arrayList.add(new Vec3(0.0d, i + f + 1.0f, 0.0d));
            Iterator<Vec3> it = getSurroundOffsetVectors(i, f).iterator();
            if (it.hasNext()) {
                Vec3 next = it.next();
                BlockPos add = BlockPos.containing((Position) next).offset((Vec3i) blockPos);
                if (minecraftClient.level.getBlockState(add).canBeReplaced()) {
                    if (BlockPlacer.findSupportSide(add, this.setting4.getValue().booleanValue(), this.setting5.getValue().booleanValue()) == null) {
                        if (next.y > i) {
                            arrayList.add(new Vec3((-1.0d), i + 1, 0.0d));
                        } else {
                            arrayList.add(new Vec3((-1.0d), i, 0.0d));
                        }
                    }
                }
            }
        } else if (this.setting4.getValue().booleanValue()) {
            arrayList.add(new Vec3(0.0d, i + f + 1.0f, 0.0d));
        }
        for (Vec3 vec3d : getSurroundOffsetVectors(i, f)) {
            if (vec3d.y <= f && (this.setting5.getValue().booleanValue() || vec3d.y <= i)) {
                if (this.setting6.getValue().booleanValue() || vec3d.y != i) {
                    arrayList.add(vec3d);
                }
            }
        }
        if (this.setting7.getValue().booleanValue()) {
            arrayList.add(new Vec3(0.0d, i + f + 2.0f, 0.0d));
        }
        return arrayList;
    }

    public List<Vec3> getSurroundOffsetVectors(int i, float f) {
        return Arrays.asList(new Vec3(0.0d, i + f + 1.0f, 0.0d), new Vec3(1.0d, i, 0.0d), new Vec3(0.0d, i, 1.0d), new Vec3(0.0d, i, (-1.0d)), new Vec3((-1.0d), i, 0.0d), new Vec3(1.0d, i + 1, 0.0d), new Vec3(0.0d, i + 1, 1.0d), new Vec3(0.0d, i + 1, (-1.0d)), new Vec3((-1.0d), i + 1, 0.0d));
    }

    public boolean isValidRangeTarget(Player playerEntity) {
        if (playerEntity.isDeadOrDying() || !playerEntity.isAlive() || playerEntity == minecraftClient.player || ClientServices.socialManager.isFriendEntity(playerEntity)) {
            return false;
        }
        return (!this.setting8.getValue().booleanValue() || PlayerUtils.hasArmor((LivingEntity) playerEntity)) && playerEntity.distanceTo(minecraftClient.player) <= this.setting.getValue().floatValue() && playerEntity.onGround();
    }

    public Vec3 getTargetPos(Player playerEntity) {
        Vec3 pos = playerEntity.position();
        if (!(this.playerEntity instanceof FakePlayerCommand.RemotePlayer) && !(this.playerEntity instanceof LocalPlayer)) {
            DuckLivingEntity duckLivingEntity = (DuckLivingEntity)(this.playerEntity);
            pos = new Vec3(duckLivingEntity.mio$getServerX(), duckLivingEntity.mio$getServerY(), duckLivingEntity.mio$getServerZ());
        }
        return pos;
    }

    public Player getTargetPlayer() {
        return (Player) minecraftClient.level.players().stream().filter((v1) -> {
            return isValidRangeTarget(v1);
        }).min(Comparator.comparing(abstractClientPlayerEntity -> {
            return Float.valueOf(Mth.degreesDifferenceAbs(minecraftClient.player.getYRot(), RotationManager.getRotationsTo((Entity) abstractClientPlayerEntity)[0]));
        })).orElse(null);
    }

    public boolean is887() {
        return this.setting4.getValue().booleanValue() && !this.setting5.getValue().booleanValue();
    }

    @Override // me.mioclient.module.Delay
    public int getPriority() {
        return 150;
    }
}