package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.interaction.SlotChangeEvent;
import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.mode.combat.CrystalRotationMode;
import me.mioclient.module.combat.autocrystal.DamageOutcome;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.combat.AutoCrystalMode_2;
import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.mode.combat.ScaffoldHelperMode;
import me.mioclient.mode.player.AutoCraftMode;
import me.mioclient.mode.player.SpeedMineMode_2;
import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.module.combat.autocrystal.AutoCrystalDataMode;
import me.mioclient.module.combat.autocrystal.AutoCrystalMode;
import me.mioclient.module.combat.autocrystal.AutoCrystalMode_3;
import me.mioclient.module.combat.autocrystal.AutoCrystalMode_4;
import me.mioclient.module.combat.autocrystal.AutoCrystalMode_5;
import me.mioclient.render.shape.BlockHighlightRenderer;
import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.concurrent.LockedTaskRunner;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.inventory.ItemTypeUtils;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.module.combat.autocrystal.CrystalAttackTarget;
import me.mioclient.module.combat.autocrystal.CrystalPlacement;
import me.mioclient.module.combat.autocrystal.PlacementValidator;
import me.mioclient.module.combat.autocrystal.SpawnedEntityEntry;
import me.mioclient.network.PacketSender;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import me.mioclient.ducks.DuckPlayerListEntry;
import me.mioclient.ducks.DuckLivingEntityServerDead;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.ducks.DuckEndCrystalEntity;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.render.RenderEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.mixin.ducks.DuckEntityStatusS2CPacket;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.misc.KillEffects;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ClipContext;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AutoCrystal extends Module {
    public static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null. */
    private static SpeedMine speedmine;
    private static AntiCheat antiCheat;
    private static AnchorAura anchorAura;

    public static SpeedMine getSpeedmine() {
        if (speedmine == null && ClientServices.moduleInjector != null) {
            try {
                speedmine = (SpeedMine) ClientServices.moduleInjector.getModule(SpeedMine.class);
            } catch (Exception e) {
            }
        }
        return speedmine;
    }

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }

    public static AnchorAura getAnchorAura() {
        if (anchorAura == null && ClientServices.moduleInjector != null) {
            try {
                anchorAura = (AnchorAura) ClientServices.moduleInjector.getModule(AnchorAura.class);
            } catch (Exception e) {
            }
        }
        return anchorAura;
    }
    public Setting<Boolean> place2 = add(new BooleanSetting("Place", true).setVisible());
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 0, 0, 1000).setSuffix("ms").requiresSettings(this.place2));
    public Setting<Float> range = add(new NumberSetting<>("Range", 6.0f, 0.0f, 6.0f).setSuffix("m").requiresSettings(this.place2));
    public Setting<Float> wallRange = add(new NumberSetting<>("WallRange", 6.0f, 0.0f, 6.0f).setSuffix("m").requiresSettings(this.place2));
    public Setting<Float> minDamage2 = add(new NumberSetting<>("MinDamage", 6.0f, 0.1f, 20.1f).setSuffix("hp").setHoleSnapConfig("Lethal", HoleSnapMode.MAX).requiresSettings(this.place2));
    public Setting<Float> maxSelfDamage = add(new NumberSetting<>("MaxSelfDamage", 12.0f, 0.0f, 20.0f).setSuffix("hp").requiresSettings(this.place2));
    public Setting<Float> damageRatio = add(new NumberSetting<>("DamageRatio", 1.0f, 0.0f, 3.0f).setSuffix("x").requiresSettings(this.place2));
    public Setting<AutoCrystalMode_5> sequential = add(new EnumSetting<>("Sequential", AutoCrystalMode_5.STRICT).requiresSettings(this.place2));
    public Setting<Boolean> f112 = add(new BooleanSetting("1.12", false).requiresSettings(this.place2));
    public Setting<Boolean> strictDirection = add(new BooleanSetting("StrictDirection", false).requiresSettings(this.place2));
    public Setting<Boolean> mineIgnore = add(new BooleanSetting("MineIgnore", true).requiresSettings(this.place2));
    public Setting<Boolean> limit = add(new BooleanSetting("Limit", false).requiresSettings(this.place2));
    public Setting<Boolean> lowerHitBox = add(new BooleanSetting("LowerHitBox", false).requiresSettings(this.place2));
    public Setting<Boolean> smartTrace = add(new BooleanSetting("SmartTrace", false).requiresSettings(this.place2));
    public Setting<Boolean> basePlace = add(new BooleanSetting("BasePlace", false).requiresSettings(this.place2));
    public Setting<Boolean> air = add(new BooleanSetting("Air", false).requiresSettings(this.place2, this.basePlace));
    public Setting<Boolean> break_ = add(new BooleanSetting("Break", true).setVisible());
    public Setting<Integer> delay2 = add(new NumberSetting<>("Delay", 0, 0, 1000).setSuffix("ms").requiresSettings(this.break_));
    public Setting<Integer> factor = add(new NumberSetting<>("Factor", 1, 1, 5).requiresSettings(this.break_));
    public Setting<Float> range2 = add(new NumberSetting<>("Range", 6.0f, 0.0f, 6.0f).setSuffix("m").requiresSettings(this.break_));
    public Setting<Float> wallRange2 = add(new NumberSetting<>("WallRange", 6.0f, 0.0f, 6.0f).setSuffix("m").requiresSettings(this.break_));
    public Setting<Float> minDamage = add(new NumberSetting<>("MinDamage", 3.0f, 0.1f, 20.0f).setSuffix("hp").requiresSettings(this.break_));
    public Setting<Float> maxSelfDamage2 = add(new NumberSetting<>("MaxSelfDamage", 8.0f, 0.0f, 20.0f).setSuffix("hp").requiresSettings(this.break_));
    public Setting<Boolean> antiSuicide = add(new BooleanSetting("AntiSuicide", true).requiresSettings(this.break_));
    public Setting<Boolean> balance = add(new BooleanSetting("Balance", true).requiresSettings(this.break_));
    public Setting<Boolean> resync = add(new BooleanSetting("Resync", false).requiresSettings(this.break_));
    public Setting<Boolean> inhibit = add(new BooleanSetting("Inhibit", true).requiresSettings(this.break_));
    public Setting<AutoCrystalMode> instant = add(new EnumSetting<>("Instant", AutoCrystalMode.ALWAYS).requiresSettings(this.break_));
    public Setting<Boolean> await = add(new BooleanSetting("Await", true));
    public Setting<Integer> ticksExisted = add(new NumberSetting<>("TicksExisted", 2, 0, 40).requiresSettings(this.break_));
    public Setting<ScaffoldHelperMode> weakness = add(new EnumSetting<>("Weakness", ScaffoldHelperMode.NONE).requiresSettings(this.break_));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false).setVisible());
    public Setting<Boolean> place = add(new BooleanSetting("Place", true).requiresSettings(this.rotate));
    public Setting<Boolean> break_2 = add(new BooleanSetting("Break", true).requiresSettings(this.rotate));
    public Setting<Boolean> facePlace = add(new BooleanSetting("FacePlace", false).setVisible());
    public Setting<Float> health2 = add(new NumberSetting<>("Health", 4.0f, 0.0f, 36.0f).setSuffix("hp").requiresSettings(this.facePlace));
    public Setting<Integer> armorThreshold = add(new NumberSetting<>("ArmorThreshold", 10, 0, 100).setSuffix("%").requiresSettings(this.facePlace));
    public Setting<Boolean> damageSync = add(new BooleanSetting("DamageSync", false).requiresSettings(this.facePlace));
    public Setting<Boolean> extrapolation = add(new BooleanSetting("Extrapolation", true).setVisible());
    public Setting<Integer> ticks = add(new NumberSetting<>("Ticks", 4, 0, 8).setHoleSnapConfig("Auto", HoleSnapMode.MIN).requiresSettings(this.extrapolation));
    public Setting<Boolean> draw = add(new BooleanSetting("Draw", false).requiresSettings(this.extrapolation));
    public Setting<Boolean> render = add(new BooleanSetting("Render", true).setVisible());
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(1136499199, true)).setThemeSync(true).requiresSettings(this.render));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.render));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f).requiresSettings(this.render));
    public Setting<Boolean> fade = add(new BooleanSetting("Fade", true).setVisible().requiresSettings(this.render));
    public Setting<Boolean> multiFade = add(new BooleanSetting("MultiFade", false).requiresSettings(this.fade, this.render));
    public Setting<Float> fadeTime = add(new NumberSetting<>("FadeTime", 1.0f, 0.1f, 1.0f).setSuffix("s").requiresSettings(this.fade, this.render));
    public Setting<Boolean> pause = add(new BooleanSetting("Pause", false).setDependent());
    public Setting<Boolean> mining = add(new BooleanSetting("Mining", false).requiresSettings(this.pause));
    public Setting<Boolean> eating = add(new BooleanSetting("Eating", false).requiresSettings(this.pause));
    public Setting<Float> health = add(new NumberSetting<>("Health", 4.0f, 0.0f, 36.0f).setSuffix("hp").requiresSettings(this.pause));
    public Setting<Boolean> targeting = add(new BooleanSetting("Targeting", false).setDependent());
    public Setting<Float> targetRange = add(new NumberSetting<>("TargetRange", 12.0f, 1.0f, 16.0f).setSuffix("m").requiresSettings(this.targeting));
    public Setting<Float> crystalRange = add(new NumberSetting<>("CrystalRange", 8.0f, 0.1f, 12.0f).setSuffix("m").requiresSettings(this.targeting));
    public Setting<Boolean> antiFriendKill = add(new BooleanSetting("AntiFriendKill", false).requiresSettings(this.targeting));
    public Setting<Boolean> forceSuicide = add(new BooleanSetting("ForceSuicide", false).requiresSettings(this.targeting));
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", false).requiresSettings(this.targeting));
    public Setting<Boolean> assumeBestArmor = add(new BooleanSetting("AssumeBestArmor", false).requiresSettings(this.targeting));
    public Setting<Boolean> assumeInvincibility = add(new BooleanSetting("AssumeInvincibility", false).setVisible().requiresSettings(this.targeting));
    public Setting<Boolean> self = add(new BooleanSetting("Self", true).requiresSettings(this.assumeInvincibility, this.targeting));
    public Setting<Float> timeout = add(new NumberSetting<>("Timeout", 0.0f, 0.0f, 4.0f).setSuffix("s").requiresSettings(this.assumeInvincibility, this.targeting));
    public Setting<Boolean> misc = add(new BooleanSetting("Misc", false).setDependent());
    public Setting<AutoCrystalMode_2> autoSwap = add(new EnumSetting<>("AutoSwap", AutoCrystalMode_2.NONE).requiresSettings(this.misc));
    public Setting<AutoCrystalMode_4> silent = add(new EnumSetting<>("Silent", AutoCrystalMode_4.NORMAL));
    public Setting<Boolean> swapBack = add(new BooleanSetting("SwapBack", true));
    public Setting<Boolean> strictTiming = add(new BooleanSetting("StrictTiming", false));
    public Setting<Float> swapDelay = add(new NumberSetting<>("SwapDelay", 0.0f, 0.0f, 10.0f));
    public Setting<Float> swapPenalty = add(new NumberSetting<>("SwapPenalty", 0.0f, 0.0f, 10.0f).requiresSettings(this.misc));
    public Setting<AutoCraftMode> multipoint = add(new EnumSetting<>("Multipoint", AutoCraftMode.FAST).requiresSettings(this.misc));
    public Setting<Boolean> hBFix = add(new BooleanSetting("HBFix", false).requiresSettings(this.misc));
    public final Stopwatch stopwatch;
    public final AtomicReference<CrystalPlacement> atomicReference;
    public final AtomicReference<Vec3> atomicReference2;
    public final AtomicBoolean atomicBoolean;
    public final Stopwatch stopwatch2;
    public final Stopwatch stopwatch3;
    public final Stopwatch stopwatch4;
    public final Stopwatch stopwatch5;
    public final Stopwatch stopwatch6;
    public final Stopwatch stopwatch7;
    public final Stopwatch stopwatch8;
    public final Stopwatch stopwatch9;
    public final LinkedList<Long> linkedList;
    public final List<Integer> list;
    public final BlockPos.MutableBlockPos mutable;
    public CompletableFuture<CrystalPlacement> completableFuture;
    public volatile BlockPos blockPos;
    public volatile BlockPos blockPos2;
    public volatile BlockPos blockPos3;
    public volatile Direction direction;
    public volatile boolean flag;
    public volatile boolean flag2;
    public volatile boolean flag3;
    public volatile float val;
    public int num;
    public int num2;
    public int num3;
    public int num4;
    public BlockPos blockPos4;
    public boolean skip;
    public float[] floatArr;
    public SpawnedEntityEntry autoCrystalData_3;
    public Player playerEntity;
    public Player playerEntity2;

    public AutoCrystal() {
        super("AutoCrystal", "Kills people with end crystals (if you're good).", Category.COMBAT, "crystalaura", "ca", "ac");
        this.stopwatch = new Stopwatch();
        this.atomicReference = new AtomicReference<>();
        this.atomicReference2 = new AtomicReference<>();
        this.atomicBoolean = new AtomicBoolean();
        this.stopwatch2 = new Stopwatch();
        this.stopwatch3 = new Stopwatch();
        this.stopwatch4 = new Stopwatch();
        this.stopwatch5 = new Stopwatch();
        this.stopwatch6 = new Stopwatch();
        this.stopwatch7 = new Stopwatch();
        this.stopwatch8 = new Stopwatch();
        this.stopwatch9 = new Stopwatch();
        this.linkedList = new LinkedList<>();
        this.list = Collections.synchronizedList(new ArrayList());
        this.mutable = new BlockPos.MutableBlockPos();
        this.num = -1;
        ClientServices.antiPhaseBlockHelper.register(new BlockHighlightRenderer.Record(this, this.fill, this.outline, this.lineWidth, this.fadeTime, this.multiFade, this.fade, 1000));
        this.delay.setConfigName("PlaceDelay");
        this.range.setConfigName("PlaceRange");
        this.wallRange.setConfigName("PlaceWallRange");
        this.minDamage2.setConfigName("MinPlaceDamage");
        this.maxSelfDamage.setConfigName("MaxSelfPlace");
        this.delay2.setConfigName("HitDelay");
        this.factor.setConfigName("HitFactor");
        this.range2.setConfigName("BreakRange");
        this.wallRange2.setConfigName("BreakWallRange");
        this.minDamage.setConfigName("MinBreakDamage");
        this.maxSelfDamage2.setConfigName("MaxSelfHit");
        this.place.setConfigName("PlaceRotate");
        this.break_2.setConfigName("BreakRotate");
        this.health2.setConfigName("FacePlaceHealth");
        this.armorThreshold.setConfigName("FacePlaceArmor");
        this.extrapolation.setConfigName("ExtraTicks");
        this.timeout.setConfigName("InvincTimeout");
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.num = -1;
        this.playerEntity2 = this.playerEntity;
        this.playerEntity = null;
        this.direction = Direction.DOWN;
        this.blockPos = null;
        this.atomicReference.set(null);
        if (this.completableFuture != null) {
            try {
                this.completableFuture.complete(null);
            } catch (CancellationException e) {
            }
        }
        this.stopwatch5.reset();
        this.stopwatch9.setTime(-1L);
        this.num4 = -1;
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        restoreSlot();
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Pre) {
            this.flag = false;
            if (this.flag3) {
                this.flag3 = false;
                return;
            }
            this.flag3 = false;
            Player playerEntity = (this.playerEntity != null || isTargetInvalid(this.playerEntity2)) ? this.playerEntity : this.playerEntity2;
            if (playerEntity == null) {
                double longBitsToDouble = 1.7976931348623157E308d;
                for (Player playerEntity2 : minecraftClient.level.players()) {
                    double distanceTo = minecraftClient.player.distanceTo((Entity) playerEntity2);
                    if (!isTargetInvalid((LivingEntity) playerEntity2) && distanceTo <= longBitsToDouble) {
                        longBitsToDouble = distanceTo;
                        playerEntity = playerEntity2;
                    }
                }
                if (playerEntity == null) {
                    return;
                } else {
                    this.playerEntity = playerEntity;
                }
            }
            for (Entity entity : minecraftClient.level.entitiesForRendering()) {
                if (entity instanceof ItemEntity) {
                    ItemEntity itemEntity = (ItemEntity) entity;
                    if (itemEntity.getItem().getItem() == Items.OBSIDIAN && itemEntity.getItem().getCount() >= 8 && entity.isAlive()) {
                        AABB boundingBox = playerEntity.getBoundingBox();
                        if (entity.getBoundingBox().intersects(boundingBox.setMinY(boundingBox.getCenter().y).inflate(MathConstants.val2, 0.0d, MathConstants.val2))) {
                            this.flag3 = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    @Listen
    public void onRender(RenderEvent renderEvent) {
        if (isNotInGame() || isHoldingShulker()) {
            return;
        }
        if (this.completableFuture != null && this.completableFuture.isDone()) {
            try {
                CrystalPlacement autoCrystalData = this.completableFuture.get();
                if (autoCrystalData == null) {
                    this.atomicReference.set(null);
                    this.val = 0.0f;
                } else {
                    this.atomicReference.set(autoCrystalData);
                    this.val = (float) autoCrystalData.getDamage();
                    this.playerEntity = autoCrystalData.getPlayerTarget();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        if (this.completableFuture == null || this.completableFuture.isDone() || this.completableFuture.isCancelled()) {
            do1148();
            this.completableFuture = CompletableFuture.supplyAsync(this::findBestPlacement, executorService).exceptionally(th -> {
                return getAutoCrystalData1160(th, "PlaceThread");
            });
        }
        synchronized (this.list) {
            Iterator<Integer> it = this.list.iterator();
            while (it.hasNext()) {
                Entity entityById2336 = minecraftClient.level.getEntity(it.next().intValue());
                DuckEndCrystalEntity entityById = entityById2336 instanceof EndCrystal ? (DuckEndCrystalEntity) entityById2336 : null;
                if (entityById instanceof EndCrystal) {
                    entityById.setMioAttacked(true);
                }
            }
            this.list.removeIf(num -> {
                return minecraftClient.level.getEntity(num.intValue()) != null;
            });
        }
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        this.flag2 = false;
        this.blockPos2 = null;
        Entity entityById = minecraftClient.level.getEntity(this.num);
        if (entityById == null || !entityById.isAlive()) {
            this.num2 = 0;
        }
        if (minecraftClient.player.tickCount % 4 == 0) {
            long currentTimeMillis = System.currentTimeMillis();
            while (!this.linkedList.isEmpty() && this.linkedList.get(0) != null && currentTimeMillis - this.linkedList.get(0).longValue() > 1000) {
                try {
                    this.linkedList.remove();
                } catch (NullPointerException e) {
                }
            }
        }
        KillEffects killEffects = (KillEffects) ClientServices.moduleRegistry.findFirstByClass(KillEffects.class);
        if (killEffects != null && killEffects.isToggled() && !isTargetInvalid(this.playerEntity)) {
            killEffects.registerAttackTarget(this.playerEntity);
        }
        if (is929()) {
            return;
        }
        executeBreakTick();
        if (this.stopwatch3.hasElapsedMs(this.delay.getValue().intValue())) {
            LockedTaskRunner.executeLocked(this::executePlacementTick);
        }
        Vec3 vec3d = this.atomicReference2.get();
        if (this.rotate.getValue().booleanValue()) {
            if (vec3d == null || (RotationManager.is1144() && !isAttackOnCooldown())) {
                if (!RotationManager.is1144() || isAttackOnCooldown() || this.floatArr == null) {
                    return;
                }
                PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), this.floatArr[0], this.floatArr[1], minecraftClient.player.onGround());
                this.floatArr = null;
                return;
            }
            ClientServices.RotationManager.requestRotations(RotationManager.limitYawSpeed(RotationManager.getRotationsTo(vec3d), ClientServices.RotationManager.getYawStep()), 100, true);
            if (this.stopwatch2.hasElapsedMs(500L)) {
                this.atomicReference.lazySet(null);
                this.atomicReference2.lazySet(null);
            }
        }
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (this.draw.getValue().booleanValue() && this.extrapolation.getValue().booleanValue() && this.playerEntity != null) {
            for (int i = 1; i < BoxMath.getSnapDivisions(this.playerEntity, this.ticks); i++) {
                RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), BoxMath.getBottomCenter(ClientServices.mainhandHelper.getPredictedBox(this.playerEntity, i - 1)), BoxMath.getBottomCenter(ClientServices.mainhandHelper.getPredictedBox(this.playerEntity, i)), this.outline.getValue());
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundAddEntityPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundAddEntityPacket ? (ClientboundAddEntityPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket entitySpawnS2CPacket = packet904;
            this.autoCrystalData_3 = SpawnedEntityEntry.fromPacket(entitySpawnS2CPacket);
            if (!isSwapOnCooldown()) {
                try {
                    if (is929()) {
                        return;
                    } else {
                        do1130(entitySpawnS2CPacket);
                    }
                } catch (Throwable th) {
                }
            }
        }
        // cast to DuckEntityStatusS2CPacket and all but one type threw ClassCastException,
        // killing this listener for the rest of the event.
        if (channelRead0Event.getPacket() instanceof ClientboundEntityEventPacket) {
            ClientboundEntityEventPacket entityStatusS2CPacket = (ClientboundEntityEventPacket) channelRead0Event.getPacket();
            DuckEntityStatusS2CPacket duckEntityStatusS2CPacket = (DuckEntityStatusS2CPacket) entityStatusS2CPacket;
            if (entityStatusS2CPacket.getEventId() == 3 && this.playerEntity != null && duckEntityStatusS2CPacket.getId() == this.playerEntity.getId()) {
                ((DuckLivingEntityServerDead) this.playerEntity).setServerSideDead(true);
            }
        }
        if (channelRead0Event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
            this.forceSuicide.setValue(false);
        }
    }

    @Listen
    public void onSlotChange(SlotChangeEvent speedMineEvent) {
        if (this.num3 > 0) {
            this.num3--;
        } else {
            this.stopwatch6.reset();
            this.stopwatch5.reset();
        }
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        this.forceSuicide.setValue(false);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return (this.playerEntity == null || isTargetInvalid(this.playerEntity)) ? super.getInfo() : "%s, %.1f, %d".formatted(this.playerEntity.getGameProfile().name(), Float.valueOf(this.val), Integer.valueOf(this.linkedList.size()));
    }

    public void do1130(ClientboundAddEntityPacket entitySpawnS2CPacket) {
        if (entitySpawnS2CPacket.getType() != EntityTypes.END_CRYSTAL || this.playerEntity == null) {
            return;
        }
        Vec3 vec3d = new Vec3(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ());
        if (this.blockPos != null && vec3d.distanceToSqr(Vec3.atCenterOf(this.blockPos)) <= 9.0d) {
            this.linkedList.add(Long.valueOf(System.currentTimeMillis()));
        }
        if (this.instant.getValue() == AutoCrystalMode.NONE || !this.break_.getValue().booleanValue() || isPlayerInSpawnGrace(this.playerEntity)) {
            return;
        }
        if (isMineIgnoreActive() && shouldAwaitBeforeBreak()) {
            return;
        }
        if (minecraftClient.player.getEyePosition().distanceTo(vec3d) > (WorldUtils.isPositionVisible(vec3d) ? this.range2.getValue() : this.wallRange2.getValue()).floatValue()) {
            return;
        }
        double d = ExplosionDamageCalculator.getCrystalDamage(vec3d, this.playerEntity);
        double d2 = ExplosionDamageCalculator.getCrystalDamage(vec3d, minecraftClient.player);
        DamageOutcome autoCrystalMode_71882 = DamageOutcome.evaluate(this.playerEntity, d);
        if (!this.balance.getValue().booleanValue() || d > d2 || this.forceSuicide.getValue().booleanValue() || autoCrystalMode_71882.flag) {
            if (d >= AutoCrystalMode_3.HIT.getMinDamageThreshold(this.playerEntity)) {
                if (AutoCrystalMode_3.HIT.shouldSkipAttack(this.playerEntity, d2, d, autoCrystalMode_71882)) {
                    return;
                }
                BlockPos ofFloored = BlockPos.containing((Position) vec3d);
                if (this.instant.getValue() == AutoCrystalMode.STRICT && this.blockPos4.equals(ofFloored)) {
                    return;
                }
                if (this.rotate.getValue().booleanValue() && this.break_2.getValue().booleanValue()) {
                    rotateToTarget(vec3d);
                }
                if (ClientServices.scaffoldHelper.sendAttackPacket(entitySpawnS2CPacket.getId(), true)) {
                    this.blockPos2 = ofFloored;
                    this.blockPos4 = ofFloored;
                    this.num = entitySpawnS2CPacket.getId();
                    this.num2 = 0;
                    this.stopwatch9.reset();
                }
                boolean z = minecraftClient.player.isHolding(Items.END_CRYSTAL) || EntityUtil.getPlayerHealth() >= 8.0f;
                AntiCheat antiCheat_lcl = getAntiCheat();
                if (antiCheat_lcl == null || !antiCheat_lcl.isF2b2t()) {
                    z = true;
                }
                if (this.sequential.getValue().isActiveCrystalMode() && !this.flag && z) {
                    this.stopwatch7.reset();
                    LockedTaskRunner.executeLocked(this::executePlacementTick);
                    if (this.sequential.getValue() == AutoCrystalMode_5.STRICT) {
                        this.flag = true;
                    }
                }
                sendFactorAttacks(entitySpawnS2CPacket.getId());
            }
        }
    }

    public CrystalPlacement findBestPlacement() {
        CrystalPlacement autoCrystalData = this.atomicReference.get();
        CrystalAttackTarget autoCrystalData_4 = null;
        CrystalPlacement autoCrystalData2 = null;
        SpeedMine speedmine_lcl_search = getSpeedmine();
        PlacementValidator autoCrystalSearchHelper42080 = new PlacementValidator().getAutoCrystalSearchHelper42071(this.f112.getValue().booleanValue()).getAutoCrystalSearchHelper42075(this.lowerHitBox.getValue().booleanValue()).withHitboxFix(this.hBFix.getValue().booleanValue() && !isMineIgnoreActive()).withBasePlace(this.basePlace.getValue().booleanValue() && isBasePlaceNeeded()).getAutoCrystalSearchHelper42078(isMineIgnoreActive() && speedmine_lcl_search != null && speedmine_lcl_search.rebreak2.getValue() != SpeedMineMode_2.INSTANT).getAutoCrystalSearchHelper42079(this.extrapolation.getValue().booleanValue() ? this.ticks.getValue().intValue() : 0).getAutoCrystalSearchHelper42080(this.range2.getValue().floatValue());
        if (autoCrystalData != null) {
            if (autoCrystalSearchHelper42080.isPositionValid(autoCrystalData.getBlockPos())) {
                autoCrystalData_4 = findBestTarget(autoCrystalData.getBlockPos(), WorldUtils.isBedrockOrObsidian(autoCrystalData.getBlockPos()));
            }
        }
        double d = -Math.floor(this.range.getValue().floatValue());
        while (true) {
            double d2 = d;
            if (d2 >= Math.ceil(this.range.getValue().floatValue())) {
                return autoCrystalData2;
            }
            double d3 = -Math.floor(this.range.getValue().floatValue());
            while (true) {
                double d4 = d3;
                if (d4 < Math.ceil(this.range.getValue().floatValue())) {
                    double d5 = -Math.floor(this.range.getValue().floatValue());
                    while (true) {
                        double d6 = d5;
                        if (d6 < Math.ceil(this.range.getValue().floatValue())) {
                            this.mutable.set(minecraftClient.player.getX() + d2, minecraftClient.player.getEyeY() + d4, minecraftClient.player.getZ() + d6);
                            boolean isBedrockOrObsidian = WorldUtils.isBedrockOrObsidian(this.mutable);
                            if (autoCrystalData2 == null || autoCrystalData2.getDataMode() == AutoCrystalDataMode.OBSIDIAN || isBedrockOrObsidian) {
                                double distanceTo = minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(this.mutable));
                                if (distanceTo <= this.range.getValue().floatValue() && autoCrystalSearchHelper42080.isPositionValid(this.mutable)) {
                                    boolean isAnyPositionVisible = WorldUtils.isAnyPositionVisible(this.multipoint.getValue().getWorldCorners(this.mutable));
                                    if ((isAnyPositionVisible || distanceTo <= this.wallRange.getValue().floatValue() || this.smartTrace.getValue().booleanValue()) && (!this.strictDirection.getValue().booleanValue() || !BlockPlacer.getInteractableSides(this.mutable).isEmpty())) {
                                        double d7 = ExplosionDamageCalculator.getExplosionDamage(Vec3.atCenterOf(this.mutable).add(0.0d, 0.5d, 0.0d), minecraftClient.player, getTargetBox(minecraftClient.player), 6.0d, true, (BlockPos) null, this.mutable);
                                        CrystalAttackTarget autoCrystalData_41147 = findBestTarget(this.mutable, isBedrockOrObsidian);
                                        if (autoCrystalData_41147 != null) {
                                            if (!AutoCrystalMode_3.PLACE.shouldSkipAttack(autoCrystalData_41147.getPlayerTarget(), d7, autoCrystalData_41147.getDamage(), DamageOutcome.evaluate(autoCrystalData_41147.getPlayerTarget(), autoCrystalData_41147.getDamage()))) {
                                                AutoCrystalDataMode autoCrystalDataMode = AutoCrystalDataMode.NONE;
                                                if (autoCrystalData_4 == null || autoCrystalData.getBlockPos().equals(this.mutable) || Math.round(autoCrystalData_4.getDamage()) < autoCrystalData_41147.getDamage()) {
                                                    if (!isAnyPositionVisible && distanceTo > this.wallRange.getValue().floatValue()) {
                                                        autoCrystalDataMode = AutoCrystalDataMode.RAYTRACE;
                                                    }
                                                    boolean store = true;
                                                    if (this.basePlace.getValue().booleanValue() && !isBedrockOrObsidian) {
                                                        if (getTargetBox(autoCrystalData_41147.getPlayerTarget()).minmax(autoCrystalData_41147.getPlayerTarget().getBoundingBox()).intersects(new AABB(this.mutable))) {
                                                            store = false;
                                                        } else if (!BlockPlacer.isSpaceFree(this.mutable, false)) {
                                                            store = false;
                                                        } else if (!this.air.getValue().booleanValue() && BlockPlacer.findSupportSide(this.mutable, this.strictDirection.getValue().booleanValue()) == null) {
                                                            store = false;
                                                        } else {
                                                            autoCrystalDataMode = AutoCrystalDataMode.OBSIDIAN;
                                                        }
                                                    }
                                                    if (store) {
                                                        autoCrystalData2 = new CrystalPlacement(this.mutable.immutable(), autoCrystalData_41147.getPlayerTarget(), autoCrystalData_41147.getDamage(), autoCrystalDataMode).preferOver(autoCrystalData2);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            d5 = d6 + 1.0d;
                        } else {
                            break;
                        }
                    }
                    d3 = d4 + 1.0d;
                } else {
                    break;
                }
            }
            d = d2 + 1.0d;
        }
    }

    public void executePlacementTick() {
        if (!isMineIgnoreActive()) {
            this.atomicBoolean.set(false);
        }
        if (!this.place2.getValue().booleanValue()) {
            return;
        }
        if (isSwapOnCooldown()) {
            return;
        }
        if (this.atomicBoolean.get()) {
            return;
        }
        boolean z = minecraftClient.player.getOffhandItem().getItem() == Items.END_CRYSTAL;
        boolean isHolding = minecraftClient.player.isHolding(Items.END_CRYSTAL);
        if ((this.damageSync.getValue().booleanValue() || this.flag3) && this.val < this.minDamage2.getValue().floatValue() && this.val < 5.0f) {
            if (!this.stopwatch3.hasElapsedMs(getBasePlaceDelay())) {
                return;
            }
        }
        CrystalPlacement autoCrystalData = this.atomicReference.get();
        if (autoCrystalData == null) {
            restoreSlot();
            return;
        }
        BlockPos blockPos12 = autoCrystalData.getBlockPos();
        if (blockPos12 == null || this.playerEntity == null) {
            restoreSlot();
            return;
        }
        double d = AutoCrystalMode_3.PLACE.getMinDamageThreshold(this.playerEntity);
        SpeedMine speedmine_lcl_place = getSpeedmine();
        double d2 = ExplosionDamageCalculator.getExplosionDamage(Vec3.atCenterOf(blockPos12).add(0.0d, 0.5d, 0.0d), this.playerEntity, getTargetBox(this.playerEntity), 6.0d, true, (isMineIgnoreActive() && speedmine_lcl_place != null) ? speedmine_lcl_place.getBreakingPos() : null, blockPos12);
        if (this.extrapolation.getValue().booleanValue() && this.ticks.getValue().intValue() > 0 && d2 > 2.0d && d2 < d) {
            d2 = Math.max(d2, calcExplosionDamage(blockPos12, this.playerEntity, BoxMath.getServerBox(this.playerEntity), true));
        }
        if (d2 < d) {
            restoreSlot();
            return;
        }
        if (!WorldUtils.isBedrockOrObsidian(blockPos12)) {
            if (autoCrystalData.getDataMode() == AutoCrystalDataMode.OBSIDIAN && this.basePlace.getValue().booleanValue()) {
                do1134(blockPos12);
                return;
            }
            return;
        }
        int i = -1;
        int i2 = InventoryUtils.findInHotbar(Items.END_CRYSTAL);
        if (this.limit.getValue().booleanValue()) {
            if (!BlockPlacer.isCrystalPlaceable(blockPos12, this.f112.getValue().booleanValue(), true, true, true, this.lowerHitBox.getValue().booleanValue(), this.hBFix.getValue().booleanValue())) {
                return;
            }
        }
        if (minecraftClient.level.getBlockState(blockPos12.above()).getBlock() == Blocks.FIRE) {
            minecraftClient.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos12.above(), Direction.DOWN));
            minecraftClient.player.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos12.above(), Direction.DOWN));
            return;
        }
        boolean z2 = false;
        boolean z3 = is1144() && this.silent.getValue() == AutoCrystalMode_4.ALTERNATIVE;
        boolean z4 = false;
        SpeedMine speedmine_lcl_swap = getSpeedmine();
        if (!isHolding && isMineIgnoreActive() && speedmine_lcl_swap != null && speedmine_lcl_swap.swapReset.getValue().booleanValue() && speedmine_lcl_swap.alternative.getValue().booleanValue()) {
            z3 = true;
            z4 = true;
        }
        if (z3 && (!this.skip || !minecraftClient.player.containerMenu.getCarried().isEmpty())) {
            this.skip = true;
            return;
        }
        if (z3 && i2 == -1) {
            i2 = InventoryUtils.findSlot(Items.END_CRYSTAL);
        }
        if (this.autoSwap.getValue() != AutoCrystalMode_2.NONE && !isHolding && !isAutoSwapOnCooldown() && i2 != -1) {
            i = minecraftClient.player.getInventory().getSelectedSlot();
            if (z3) {
                InventoryUtils.swapToHotbar(i2);
                z2 = true;
                this.atomicBoolean.set(z4);
            } else if (is1144() && this.silent.getValue() == AutoCrystalMode_4.PICK) {
                minecraftClient.player.getInventory().pickSlot(i2);
            } else {
                if (this.autoSwap.getValue() == AutoCrystalMode_2.SILENT) {
                    SpeedMine speedmine_lcl_flag = getSpeedmine();
                    if (speedmine_lcl_flag != null) {
                        speedmine_lcl_flag.flag2 = true;
                    }
                    incrementSwapCounter();
                }
                if (shouldSwapBack()) {
                    this.num4 = i;
                }
                InventoryUtils.selectSlot(i2);
            }
        }
        if ((i2 == -1 || this.autoSwap.getValue() == AutoCrystalMode_2.NONE) && !isHolding) {
            return;
        }
        placeCrystal(blockPos12, z, this.direction);
        if (is1144() || z2) {
            if (i != -1) {
                if (z2) {
                    InventoryUtils.swapToHotbar(i2);
                } else if (this.autoSwap.getValue() == AutoCrystalMode_2.SILENT) {
                    if (is1144() && this.silent.getValue() == AutoCrystalMode_4.PICK) {
                        minecraftClient.player.getInventory().pickSlot(i2);
                    } else {
                        incrementSwapCounter();
                        InventoryUtils.selectSlot(i);
                    }
                }
            }
            if (this.strictTiming.getValue().booleanValue() || z4 || this.skip) {
                this.skip = !this.skip;
            }
        }
    }

    public void placeCrystal(BlockPos blockPos, boolean z, Direction direction) {
        if (blockPos == null || direction == null) {
            return;
        }
        if (blockPos.equals(this.blockPos2) || this.blockPos2 == null || this.sequential.getValue().isActiveCrystalMode()) {
            List<Direction> list3031 = BlockPlacer.getInteractableSides(blockPos);
            Direction direction2 = direction;
            if (!list3031.isEmpty()) {
                direction2 = list3031.get(0);
            }
            Vec3 offset = Vec3.atCenterOf(blockPos).relative(direction2, MathConstants.val2);
            double longBitsToDouble = 1.7976931348623157E308d;
            double[] dArr = {0.05d, 0.95d};
            for (double d : dArr) {
                for (double d2 : new double[]{0.05d, 0.95d, 0.99d}) {
                    for (double d3 : dArr) {
                        Vec3 add = Vec3.atLowerCornerOf( blockPos).add(d, d2, d3);
                        BlockHitResult raycast = minecraftClient.level.clip(new ClipContext(minecraftClient.player.getEyePosition(), add, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player));
                        double distanceTo = raycast.getLocation().distanceTo(minecraftClient.player.getEyePosition());
                        if ((raycast.getType() == HitResult.Type.MISS || raycast.getBlockPos().equals(blockPos)) && add.distanceTo(minecraftClient.player.getEyePosition()) <= this.range.getValue().floatValue() && distanceTo < longBitsToDouble && (list3031.contains(raycast.getDirection()) || !this.strictDirection.getValue().booleanValue())) {
                            offset = raycast.getLocation();
                            direction2 = raycast.getDirection();
                            longBitsToDouble = distanceTo;
                        }
                    }
                }
            }
            if (this.place.getValue().booleanValue() && this.rotate.getValue().booleanValue() && !this.flag2) {
                rotateToTarget(offset);
            }
            InteractionHand hand = z ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            PacketSender.sendInteractBlock(hand, new BlockHitResult(Vec3.atCenterOf(blockPos).relative(direction2, MathConstants.val2), direction2, blockPos, false));
            minecraftClient.player.swing(hand);
            this.blockPos = blockPos;
            if (this.render.getValue().booleanValue()) {
                ClientServices.antiPhaseBlockHelper.addBlockHighlight(this, blockPos);
            }
            this.stopwatch3.reset();
        }
    }

    public void do1134(BlockPos blockPos) {
        if (isBasePlaceNeeded()) {
            int i = InventoryUtils.findInHotbar(Items.END_CRYSTAL);
            if (this.autoSwap.getValue() == AutoCrystalMode_2.SILENT && this.silent.getValue() == AutoCrystalMode_4.ALTERNATIVE) {
                i = InventoryUtils.findSlot(Items.END_CRYSTAL);
            }
            if (minecraftClient.player.getOffhandItem().is(Items.END_CRYSTAL) || i != -1) {
                int i2 = minecraftClient.player.getInventory().getSelectedSlot();
                int i3 = InventoryUtils.findInHotbar(Items.OBSIDIAN);
                Direction direction3029 = BlockPlacer.findSupportSide(blockPos, this.strictDirection.getValue().booleanValue());
                SpeedMine speedmine_lcl_base = getSpeedmine();
                AntiCheat antiCheat_lcl_base = getAntiCheat();
                boolean z = (is1144() && this.silent.getValue() == AutoCrystalMode_4.ALTERNATIVE) || (this.silent.getValue() == AutoCrystalMode_4.PICK && isMineIgnoreActive() && speedmine_lcl_base != null && speedmine_lcl_base.getBreakingPos() != null);
                boolean z2 = this.air.getValue().booleanValue() && direction3029 == null && antiCheat_lcl_base != null && antiCheat_lcl_base.isF2b2t();
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (z && i3 == -1) {
                    i3 = InventoryUtils.findSlot(Items.OBSIDIAN);
                }
                if (i3 != -1) {
                    if ((direction3029 != null || this.air.getValue().booleanValue()) && BlockPlacer.isSpaceFree(blockPos, false)) {
                        if (i3 != i2) {
                            if (z) {
                                InventoryUtils.swapToHotbar(i3);
                            } else if (is1144() && this.silent.getValue() == AutoCrystalMode_4.PICK) {
                                minecraftClient.player.getInventory().pickSlot(i3);
                            } else {
                                SpeedMine speedmine_lcl_flag2 = getSpeedmine();
                                if (speedmine_lcl_flag2 != null) {
                                    speedmine_lcl_flag2.flag2 = true;
                                }
                                InventoryUtils.selectSlot(i3);
                            }
                        }
                        if (z2) {
                            PacketSender.sendSwapOffhand();
                            hand = InteractionHand.OFF_HAND;
                        }
                        BlockPlacer.placeBlockPacket(blockPos, direction3029, this.air.getValue().booleanValue(), hand);
                        this.stopwatch9.reset();
                        if (z2) {
                            PacketSender.sendSwapOffhand();
                        }
                        if (this.place.getValue().booleanValue() && this.rotate.getValue().booleanValue()) {
                            Vec3 centerPos = Vec3.atCenterOf(blockPos);
                            if (direction3029 != null) {
                                centerPos = centerPos.relative(direction3029, 0.5d);
                            }
                            rotateToTarget(centerPos);
                        }
                        this.blockPos3 = blockPos;
                        if (i2 == -1 || i3 == i2) {
                            return;
                        }
                        if (z) {
                            InventoryUtils.swapToHotbar(i3);
                        } else if (is1144() && this.silent.getValue() == AutoCrystalMode_4.PICK) {
                            minecraftClient.player.getInventory().pickSlot(i3);
                        } else {
                            InventoryUtils.selectSlot(i2);
                        }
                    }
                }
            }
        }
    }

    public void executeBreakTick() {
        if (this.break_.getValue().booleanValue()) {
            if (!this.stopwatch4.hasElapsedMs(this.delay2.getValue().intValue())) {
                return;
            }
            if (isSwapOnCooldown()) {
                return;
            }
            if (isPlayerInSpawnGrace(this.playerEntity)) {
                return;
            }
            if (this.stopwatch7.hasElapsedMs(ClientServices.holeSnapRenderHelper.getPing()) || this.instant.getValue() == AutoCrystalMode.NONE || !shouldAwaitBeforeBreak() || isMineIgnoreActive()) {
                SpeedMine speedMine = (SpeedMine) ClientServices.moduleRegistry.findFirstByClass(SpeedMine.class);
                Entity entity = null;
                double longBitsToDouble = (-1337.0d);
                boolean isMineIgnoreActive = isMineIgnoreActive();
                SpeedMine speedmine_lcl_break = getSpeedmine();
                if (speedmine_lcl_break == null || !WorldUtils.isBreakable(speedmine_lcl_break.getBreakingPos())) {
                    isMineIgnoreActive = false;
                }
                BlockPos blockPos1051 = speedMine != null ? speedMine.getBreakingPos() : (speedmine_lcl_break != null ? speedmine_lcl_break.getBreakingPos() : null);
                if (blockPos1051 == null || !WorldUtils.isBreakable(blockPos1051)) {
                    blockPos1051 = null;
                }
                for (Entity entity2 : minecraftClient.level.entitiesForRendering()) {
                    if (entity2 instanceof EndCrystal) {
                        if (isCrystalValid((EndCrystal) entity2)) {
                            double d = this.skip ? ExplosionDamageCalculator.getCrystalDamage(entity2.position(), this.playerEntity, getTargetBox(this.playerEntity)) : calcExplosionDamage(BlockPos.containing(entity2.position()).below(), this.playerEntity, getTargetBox(this.playerEntity), false);
                            double d2 = ExplosionDamageCalculator.getCrystalDamage(entity2.position(), minecraftClient.player, minecraftClient.player.getBoundingBox());
                            DamageOutcome autoCrystalMode_71882 = DamageOutcome.evaluate(this.playerEntity, d);
                            if (!this.balance.getValue().booleanValue() || d > d2 || this.forceSuicide.getValue().booleanValue() || autoCrystalMode_71882.flag) {
                                if (!AutoCrystalMode_3.HIT.shouldSkipAttack(this.playerEntity, d2, d, autoCrystalMode_71882)) {
                                    if (this.blockPos != null) {
                                        if (entity2.getBoundingBox().intersects(new AABB(this.blockPos.above()).expandTowards(0.0d, 1.0d, 0.0d)) && entity == null && (!isMineIgnoreActive || blockPos1051 == null)) {
                                            entity = entity2;
                                        }
                                    }
                                    if (d >= AutoCrystalMode_3.HIT.getMinDamageThreshold(this.playerEntity) && d > longBitsToDouble) {
                                        longBitsToDouble = d;
                                        entity = entity2;
                                    }
                                }
                            }
                        }
                    }
                }
                if (entity == null) {
                    return;
                }
                attackCrystal((EndCrystal) entity, minecraftClient.player.getOffhandItem().getItem() == Items.END_CRYSTAL);
            }
        }
    }

    public void attackCrystal(EndCrystal endCrystalEntity, boolean z) {
        if (endCrystalEntity.getId() == this.num && this.stopwatch8.hasElapsedMs(500L) && endCrystalEntity.isAlive() && !endCrystalEntity.isRemoved()) {
            this.num2++;
        } else {
            this.num2 = 0;
        }
        this.num = endCrystalEntity.getId();
        if (this.num2 >= 5) {
            this.stopwatch8.reset();
        }
        if (isAttackOnCooldown() && this.resync.getValue().booleanValue()) {
            return;
        }
        // tryAttackCrystal mutates attack-handler state, so it must be called exactly once per attack.
        boolean attackAccepted = ClientServices.scaffoldHelper.tryAttackCrystal(endCrystalEntity.getId());
        if (attackAccepted) {
            ((DuckEndCrystalEntity) endCrystalEntity).setMioAttacked(true);
            sendFactorAttacks(endCrystalEntity.getId());
            this.blockPos2 = BlockPos.containing(endCrystalEntity.position()).below();
            if (this.break_2.getValue().booleanValue() && this.rotate.getValue().booleanValue()) {
                rotateToTarget(BoxMath.getCenter(endCrystalEntity.getBoundingBox()));
                this.flag2 = true;
            }
            this.stopwatch4.reset();
            this.stopwatch9.reset();
        }
    }

    public boolean isCrystalValid(EndCrystal endCrystalEntity) {
        if ((endCrystalEntity.tickCount < this.ticksExisted.getValue().intValue() && !isMineIgnoreActive()) || endCrystalEntity.isRemoved() || this.playerEntity == null) {
            return false;
        }
        return minecraftClient.player.getEyePosition().distanceTo(endCrystalEntity.position()) <= ((double) (minecraftClient.player.hasLineOfSight((Entity) endCrystalEntity) ? this.range2.getValue() : this.wallRange2.getValue()).floatValue());
    }

    public void rotateToTarget(Vec3 vec3d) {
        if (vec3d == null) {
            return;
        }
        AntiCheat antiCheat_lcl_rot = getAntiCheat();
        if (RotationManager.is724() && antiCheat_lcl_rot != null && antiCheat_lcl_rot.rotations.getValue() == CrystalRotationMode.SILENT && !isOnCooldown(100L)) {
            float[] floatArray2484 = RotationManager.getRotationsTo(vec3d);
            ClientServices.RotationManager.applyRotations(floatArray2484);
            this.floatArr = floatArray2484;
        }
        this.stopwatch2.reset();
        this.atomicReference2.set(vec3d);
    }

    public void sendFactorAttacks(int i) {
        if (this.factor.isAtMinimum()) {
            return;
        }
        AntiCheat antiCheat_lcl_factor = getAntiCheat();
        for (int i2 = 1; i2 < this.factor.getValue().intValue(); i2++) {
            int i3 = i + i2;
            if (antiCheat_lcl_factor != null && antiCheat_lcl_factor.movementSync.getValue().booleanValue()) {
                i3--;
            }
            for (Entity entity : minecraftClient.level.entitiesForRendering()) {
                if (((entity instanceof LivingEntity) && entity.isRemoved()) || !entity.isAlive()) {
                    return;
                }
            }
            if (this.autoCrystalData_3 != null && this.autoCrystalData_3.getEntityType() != EntityTypes.END_CRYSTAL && this.autoCrystalData_3.getEntityId() == i3) {
                return;
            }
            PacketSender.sendInteractEntity(i3);
            PacketSender.sendHandSwing(InteractionHand.MAIN_HAND);
            this.list.add(Integer.valueOf(i3));
        }
    }

    public boolean isAttackOnCooldown() {
        return isOnCooldown(500L);
    }

    public boolean isOnCooldown(long j) {
        if (ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(200L)) {
            return !this.stopwatch8.hasElapsedMs((long) (((float) j) / ClientServices.holeSnapRenderHelper.getTickRateFactor()));
        }
        return true;
    }

    public boolean is929() {
        if (isNotInGame()) {
            return true;
        }
        if (BlockPlacer.isPlaceOnCooldown()) {
            return true;
        }
        if (Offhand.is929()) {
            return true;
        }
        if (SpeedMine.isSwapLocked()) {
            return true;
        }
        if (isTargetInvalid(this.playerEntity)) {
            return true;
        }
        if (minecraftClient.player.isSleeping()) {
            return true;
        }
        if (minecraftClient.player.isUsingItem() && this.eating.getValue().booleanValue()) {
            return true;
        }
        SpeedMine speedmine_lcl_929 = getSpeedmine();
        AnchorAura anchorAura_lcl = getAnchorAura();
        if ((minecraftClient.gameMode.isDestroying() || (speedmine_lcl_929 != null && speedmine_lcl_929.isToggled() && speedmine_lcl_929.getMineTargetPos() != null && WorldUtils.isBreakable(speedmine_lcl_929.getMineTargetPos()))) && this.mining.getValue().booleanValue()) {
            return true;
        }
        if (this.forceSuicide.getValue().booleanValue()) {
            return false;
        }
        if (anchorAura_lcl == null || !anchorAura_lcl.isToggled() || anchorAura_lcl.atomicReference.get() == null || anchorAura_lcl.autoCrystalData_2 == null || this.atomicReference.get() == null || anchorAura_lcl.autoCrystalData_2.getDamage() <= this.atomicReference.get().getDamage()) {
            if (EntityUtil.getPlayerHealth() < this.health.getValue().floatValue() && !this.forceSuicide.getValue().booleanValue()) {
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean isSwapOnCooldown() {
        return !this.stopwatch6.hasElapsedMs((long) (50.0f * this.swapPenalty.getValue().floatValue()));
    }

    public boolean isAutoSwapOnCooldown() {
        if (this.autoSwap.getValue() == AutoCrystalMode_2.NORMAL) {
            if (!this.stopwatch5.hasElapsedMs((long) (50.0f * this.swapDelay.getValue().floatValue()))) {
                return true;
            }
        }
        return false;
    }

    public boolean is1144() {
        return this.autoSwap.getValue() == AutoCrystalMode_2.SILENT;
    }

    public AABB getTargetBox(Player playerEntity) {
        if (!this.extrapolation.getValue().booleanValue()) {
            return BoxMath.getServerBox((LivingEntity) playerEntity);
        }
        return ClientServices.mainhandHelper.getPredictedBox(playerEntity, BoxMath.getSnapDivisions(playerEntity, this.ticks));
    }

    public double calcExplosionDamage(BlockPos blockPos, LivingEntity livingEntity, AABB box, boolean z) {
        SpeedMine speedmine_lcl_calc = getSpeedmine();
        return ExplosionDamageCalculator.getExplosionDamage(Vec3.atCenterOf(blockPos).add(0.0d, 0.5d, 0.0d), livingEntity, box, 6.0d, true, (isMineIgnoreActive() && speedmine_lcl_calc != null) ? speedmine_lcl_calc.getBreakingPos() : null, z ? blockPos.immutable() : null);
    }

    public CrystalAttackTarget findBestTarget(BlockPos blockPos, boolean z) {
        CrystalAttackTarget autoCrystalData_4 = null;
        Iterator it = new ArrayList(minecraftClient.level.players()).iterator();
        while (it.hasNext()) {
            LivingEntity livingEntity = (AbstractClientPlayer) it.next();
            if (!isTargetInvalid(livingEntity) && (blockPos.getY() <= ((AbstractClientPlayer) livingEntity).getY() + 1.5d || z)) {
                AABB box1145 = getTargetBox((Player) livingEntity);
                double d = AutoCrystalMode_3.PLACE.getMinDamageThreshold((Player) livingEntity);
                double d2 = calcExplosionDamage(blockPos, livingEntity, box1145, true);
                DamageOutcome autoCrystalMode_71882 = DamageOutcome.evaluate(livingEntity, d2);
                if (!this.ignoreNakeds.getValue().booleanValue() || PlayerUtils.hasArmor(livingEntity)) {
                    if (BoxMath.getBottomCenter(box1145).distanceTo(Vec3.atBottomCenterOf( blockPos)) <= this.crystalRange.getValue().floatValue() || autoCrystalMode_71882.flag || this.flag3) {
                        if (!ClientServices.socialManager.isFriendEntity((Player) livingEntity) || livingEntity == minecraftClient.player) {
                            if ((d <= d2 && d2 >= MathConstants.val2) || autoCrystalMode_71882.flag) {
                                if (autoCrystalData_4 == null || d2 > autoCrystalData_4.getDamage()) {
                                    autoCrystalData_4 = new CrystalAttackTarget((Player) livingEntity, d2, AutoCrystalDataMode.NONE, autoCrystalMode_71882);
                                }
                            }
                        } else if (this.antiFriendKill.getValue().booleanValue() && d2 > EntityUtil.getEntityHealth((Entity) livingEntity)) {
                            return null;
                        }
                    }
                }
            }
        }
        return autoCrystalData_4;
    }

    public void do1148() {
        if (isTargetInvalid(this.playerEntity)) {
            this.playerEntity = null;
        }
    }

    public boolean isTargetInvalid(LivingEntity livingEntity) {
        if (livingEntity == null) {
            return true;
        }
        return this.forceSuicide.getValue().booleanValue() ? livingEntity != minecraftClient.player : ClientServices.socialManager.isFriendEntity((Player) livingEntity) || !livingEntity.isAlive() || livingEntity.isRemoved() || livingEntity.distanceTo(minecraftClient.player) > this.targetRange.getValue().floatValue() || ((DuckLivingEntityServerDead) livingEntity).isServerSideDead() || minecraftClient.player == livingEntity || livingEntity.isSpectator() || ((Player) livingEntity).getAbilities().instabuild;
    }

    public boolean shouldSwapBack() {
        return this.autoSwap.getValue() == AutoCrystalMode_2.NORMAL && this.swapBack.getValue().booleanValue();
    }

    public boolean isBasePlaceNeeded() {
        if (this.blockPos3 != null && this.playerEntity != null) {
            if (!BlockPlacer.isCrystalPlaceable(this.blockPos3, this.f112.getValue().booleanValue(), true, false, true, this.lowerHitBox.getValue().booleanValue(), this.hBFix.getValue().booleanValue())) {
                return true;
            }
            boolean isAnyPositionVisible = WorldUtils.isAnyPositionVisible(this.multipoint.getValue().getWorldCorners(this.blockPos3));
            double distanceTo = minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(this.blockPos3));
            if (distanceTo > this.range.getValue().floatValue()) {
                return true;
            }
            if (!isAnyPositionVisible && distanceTo > this.wallRange.getValue().floatValue() && !this.smartTrace.getValue().booleanValue()) {
                return true;
            }
            Vec3 ofCenter = Vec3.atCenterOf(this.blockPos3).add(0.0d, 0.5d, 0.0d);
            LivingEntity livingEntity = this.playerEntity;
            if (ExplosionDamageCalculator.getExplosionDamage(ofCenter, livingEntity, getTargetBox(this.playerEntity), 6.0d, true, (BlockPos) null, (BlockPos) null) >= AutoCrystalMode_3.PLACE.getMinDamageThreshold(this.playerEntity)) {
                return false;
            }
        }
        return this.stopwatch9.hasElapsedMs(getBasePlaceDelay());
    }

    public void restoreSlot() {
        if (isNotInGame() || !shouldSwapBack() || this.num4 == -1) {
            return;
        }
        InventoryUtils.selectSlot(this.num4);
        this.num4 = -1;
    }

    public long getBasePlaceDelay() {
        return (long) (500.0f / ClientServices.holeSnapRenderHelper.getTickRateFactor());
    }

    public float getInvincibilityTimeout() {
        return !this.assumeInvincibility.getValue().booleanValue() ? (-1.0f) : this.timeout.getValue().floatValue();
    }

    public boolean isPlayerInSpawnGrace(Player playerEntity) {
        if (isSelfInSpawnGracePeriod()) {
            return false;
        }
        DuckPlayerListEntry playerListEntry = (DuckPlayerListEntry)(minecraftClient.player.connection.getPlayerInfo(playerEntity.getGameProfile().id()));
        return playerListEntry != null && ((float) (System.currentTimeMillis() - playerListEntry.mio$getJoinTime())) <= getInvincibilityTimeout() * 1000.0f;
    }

    public boolean isSelfInSpawnGracePeriod() {
        return this.self.getValue().booleanValue() && ((float) minecraftClient.player.tickCount) / 20.0f < getInvincibilityTimeout();
    }

    public boolean isMineIgnoreActive() {
        SpeedMine speedmine_lcl = getSpeedmine();
        if (speedmine_lcl == null) {
            return false;
        }
        return this.mineIgnore.getValue().booleanValue() && ((((((double) speedmine_lcl.getMiningProgress()) + (speedmine_lcl.getCurrentMiningSpeed() * ((double) 2.5f))) > ((double) speedmine_lcl.damage.getValue().floatValue()) ? 1 : ((((double) speedmine_lcl.getMiningProgress()) + (speedmine_lcl.getCurrentMiningSpeed() * ((double) 2.5f))) == ((double) speedmine_lcl.damage.getValue().floatValue()) ? 0 : -1)) >= 0 && speedmine_lcl.getBreakingPos() != null && WorldUtils.isBreakable(speedmine_lcl.getBreakingPos())) || !speedmine_lcl.hasCooldownElapsed(100L));
    }

    public boolean shouldAwaitBeforeBreak() {
        return this.await.getValue().booleanValue();
    }

    public void incrementSwapCounter() {
        this.num3++;
    }

    public CrystalPlacement getAutoCrystalData1160(Throwable th, String str) {
        // exceptionally() handler for the async placement search. This used to swallow the
        // throwable and return null, which left atomicReference null forever and silently
        // disabled placement. Always report so a broken search thread is visible.
        System.err.println("[mio] AutoCrystal search failed in " + str);
        th.printStackTrace();
        return null;
    }

    public boolean isHoldingShulker() {
        boolean isHolding = minecraftClient.player.isHolding(itemStack -> {
            return ItemTypeUtils.isShulkerBox(itemStack.getItem());
        });
        if (!isHolding && this.stopwatch.hasElapsedMs(50L)) {
            return false;
        }
        if (isHolding) {
            this.stopwatch.reset();
        }
        if (this.completableFuture == null || this.completableFuture.isDone()) {
            return true;
        }
        cancel();
        return true;
    }

    public void cancel() {
        CompletableFuture<CrystalPlacement> completableFuture = this.completableFuture;
        if (completableFuture == null || completableFuture.isDone() || completableFuture.isCancelled()) {
            return;
        }
        completableFuture.complete(null);
    }
}