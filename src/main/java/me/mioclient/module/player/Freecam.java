package me.mioclient.module.player;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.input.InputTickEvent;
import me.mioclient.event.events.interaction.InteractItemPacketEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.module.player.freecam.FreecamInput;

import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.InteractBlockEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.input.TickHookPreEvent;
import me.mioclient.event.events.input.UpdateMouseEvent;
import me.mioclient.event.events.player.UpdateSetPosEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class Freecam extends Module {
    public final FreecamInput freecamSearchHelper4;
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.1f, 5.0f));
    public Setting<Float> verticalSpeed = add(new NumberSetting<>("VerticalSpeed", 0.5f, 0.1f, 5.0f));
    public Setting<Boolean> noHands = add(new BooleanSetting("NoHands", true));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false).setVisible());
    public Setting<Boolean> always = add(new BooleanSetting("Always", false).requiresSettings(this.rotate));
    public Vec3 vec3d;
    public Vec3 vec3d2;
    public float[] floatArr;
    public CameraType perspective;
    public float val;
    public float val2;
    public float val3;
    public float val4;

    public Freecam() {
        super("Freecam", "Allows your camera to fly through walls.", Category.PLAYER, new String[0]);
        this.freecamSearchHelper4 = new FreecamInput(minecraftClient.options);
        this.floatArr = null;
        this.speed.removeBounds();
        this.verticalSpeed.removeBounds();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        if (isNotInGame()) {
            disable();
            return;
        }
        this.perspective = minecraftClient.options.getCameraType();
        new InputTickEvent(minecraftClient.player.input).reset();
        Vec3 pos = minecraftClient.gameRenderer.mainCamera().position();
        this.vec3d = pos;
        this.vec3d2 = pos;
        this.floatArr = new float[]{minecraftClient.player.getYRot(), minecraftClient.player.getXRot()};
        float f = this.floatArr[0];
        this.val = f;
        this.val2 = f;
        float f2 = this.floatArr[1];
        this.val3 = f2;
        this.val4 = f2;
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        if (isNotInGame()) {
            return;
        }
        minecraftClient.player.setYRot(this.floatArr[0]);
        minecraftClient.player.setXRot(this.floatArr[1]);
        minecraftClient.options.setCameraType(this.perspective);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.gui.screen() instanceof LevelLoadingScreen) {
            disable();
        }
        minecraftClient.options.setCameraType(CameraType.FIRST_PERSON);
        this.vec3d2 = this.vec3d;
        double[] doubleArray2508 = MovementUtils.computeVelocity(this.val, this.freecamSearchHelper4, this.speed.getValue().floatValue());
        this.vec3d = this.vec3d.add(doubleArray2508[0], 0.0d, doubleArray2508[1]);
        if (minecraftClient.options.keyJump.isDown()) {
            this.vec3d = this.vec3d.add(0.0d, this.verticalSpeed.getValue().floatValue(), 0.0d);
        }
        if (minecraftClient.options.keyShift.isDown()) {
            this.vec3d = this.vec3d.subtract(0.0d, this.verticalSpeed.getValue().floatValue(), 0.0d);
        }
        this.freecamSearchHelper4.tick(false, 1.0f);
    }

    @Listen
    public void onTickHookPre(TickHookPreEvent tickHookPreEvent) {
        tickHookPreEvent.cancel();
    }

    @Listen(get219= 1000)
    public void onMotion(MotionEvent motionEvent) {
        boolean isBreakingBlock = minecraftClient.gameMode.isDestroying();
        if (motionEvent.getPhase() == KeyPearlMode.Post || !this.rotate.getValue().booleanValue() || minecraftClient.hitResult == null) {
            return;
        }
        if (!RotationManager.is724() || isBreakingBlock || this.always.getValue().booleanValue()) {
            motionEvent.setRotation(RotationManager.getRotationsTo(minecraftClient.hitResult.getLocation()));
        }
    }

    @Listen
    public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
        if ((minecraftClient.player.getItemInHand(interactBlockEvent.getHand()).getItem() instanceof BucketItem) || (this.rotate.getValue().booleanValue() && RotationManager.is724())) {
            ClientServices.RotationManager.requestRotations(RotationManager.getRotationsTo(interactBlockEvent.getHitResult().getLocation()), 1000);
        }
    }

    @Listen
    public void onInteractItemPacket(InteractItemPacketEvent interactItemEvent_2) {
        if (!this.rotate.getValue().booleanValue() || minecraftClient.hitResult == null || this.always.getValue().booleanValue()) {
            return;
        }
        interactItemEvent_2.setRotation(RotationManager.getRotationsTo(minecraftClient.hitResult.getLocation()));
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundDisconnectPacket) {
            disable();
        }
    }

    @Listen
    public void onUpdateMouse(UpdateMouseEvent updateMouseEvent) {
        do173(updateMouseEvent.getDeltaX() * 0.15000000596046448d, updateMouseEvent.getDeltaY() * 0.15000000596046448d);
        updateMouseEvent.cancel();
    }

    @Listen
    public void onUpdateSetPos(UpdateSetPosEvent updateSetPosEvent) {
        float tickDelta = minecraftClient.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        updateSetPosEvent.setPos(new Vec3(lerpX(tickDelta), get175(tickDelta), get176(tickDelta)));
        updateSetPosEvent.cancel();
    }

    public void do173(double d, double d2) {
        this.val2 = this.val;
        this.val4 = this.val3;
        this.val = (float) (this.val + d);
        this.val3 = (float) (this.val3 + d2);
        this.val3 = Mth.clamp(this.val3, -MathConstants.num2, MathConstants.num2);
    }

    public double lerpX(float f) {
        return Mth.lerp(f, this.vec3d2.x, this.vec3d.x);
    }

    public double get175(float f) {
        return Mth.lerp(f, this.vec3d2.y, this.vec3d.y);
    }

    public double get176(float f) {
        return Mth.lerp(f, this.vec3d2.z, this.vec3d.z);
    }

    public double get177(float f) {
        return minecraftClient.gui.screen() != null ? this.val : Mth.lerp(f, this.val2, this.val);
    }

    public double get178(float f) {
        return minecraftClient.gui.screen() != null ? this.val3 : Mth.lerp(f, this.val4, this.val3);
    }

    public boolean is179() {
        return isToggled() && this.noHands.getValue().booleanValue();
    }
}