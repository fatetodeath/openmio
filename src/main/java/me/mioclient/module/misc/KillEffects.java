package me.mioclient.module.misc;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.setting.SoundIdentifierSetting;
import me.mioclient.util.client.SoundIdentifier;
import me.mioclient.util.client.SoundPlayer;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.io.FileIO;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.manager.ConfigManager;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.player.AttackHookPostEvent;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.IRC;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import org.apache.commons.io.FileUtils;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.setting.StringSetting;

public class KillEffects extends Module {
    public Setting<Boolean> killSound = add(new BooleanSetting("KillSound", false).setVisible());
    public Setting<Boolean> players = add(new BooleanSetting("Players", false, v -> this.killSound.isShown()).setVisible());
    public Setting<SoundIdentifier> sound = add(new SoundIdentifierSetting("Sound", new SoundIdentifier("system", "css"), v -> this.killSound.isShown() && this.players.isShown()));
    public Setting<Float> volume = add(new NumberSetting<>("Volume", 0.5f, 0.0f, 1.0f, v -> this.killSound.isShown() && this.players.isShown()));
    public Setting<Boolean> self = add(new BooleanSetting("Self", false, v -> this.killSound.isShown()).setVisible());
    public Setting<SoundIdentifier> sound2 = add(new SoundIdentifierSetting("Sound", new SoundIdentifier("system", "bodysplat"), v -> this.killSound.isShown() && this.self.isShown()));
    public Setting<Float> volume3 = add(new NumberSetting<>("Volume", 0.5f, 0.0f, 1.0f, v -> this.killSound.isShown() && this.self.isShown()));
    public Setting<Boolean> killStreak = add(new BooleanSetting("KillStreak", false).setVisible());
    public Setting<Float> volume2 = add(new NumberSetting<>("Volume", 0.4f, 0.0f, 1.0f, v -> this.killStreak.isShown()));
    public Setting<Boolean> thunder = add(new BooleanSetting("Thunder", true).setVisible());
    public Setting<Integer> lightnings = add(new NumberSetting<>("Lightnings", 1, 1, 4).requiresSettings(this.thunder));
    public Setting<Boolean> self2 = add(new BooleanSetting("Self", false, v -> this.thunder.isShown()));
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", true).requiresSettings(this.thunder));
    public Setting<Boolean> ashes = add(new BooleanSetting("Ashes", false).setVisible());
    public Setting<Boolean> self3 = add(new BooleanSetting("Self", false).requiresSettings(this.ashes));
    public Setting<Boolean> autoEZ = add(new BooleanSetting("AutoEZ", false).setVisible());
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 0.0f, 0.0f, 5.0f).setSuffix("s").requiresSettings(this.autoEZ));
    public Setting<String> path = add(new StringSetting("Path", "autoez.txt").requiresSettings(this.autoEZ));
    public Setting<Boolean> random = add(new BooleanSetting("Random", true).requiresSettings(this.autoEZ));
    public Setting<Boolean> refresh = add(new BooleanSetting("Refresh", false).requiresSettings(this.autoEZ));
    public static IRC iRC = (IRC) ClientServices.moduleInjector.getModule(IRC.class);
    public static final RandomSource random2 = RandomSource.create();
    public final ConcurrentHashMap<UUID, Integer> concurrentHashMap;
    public final ObjectList<UUID> objectList;
    public int num;
    public int num2;
    public List<String> list;

    public KillEffects() {
        super("KillEffects", "Does various things when someone dies.", Category.MISC, new String[0]);
        this.concurrentHashMap = new ConcurrentHashMap<>();
        this.objectList = ObjectLists.synchronize(new ObjectArrayList());
        this.num = 0;
        this.num2 = 0;
        this.list = Collections.synchronizedList(new ArrayList());
        this.players.setConfigName("KillSoundPlayers");
        this.sound.setConfigName("KillSoundPlayersSound");
        this.volume.setConfigName("KillSoundPlayersVolume");
        this.self.setConfigName("KillSoundSelf");
        this.sound2.setConfigName("KillSoundSelfSound");
        this.volume3.setConfigName("KillSoundSelfVolume");
        this.self2.setConfigName("ThunderSelf");
        this.volume2.setConfigName("KillStreakSoundVolume");
        this.self3.setConfigName("AshesSelf");
        this.autoEZ.setChangeCallback(() -> {
            if (this.autoEZ.getValue().booleanValue()) {
                loadAutoEZLines(false);
            }
        });
        this.refresh.setChangeCallback(() -> {
            if (this.refresh.getValue().booleanValue()) {
                loadAutoEZLines(true);
                this.refresh.setValue(false);
            }
        });
        this.random.setChangeCallback(() -> {
            this.num2 = 0;
        });
        this.path.setChangeCallback(() -> {
            loadAutoEZLines(true);
        });
        this.path.setConfigName("AutoEZPath");
        this.random.setConfigName("AutoEZRandom");
        this.refresh.setConfigName("AutoEZRefresh");
        setDrawn(false);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.num = 0;
        loadAutoEZLines(true);
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        this.num = 0;
    }

    @Listen
    public void onAttackHookPost(AttackHookPostEvent attackHookPostEvent) {
        Player playerEntity;
        if (attackHookPostEvent.getPhase() == KeyPearlMode.Post || isNotInGame()) {
            return;
        }
        Player entity181 = (attackHookPostEvent.getEntity()) instanceof Player ? (Player) (attackHookPostEvent.getEntity()) : null;
        if (!(entity181 instanceof Player) || (playerEntity = entity181) == minecraftClient.player) {
            return;
        }
        if (ClientServices.socialManager.isFriend(playerEntity.getName().getString()) || EntityUtil.getEntityHealth((Entity) playerEntity) <= 0.0f) {
            return;
        }
        try {
            this.concurrentHashMap.put(playerEntity.getUUID(), Integer.valueOf(minecraftClient.player.tickCount));
        } catch (Exception e) {
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
            this.num = -1;
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        Player playerByUuid = null;
        String string3005;
        if (isNotInGame()) {
            return;
        }
        if (this.num == -1) {
            this.num = 0;
            if (this.killSound.getValue().booleanValue() && this.self.getValue().booleanValue() && this.volume3.getValue().floatValue() > 0.0f) {
                SoundPlayer searchHelper4_42970 = ClientServices.killstreakSoundManager.findSoundPlayer(this.sound2.getValue());
                if (searchHelper4_42970 != null) {
                    searchHelper4_42970.play(this.volume3.getValue().floatValue());
                }
            }
            if (this.thunder.getValue().booleanValue() && this.self2.getValue().booleanValue()) {
                spawnLightning(minecraftClient.player.position());
            }
            if (this.ashes.getValue().booleanValue() && this.self3.getValue().booleanValue()) {
                spawnAshParticles(minecraftClient.player.position());
            }
        }
        this.objectList.clear();
        synchronized (this.concurrentHashMap) {
            Iterator it = this.concurrentHashMap.keySet().iterator();
            while (it.hasNext()) {
                UUID uuid = (UUID) it.next();
                try {
                    playerByUuid = minecraftClient.level.getPlayerByUUID(uuid);
                } catch (Exception e) {
                }
                if (playerByUuid == null) {
                    this.objectList.add(uuid);
                } else if (minecraftClient.player.tickCount - this.concurrentHashMap.get(uuid).intValue() > 20) {
                    this.objectList.add(uuid);
                } else if (playerByUuid.getHealth() <= 0.0f) {
                    this.objectList.add(uuid);
                    this.num++;
                    if (this.autoEZ.getValue().booleanValue() && (string3005 = getNextAutoEZMessage()) != null) {
                        String replace = string3005.replace("{name}", playerByUuid.getName().getString());
                        if (!iRC.isToggled() || !replace.startsWith(iRC.prefix.getValue())) {
                            Runnable runnable = () -> {
                                ChatUtils.sendChatOrCommand(replace);
                            };
                            if (this.delay.isAtMinimum()) {
                                runnable.run();
                            } else {
                                ClientServices.tooltipsHelper.schedule(runnable, (int) (this.delay.getValue().floatValue() / 0.05f));
                            }
                        }
                    }
                    if (this.thunder.getValue().booleanValue() && (PlayerUtils.hasArmor((LivingEntity) playerByUuid) || !this.ignoreNakeds.getValue().booleanValue())) {
                        spawnLightning(playerByUuid.position());
                    }
                    if (this.ashes.getValue().booleanValue()) {
                        spawnAshParticles(playerByUuid.position());
                    }
                    if (this.killSound.getValue().booleanValue() && this.players.getValue().booleanValue() && this.volume.getValue().floatValue() > 0.0f) {
                        SoundPlayer searchHelper4_429702 = ClientServices.killstreakSoundManager.findSoundPlayer(this.sound.getValue());
                        if (searchHelper4_429702 != null) {
                            searchHelper4_429702.play(this.volume.getValue().floatValue());
                        }
                    }
                    if (this.killStreak.getValue().booleanValue() && this.num >= 2 && this.volume2.getValue().floatValue() > 0.0f) {
                        SoundPlayer searchHelper4_42968 = ClientServices.killstreakSoundManager.getRandomKillstreakSound(this.num);
                        if (searchHelper4_42968 != null) {
                            searchHelper4_42968.play(this.volume2.getValue().floatValue());
                        }
                    }
                }
            }
        }
        synchronized (this.objectList) {
            try {
                Iterator it2 = this.objectList.iterator();
                while (it2.hasNext()) {
                    this.concurrentHashMap.remove((UUID) it2.next());
                }
            } catch (Exception e2) {
            }
        }
    }

    public void registerAttackTarget(Player playerEntity) {
        if (isNotInGame() || playerEntity == minecraftClient.player) {
            return;
        }
        if (ClientServices.socialManager.isFriend(playerEntity.getName().getString())) {
            return;
        }
        try {
            this.concurrentHashMap.put(playerEntity.getUUID(), Integer.valueOf(minecraftClient.player.tickCount));
        } catch (Exception e) {
        }
    }

    public void loadAutoEZLines(boolean z) {
        if (this.autoEZ.getValue().booleanValue() || z) {
            Path path1566 = FileIO.resolvePath(ConfigManager.path4.resolve(this.path.getValue()), ".txt");
            try {
                if (!path1566.toFile().exists()) {
                    throw new IOException("AutoEZ file %s not found".formatted(this.path.getValue()));
                }
                this.list.clear();
                this.list.addAll(FileUtils.readLines(path1566.toFile(), StandardCharsets.UTF_8));
                this.num2 = 0;
            } catch (Exception e) {
                if (this.autoEZ.getValue().booleanValue()) {
                    ChatUtils.sendPrefixedMessage(Component.literal(new StringTemplate().arg(String.valueOf(e)).format("Failed to update AutoEZ lines: \u0001")), ChatUtils.createSignature(-9634482), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode3);
                    try {
                        Files.createFile(path1566, new FileAttribute[0]);
                    } catch (Exception e2) {
                    }
                }
            }
        }
    }

    public String getNextAutoEZMessage() {
        try {
            if (this.list.isEmpty()) {
                return null;
            }
            if (this.random.getValue().booleanValue()) {
                List<String> list = this.list;
                return list.get(ThreadLocalRandom.current().nextInt(this.list.size()));
            }
            List<String> list2 = this.list;
            int i = this.num2;
            this.num2 = i + 1;
            return list2.get(i % this.list.size());
        } catch (Exception e) {
            return null;
        }
    }

    public void spawnLightning(Vec3 vec3d) {
        // 26.2: LightningBolt(EntityType, Level) + setVisualOnly(true) for client-side-only kill effect.
        if (minecraftClient.level == null) {
            return;
        }
        for (int i = 0; i < this.lightnings.getValue().intValue(); i++) {
            LightningBolt lightningEntity = new LightningBolt(EntityTypes.LIGHTNING_BOLT, minecraftClient.level);
            lightningEntity.setVisualOnly(true);
            lightningEntity.setPos(vec3d.x, vec3d.y, vec3d.z);
            minecraftClient.level.addEntity(lightningEntity);
        }
    }

    public void spawnAshParticles(Vec3 vec3d) {
        // 26.2: ParticleEngine#createParticle(ParticleOptions, x, y, z, vx, vy, vz) + add(Particle).
        if (minecraftClient.level == null || minecraftClient.particleEngine == null) {
            return;
        }
        for (int i = 0; i < 24; i++) {
            double vx = (random2.nextDouble() - 0.5) * 0.6;
            double vy = random2.nextDouble() * 0.8 + 0.2;
            double vz = (random2.nextDouble() - 0.5) * 0.6;
            Particle p = minecraftClient.particleEngine.createParticle(ParticleTypes.ASH,
                    vec3d.x + (random2.nextDouble() - 0.5) * 0.8, vec3d.y + random2.nextDouble() * 0.6, vec3d.z + (random2.nextDouble() - 0.5) * 0.8,
                    vx, vy, vz);
            if (p != null) {
                minecraftClient.particleEngine.add(p);
            }
        }
    }
}