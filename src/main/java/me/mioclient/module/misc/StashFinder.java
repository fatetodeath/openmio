package me.mioclient.module.misc;

import me.mioclient.api.ClientServices;
import me.mioclient.setting.SoundIdentifierSetting;
import me.mioclient.util.client.SoundIdentifier;
import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.misc.stashfinder.StashEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import me.mioclient.manager.KillstreakSoundManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.network.LoadChunkFromPacketEvent;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class StashFinder extends Module {
    public Setting<Boolean> chests = add(new BooleanSetting("Chests", true).setVisible());
    public Setting<Integer> chestCount = add(new NumberSetting<>("ChestCount", 5, 2, 20).requiresSettings(this.chests));
    public Setting<Boolean> shulkers = add(new BooleanSetting("Shulkers", true));
    public Setting<Boolean> stackedCarts = add(new BooleanSetting("StackedCarts", true));
    public Setting<Boolean> notify = add(new BooleanSetting("Notify", true).setVisible());
    public Setting<Boolean> censorCoords = add(new BooleanSetting("CensorCoords", false).requiresSettings(this.notify));
    public Setting<Boolean> log = add(new BooleanSetting("Log", true));
    public Setting<Boolean> sound = add(new BooleanSetting("Sound", true).setVisible());
    public Setting<SoundIdentifier> type = add(new SoundIdentifierSetting("Type", new SoundIdentifier("system", "warning")).requiresSettings(this.sound));
    public Setting<Float> volume = add(new NumberSetting<>("Volume", 1.0f, 0.0f, 1.0f).requiresSettings(this.sound));
    public Setting<Boolean> ignoreNatural = add(new BooleanSetting("IgnoreNatural", false).setVisible());
    public Setting<Boolean> dungeon = add(new BooleanSetting("Dungeon", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> fortress = add(new BooleanSetting("Fortress", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> bastion = add(new BooleanSetting("Bastion", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> ancientCity = add(new BooleanSetting("AncientCity", false).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> trialChambers = add(new BooleanSetting("TrialChambers", true).requiresSettings(this.ignoreNatural));
    public final Stopwatch stopwatch;

    public StashFinder() {
        super("StashFinder", "Logs and/or notifies you about possible stashes.", Category.MISC, new String[0]);
        this.stopwatch = new Stopwatch();
        setDrawn(false);
    }

    
    
    @Listen
    
    public void onLoadChunkFromPacket(LoadChunkFromPacketEvent loadChunkFromPacketEvent) {
        String format = new SimpleDateFormat("MM/dd HH:mm a").format(new Date());
        LevelChunk worldChunk2555 = loadChunkFromPacketEvent.getChunk();
        ChunkPos pos = worldChunk2555.getPos();
        int i = 0;
        for (BlockEntity blockEntity : worldChunk2555.getBlockEntities().values()) {
            if (this.ignoreNatural.getValue().booleanValue()) {
                if (WorldUtils.isStashChest(minecraftClient.level.getChunk(blockEntity.getBlockPos()), blockEntity.getBlockPos(), this.fortress.getValue().booleanValue(), this.bastion.getValue().booleanValue(), this.ancientCity.getValue().booleanValue(), this.dungeon.getValue().booleanValue(), this.trialChambers.getValue().booleanValue())) {
                    continue;
                }
            }
            if (!(blockEntity instanceof ChestBlockEntity)) {
                if (blockEntity instanceof BarrelBlockEntity) {
                }
                if (!this.chests.getValue().booleanValue() && i >= this.chestCount.getValue().intValue()) {
                    String string2921 = new StringTemplate().arg(i).format("\u0001 chests");
                    if (tryRegisterStash(createStashEntry(new StringTemplate().arg(format).arg(string2921).format("\u0001 (\u0001)"), pos.getMiddleBlockX(), pos.getMiddleBlockZ()), string2921)) {
                        break;
                    }
                } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
                    continue;
                } else {
                    ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity) blockEntity;
                    if (this.shulkers.getValue().booleanValue()) {
                        if (!ClientServices.stashFinderHelper.isBlockConfirmed(shulkerBoxBlockEntity.getBlockPos())) {
                            if (tryRegisterStash(createStashEntry(new StringTemplate().arg(format).arg("a shulkerbox").format("\u0001 (\u0001)"), pos.getMiddleBlockX(), pos.getMiddleBlockZ()), "a shulkerbox")) {
                                break;
                            }
                        } else {
                            continue;
                        }
                    } else {
                        continue;
                    }
                }
            }
            i++;
            if (!this.chests.getValue().booleanValue()) {
            }
            if (blockEntity instanceof ShulkerBoxBlockEntity) {
            }
        }
        if (this.stackedCarts.getValue().booleanValue()) {
            // 26.2: MinecartChest (renamed from ChestMinecartEntity) via EntityGetter#getEntitiesOfClass.
            net.minecraft.world.phys.AABB chunkBox = new net.minecraft.world.phys.AABB(
                    pos.getMinBlockX(), minecraftClient.level.getMinY(), pos.getMinBlockZ(),
                    pos.getMaxBlockX() + 1, minecraftClient.level.getMaxY(), pos.getMaxBlockZ() + 1);
            java.util.List<net.minecraft.world.entity.vehicle.minecart.MinecartChest> carts =
                    minecraftClient.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.minecart.MinecartChest.class, chunkBox, e -> !e.isRemoved());
            java.util.List<net.minecraft.world.entity.vehicle.boat.ChestBoat> boats =
                    minecraftClient.level.getEntitiesOfClass(net.minecraft.world.entity.vehicle.boat.ChestBoat.class, chunkBox, e -> !e.isRemoved());
            int vehicles = carts.size() + boats.size();
            if (vehicles >= 2) {
                String label = vehicles + " storage vehicles";
                tryRegisterStash(createStashEntry(new StringTemplate().arg(format).arg(label).format("\u0001 (\u0001)"), pos.getMiddleBlockX(), pos.getMiddleBlockZ()), label);
            }
        }
    }

    public StashEntry createStashEntry(String str, int i, int i2) {
        var serverData = minecraftClient.getConnection() == null ? null : minecraftClient.getConnection().getServerData();
        return new StashEntry(str, i, i2, WorldUtils.getDimension().name(), serverData == null ? "singleplayer" : serverData.ip);
    }

    public boolean tryRegisterStash(StashEntry stashFinderHelper, String str) {
        if (!ClientServices.stashFinderDataManager.findFirst(stashFinderHelper2 -> {
            return stashFinderHelper2.getX() == stashFinderHelper.getX() && stashFinderHelper2.getZ() == stashFinderHelper.getZ();
        }).isEmpty() || !ClientServices.stashFinderDataManager.register(stashFinderHelper)) {
            return false;
        }
        if (this.notify.getValue().booleanValue()) {
            MutableComponent append = Component.literal(getName()).append(new StringTemplate().arg(str).format(" has found \u0001 at: "));
            if (!this.censorCoords.getValue().booleanValue()) {
                append.append(stashFinderHelper.formatCoords());
            }
            ChatUtils.sendPrefixedMessage((Component) append, ChatUtils.createSignature(-13579), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode3);
        }
        if (!this.sound.getValue().booleanValue()) {
            return true;
        }
        if (!this.stopwatch.hasElapsed(5.0d, TimeUnit.SECONDS)) {
            return true;
        }
        KillstreakSoundManager KillstreakSoundManager = ClientServices.killstreakSoundManager;
        KillstreakSoundManager.playSound(this.type.getValue(), this.volume.getValue().floatValue());
        this.stopwatch.reset();
        return true;
    }
}