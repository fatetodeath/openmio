package me.mioclient.module.movement.entitycontrol;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import me.mioclient.module.movement.EntityControl;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.phys.Vec3;
import me.mioclient.event.events.network.Vec3dEvent;

public final class VehicleMoveHandler implements ClientAccessor {
    public final EntityControl entityControl;
    public int num;
    public int num2;
    public Vec3 vec3d;
    public final Stopwatch stopwatch = new Stopwatch();
    public boolean flag = true;

    public VehicleMoveHandler(EntityControl entityControl) {
        this.entityControl = entityControl;
    }

    public boolean updateVehicleState() {
        if (!minecraftClient.player.isPassenger()) {
            return true;
        }
        long j = 1000;
        if (this.num >= 3) {
            j = 250;
        }
        if (this.stopwatch.hasElapsedMs(j)) {
            sendVehicleSync();
        }
        Vec3 pos = minecraftClient.player.getVehicle().position();
        if (this.num2 > 0) {
            this.num2--;
        }
        if (this.vec3d == null || 0 != 0 || Math.abs(pos.y - this.vec3d.y) >= 18.0d) {
            sendVehicleSync();
            this.vec3d = pos;
        }
        return this.flag;
    }

    public void handlePacketSend(PacketSendEvent sendImmediatelyEvent) {
        if ((sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket) || (sendImmediatelyEvent.getPacket() instanceof ServerboundPaddleBoatPacket)) {
            sendImmediatelyEvent.cancel();
        }
    }

    public void handleVehiclePosition(Vec3dEvent vec3dEvent) {
        this.flag = true;
        Entity vehicle = minecraftClient.player.getVehicle();
        if (vehicle == null) {
            return;
        }
        Vec3 vec3d1954 = vec3dEvent.getPosition();
        vehicle.setPos(vec3d1954.x, vec3d1954.y, vec3d1954.z);
        sendVehicleMovePacket(0.0f);
        vec3dEvent.cancel();
        this.num2 = 2;
    }

    public void sendVehicleSync() {
        Entity vehicle = minecraftClient.player.getVehicle();
        if (vehicle == null) {
            return;
        }
        if (this.vec3d == null || this.vec3d.y >= vehicle.getY()) {
            this.num = 0;
        } else {
            this.num++;
        }
        sendVehicleMovePacket(1000.0f);
        this.stopwatch.reset();
        this.flag = false;
    }

    public void sendVehicleMovePacket(float f) {
        Entity vehicle = minecraftClient.player.getVehicle();
        if (vehicle == null) {
            return;
        }
        // 26.2 port: ServerboundMoveVehiclePacket is a record - build replacement instead of mutating.
        ServerboundMoveVehiclePacket base = ServerboundMoveVehiclePacket.fromEntity(vehicle);
        Vec3 pos = base.position();
        ServerboundMoveVehiclePacket vehicleMoveC2SPacket = new ServerboundMoveVehiclePacket(new Vec3(pos.x, pos.y + f, pos.z), base.yRot(), base.xRot(), base.onGround());
        PacketSender.sendPacket(vehicleMoveC2SPacket);
    }

    public boolean isAboveTickThreshold(int i) {
        return this.num >= i;
    }
}