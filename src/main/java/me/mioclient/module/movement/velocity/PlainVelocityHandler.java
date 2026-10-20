package me.mioclient.module.movement.velocity;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.network.ExplosionVelocityEvent;
import me.mioclient.module.movement.Velocity;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class PlainVelocityHandler extends VelocityModeHandler {
    public PlainVelocityHandler(Velocity velocity) {
        super(velocity);
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
        int intValue = this.velocity.horizontal.getValue().intValue();
        int intValue2 = this.velocity.vertical.getValue().intValue();
        int i = this.velocity.inverse.getValue().booleanValue() ? -1 : 1;
        if (!(channelRead0Event.getPacket() instanceof ClientboundSetEntityMotionPacket entityVelocityUpdateS2CPacket)) {
            return;
        }
        if (entityVelocityUpdateS2CPacket.id() == minecraftClient.player.getId()) {
            // 26.2 port: record is immutable - cancel + apply scaled motion directly.
            float hScale = intValue * 0.01f * i;
            float vScale = intValue2 * 0.01f;
            Vec3 m = entityVelocityUpdateS2CPacket.movement();
            if (intValue == 0 && intValue2 == 0) {
                channelRead0Event.cancel();
            } else {
                channelRead0Event.cancel();
                minecraftClient.player.setDeltaMovement(new Vec3(m.x * hScale, m.y * vScale, m.z * hScale));
            }
        }
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void handleExplosionVelocity(ExplosionVelocityEvent explosionVelocityEvent) {
        if (this.velocity.explosions.getValue().booleanValue()) {
            int intValue = this.velocity.horizontal.getValue().intValue();
            int intValue2 = this.velocity.vertical.getValue().intValue();
            int i = this.velocity.inverse.getValue().booleanValue() ? -1 : 1;
            explosionVelocityEvent.setPlayerVelocityX((int) (explosionVelocityEvent.getPlayerVelocityX() * intValue * 0.01f * i));
            explosionVelocityEvent.setPlayerVelocityY((int) (explosionVelocityEvent.getPlayerVelocityY() * intValue2 * 0.01f));
            explosionVelocityEvent.setPlayerVelocityZ((int) (explosionVelocityEvent.getPlayerVelocityZ() * intValue * 0.01f * i));
        }
    }
}