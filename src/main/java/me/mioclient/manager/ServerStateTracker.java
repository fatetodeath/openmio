package me.mioclient.manager;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.mioclient.event.events.network.ConnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.exploit.FastLatency;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import me.mioclient.render.RenderConstants;

public final class ServerStateTracker implements ClientAccessor {
    public static final FastLatency fastLatency = (FastLatency) ClientServices.moduleInjector.getModule(FastLatency.class);
    public static final Pattern pattern = Pattern.compile("\\d+ ping");
    public final ArrayDeque<Float> arrayDeque = new ArrayDeque<>(20);
    public final Stopwatch stopwatch = new Stopwatch();
    public final Stopwatch stopwatch2 = new Stopwatch();
    public float val;
    public float val2;
    public long num;
    public long num2;
    public int num3;
    public boolean flag;
    public boolean flag2;
    public ServerData serverInfo;
    public int num4;
    public boolean flag3;

    public ServerStateTracker() {
        eventBus.register(this);
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        this.num = System.currentTimeMillis();
        ClientboundPlayerPositionPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundPlayerPositionPacket ? (ClientboundPlayerPositionPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundPlayerPositionPacket) {
            this.num3 = packet904.id();
            if (this.stopwatch2.hasElapsedMs(50L)) {
                this.stopwatch.reset();
            }
            this.flag = minecraftClient.player != null && minecraftClient.player.isSprinting();
        }
        if (channelRead0Event.getPacket() instanceof ClientboundSetTimePacket) {
            if (this.num2 != 0) {
                if (this.arrayDeque.size() > 20) {
                    this.arrayDeque.poll();
                }
                this.val = Math.max(0.0f, Math.min(20.0f, 20.0f * (1000.0f / ((float) (System.currentTimeMillis() - this.num2)))));
                this.arrayDeque.add(Float.valueOf(this.val));
                float f = 0.0f;
                Iterator<Float> it = this.arrayDeque.iterator();
                while (it.hasNext()) {
                    f += Math.max(0.0f, Math.min(20.0f, it.next().floatValue()));
                }
                if (this.arrayDeque.size() > 0) {
                    f /= this.arrayDeque.size();
                }
                this.val2 = f;
            }
            this.num2 = System.currentTimeMillis();
        }
        ClientboundBundlePacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundBundlePacket ? (ClientboundBundlePacket) (channelRead0Event.getPacket()) : null;
        if (packet9042 instanceof ClientboundBundlePacket) {
            Iterator it2 = packet9042.subPackets().iterator();
            while (it2.hasNext()) {
                eventBus.fireEvent(new PacketReceiveEvent((Packet) it2.next()));
            }
        }
        ClientboundTabListPacket packet9043 = (channelRead0Event.getPacket()) instanceof ClientboundTabListPacket ? (ClientboundTabListPacket) (channelRead0Event.getPacket()) : null;
        if (packet9043 instanceof ClientboundTabListPacket) {
            ClientboundTabListPacket playerListHeaderS2CPacket = packet9043;
            if (minecraftClient.player.connection.getServerData() == null) {
                return;
            }
            Matcher matcher = pattern.matcher(playerListHeaderS2CPacket.footer().getString());
            if (matcher.find()) {
                String group = matcher.group();
                try {
                    this.num4 = Integer.parseInt(group.substring(0, group.length() - 5));
                } catch (Throwable th) {
                }
            }
        }
    }

    @Listen(get219= RenderConstants.num)
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        ServerboundPlayerCommandPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundPlayerCommandPacket ? (ServerboundPlayerCommandPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundPlayerCommandPacket) {
            ServerboundPlayerCommandPacket clientCommandC2SPacket = packet904;
            if (clientCommandC2SPacket.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
                if (this.flag) {
                    sendImmediatelyEvent.cancel();
                }
                this.flag = true;
            }
            if (clientCommandC2SPacket.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                if (!this.flag) {
                    sendImmediatelyEvent.cancel();
                }
                this.flag = false;
            }
        }
        if (sendImmediatelyEvent.getPacket() instanceof ServerboundAcceptTeleportationPacket) {
            this.stopwatch2.reset();
        }
    }

    @Listen
    public void onConnect(ConnectEvent connectEvent) {
        this.flag3 = false;
        this.num4 = 0;
    }

    public int getPing() {
        if (minecraftClient.player == null) {
            return 0;
        }
        if (fastLatency.isToggled()) {
            return fastLatency.getPing();
        }
        PlayerInfo playerListEntry = minecraftClient.player.connection.getPlayerInfo(minecraftClient.player.getGameProfile().id());
        int latency = playerListEntry == null ? 0 : playerListEntry.getLatency();
        return latency == 0 ? this.num4 : latency;
    }

    public Stopwatch getSetbackStopwatch() {
        return this.stopwatch;
    }

    public Stopwatch getTeleportStopwatch() {
        return this.stopwatch2;
    }

    public int getPendingTeleportId() {
        return this.num3;
    }

    public long getLastPacketTime() {
        return this.num;
    }

    public float getCurrentTickRate() {
        return this.val;
    }

    public float getSmoothedTickRate() {
        return this.val2;
    }

    public float getTickRateFactor() {
        return this.val2 / 20.0f;
    }

    public ServerData getServerInfo() {
        return this.serverInfo;
    }

    public void setServerInfo(ServerData serverInfo) {
        this.serverInfo = serverInfo;
    }

    public boolean isSprinting() {
        return this.flag;
    }

    public boolean isSneaking() {
        return this.flag2;
    }
}
