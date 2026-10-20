package me.mioclient.manager;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.mode.combat.CrystalRotationMode;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.RotationRequest;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;
import me.mioclient.render.RenderUtil;

import me.mioclient.event.EventBus;

import java.util.ArrayDeque;
import java.util.Queue;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.event.events.player.RotationEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.mixin.ducks.DuckClientPlayerEntity;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;

public final class RotationManager implements ClientAccessor {
    public static AntiCheat antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
    public static boolean flag;
    public float val;
    public float val2;
    public float val3;
    public float val4;
    public float val5;
    public float val6;
    public float val7;
    public float val8;
    public float[] floatArr;
    public int num;
    public int num2;
    public final Queue<Packet<?>> queue = new ArrayDeque();
    public boolean flag2;
    public boolean flag3;
    public RotationRequest elytraFlyData;
    public boolean flag4;

    public RotationManager() {
        eventBus.register(this);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.flag4 = false;
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (is724()) {
            do978();
            tickRotations();
        }
    }

    @Listen
    public void onRotation(RotationEvent event_3) {
        boolean z = antiCheat.movementSync.getValue().booleanValue() || this.flag4;
        if (this.elytraFlyData == null || !z) {
            return;
        }
        float[] floatArray218 = this.elytraFlyData.getRotationAngles();
        float f = getNearestYaw(floatArray218);
        if (f != floatArray218[0] && f != Mth.wrapDegrees(floatArray218[0] - MathConstants.num) && f != Mth.wrapDegrees(floatArray218[0] + MathConstants.num)) {
            minecraftClient.player.setSprinting(false);
        }
        event_3.cancel();
        event_3.setYaw(f);
        event_3.setPitch(floatArray218[1]);
    }

    public static float getNearestYaw(float[] fArr) {
        if (minecraftClient.player.isFallFlying()) {
            return fArr[0];
        }
        float f = MathConstants.num3;
        float f2 = fArr[0];
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= MathConstants.num3) {
                return f2;
            }
            float wrapDegrees = Mth.wrapDegrees(fArr[0] + i2);
            float angleBetween = Mth.degreesDifferenceAbs(wrapDegrees, minecraftClient.player.getYRot());
            if (angleBetween <= f) {
                f2 = wrapDegrees;
                f = angleBetween;
            }
            i = i2 + MathConstants.num2;
        }
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        if (this.num2 > 0 && this.floatArr != null) {
            applyRotations(this.floatArr);
            this.num2--;
        }
        if (motionEvent.getPhase() == KeyPearlMode.Post) {
            if (is724()) {
                return;
            }
            do978();
        } else {
            if (!is724()) {
                tickRotations();
            }
            if (this.elytraFlyData != null) {
                motionEvent.setRotation(this.elytraFlyData.getRotationAngles());
                this.elytraFlyData = null;
            }
        }
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (!this.flag2 || sendImmediatelyEvent.isCancelled() || this.flag3) {
            return;
        }
        if (sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket) {
            sendImmediatelyEvent.cancel();
            PacketSender.sendPacketDirect(sendImmediatelyEvent.getPacket());
            do978();
        } else {
            this.queue.add(sendImmediatelyEvent.getPacket());
            sendImmediatelyEvent.cancel();
        }
    }

    public boolean isSneakCommand(ServerboundPlayerCommandPacket.Action mode) {
        // 26.2 MojMap: PRESS_SHIFT_KEY / RELEASE_SHIFT_KEY removed (sneak now via ServerboundPlayerInputPacket).
        return false;
    }

    public void setInRotationTick(boolean z) {
        this.flag2 = z;
    }

    public boolean isInRotationTick() {
        return this.flag2;
    }

    public void do978() {
        long j = 0;
        while (true) {
            if (this.queue.isEmpty()) {
                break;
            }
            j++;
            if (j > 500) {
                System.out.println("anticrash1337");
                break;
            }
            Packet<?> poll = this.queue.poll();
            if (poll != null) {
                this.flag3 = true;
                PacketSender.sendPacket(poll);
                this.flag3 = false;
            }
        }
        this.queue.clear();
    }

    public void tickRotations() {
        this.flag2 = true;
        eventBus.fireEvent(new RotationTickEvent());
        this.flag2 = false;
        if (this.elytraFlyData == null) {
            do978();
        }
    }

    public float getYawStep() {
        return antiCheat.getYawStep();
    }

    public RotationRequest getActiveRequest() {
        return this.elytraFlyData;
    }

    public void submitRequest(RotationRequest elytraFlyData) {
        if (this.elytraFlyData == null || this.elytraFlyData.get219() <= elytraFlyData.get219()) {
            this.elytraFlyData = elytraFlyData;
        }
    }

    public void requestRotations(float[] fArr, int i) {
        requestRotations(fArr, i, false);
    }

    public void requestRotations(float[] fArr, int i, boolean z) {
        if (antiCheat.isF2b2t() && (minecraftClient.gui.screen() instanceof AbstractContainerScreen)) {
            return;
        }
        if (!is1144() || z) {
            submitRequest(new RotationRequest(fArr, i));
        } else if (this.flag2) {
            applyRotations(fArr);
            PacketSender.sendFullMove(minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), fArr[0], fArr[1], ClientServices.antiPhaseHelper.isOnGround());
        }
    }

    public void markRotationForced() {
        this.flag4 = true;
    }

    public void applyRotations(float[] fArr) {
        applyRotations(fArr, 0);
    }

    public void applyRotations(float[] fArr, int i) {
        this.num = minecraftClient.player.tickCount;
        this.val4 = this.val;
        this.val5 = this.val2;
        this.val6 = this.val3;
        this.val3 = computeBodyYaw(fArr[0], this.val6);
        this.val7 = this.val8;
        this.val8 = fArr[0];
        this.val = fArr[0];
        this.val2 = fArr[1];
        this.floatArr = fArr;
        if (i > 0) {
            this.num2 = i;
        }
    }

    public static float[] getRotationsToBlock(BlockPos blockPos) {
        return getRotationsTo(new Vec3(blockPos.getX() + MathConstants.val2, blockPos.getY() + MathConstants.val2, blockPos.getZ() + MathConstants.val2));
    }

    public static float[] getRotationsTo(Entity entity) {
        return getRotationsTo(BoxMath.getClosestPointInBox(minecraftClient.player.getEyePosition(), entity.getBoundingBox()));
    }

    public static float[] getRotationsTo(Vec3 vec3d) {
        return getRotations(minecraftClient.player.getEyePosition(RenderUtil.getPartialTick()), vec3d);
    }

    public static float[] getRotationsToSide(Vec3 vec3d, Direction direction) {
        Vec3 offset;
        Vec3 cameraPosVec = minecraftClient.player.getEyePosition(RenderUtil.getPartialTick());
        if (direction == null) {
            offset = vec3d;
        } else {
            offset = vec3d.relative(direction, MathConstants.val2);
        }
        return getRotations(cameraPosVec, offset);
    }

    public static float[] getRotations(Vec3 vec3d, Vec3 vec3d2) {
        double d = vec3d2.x - vec3d.x;
        double longBitsToDouble = (vec3d2.y - vec3d.y) * (-1.0d);
        double d2 = vec3d2.z - vec3d.z;
        return new float[]{(float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(d2, d)) - MathConstants.num2), (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(longBitsToDouble, Math.sqrt((d * d) + (d2 * d2)))))};
    }

    public static float[] limitYawSpeed(float[] fArr, float f) {
        if (fArr == null) {
            return null;
        }
        float clamp = Mth.clamp(f, 0.1f, 1.0f);
        if (clamp < 1.0f) {
            float lastYaw = ((DuckClientPlayerEntity)(Object) minecraftClient.player).lastYaw();
            float wrapDegrees = Mth.wrapDegrees(fArr[0] - lastYaw);
            if (Math.abs(wrapDegrees) > 180.0f * clamp) {
                fArr[0] = lastYaw + (wrapDegrees * ((180.0f * clamp) / Math.abs(wrapDegrees)));
            }
        }
        return new float[]{fArr[0], fArr[1]};
    }

    public static int getYawOctant(float f) {
        return Mth.floor(((f * 8.0f) / MathConstants.num3) + MathConstants.val2) & 7;
    }

    public static int getYawQuadrant() {
        return getYawQuadrant(minecraftClient.gameRenderer.mainCamera().yRot());
    }

    public static int getYawQuadrant(float f) {
        return Mth.floor(((f * 4.0f) / MathConstants.num3) + MathConstants.val2) & 3;
    }

    public static Direction getDirectionFromQuadrant(int i) {
        switch (i) {
            case 0:
                return Direction.SOUTH;
            case 1:
                return Direction.WEST;
            case 2:
                return Direction.NORTH;
            case 3:
                return Direction.EAST;
            default:
                return Direction.UP;
        }
    }

    public static boolean isInFrustum(AABB box) {
        // 26.2 (verified via DuckLevelRenderer): LevelRenderer has no frustum field
        // (culling moved to SubmitNodeCollector/Frustum via EntityRenderDispatcher).
        // Fail-open so visibility checks never cull until re-ported.
        return true;
    }

    public static boolean isInFrustum(Vec3 vec3d) {
        return isInFrustum(new AABB(BlockPos.containing((Position) vec3d)));
    }

    public void applyRotationsOnce(float f, float f2) {
        if (minecraftClient.player.tickCount == this.num) {
            return;
        }
        applyRotations(new float[]{f, f2});
    }

    public static boolean is724() {
        if (antiCheat == null) {
            return false;
        }
        return antiCheat.movementSync.getValue().booleanValue();
    }

    public static boolean is1144() {
        return antiCheat != null && is724() && antiCheat.rotations.getValue() == CrystalRotationMode.SILENT;
    }

    public float getYaw() {
        return this.val;
    }

    public float getPitch() {
        return this.val2;
    }

    public float getBodyYaw() {
        return this.val3;
    }

    public float getPrevYaw() {
        return this.val4;
    }

    public float getPrevPitch() {
        return this.val5;
    }

    public float getPrevBodyYaw() {
        return this.val6;
    }

    public float getPrevRawYaw() {
        return this.val7;
    }

    public float getRawYaw() {
        return this.val8;
    }

    public int get1116() {
        return this.num;
    }

    public float computeBodyYaw(float f, float f2) {
        if (minecraftClient.player.isPassenger()) {
            return f;
        }
        float f3 = f2;
        double x = minecraftClient.player.getX() - minecraftClient.player.xo;
        double z = minecraftClient.player.getZ() - minecraftClient.player.zo;
        if ((x * x) + (z * z) > 0.002500000176951289d) {
            float atan2 = (((float) Mth.atan2(z, x)) * 57.295776f) - MathConstants.num2;
            float abs = Mth.abs(Mth.wrapDegrees(f) - atan2);
            f3 = (95.0f >= abs || abs >= 265.0f) ? atan2 : atan2 - 180.0f;
        }
        if (minecraftClient.player.attackAnim > 0.0f) {
            f3 = f;
        }
        float wrapDegrees = Mth.wrapDegrees(f - (f2 + (Mth.wrapDegrees(f3 - f2) * 0.3f)));
        if (wrapDegrees < (-75.0f)) {
            wrapDegrees = (-75.0f);
        } else if (wrapDegrees >= 75.0f) {
            wrapDegrees = 75.0f;
        }
        float f4 = f - wrapDegrees;
        if (wrapDegrees * wrapDegrees > 2500.0f) {
            f4 += wrapDegrees * 0.2f;
        }
        return f4;
    }
}
