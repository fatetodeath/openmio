package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.player.RotationRequest;
import me.mioclient.util.registry.ModeValueMap;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.movement.elytrafly.BoostElytraFlight;
import me.mioclient.module.movement.elytrafly.BounceElytraFlight;
import me.mioclient.module.movement.elytrafly.ControlElytraFlight;
import me.mioclient.module.movement.elytrafly.ElytraFlyModeBase;
import me.mioclient.module.movement.elytrafly.GlideElytraFlight;
import me.mioclient.module.movement.elytrafly.PacketElytraFlight;
import me.mioclient.network.PacketSender;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.InteractItemEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.sound.PlayEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.mixin.ducks.DuckMinecraftClient;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.misc.MiddleClick;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class ElytraFly extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static MiddleClick middleClick;
    private static ObstaclePasser obstaclePasser;
    private static NoSlow noSlow;
    private static AntiCheat antiCheat;

    public static MiddleClick getMiddleClick() {
        if (middleClick == null && ClientServices.moduleInjector != null) {
            try {
                middleClick = (MiddleClick) ClientServices.moduleInjector.getModule(MiddleClick.class);
            } catch (Exception e) {
            }
        }
        return middleClick;
    }

    public static ObstaclePasser getObstaclePasser() {
        if (obstaclePasser == null && ClientServices.moduleInjector != null) {
            try {
                obstaclePasser = (ObstaclePasser) ClientServices.moduleInjector.getModule(ObstaclePasser.class);
            } catch (Exception e) {
            }
        }
        return obstaclePasser;
    }

    public static NoSlow getNoSlow() {
        if (noSlow == null && ClientServices.moduleInjector != null) {
            try {
                noSlow = (NoSlow) ClientServices.moduleInjector.getModule(NoSlow.class);
            } catch (Exception e) {
            }
        }
        return noSlow;
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
    public static boolean flag = false;
    public Setting<ElytraFlyPredicateMode> mode = add(new EnumSetting<>("Mode", ElytraFlyPredicateMode.CONTROL));
    public Setting<ElytraFlyMode_2> takeoff = add(new EnumSetting<>("Takeoff", ElytraFlyMode_2.PLAIN, v -> this.mode.getValue() != ElytraFly.ElytraFlyPredicateMode.PACKET));
    public Setting<Boolean> factorize = add(new BooleanSetting("Factorize", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST));
    public Setting<Boolean> always = add(new BooleanSetting("Always", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST));
    public Setting<Boolean> nCPBoost = add(new BooleanSetting("NCPBoost", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.PACKET || this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL));
    public Setting<Float> minBoost = add(new NumberSetting<>("MinBoost", 1.5f, 0.0f, 5.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST).setHoleSnapConfig("GrimV3", HoleSnapMode.MAX));
    public Setting<Integer> verticalBoost = add(new NumberSetting<>("VerticalBoost", 1, 1, 7));
    public Setting<Float> limit = add(new NumberSetting<>("Limit", 500.0f, 20.0f, 500.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST).setSuffix("km/h").setHoleSnapConfig("Unlimited", HoleSnapMode.MAX));
    public Setting<Boolean> autoJump = add(new BooleanSetting("AutoJump", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST));
    public Setting<Boolean> inLava = add(new BooleanSetting("InLava", true, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST).setVisible());
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.0f, 2.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST && this.inLava.isShown()));
    public Setting<Boolean> autoBoost = add(new BooleanSetting("AutoBoost", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST).setVisible());
    public Setting<Integer> minY = add(new NumberSetting<>("MinY", 100, 0, 500, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST && this.autoBoost.isShown()));
    public Setting<Integer> add = add(new NumberSetting<>("Add", 20, 15, 50, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST && this.autoBoost.isShown()));
    public Setting<Float> boostPitch = add(new NumberSetting<>("BoostPitch", 40.0f, 0.0f, 90.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST && this.autoBoost.isShown()));
    public Setting<Float> speed3 = add(new NumberSetting<>("Speed", 1.6f, 0.1f, 10.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL));
    public Setting<ElytraFlyMode> vertical = add(new EnumSetting<>("Vertical", ElytraFlyMode.NONE, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL || this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.PACKET));
    public Setting<Float> vSpeed = add(new NumberSetting<>("VSpeed", 0.6f, 0.0f, 15.0f, v -> this.vertical.getValue() == ElytraFly.ElytraFlyMode.PLAIN && this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL));
    public Setting<Float> vPitch = add(new NumberSetting<>("VPitch", 45.0f, 0.0f, 90.0f, v -> this.vertical.getValue() == ElytraFly.ElytraFlyMode.STRICT && this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL));
    public Setting<Boolean> antiKick = add(new BooleanSetting("AntiKick", true, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL));
    public Setting<Float> glide = add(new NumberSetting<>("Glide", 0.0f, 0.0f, 5.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL || this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.PACKET));
    public Setting<Boolean> spoofPitch = add(new BooleanSetting("SpoofPitch", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL).setVisible());
    public Setting<Float> pitch = add(new NumberSetting<>("Pitch", 0.5f, -90.0f, 90.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.CONTROL && this.spoofPitch.isShown()));
    public Setting<Boolean> antiAfk = add(new BooleanSetting("AntiAfk", true, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.PACKET));
    public Setting<Boolean> vertical2 = add(new BooleanSetting("Vertical", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.PACKET));
    public Setting<Boolean> accelerate = add(new BooleanSetting("Accelerate", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST).setVisible());
    public Setting<Float> verAccelMin = add(new NumberSetting<>("VerAccelMin", 1.0f, 0.0f, 5.0f));
    public Setting<Float> accelMin = add(new NumberSetting<>("AccelMin", 1.0f, 0.0f, 5.0f));
    public Setting<Float> accelTime = add(new NumberSetting<>("AccelTime", 1.0f, 0.1f, 10.0f).setSuffix("s"));
    public Setting<Float> start = add(new NumberSetting<>("Start", 1.8f, 0.0f, 3.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.STRICT));
    public Setting<Float> speed2 = add(new NumberSetting<>("Speed", 1.8f, 0.1f, 3.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.STRICT));
    public Setting<Integer> deployTime = add(new NumberSetting<>("DeployTime", 400, 50, 1000, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.STRICT).setSuffix("ms"));
    public Setting<Boolean> pitchLock = add(new BooleanSetting("PitchLock", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE).setVisible());
    public Setting<Boolean> silent = add(new BooleanSetting("Silent", true, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE && this.pitchLock.isShown()));
    public Setting<Float> pitch2 = add(new NumberSetting<>("Pitch", 90.0f, -90.0f, 90.0f, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE && this.pitchLock.isShown()));
    public Setting<Boolean> compensate = add(new BooleanSetting("Compensate", true, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE));
    public Setting<Boolean> infDurability = add(new BooleanSetting("InfDurability", false));
    public Setting<Boolean> muteElytra = add(new BooleanSetting("MuteElytra", false));
    public Setting<Boolean> grimDurability = add(new BooleanSetting("GrimDurability", false, v -> this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOOST || this.mode.getValue() == ElytraFly.ElytraFlyPredicateMode.BOUNCE));
    public final ModeValueMap<ElytraFlyPredicateMode, ElytraFlyModeBase> elytraFlyHelper_4;
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public final Stopwatch stopwatch3;
    public final Stopwatch stopwatch4;
    public final Stopwatch stopwatch5;
    public boolean flag2;
    public boolean flag3;
    public boolean flag4;

    
    public enum ElytraFlyMode implements NamedValue {
        NONE("None"),
        PLAIN("Plain"),
        MANUAL("Manual"),
        STRICT("Strict");

        public final String name;

        ElytraFlyMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum ElytraFlyMode_2 implements NamedValue {
        NONE("None"),
        PLAIN("Plain"),
        STRICT("Strict");

        public final String name;

        ElytraFlyMode_2(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum ElytraFlyPredicateMode implements NamedValue {
        CONTROL("Control"),
        BOOST("Boost"),
        PACKET("Packet"),
        STRICT("Strict"),
        BOUNCE("Bounce");

        public final String name;

        ElytraFlyPredicateMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public ElytraFly() {
        super("ElytraFly", "Turns you into a block game boeing.", Category.MOVEMENT, "elytraflight");
        this.elytraFlyHelper_4 = new ModeValueMap<>(this.mode);
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.stopwatch3 = new Stopwatch();
        this.stopwatch4 = new Stopwatch();
        this.stopwatch5 = new Stopwatch();
        this.speed.setConfigName("LavaSpeed");
        this.vertical2.setConfigName("VerticalPacket");
        this.speed2.setConfigName("StrictSpeed");
        this.pitch2.setConfigName("BouncePitch");
        this.minBoost.setHoleSnapConfig("GrimV3", HoleSnapMode.MAX);
        this.limit.setHoleSnapConfig("Unlimited", HoleSnapMode.MAX);
        this.verticalBoost.addVisibilityPredicate(num -> {
            return !this.minBoost.isAtMinimum();
        });
        this.infDurability.addVisibilityPredicate(bool -> {
            return !this.grimDurability.getValue().booleanValue();
        });
        this.elytraFlyHelper_4.put(ElytraFlyPredicateMode.BOOST, new BoostElytraFlight(this));
        this.elytraFlyHelper_4.put(ElytraFlyPredicateMode.CONTROL, new ControlElytraFlight(this));
        this.elytraFlyHelper_4.put(ElytraFlyPredicateMode.PACKET, new GlideElytraFlight(this));
        this.elytraFlyHelper_4.put(ElytraFlyPredicateMode.STRICT, new BounceElytraFlight(this));
        this.elytraFlyHelper_4.put(ElytraFlyPredicateMode.BOUNCE, new PacketElytraFlight(this));
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.stopwatch4.setTime(-1L);
        this.flag2 = true;
        if (isNotInGame()) {
            return;
        }
        this.elytraFlyHelper_4.getCurrent().onEnable();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        if (!isNotInGame()) {
            this.elytraFlyHelper_4.getCurrent().onDisable();
        }
        if (!isNotInGame() && this.grimDurability.getValue().booleanValue() && this.grimDurability.isVisible() && !minecraftClient.player.isShiftKeyDown()) {
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; PacketSender.sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), true, _cur.sprint()))); };
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; PacketSender.sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), false, _cur.sprint()))); };
        }
        this.stopwatch.reset();
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        try {
            return ClientTextRenderer.capitalize(this.mode.getValue().getName());
        } catch (Exception e) {
            return null;
        }
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        if (PlayerUtils.isReallyFallFlying()) {
            ElytraFlyModeBase object996 = this.elytraFlyHelper_4.getCurrent();
            if (object996 instanceof BoostElytraFlight) {
                BoostElytraFlight elytraFlyHelper_5 = (BoostElytraFlight) object996;
                if (this.autoBoost.getValue().booleanValue()) {
                    RotationRequest elytraFlyData2475 = ClientServices.RotationManager.getActiveRequest();
                    ClientServices.RotationManager.requestRotations(new float[]{elytraFlyData2475 != null ? elytraFlyData2475.getRotationAngles()[0] : holeSnapEvent.getYaw(), elytraFlyHelper_5.getPitch()}, 999, true);
                }
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (!minecraftClient.player.isFallFlying()) {
            this.flag2 = true;
        }
        this.elytraFlyHelper_4.getCurrent().handleTick(tickEvent);
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (isNotInGame()) {
            return;
        }
        ServerboundPlayerActionPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundPlayerActionPacket ? (ServerboundPlayerActionPacket) (sendImmediatelyEvent.getPacket()) : null;
        if ((packet904 instanceof ServerboundPlayerActionPacket) && packet904.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            this.stopwatch3.reset();
        }
        this.elytraFlyHelper_4.getCurrent().handlePacketSend(sendImmediatelyEvent);
    }

    @Listen
    public void onInteractItem(InteractItemEvent interactItemEvent) {
        if (this.flag4) {
            return;
        }
        if (minecraftClient.player.getItemInHand(interactItemEvent.getHand()).is(Items.FIREWORK_ROCKET)) {
            this.flag3 = true;
            interactItemEvent.cancel();
        }
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        int i = InventoryUtils.findInHotbar(Items.ELYTRA);
        boolean z = i == -1;
        if (i == -1) {
            i = InventoryUtils.findSlot(Items.ELYTRA);
        }
        boolean shouldSwapElytra = shouldSwapElytra();
        if (this.stopwatch3.hasElapsedMs(200L)) {
            AntiCheat antiCheat_lcl = getAntiCheat();
            if (antiCheat_lcl != null && antiCheat_lcl.isF2b2t() && z) {
                return;
            }
            ObstaclePasser obstaclePasser_lcl = getObstaclePasser();
            if (obstaclePasser_lcl == null || !obstaclePasser_lcl.is929()) {
                if (shouldSwapElytra) {
                    if (PlayerUtils.hasElytraEquipped(minecraftClient.player)) {
                        minecraftClient.gameMode.handleContainerInput(0, 6, 0, ContainerInput.PICKUP, minecraftClient.player);
                        minecraftClient.gameMode.handleContainerInput(0, 6, 0, ContainerInput.PICKUP, minecraftClient.player);
                    } else if (i != -1) {
                        int i2 = i;
                        if (z) {
                            i = 0;
                            minecraftClient.gameMode.handleContainerInput(0, i2, 0, ContainerInput.SWAP, minecraftClient.player);
                        }
                        minecraftClient.gameMode.handleContainerInput(0, 6, i, ContainerInput.SWAP, minecraftClient.player);
                        sendStartFlightIfReady();
                        if (this.flag3 && this.stopwatch.hasElapsedAndResetMs(150L)) {
                            this.flag4 = true;
                            MiddleClick middleClick_lcl = getMiddleClick();
                            if (middleClick_lcl != null && middleClick_lcl.isToggled()) {
                                middleClick_lcl.useFireworkRocket();
                            } else {
                                ((DuckMinecraftClient) minecraftClient).interact();
                            }
                            this.flag4 = false;
                            this.flag3 = false;
                            this.stopwatch3.reset();
                        }
                        minecraftClient.gameMode.handleContainerInput(0, 6, i, ContainerInput.SWAP, minecraftClient.player);
                        if (z) {
                            minecraftClient.gameMode.handleContainerInput(0, i2, 0, ContainerInput.SWAP, minecraftClient.player);
                        }
                    }
                }
                this.elytraFlyHelper_4.getCurrent().handleTickPost(tickPostEvent);
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundAddEntityPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundAddEntityPacket ? (ClientboundAddEntityPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket entitySpawnS2CPacket = packet904;
            if (entitySpawnS2CPacket.getType() == EntityTypes.FIREWORK_ROCKET) {
                if (new Vec3(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ()).distanceToSqr(minecraftClient.player.position()) <= 36.0d) {
                    this.stopwatch3.reset();
                }
            }
        }
        this.elytraFlyHelper_4.getCurrent().handlePacketReceive(channelRead0Event);
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        this.elytraFlyHelper_4.getCurrent().handleMove(moveEvent);
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        this.elytraFlyHelper_4.getCurrent().handleMotion(motionEvent);
    }

    @Listen
    public void onRotation(RotationEvent event_3) {
        this.elytraFlyHelper_4.getCurrent().handleRotation(event_3);
    }

    @Listen
    public void onInputTick(InputTickEvent tickEvent_2) {
        this.elytraFlyHelper_4.getCurrent().handleInputTick(tickEvent_2);
    }

    @Listen
    public void onPlay(PlayEvent playEvent) {
        if (!shouldSwapElytra() || playEvent.getSoundInstance() == null || playEvent.getSoundInstance().getIdentifier() == null || !playEvent.getSoundInstance().getIdentifier().toString().contains("item.armor.equip")) {
            return;
        }
        playEvent.cancel();
    }

    public void sendStartFlightIfReady() {
        if (!this.stopwatch4.hasElapsedMs(500L) || this.grimDurability.isVisible()) {
            PacketSender.sendStartFallFlying();
            minecraftClient.player.startFallFlying();
        }
    }

    public void sendStartFallFlying() {
        if (!hasElytra() || minecraftClient.player.isFallFlying() || minecraftClient.player.onGround() || this.takeoff.getValue() == ElytraFlyMode_2.NONE) {
            ClientServices.holeSnapHelper.releaseTickRate(this);
            return;
        }
        if (this.takeoff.getValue() == ElytraFlyMode_2.STRICT) {
            ClientServices.holeSnapHelper.requestTickRate(this, 0.1f);
        }
        long j = 100;
        if (this.mode.getValue() == ElytraFlyPredicateMode.STRICT) {
            j = this.deployTime.getValue().intValue();
        }
        if (this.stopwatch2.hasElapsedMs(j)) {
            minecraftClient.player.startFallFlying();
            PacketSender.sendStartFallFlying();
            this.stopwatch2.reset();
        }
    }

    public boolean hasElytra() {
        return PlayerUtils.hasElytraEquipped(minecraftClient.player);
    }

    public boolean isPacketFlyActive() {
        if (isNotInGame() || minecraftClient.player.input == null) {
            return false;
        }
        return !(this.vertical2.getValue().booleanValue() && minecraftClient.player.input.keyPresses.jump()) && isToggled() && !minecraftClient.player.onGround() && this.mode.getValue() == ElytraFlyPredicateMode.PACKET && hasElytra();
    }

    public boolean tryStartFallFlying() {
        if (this.grimDurability.getValue().booleanValue() || ClientServices.obstaclePasserHelper.isPathing()) {
            return false;
        }
        ObstaclePasser obstaclePasser_lcl = getObstaclePasser();
        if ((obstaclePasser_lcl != null && obstaclePasser_lcl.is929()) || !is951()) {
            return false;
        }
        minecraftClient.player.startFallFlying();
        return true;
    }

    public boolean is951() {
        if (ClientServices.obstaclePasserHelper.isPathing()) {
            return false;
        }
        ObstaclePasser obstaclePasser_lcl = getObstaclePasser();
        if ((obstaclePasser_lcl == null || !obstaclePasser_lcl.is929()) && !minecraftClient.player.getAbilities().flying && !minecraftClient.player.isPassenger() && !minecraftClient.player.onClimbable() && hasElytra() && !minecraftClient.player.isInWater()) {
            if (!minecraftClient.player.hasEffect(MobEffects.LEVITATION) && minecraftClient.player.input.keyPresses.jump()) {
                return true;
            }
        }
        return false;
    }

    public boolean isUsingItemBlocked() {
        AntiCheat antiCheat_lcl = getAntiCheat();
        NoSlow noSlow_lcl = getNoSlow();
        boolean antiCheatF2b2t = antiCheat_lcl != null && antiCheat_lcl.isF2b2t();
        boolean noSlowActive = noSlow_lcl != null && noSlow_lcl.isToggled() && noSlow_lcl.items.getValue().booleanValue();
        return (antiCheatF2b2t || !minecraftClient.player.isUsingItem() || noSlowActive) ? false : true;
    }

    public boolean isManualDescending() {
        return isManualVerticalActive() && minecraftClient.player.getXRot() > 0.0f;
    }

    public boolean isManualVerticalActive() {
        if (isNotInGame()) {
            return false;
        }
        return (minecraftClient.player.input.keyPresses.jump() || minecraftClient.player.input.keyPresses.shift()) && this.vertical.getValue() == ElytraFlyMode.MANUAL && minecraftClient.player.isFallFlying() && hasElytra();
    }

    public boolean isReallyFallFlying() {
        if (flag) {
            return minecraftClient.player.isFallFlying();
        }
        flag = true;
        boolean isFallFlying = minecraftClient.player.isFallFlying();
        flag = false;
        return isFallFlying;
    }

    public boolean shouldSwapElytra() {
        if (ClientServices.obstaclePasserHelper.isPathing() || isNotInGame() || flag) {
            return false;
        }
        int i = InventoryUtils.findInHotbar(Items.ELYTRA);
        if (i == -1) {
            i = InventoryUtils.findSlot(Items.ELYTRA);
        }
        boolean z = i != -1;
        if (z) {
            this.stopwatch5.reset();
        }
        boolean z2 = z | (!this.stopwatch5.hasElapsedMs(150L));
        if (PlayerUtils.hasElytraEquipped(minecraftClient.player)) {
            z2 = true;
        }
        if (z2) {
            return isToggled() && (this.grimDurability.getValue().booleanValue() && this.grimDurability.isVisible());
        }
        return false;
    }
}