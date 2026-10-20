package me.mioclient.module.player;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.util.time.Stopwatch;

import java.util.concurrent.TimeUnit;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.NumberSetting;

public class AutoFish extends Module {
    public Setting<Float> castDelay = add(new NumberSetting<>("CastDelay", 2.0f, 1.0f, 5.0f).setSuffix("s"));
    public final Stopwatch stopwatch;

    public AutoFish() {
        super("AutoFish", "Fishes.", Category.PLAYER, new String[0]);
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.stopwatch.reset();
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundSoundPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundSoundPacket ? (ClientboundSoundPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundSoundPacket) {
            ClientboundSoundPacket playSoundS2CPacket = packet904;
            if (!playSoundS2CPacket.getSound().is(registryKey -> {
                return registryKey.identifier().toString().equalsIgnoreCase("minecraft:entity.fishing_bobber.splash");
            }) || minecraftClient.player.fishing == null) {
                return;
            }
            if (new Vec3(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ()).distanceTo(minecraftClient.player.fishing.position()) <= 4.0d) {
                minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.MAIN_HAND);
                minecraftClient.player.swing(InteractionHand.MAIN_HAND);
                this.stopwatch.reset();
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.player.getMainHandItem().is(Items.FISHING_ROD)) {
            if (!this.stopwatch.hasElapsed(1.0d, TimeUnit.MINUTES)) {
                if (minecraftClient.player.fishing != null) {
                    return;
                }
                if (!this.stopwatch.hasElapsed(this.castDelay.getValue().floatValue(), TimeUnit.SECONDS)) {
                    return;
                }
            }
            minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.MAIN_HAND);
            minecraftClient.player.swing(InteractionHand.MAIN_HAND);
            this.stopwatch.reset();
        }
    }
}