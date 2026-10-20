package me.mioclient.module.client;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.mode.combat.CrystalRotationMode;
import me.mioclient.module.KeybindModule;

import me.mioclient.mode.client.AntiCheatMode;
import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.module.movement.velocity.AntiCheatVelocityHandler;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.module.client.anticheat.YawSpoofHandler;
import me.mioclient.module.movement.velocity.VelocityModeHandler;
import me.mioclient.network.PacketSender;

import me.mioclient.event.EventBus;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.movement.Velocity;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.util.Mth;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AntiCheat extends KeybindModule {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving velocity null. */
    private static Velocity velocity;

    public static Velocity getVelocity() {
        if (velocity == null && ClientServices.moduleInjector != null) {
            try {
                velocity = (Velocity) ClientServices.moduleInjector.getModule(Velocity.class);
            } catch (Exception e) {
            }
        }
        return velocity;
    }
    public static final float val = (float) (((Math.pow(0.20000000298023224d, 3.0d) * 8.0d) * 0.15d) - 0.001d);
    public Setting<Boolean> f2b2t = add(new BooleanSetting("2b2t", false));
    public Setting<Float> yawStep = add(new NumberSetting<>("YawStep", 1.0f, 0.1f, 1.0f));
    public Setting<Boolean> movementSync = add(new BooleanSetting("MovementSync", false).setVisible());
    public Setting<CrystalRotationMode> rotations = add(new EnumSetting<>("Rotations", CrystalRotationMode.SILENT).requiresSettings(this.movementSync));
    public Setting<AntiCheatMode> strictDirection = add(new EnumSetting<>("StrictDirection", AntiCheatMode.NCP));
    public final YawSpoofHandler baritoneSearchHelper4;
    public boolean flag;
    public boolean flag2;

    public AntiCheat() {
        super("AntiCheat", "Manages the client's behavior on different anti-cheats.", Category.CLIENT, new String[0]);
        this.baritoneSearchHelper4 = new YawSpoofHandler(this);
        eventBus.register(this);
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        this.baritoneSearchHelper4.setYawSpoofActive(false);
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        this.flag2 = motionEvent.getPhase() == KeyPearlMode.Pre;
        if (motionEvent.getPhase() == KeyPearlMode.Pre && this.movementSync.getValue().booleanValue()) {
            if ((isF2b2t() && (minecraftClient.gui.screen() instanceof AbstractContainerScreen) && !this.baritoneSearchHelper4.isYawSpoofActive()) || PlayerUtils.isReallyFallFlying()) {
                return;
            }
            Velocity velocity_lcl = getVelocity();
            if (velocity_lcl != null && velocity_lcl.isToggled()) {
                VelocityModeHandler velocityHelper1894 = velocity_lcl.getVelocityHelper1894();
                if ((velocityHelper1894 instanceof AntiCheatVelocityHandler) && ((AntiCheatVelocityHandler) velocityHelper1894).is951()) {
                    return;
                }
            }
            float intBitsToFloat = val * 2.0f * (this.flag ? -1 : 1);
            this.flag = !this.flag;
            motionEvent.setPitch(Mth.clamp(motionEvent.getPitch() + intBitsToFloat, -MathConstants.num2, MathConstants.num2));
        }
    }

    @Listen(get219= 999)
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        ServerboundPlayerActionPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundPlayerActionPacket ? (ServerboundPlayerActionPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundPlayerActionPacket) {
            ServerboundPlayerActionPacket playerActionC2SPacket = packet904;
            if (isF2b2t() && playerActionC2SPacket.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, playerActionC2SPacket.getPos(), playerActionC2SPacket.getDirection());
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (isF2b2t()) {
            ClientboundContainerClosePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundContainerClosePacket ? (ClientboundContainerClosePacket) (channelRead0Event.getPacket()) : null;
            if ((packet904 instanceof ClientboundContainerClosePacket) && packet904.getContainerId() == 0) {
                channelRead0Event.cancel();
            }
        }
    }

    public float getYawStep() {
        return this.yawStep.getValue().floatValue();
    }

    public boolean isF2b2t() {
        return this.f2b2t.getValue().booleanValue();
    }

    public YawSpoofHandler getYawSpoofHandler() {
        return this.baritoneSearchHelper4;
    }
}