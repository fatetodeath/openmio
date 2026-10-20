package me.mioclient.module.client.anticheat;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.mode.combat.CrystalRotationMode;

import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import me.mioclient.module.client.AntiCheat;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class YawSpoofHandler implements ClientAccessor {
    public final AntiCheat antiCheat;
    public boolean flag;

    public YawSpoofHandler(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }

    public void onUpdate() {
        if (is328()) {
            if (this.antiCheat.rotations.getValue() == CrystalRotationMode.SILENT) {
                PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), minecraftClient.player.getYRot(), minecraftClient.player.getXRot(), minecraftClient.player.onGround());
            }
            setYawSpoofActive(true);
        }
    }

    public void spoofYawOnMovePacket(PacketSendEvent sendImmediatelyEvent) {
        // 26.2 port (verified): ServerboundMovePlayerPacket is immutable — cancel + resend
        // with spoofed yaw, preserving the original subtype (NoFall pattern).
        if (!(sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket pkt)) {
            return;
        }
        if (!shouldSpoofYaw(pkt)) {
            return;
        }
        net.minecraft.client.player.LocalPlayer pl = minecraftClient.player;
        float spoofedYaw = pkt.getYRot(pl.getYRot()) + 3.5999998E10f;
        boolean onGround = pkt.isOnGround();
        boolean hc = pkt.horizontalCollision();
        sendImmediatelyEvent.cancel();
        if (pkt instanceof ServerboundMovePlayerPacket.PosRot) {
            pl.connection.send(new ServerboundMovePlayerPacket.PosRot(pkt.getX(pl.getX()), pkt.getY(pl.getY()), pkt.getZ(pl.getZ()), spoofedYaw, pkt.getXRot(pl.getXRot()), onGround, hc));
        } else if (pkt instanceof ServerboundMovePlayerPacket.Rot) {
            pl.connection.send(new ServerboundMovePlayerPacket.Rot(spoofedYaw, pkt.getXRot(pl.getXRot()), onGround, hc));
        }
    }

    public boolean is328() {
        return shouldSpoofYaw((ServerboundMovePlayerPacket) null);
    }

    public boolean shouldSpoofYaw(ServerboundMovePlayerPacket playerMoveC2SPacket) {
        if ((!MovementUtils.is2181() && ClientServices.feetPlaceHelper.getEffectiveSpeed() < 5.0d) || !this.antiCheat.f2b2t.getValue().booleanValue() || PlayerUtils.isReallyFallFlying()) {
            return false;
        }
        if (playerMoveC2SPacket != null) {
            boolean z = (minecraftClient.gui.screen() instanceof AbstractContainerScreen) && minecraftClient.player.containerMenu == minecraftClient.player.inventoryMenu;
            if (!playerMoveC2SPacket.hasRotation() && !z) {
                return false;
            }
        }
        return this.antiCheat.flag2 || playerMoveC2SPacket == null;
    }

    public boolean isYawSpoofActive() {
        return this.flag;
    }

    public void setYawSpoofActive(boolean z) {
        this.flag = z;
    }
}