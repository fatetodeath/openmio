package me.mioclient.network.packet;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.network.codec.NetworkMessage;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.math.ColorUtils;

import io.netty.channel.ChannelHandlerContext;
import java.awt.Color;
import java.io.DataInputStream;
import java.util.Objects;
import me.mioclient.module.client.IRC;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

public class IrcChatPacket implements NetworkMessage {
    public static IRC iRC = (IRC) ClientServices.moduleInjector.getModule(IRC.class);
    public String string;
    public String string2;
    public String string3;

    @Override // me.mioclient.network.codec.NetworkMessage
    public void handle(ChannelHandlerContext channelHandlerContext) {
        if (!iRC.chat.getValue().booleanValue() || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return;
        }
        if (ClientServices.nameTagsManager.getIgnoredUsers().contains(this.string2.toLowerCase())) {
            return;
        }
        iRC.num++;
        Component empty = Component.empty();
        ((MutableComponent) empty).append("<");
        Color color814 = iRC.gradient.getValue().booleanValue() ? ColorUtils.getGradientColor(ColorUtils.getThemeColor(), ColorUtils.getThemeColor().darker(), 3000.0d, 0.0d) : ColorUtils.getThemeColor();
        ((MutableComponent) empty).append(Component.literal(this.string).withStyle(style -> {
            Objects.requireNonNull(color814);
            return ChatUtils.withDynamicColor(style, color814::hashCode);
        }));
        ((MutableComponent) empty).append("> ");
        ((MutableComponent) empty).append(this.string3);
        ChatUtils.sendMessage(empty, ChatUtils.createSignature(new StringTemplate().arg(iRC.num).arg(this.string).arg(this.string3).format("\u0001\u0001\u0001").hashCode()));
        if (iRC.chatSound.getValue().booleanValue()) {
            if (this.string.contains(ClientServices.welcomerHelper.getUsername())) {
                return;
            }
            if (this.string.contains(Minecraft.getInstance().getUser().getName())) {
                return;
            }
            ClientServices.killstreakSoundManager.findSoundPlayer(iRC.mode.getValue()).play(iRC.volume2.getValue().floatValue());
        }
    }

    @Override // me.mioclient.network.codec.NetworkMessage
    /* renamed from: getByteToMessageDecoderHelper_141189, reason: merged with bridge method [inline-methods] */
    public IrcChatPacket fromBytes(byte[] bArr) throws java.io.IOException {
        DataInputStream dataInputStream2177 = openInput(bArr);
        IrcChatPacket byteToMessageDecoderHelper_14 = new IrcChatPacket();
        byteToMessageDecoderHelper_14.string = dataInputStream2177.readUTF();
        byteToMessageDecoderHelper_14.string2 = dataInputStream2177.readUTF();
        byteToMessageDecoderHelper_14.string3 = dataInputStream2177.readUTF();
        return byteToMessageDecoderHelper_14;
    }

    @Override // me.mioclient.network.codec.NetworkMessage
    public byte[] toBytes() throws java.io.IOException {
        NetworkMessage.DataOutputStream dataOutputStream2178 = openOutput();
        dataOutputStream2178.writeUTF(this.string);
        dataOutputStream2178.writeUTF(this.string2);
        dataOutputStream2178.writeUTF(this.string3);
        return dataOutputStream2178.toByteArray();
    }

    @Override // me.mioclient.network.codec.NetworkMessage
    public short getPacketId() {
        return (short) 5;
    }
}
