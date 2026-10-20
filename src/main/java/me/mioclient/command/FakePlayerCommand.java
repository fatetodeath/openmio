package me.mioclient.command;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.client.InteractPacketReader;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.player.Freecam;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Explosion;

public final class FakePlayerCommand extends CommandFeature {
    public static Freecam freecam = (Freecam) ClientServices.moduleInjector.getModule(Freecam.class);
    public final List<Record> list;
    public RemotePlayer otherClientPlayerEntity;
    public boolean flag;
    public boolean flag2;
    public String string;
    public int current;

    
    public static class RemotePlayer extends net.minecraft.client.player.RemotePlayer {
        public RemotePlayer(Player playerEntity, ClientLevel clientWorld, GameProfile gameProfile) {
            super(clientWorld, gameProfile);
            copyPosition(playerEntity);
            this.yRotO = getYRot();
            this.xRotO = getXRot();
            this.yHeadRot = playerEntity.getYHeadRot();
            this.yHeadRotO = this.yHeadRot;
            this.yBodyRot = playerEntity.yBodyRot;
            this.yBodyRotO = this.yBodyRot;
            setPose(playerEntity.getPose());
            setHealth(20.0f);
            getInventory().replaceWith(playerEntity.getInventory());
        }

        public void setHealth(float f) {
            super.setHealth(f);
            if (getHealth() <= 0.0f) {
                super.setHealth(9.0f);
                removeAllEffects();
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                ClientAccessor.minecraftClient.particleEngine.createTrackingEmitter(this, ParticleTypes.TOTEM_OF_UNDYING, 30);
                ClientLevel clientWorld = ClientAccessor.minecraftClient.level;
                Player playerEntity = ClientAccessor.minecraftClient.player;
                BlockPos blockPos = blockPosition();
                SoundEvent soundEvent = SoundEvents.TOTEM_USE;
                clientWorld.playSound(playerEntity, blockPos, soundEvent, SoundSource.PLAYERS, 1.0f, 1.0f);
                ClientAccessor.eventBus.fireEvent(new PacketReceiveEvent(new ClientboundEntityEventPacket(this, (byte) 35)));
            }
        }

        public void tick() {
            this.tickCount++;
            if (this.tickCount % 80 == 0) {
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 1));
                addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 6000, 0));
                addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0));
                addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3));
                setAbsorptionAmount(16.0f);
            }
            if (getOffhandItem().getItem() != Items.TOTEM_OF_UNDYING) {
                getInventory().setItem(40, new ItemStack(Items.TOTEM_OF_UNDYING, 1));
            }
            super.tick();
        }

        public boolean isImmuneToExplosion(Explosion explosion) {
            return false;
        }

        public boolean isAttackable() {
            return true;
        }

        public boolean isFireImmune() {
            return true;
        }

        public boolean isPushable() {
            return false;
        }

        public boolean isAlive() {
            return true;
        }

        public void updatePostDeath() {
            this.deathTime = 0;
        }

        public void pushAway(Entity entity) {
        }
    }

    
    
    public static final class Record {
        public final Vec3 vec3d;
        public final float val;
        public final float val2;
        public final float val3;
        public final Vec3 vec3d2;
        public final boolean flag;
        public final boolean flag2;

        public Record(Vec3 vec3d, float f, float f2, float f3, Vec3 vec3d2, boolean z, boolean z2) {
            this.vec3d = vec3d;
            this.val = f;
            this.val2 = f2;
            this.val3 = f3;
            this.vec3d2 = vec3d2;
            this.flag = z;
            this.flag2 = z2;
        }

        public Vec3 getPosition() {
            return this.vec3d;
        }

        public float getYaw() {
            return this.val;
        }

        public float getPitch() {
            return this.val2;
        }

        public float getHeadYaw() {
            return this.val3;
        }

        public Vec3 getTargetPos() {
            return this.vec3d2;
        }

        public boolean isSneaking() {
            return this.flag;
        }

        public boolean isOnGround() {
            return this.flag2;
        }
    }

    public FakePlayerCommand() {
        super("fakeplayer");
        this.list = new ArrayList();
        eventBus.register(this);
    }

    @Override // me.mioclient.command.CommandFeature
    public void exec(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder) {
        literalArgumentBuilder.then(CommandFeature.argument("name", StringArgumentType.word()).executes(commandContext -> {
            spawnOrRemoveFakePlayer((String) commandContext.getArgument("name", String.class));
            return 1;
        })).then(CommandFeature.literal("record").executes(commandContext2 -> {
            this.flag = !this.flag;
            if (!this.flag) {
                ChatUtils.sendMessage(Component.literal("Stopped recording."), ChatUtils.createSignature(-1));
                return 1;
            }
            this.list.clear();
            ChatUtils.sendMessage(Component.literal("Started recording."), ChatUtils.createSignature(-1));
            return 1;
        })).then(CommandFeature.literal("play").executes(commandContext3 -> {
            this.current = 0;
            this.flag2 = !this.flag2;
            return 1;
        })).executes(commandContext4 -> {
            spawnOrRemoveFakePlayer("Herobrine");
            return 1;
        });
    }

    public void spawnOrRemoveFakePlayer(String str) {
        this.list.clear();
        this.flag = false;
        this.flag2 = false;
        if (this.otherClientPlayerEntity != null) {
            minecraftClient.level.removeEntity(-9344, Entity.RemovalReason.DISCARDED);
            this.otherClientPlayerEntity = null;
            ChatUtils.sendMessage(Component.literal(new StringTemplate().arg(this.string == null ? str : this.string).format("\u0001 has been removed.")), ChatUtils.createSignature(-1));
            return;
        }
        this.otherClientPlayerEntity = new RemotePlayer(minecraftClient.player, minecraftClient.level, new GameProfile(UUID.randomUUID(), str));
        if (freecam.isToggled()) {
            this.otherClientPlayerEntity.absSnapTo(freecam.vec3d.x, freecam.vec3d.y, freecam.vec3d.z, minecraftClient.player.getYRot(), minecraftClient.player.getXRot());
        }
        this.otherClientPlayerEntity.setId(-9344);
        minecraftClient.level.addEntity(this.otherClientPlayerEntity);
        this.otherClientPlayerEntity.setOnGround(minecraftClient.player.onGround());
        this.string = str;
        ChatUtils.sendMessage(Component.literal(new StringTemplate().arg(str).format("\u0001 has been spawned.")), ChatUtils.createSignature(-1));
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post && this.flag) {
            this.list.add(new Record(minecraftClient.player.position(), minecraftClient.player.getYRot(), minecraftClient.player.getXRot(), minecraftClient.player.getYHeadRot(), minecraftClient.player.getDeltaMovement(), minecraftClient.player.isCrouching(), minecraftClient.player.onGround()));
            return;
        }
        if (motionEvent.getPhase() != KeyPearlMode.Pre || this.otherClientPlayerEntity == null || !this.flag2 || this.list.isEmpty()) {
            return;
        }
        this.current = (this.current + 1) % this.list.size();
        Record record = this.list.get(this.current);
        this.otherClientPlayerEntity.absSnapTo(record.vec3d.x, record.vec3d.y, record.vec3d.z, record.val, record.val2);
        this.otherClientPlayerEntity.setDeltaMovement(record.vec3d2);
        this.otherClientPlayerEntity.setYRot(record.val);
        this.otherClientPlayerEntity.setXRot(record.val2);
        this.otherClientPlayerEntity.setYHeadRot(record.val3);
        this.otherClientPlayerEntity.setShiftKeyDown(record.flag);
        this.otherClientPlayerEntity.setOnGround(record.flag2);
        if (this.otherClientPlayerEntity.isCrouching()) {
            this.otherClientPlayerEntity.setPose(Pose.CROUCHING);
        } else {
            this.otherClientPlayerEntity.setPose(Pose.STANDING);
        }
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        this.otherClientPlayerEntity = null;
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (this.otherClientPlayerEntity == null) {
            return;
        }
        if ((sendImmediatelyEvent.getPacket() instanceof ClientboundDisconnectPacket) || (sendImmediatelyEvent.getPacket() instanceof ClientboundPlayerCombatKillPacket)) {
            this.otherClientPlayerEntity = null;
        }
        ServerboundInteractPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundInteractPacket ? (ServerboundInteractPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 != null) {
            // resolvePacketEntity resolves the packet's entity id against the client world and returns
            // null whenever that entity is already gone (common: the crystal we just attacked).
            // JADX dropped the guard, so every attack packet for a dead entity NPE'd out of this
            // listener, which also killed the AutoCrystal break path that sends through here.
            final Entity target = InteractPacketReader.resolvePacketEntity(packet904);
            if (target != null
                    && InteractPacketReader.getCrystalInteractionType(packet904) == InteractPacketReader.LegacyCrystalMode.ATTACK
                    && target.getId() == this.otherClientPlayerEntity.getId()) {
                minecraftClient.execute(() -> {
                    this.otherClientPlayerEntity.attack(target);
                });
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundLoginPacket) {
            this.otherClientPlayerEntity = null;
        }
        if (this.otherClientPlayerEntity == null) {
            return;
        }
        ClientboundExplodePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundExplodePacket ? (ClientboundExplodePacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundExplodePacket) {
            // 26.2 port: ClientboundExplodePacket is a record (center()/radius()); fake-player
            // explosion damage application removed (was Yarn Explosion.getExposure/damage path).
            // Keep the event consumed so future damage hooks can attach here.
            ClientboundExplodePacket explosionPacket = packet904;
            Vec3 center = explosionPacket.center();
            double distanceTo = this.otherClientPlayerEntity.position().distanceTo(center) / 12.0d;
            if (distanceTo > 1.0d) {
                return;
            }
        }
    }
}
