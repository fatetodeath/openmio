package me.mioclient.module;

import me.mioclient.event.events.network.PacketReceiveEvent;
import net.minecraft.core.Direction;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.event.events.player.MoveSpeedEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.mode.combat.CrystalRotationMode;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.render.shape.BlockHighlightRenderer;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.concurrent.LockedTaskRunner;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;
import me.mioclient.util.world.BlockRaycastFilter;
import me.mioclient.util.world.CustomRaycastContext;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.render.RenderConstants;
import me.mioclient.setting.NumberSetting;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.player.FinishUsingEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.combat.Offhand;
import me.mioclient.module.exploit.ChorusControl;
import me.mioclient.module.movement.NoSlow;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;

public abstract class Delay extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static AntiCheat antiCheat;
    private static ChorusControl chorusControl;
    private static NoSlow noslow;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }

    public static ChorusControl getChorusControl() {
        if (chorusControl == null && ClientServices.moduleInjector != null) {
            try {
                chorusControl = (ChorusControl) ClientServices.moduleInjector.getModule(ChorusControl.class);
            } catch (Exception e) {
            }
        }
        return chorusControl;
    }

    public static NoSlow getNoslow() {
        if (noslow == null && ClientServices.moduleInjector != null) {
            try {
                noslow = (NoSlow) ClientServices.moduleInjector.getModule(NoSlow.class);
            } catch (Exception e) {
            }
        }
        return noslow;
    }
    public Setting<Integer> setting;
    public Setting<Integer> setting2;
    public Setting<Boolean> setting3;
    public Setting<Boolean> setting4;
    public Setting<Boolean> setting5;
    public Setting<Boolean> setting6;
    public Setting<Boolean> setting7;
    public Setting<Boolean> setting8;
    public Setting<Boolean> setting9;
    public Setting<Boolean> setting10;
    public Setting<Boolean> setting11;
    public Setting<Boolean> setting12;
    public Setting<Boolean> setting13;
    public Setting<Color> setting14;
    public Setting<Color> setting15;
    public Setting<Float> setting16;
    public Setting<Boolean> setting17;
    public Setting<Float> setting18;
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public boolean flag;
    public Vec3 vec3d;
    public net.minecraft.core.Direction direction;
    public Entity entity;
    public int num;
    public int num2;
    public double val;
    public boolean flag2;
    public boolean flag3;
    public boolean flag4;
    public boolean flag5;
    public boolean flag6;

    
    public static final class Record {
        public final boolean flag;
        public final Record_2 record_2;
        public static final Record record_ = new Record(false, null);

        public Record(boolean z, Record_2 record_2) {
            this.flag = z;
            this.record_2 = record_2;
        }

        public boolean isPlacementEnabled() {
            return this.flag;
        }

        public Record_2 getRecord_2498() {
            return this.record_2;
        }
    }

    
    public static final class Record_2 {
        public final float[] floatArr;

        public Record_2(float[] fArr) {
            this.floatArr = fArr;
        }

        public boolean doesRaycastHitBlock(BlockPos blockPos) {
            HitResult hitResult985 = getHitResult985();
            if (hitResult985.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            return new AABB(hitResult985.getLocation().add((-0.01d), (-0.01d), (-0.01d)), hitResult985.getLocation().add(0.01d, 0.01d, 0.01d)).intersects(new AABB(blockPos));
        }

        public HitResult getHitResult985() {
            float f = this.floatArr[1] * MathConstants.val4;
            float f2 = (-this.floatArr[0]) * MathConstants.val4;
            float cos = Mth.cos(f2);
            float sin = Mth.sin(f2);
            float cos2 = Mth.cos(f);
            Vec3 vec3d = new Vec3(sin * cos2, -Mth.sin(f), cos * cos2);
            return RaycastUtils.performCustomBlockRaycast(new CustomRaycastContext.Inner(ClientAccessor.minecraftClient.player.getEyePosition(), ClientAccessor.minecraftClient.player.getEyePosition().add(vec3d.x * 5.0d, vec3d.y * 5.0d, vec3d.z * 5.0d)).setFilter(BlockRaycastFilter.createSolidFilter(ClientServices.stashFinderHelper.getTimedBlockMap().keySet())).build());
        }

        public float[] getLookAngles() {
            return this.floatArr;
        }
    }

    public Delay(String str, String str2, Category category, String... strArr) {
        super(str, str2, category, strArr);
        this.setting = add(new NumberSetting("Delay", 50, 0, 250).setSuffix("ms"));
        this.setting2 = add(new NumberSetting("BPT", 8, 1, 8));
        this.setting3 = add(new BooleanSetting("AirPlace", false));
        this.setting4 = add(new BooleanSetting("StrictDirection", false));
        this.setting5 = add(new BooleanSetting("Raytrace", false));
        this.setting6 = add(new BooleanSetting("Rotate", false));
        this.setting7 = add(new BooleanSetting("Attack", true));
        this.setting8 = add(new BooleanSetting("AutoDisable", false).setDependent());
        this.setting9 = add(new BooleanSetting("Jump", true).requiresSettings(this.setting8));
        this.setting10 = add(new BooleanSetting("Chorus", false).requiresSettings(this.setting8));
        this.setting11 = add(new BooleanSetting("Pearl", false).requiresSettings(this.setting8));
        this.setting12 = add(new BooleanSetting("RubberBand", false).requiresSettings(this.setting8));
        this.setting13 = add(new BooleanSetting("Render", false).setVisible());
        this.setting14 = add(new ColorSetting("Fill", new Color(189, 153, 255, 8), color -> {
            return this.setting13.isShown();
        }));
        this.setting15 = add(new ColorSetting("Outline", new Color(189, 153, 255, 91), color2 -> {
            return this.setting13.isShown();
        }));
        this.setting16 = add(new NumberSetting("LineWidth", Float.valueOf(1.0f), Float.valueOf(0.1f), Float.valueOf(3.0f)).requiresSettings(this.setting13));
        this.setting17 = add(new BooleanSetting("Fade", true).requiresSettings(this.setting13));
        this.setting18 = add(new NumberSetting("FadeTime", Float.valueOf(1.0f), Float.valueOf(0.1f), Float.valueOf(1.0f)).setSuffix("s").requiresSettings(this.setting13, this.setting17));
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        ClientServices.antiPhaseBlockHelper.register(new BlockHighlightRenderer.Record(this, this.setting14, this.setting15, this.setting16, this.setting18, () -> {
            return true;
        }, this.setting17, 450));
    }

    public Delay(String str, Category category, String... strArr) {
        this(str, "", category, strArr);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        if (!isNotInGame()) {
            this.val = minecraftClient.player.getY();
        }
        this.num = 0;
        this.num2 = 0;
        this.flag6 = false;
    }

    @Override // me.mioclient.module.Module
    public <T> Setting<T> add(Setting<T> setting) {
        if (!getRegistry().contains(this.setting18)) {
            return super.add(setting);
        }
        List<Setting<?>> registry = getRegistry();
        List<Setting<?>> list = registry;
        list.add(getRegistry().indexOf(this.setting7), setting);
        return setting;
    }

    public abstract List<BlockPos> getPlacePositions();

    public boolean is1320(BlockPos blockPos) {
        return true;
    }

    public void onBlockPlaced(BlockPos blockPos, boolean z) {
    }

    public boolean is1322() {
        return true;
    }

    public void do1323() {
        this.flag = false;
        if (this.flag6) {
            this.flag6 = false;
            return;
        }
        if (Offhand.is929() || SpeedMine.isSwapLocked() || minecraftClient.player.isSpectator() || !minecraftClient.player.isAlive() || minecraftClient.player.isPassenger() || minecraftClient.player.isSleeping() || isPlayerFellTooFar()) {
            return;
        }
        NoSlow noslow_lcl = getNoslow();
        if (this.flag3 && noslow_lcl != null && noslow_lcl.shouldApply()) {
            return;
        }
        if (this.stopwatch.hasElapsedMs(this.setting.getValue().intValue()) && is1322()) {
            List<BlockPos> list876 = getPlacePositions();
            int i = getSlotIndex();
            synchronized (list876) {
                filterPlaceablePositions(i, list876);
                sortPositionsBySupport(list876);
                if (!list876.isEmpty()) {
                    LockedTaskRunner.executeLocked(() -> {
                        executePlacements(list876);
                    });
                }
            }
            this.num = 0;
        }
    }

    public void sortPositionsBySupport(List<BlockPos> list) {
        if (this.setting2.getValue().intValue() > 1) {
            list.sort(Comparator.comparing(blockPos -> {
                return Integer.valueOf(BlockPlacer.findSupportSide(blockPos, this.setting4.getValue().booleanValue()) != null ? -1 : 1);
            }));
        }
    }

    @Listen(get219= RenderConstants.num2)
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        this.vec3d = null;
        this.entity = null;
        do1323();
    }

    @Listen
    public void onMoveSpeed(MoveSpeedEvent moveEvent_2) {
        if (moveEvent_2.getPhase() == KeyPearlMode.Post && moveEvent_2.getSpeed() > MathConstants.val2 && this.setting9.getValue().booleanValue()) {
            disable();
        }
    }

    @Listen
    public void onFinishUsing(FinishUsingEvent finishUsingEvent) {
        ChorusControl chorusControl_lcl = getChorusControl();
        if (finishUsingEvent.getItemStack().getItem() != Items.CHORUS_FRUIT || minecraftClient.player.isShiftKeyDown() || (chorusControl_lcl != null && chorusControl_lcl.isToggled())) {
            return;
        }
        if (this.setting10.getValue().booleanValue()) {
            disable();
        } else {
            this.flag6 = true;
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (this.setting7.getValue().booleanValue()) {
            ClientboundAddEntityPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundAddEntityPacket ? (ClientboundAddEntityPacket) (channelRead0Event.getPacket()) : null;
            if (packet904 instanceof ClientboundAddEntityPacket) {
                ClientboundAddEntityPacket entitySpawnS2CPacket = packet904;
                if (entitySpawnS2CPacket.getType() == EntityTypes.END_CRYSTAL) {
                    if (Offhand.is929()) {
                        return;
                    }
                    Vec3i ofFloored = BlockPos.containing(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ());
                    List<BlockPos> list876 = getPlacePositions();
                    filterPlaceablePositions(getSlotIndex(), list876);
                    Iterator<BlockPos> it = list876.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        if (it.next().closerThan(ofFloored, 2.0d)) {
                            if (!ClientServices.scaffoldHelper.sendAttackPacket(entitySpawnS2CPacket.getId(), true)) {
                                return;
                            } else {
                                PacketSender.sendHandSwing(InteractionHand.MAIN_HAND);
                            }
                        }
                    }
                    LockedTaskRunner.executeLocked(() -> {
                        executePlacements(list876);
                    });
                }
            }
        }
        ClientboundPlayerPositionPacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundPlayerPositionPacket ? (ClientboundPlayerPositionPacket) (channelRead0Event.getPacket()) : null;
        if (packet9042 instanceof ClientboundPlayerPositionPacket) {
            ClientboundPlayerPositionPacket playerPositionLookS2CPacket = packet9042;
            this.val = playerPositionLookS2CPacket.change().position().y;
            if (minecraftClient.player.position().distanceToSqr(playerPositionLookS2CPacket.change().position().x, playerPositionLookS2CPacket.change().position().y, playerPositionLookS2CPacket.change().position().z) >= 1.0d && this.setting12.getValue().booleanValue()) {
                disable();
                return;
            }
        }
        if ((channelRead0Event.getPacket() instanceof ClientboundRespawnPacket) && this.flag2) {
            disable();
        }
        ClientboundOpenScreenPacket packet9043 = (channelRead0Event.getPacket()) instanceof ClientboundOpenScreenPacket ? (ClientboundOpenScreenPacket) (channelRead0Event.getPacket()) : null;
        if (packet9043 instanceof ClientboundOpenScreenPacket) {
            ClientboundOpenScreenPacket openScreenS2CPacket = packet9043;
            if (this.stopwatch.hasElapsedMs(100L)) {
                return;
            }
            PacketSender.sendPacket(new ServerboundContainerClosePacket(openScreenS2CPacket.getContainerId()));
            channelRead0Event.cancel();
        }
    }

    public void executePlacements(List<BlockPos> list) {
        if (list.isEmpty()) {
            return;
        }
        Iterator<BlockPos> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            BlockPos next = it.next();
            if (this.setting7.getValue().booleanValue() && this.stopwatch2.hasElapsedMs(ClientServices.scaffoldHelper.getAttackDelay())) {
                this.entity = BlockPlacer.findAndAttackCrystal(next, ClientServices.scaffoldHelper.get1116());
                if (this.entity != null) {
                    if (this.setting6.getValue().booleanValue()) {
                        AntiCheat antiCheat_lcl = getAntiCheat();
                        ClientServices.RotationManager.requestRotations(RotationManager.limitYawSpeed(RotationManager.getRotationsTo(this.entity), antiCheat_lcl != null ? antiCheat_lcl.getYawStep() : 1.0f), getPriority() + 1);
                    }
                    this.stopwatch2.reset();
                }
            }
        }
        int i = getSlotIndex();
        int i2 = minecraftClient.player.getInventory().getSelectedSlot();
        Record_2 record_2 = null;
        Iterator<BlockPos> it2 = list.iterator();
        while (true) {
            if (!it2.hasNext()) {
                break;
            }
            BlockPos next2 = it2.next();
            if (this.num2 >= 30) {
                this.num2 = 0;
                ChatUtils.sendPrefixedMessage(Component.literal(getName()).append(" is out of blocks!"), ChatUtils.createSignature(-2), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode);
                this.num = 1337;
                disable();
                break;
            }
            if (i == -1 || minecraftClient.player.getInventory().getItem(i).isEmpty()) {
                break;
            }
            if (this.num >= this.setting2.getValue().intValue()) {
                break;
            }
            Record record1326 = getPlacementRecord(next2);
            if (record1326.isPlacementEnabled()) {
                this.num2 = 0;
            }
            if (record1326.isPlacementEnabled() && next2 != list.getLast() && is1329()) {
                Record_2 record_2498 = record1326.getRecord_2498();
                if (record_2 == null || !record_2.doesRaycastHitBlock(next2)) {
                    if (next2 != list.getFirst() || !new Record_2(new float[]{minecraftClient.player.getYRot(), minecraftClient.player.getXRot()}).doesRaycastHitBlock(next2)) {
                        PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), record_2498.getLookAngles()[0], record_2498.getLookAngles()[1], minecraftClient.player.onGround());
                        record_2 = record_2498;
                    }
                }
            }
        }
        this.num2++;
        if (!this.flag5) {
            this.flag5 = true;
            ClientServices.tooltipsHelper.schedule(() -> {
                this.flag5 = false;
            }, 1);
        }
        if (this.flag4 && shouldUseOffhand()) {
            PacketSender.sendSwapOffhand();
        }
        InventoryUtils.selectSlot(i2);
        this.flag4 = false;
    }

    public Record getPlacementRecord(BlockPos blockPos) {
        boolean z;
        if (Offhand.is929()) {
            return Record.record_;
        }
        this.direction = BlockPlacer.findSupportSide(blockPos, this.setting4.getValue().booleanValue(), this.setting5.getValue().booleanValue());
        if (this.direction == null && !this.setting3.getValue().booleanValue()) {
            return Record.record_;
        }
        float[] floatArray2485 = RotationManager.getRotationsToSide(Vec3.atCenterOf(blockPos), this.direction);
        boolean booleanValue = this.setting5.getValue().booleanValue();
        if (this.direction != null) {
            z = booleanValue & (!WorldUtils.isAnyPositionVisible(AutoCraftMode.X8.getVisibleFaceCorners(blockPos.relative(this.direction), this.direction.getOpposite())));
        } else {
            z = booleanValue & (!WorldUtils.isPositionVisible((Vec3i) blockPos));
        }
        if (!is1320(blockPos)) {
            return Record.record_;
        }
        boolean z2 = true;
        if (!this.flag4) {
            InventoryUtils.selectSlot(getSlotIndex());
            if (shouldUseOffhand()) {
                PacketSender.sendSwapOffhand();
            }
            this.flag4 = true;
        }
        if (!z && tryPlaceBlock(blockPos)) {
            AntiCheat antiCheat_lcl2 = getAntiCheat();
            if (RotationManager.is724() && antiCheat_lcl2 != null && antiCheat_lcl2.isF2b2t()) {
                floatArray2485[0] = (float) (floatArray2485[0] + (Math.random() * 0.009999999776482582d));
                floatArray2485[1] = (float) (floatArray2485[1] + (Math.random() * 0.009999999776482582d));
            }
            this.stopwatch.reset();
            if (this.setting13.getValue().booleanValue()) {
                ClientServices.antiPhaseBlockHelper.addBlockHighlight(this, blockPos);
            }
            this.vec3d = Vec3.atCenterOf(blockPos);
            if (this.direction != null) {
                this.vec3d = this.vec3d.relative(this.direction, MathConstants.val2);
            }
            if (this.setting6.getValue().booleanValue()) {
                AntiCheat antiCheat_lcl3 = getAntiCheat();
                ClientServices.RotationManager.requestRotations(RotationManager.limitYawSpeed(floatArray2485, antiCheat_lcl3 != null ? antiCheat_lcl3.getYawStep() : 1.0f), getPriority());
            }
            this.num++;
            z2 = false;
        }
        onBlockPlaced(blockPos, z2);
        return new Record(!z2, new Record_2(floatArray2485));
    }

    public boolean tryPlaceBlock(BlockPos blockPos) {
        return BlockPlacer.placeBlockPacket(blockPos, this.direction, this.setting3.getValue().booleanValue(), shouldUseOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    public int getSlotIndex() {
        return InventoryUtils.findInHotbar(Items.OBSIDIAN) == -1 ? InventoryUtils.findInHotbar(itemStack -> {
            BlockItem item = (itemStack.getItem()) instanceof BlockItem ? (BlockItem) (itemStack.getItem()) : null;
            if (item instanceof BlockItem) {
                BlockItem blockItem = item;
                if (blockItem.getBlock() != Blocks.ANVIL && blockItem.getBlock().getExplosionResistance() >= 600.0f) {
                    return true;
                }
            }
            return false;
        }) : InventoryUtils.findInHotbar(Items.OBSIDIAN);
    }

    public void do1328(BlockPos blockPos) {
        if (minecraftClient.level.getBlockState(blockPos).canBeReplaced()) {
            return;
        }
        if (minecraftClient.level.getBlockState(blockPos).getDestroySpeed(minecraftClient.level, blockPos) == 0.0f) {
            minecraftClient.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, net.minecraft.core.Direction.DOWN));
            minecraftClient.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, net.minecraft.core.Direction.DOWN));
        }
    }

    public boolean is1329() {
        AntiCheat antiCheat_lcl = getAntiCheat();
        boolean movementSyncSilent = antiCheat_lcl != null && antiCheat_lcl.movementSync.getValue().booleanValue() && antiCheat_lcl.rotations.getValue() == CrystalRotationMode.SILENT;
        return !movementSyncSilent && this.setting6.getValue().booleanValue() && this.setting2.getValue().intValue() > 1 && minecraftClient.player.onGround() && ClientServices.antiPhaseHelper.getGroundTicks() > 1 && !this.flag5;
    }

    public boolean isPlayerFellTooFar() {
        double y = minecraftClient.player.getY() - this.val;
        if (minecraftClient.player.onGround()) {
            this.val = minecraftClient.player.getY();
        }
        if ((minecraftClient.player.getY() <= this.val + 0.1d || y < 0.4d || !this.setting9.getValue().booleanValue()) && !minecraftClient.player.isSleeping()) {
            return false;
        }
        setToggled(false);
        return true;
    }

    public void filterPlaceablePositions(int i, List<BlockPos> list) {
        ArrayList arrayList = new ArrayList();
        Block block = null;
        if (i != -1) {
            BlockItem item = (minecraftClient.player.getInventory().getItem(i).getItem()) instanceof BlockItem ? (BlockItem) (minecraftClient.player.getInventory().getItem(i).getItem()) : null;
            if (item instanceof BlockItem) {
                block = item.getBlock();
            }
        }
        for (BlockPos blockPos : list) {
            double squaredDistanceTo = minecraftClient.player.getEyePosition().distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!BlockPlacer.canPlaceAt(blockPos, block, true, this.setting7.getValue().booleanValue()) || squaredDistanceTo > 36.0d) {
                arrayList.add(blockPos);
            } else if (BlockPlacer.findSupportSide(blockPos) == null) {
                this.flag = true;
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            list.remove((BlockPos) it.next());
        }
    }

    public boolean shouldUseOffhand() {
        AntiCheat antiCheat_lcl = getAntiCheat();
        return antiCheat_lcl != null && antiCheat_lcl.isF2b2t() && RotationManager.is724() && this.setting3.getValue().booleanValue() && this.flag;
    }

    public BlockPos getRoundedBlockPos() {
        return PlayerUtils.getRoundedBlockPos();
    }

    public int getPriority() {
        return 20;
    }

    public void setPlacementEnabled(boolean z) {
        this.flag2 = z;
    }

    public void do1335() {
        this.flag3 = true;
    }
}