package me.mioclient.module.player;
import net.minecraft.world.phys.Vec3;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import me.mioclient.module.render.Crosshair;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class Nuker extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null. */
    private static AntiCheat antiCheat;
    private static ItemSaver itemSaver;
    private static SpeedMine speedMine;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }

    public static ItemSaver getItemSaver() {
        if (itemSaver == null && ClientServices.moduleInjector != null) {
            try {
                itemSaver = (ItemSaver) ClientServices.moduleInjector.getModule(ItemSaver.class);
            } catch (Exception e) {
            }
        }
        return itemSaver;
    }

    public static SpeedMine getSpeedMine() {
        if (speedMine == null && ClientServices.moduleInjector != null) {
            try {
                speedMine = (SpeedMine) ClientServices.moduleInjector.getModule(SpeedMine.class);
            } catch (Exception e) {
            }
        }
        return speedMine;
    }
    public Setting<NukerMode_2> mode = add(new EnumSetting<>("Mode", NukerMode_2.nukerMode_2));
    public Setting<NukerMode_3> shape = add(new EnumSetting<>("Shape", NukerMode_3.nukerMode_3));
    public Setting<NukerMode> sort = add(new EnumSetting<>("Sort", NukerMode.CLOSEST));
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 50, 0, 500).setSuffix("ms"));
    public Setting<Float> range = add(new NumberSetting<>("Range", 4.5f, 0.0f, 6.0f).setSuffix("m"));
    public Setting<Float> wallRange = add(new NumberSetting<>("WallRange", 4.5f, 0.0f, 6.0f).setSuffix("m"));
    public Setting<Boolean> strictDirection = add(new BooleanSetting("StrictDirection", false));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false));
    public Setting<Boolean> creative = add(new BooleanSetting("Creative", false));
    public Setting<Boolean> flatten = add(new BooleanSetting("Flatten", false));
    public Setting<Boolean> baritoneArea = add(new BooleanSetting("BaritoneArea", false));
    public Setting<Set<Block>> whitelist = add(new EntityListObjectSetting<>("Whitelist", BuiltInRegistries.BLOCK));
    public final List<Record> list;
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public BlockPos blockPos;
    public int num;

    
    public enum NukerMode implements NamedValue {
        CLOSEST("Closest", blockPos -> {
            return Double.valueOf(ClientAccessor.minecraftClient.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos)));
        }),
        FURTHEST("Furthest", blockPos2 -> {
            return Double.valueOf(-ClientAccessor.minecraftClient.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(blockPos2)));
        }),
        TOP("Top", blockPos3 -> {
            return Double.valueOf(-blockPos3.getY());
        }),
        BOTTOM("Bottom", blockPos4 -> {
            return Double.valueOf(blockPos4.getY());
        });

        public final String name;
        public final Function<BlockPos, Double> function;

        NukerMode(String str, Function<BlockPos, Double> function) {
            this.name = str;
            this.function = function;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public static enum NukerMode_2 implements NamedValue {
        nukerMode_2("Shulkers") {
            @Override
            public boolean isBlockMatchForMode(BlockPos blockPos, Block block, Nuker nuker) {
                return block instanceof ShulkerBoxBlock;
            }
        },
        nukerMode_22("Bed") {
            @Override
            public boolean isBlockMatchForMode(BlockPos blockPos, Block block, Nuker nuker) {
                if (block instanceof BedBlock) {
                    if (ClientAccessor.minecraftClient.level.getBlockState(blockPos).getValue(BedBlock.PART) == BedPart.HEAD) {
                        return true;
                    }
                }
                return false;
            }
        },
        nukerMode_23("WhiteList") {
            @Override
            public boolean isBlockMatchForMode(BlockPos blockPos, Block block, Nuker nuker) {
                return nuker.whitelist.getValue().contains(block);
            }
        },
        nukerMode_24("BlackList") {
            @Override
            public boolean isBlockMatchForMode(BlockPos blockPos, Block block, Nuker nuker) {
                return !nukerMode_23.isBlockMatchForMode(blockPos, block, nuker);
            }
        };

        public final String name;

        NukerMode_2(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public boolean isBlockMatchForMode(BlockPos blockPos, Block block, Nuker nuker) {
            return false;
        }
    }

    
    public static enum NukerMode_3 implements NamedValue {
        nukerMode_3("Sphere") {
            @Override
            public List<BlockPos> getBlocksToBreak(Nuker nuker) {
                return WorldUtils.getBlocksInRadius(ClientAccessor.minecraftClient.player.position(), nuker.range.getValue().floatValue(), true);
            }
        },
        nukerMode_32("Tunnel") {
            @Override
            public List<BlockPos> getBlocksToBreak(Nuker nuker) {
                ArrayList arrayList = new ArrayList();
                for (int i = 0; i <= Math.ceil(nuker.range.getValue().floatValue()); i++) {
                    for (int i2 = 0; i2 < Math.ceil(ClientAccessor.minecraftClient.player.getBbHeight()); i2++) {
                        arrayList.add(ClientAccessor.minecraftClient.player.blockPosition().above(i2).relative(ClientAccessor.minecraftClient.player.getDirection(), i));
                    }
                }
                return arrayList;
            }
        },
        nukerMode_33("Crosshair") {
            @Override
            public List<BlockPos> getBlocksToBreak(Nuker nuker) {
                BlockHitResult blockHitResult = (ClientAccessor.minecraftClient.hitResult) instanceof BlockHitResult ? (BlockHitResult) (ClientAccessor.minecraftClient.hitResult) : null;
                return blockHitResult instanceof BlockHitResult ? Collections.singletonList(blockHitResult.getBlockPos()) : Collections.emptyList();
            }
        };

        public final String name;

        NukerMode_3(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public List<BlockPos> getBlocksToBreak(Nuker nuker) {
            return null;
        }
    }

    
    
    public static final class Record {
        public final BlockPos blockPos;
        public final long num;

        public Record(BlockPos blockPos, long j) {
            this.blockPos = blockPos;
            this.num = j;
        }

        public BlockPos getBlockPos() {
            return this.blockPos;
        }

        public long getTimestamp() {
            return this.num;
        }
    }

    public Nuker() {
        super("Nuker", "Breaks blocks nearby.", Category.PLAYER, new String[0]);
        this.list = new ArrayList();
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.blockPos = null;
        this.baritoneArea.requireBaritone();
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return ClientTextRenderer.capitalize(this.mode.getValue());
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        minecraftClient.options.keyAttack.setDown(false);
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        if (isNotInGame()) {
            return;
        }
        this.list.removeIf(record -> {
            return System.currentTimeMillis() - record.num > 1000;
        });
        this.blockPos = null;
        this.num = 0;
        if (this.stopwatch2.hasElapsedMs(this.delay.getValue().intValue())) {
            SpeedMine speedMine_lcl = getSpeedMine();
            if (speedMine_lcl != null && speedMine_lcl.isToggled()) {
                if (!speedMine_lcl.hasCooldownElapsed(this.delay.getValue().intValue())) {
                    return;
                }
            }
            boolean z = this.creative.getValue().booleanValue() || (speedMine_lcl != null && speedMine_lcl.isToggled() && speedMine_lcl.extraBreak.getValue().booleanValue());
            List<BlockPos> list437 = this.shape.getValue().getBlocksToBreak(this);
            list437.sort(Comparator.comparing(blockPos -> {
                return this.sort.getValue().function.apply(blockPos);
            }));
            for (BlockPos blockPos2 : list437) {
                if (is2162()) {
                    return;
                }
                if (!this.flatten.getValue().booleanValue() || blockPos2.getY() >= minecraftClient.player.getY()) {
                    if (canNuke(blockPos2)) {
                        breakBlock(blockPos2);
                        this.stopwatch2.reset();
                        if (!z && this.blockPos != null) {
                            return;
                        }
                        SpeedMine speedMine_lcl2 = getSpeedMine();
                        if (speedMine_lcl2 != null && speedMine_lcl2.isToggled() && speedMine_lcl2.extraBreak.getValue().booleanValue() && this.num >= 2) {
                            return;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
    }

    public void breakBlock(BlockPos blockPos) {
        int i;
        if (WorldUtils.isBreakable(blockPos)) {
            Direction direction = Direction.UP;
            if (this.strictDirection.getValue().booleanValue()) {
                List<Direction> list3031 = BlockPlacer.getInteractableSides(blockPos);
                if (list3031.isEmpty()) {
                    return;
                } else {
                    direction = list3031.get(0);
                }
            }
            if (this.rotate.getValue().booleanValue()) {
                ClientServices.RotationManager.requestRotations(RotationManager.getRotationsToSide(Vec3.atCenterOf(blockPos), direction), 5);
            }
            this.num++;
            SpeedMine speedMine_lcl = getSpeedMine();
            if (speedMine_lcl != null && speedMine_lcl.isToggled()) {
                SpeedMine.flag = true;
                minecraftClient.gameMode.startDestroyBlock(blockPos, direction);
                SpeedMine.flag = false;
                this.blockPos = blockPos;
                this.stopwatch.reset();
                return;
            }
            int i2 = -1;
            double longBitsToDouble = (-1.0d);
            BlockState blockState = minecraftClient.level.getBlockState(blockPos);
            ItemSaver itemSaver_lcl = getItemSaver();
            for (i = 0; i < 9; i++) {
                double miningSpeedMultiplier = minecraftClient.player.getInventory().getItem(i).getDestroySpeed(blockState);
                if (itemSaver_lcl != null && itemSaver_lcl.isToggled()) {
                    ItemSaver itemSaver2 = itemSaver_lcl;
                    i = ItemSaver.isToolSafe(minecraftClient.player.getInventory().getItem(i)) ? 0 : i + 1;
                }
                if (miningSpeedMultiplier > longBitsToDouble) {
                    longBitsToDouble = miningSpeedMultiplier;
                    i2 = i;
                }
            }
            this.blockPos = blockPos;
            this.stopwatch.reset();
            if (!this.creative.getValue().booleanValue()) {
                InventoryUtils.selectSlot(i2);
                minecraftClient.gameMode.continueDestroyBlock(blockPos, direction);
                minecraftClient.player.swing(InteractionHand.MAIN_HAND);
                return;
            }
            AntiCheat antiCheat_lcl = getAntiCheat();
            if (antiCheat_lcl != null && antiCheat_lcl.isF2b2t()) {
                InventoryUtils.selectSlot(i2);
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.blockPos, direction);
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.blockPos, direction);
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.blockPos, direction);
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.blockPos.offset(0, 500, 0), direction);
            } else {
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, this.blockPos, direction);
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, this.blockPos, direction);
            }
            PacketSender.sendHandSwing(InteractionHand.MAIN_HAND);
            this.list.add(new Record(this.blockPos, System.currentTimeMillis()));
        }
    }

    public boolean canNuke(BlockPos blockPos) {
        if (this.list.stream().anyMatch(record -> {
            return record.getBlockPos().equals(blockPos);
        })) {
            return false;
        }
        double sqrt = Math.sqrt(blockPos.distToCenterSqr(minecraftClient.player.getEyePosition()));
        if (!WorldUtils.isPositionVisible((Vec3i) blockPos) && sqrt > this.wallRange.getValue().floatValue()) {
            return false;
        }
        if (this.mode.getValue() == NukerMode_2.nukerMode_2 && ClientServices.stashFinderHelper.isBlockConfirmed(blockPos)) {
            return false;
        }
        if (this.baritoneArea.getValue().booleanValue() && ClientServices.obstaclePasserHelper.isBaritoneLoaded() && !ClientServices.obstaclePasserHelper.isInSelection(blockPos)) {
            return false;
        }
        return this.mode.getValue().isBlockMatchForMode(blockPos, BlockPlacer.getBlockAt(blockPos), this);
    }

    public boolean is2162() {
        SpeedMine speedMine_lcl = getSpeedMine();
        if (speedMine_lcl == null || !speedMine_lcl.isToggled()) {
            return false;
        }
        BlockPos blockPos1053 = speedMine_lcl.getMineTargetPos();
        if (blockPos1053 == null) {
            blockPos1053 = speedMine_lcl.getRebreakTargetPos();
        }
        BlockPos blockPos = null;
        if (speedMine_lcl.getSearchTarget() != null) {
            blockPos = speedMine_lcl.getSearchTarget().getBlockPos();
        }
        if (WorldUtils.isBreakable(blockPos1053)) {
            return blockPos == null || !blockPos.equals(blockPos1053);
        }
        return false;
    }

    public BlockHitResult getCrosshairBlockHit() {
        BlockHitResult blockHitResult = (minecraftClient.hitResult) instanceof BlockHitResult ? (BlockHitResult) (minecraftClient.hitResult) : null;
        if (!(blockHitResult instanceof BlockHitResult)) {
            return null;
        }
        BlockHitResult blockHitResult2 = blockHitResult;
        if (blockHitResult2.getType() == HitResult.Type.BLOCK) {
            return blockHitResult2;
        }
        return null;
    }
}