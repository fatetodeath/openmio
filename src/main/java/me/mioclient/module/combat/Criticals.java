package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.module.movement.velocity.AntiCheatVelocityHandler;
import me.mioclient.util.client.InteractPacketReader;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.network.PacketSender;

import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;

public class Criticals extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static Aura aura;

    public static Aura getAura() {
        if (aura == null && ClientServices.moduleInjector != null) {
            try {
                aura = (Aura) ClientServices.moduleInjector.getModule(Aura.class);
            } catch (Exception e) {
            }
        }
        return aura;
    }
    public Setting<CriticalsMode> mode = add(new EnumSetting<>("Mode", CriticalsMode.PACKET));
    public Setting<Boolean> pauseOnMove = add(new BooleanSetting("PauseOnMove", false));

    
    
    public enum CriticalsMode implements NamedValue {
        PACKET("Packet"),
        NCP("NCP"),
        GRIM("Grim"),
        GRIMV3("2b2t");

        public final String name;

        CriticalsMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Criticals() {
        super("Criticals", "Turns your hits into critical ones.", Category.COMBAT, new String[0]);
        this.pauseOnMove.addVisibilityPredicate(bool -> {
            return this.mode.getValue() != CriticalsMode.GRIMV3;
        });
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return this.mode.getValue().getName();
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        Entity entity2610;
        ServerboundInteractPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundInteractPacket ? (ServerboundInteractPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundInteractPacket) {
            ServerboundInteractPacket playerInteractEntityC2SPacket = packet904;
            if (!shouldProcessCriticalPacket(playerInteractEntityC2SPacket) || (entity2610 = InteractPacketReader.resolvePacketEntity(playerInteractEntityC2SPacket)) == null) {
                return;
            }
            if (!sendImmediatelyEvent.isCancelled()) {
                do906();
            }
            minecraftClient.execute(() -> {
                minecraftClient.player.crit(entity2610);
            });
        }
    }

    public void do906() {
        float[] fArr = {minecraftClient.player.getYRot(), minecraftClient.player.getXRot()};
        Aura aura_lcl = getAura();
        if (aura_lcl != null && aura_lcl.entity != null && aura_lcl.isToggled()) {
            fArr = RotationManager.limitYawSpeed(RotationManager.getRotationsTo(aura_lcl.entity), ClientServices.RotationManager.getYawStep());
        }
        switch (this.mode.getValue().ordinal()) {
            case 1:
                if (PlayerUtils.isInCobweb(minecraftClient.player)) {
                    sendCriticalOffsets(0.0625101d, 0.0d, 0.0125d, 0.0d);
                    return;
                } else {
                    sendCriticalOffsets(0.11d, 0.1100013579d);
                    return;
                }
            case 2:
                PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY() - 1.0E-6d, minecraftClient.player.getZ(), fArr[0], fArr[1], false);
                return;
            case 3:
                float clamp = Math.clamp(fArr[1], (-90.0f) + AntiCheat.val, 90.0f - AntiCheat.val);
                PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY() + 0.0626d, minecraftClient.player.getZ(), fArr[0], clamp + AntiCheat.val, false);
                PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY() + 0.0455d, minecraftClient.player.getZ(), fArr[0], clamp - AntiCheat.val, false);
                return;
            default:
                sendCriticalOffsets(0.0625101d, 0.0d);
                return;
        }
    }

    public boolean is907() {
        if ((this.pauseOnMove.getValue().booleanValue() || this.mode.getValue() == CriticalsMode.GRIMV3) && MovementUtils.is2181()) {
            return false;
        }
        if (this.mode.getValue() == CriticalsMode.GRIMV3) {
            return minecraftClient.player.onGround() && AntiCheatVelocityHandler.isNearGroundCollision();
        }
        if (this.mode.getValue() != CriticalsMode.GRIM || minecraftClient.player.onGround()) {
            return minecraftClient.player.onGround() && minecraftClient.player.verticalCollision && !minecraftClient.player.isInLava() && !minecraftClient.player.isInWater();
        }
        return true;
    }

    public void sendCriticalOffsets(double... dArr) {
        for (double d : dArr) {
            PacketSender.sendPacket(new ServerboundMovePlayerPacket.Pos(minecraftClient.player.getX(), minecraftClient.player.getY() + d, minecraftClient.player.getZ(), false, minecraftClient.player.horizontalCollision));
        }
    }

    public boolean shouldProcessCriticalPacket(ServerboundInteractPacket playerInteractEntityC2SPacket) {
        if (InteractPacketReader.getCrystalInteractionType(playerInteractEntityC2SPacket) != InteractPacketReader.LegacyCrystalMode.ATTACK || !is907()) {
            return false;
        }
        Entity entity2610 = InteractPacketReader.resolvePacketEntity(playerInteractEntityC2SPacket);
        Aura aura_lcl = getAura();
        return ((aura_lcl != null && aura_lcl.isToggled() && aura_lcl.flag2 && this.mode.getValue() != CriticalsMode.GRIM) || entity2610 == null || !entity2610.isAlive() || (entity2610 instanceof EndCrystal)) ? false : true;
    }
}