package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.util.client.InputState;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.render.RenderUtil;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.TickPreEvent;
import me.mioclient.module.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class EntityControl extends Module {
    public Setting<Boolean> entitySpeed = add(new BooleanSetting("EntitySpeed", true).setVisible());
    public Setting<Double> speed = add(new NumberSetting<>("Speed", 1.0, 0.1, 20.0, v -> this.entitySpeed.isShown()));
    public Setting<Boolean> noCollision = add(new BooleanSetting("NoCollision", true, v -> this.entitySpeed.isShown()));
    public Setting<Boolean> flight = add(new BooleanSetting("Flight", false, v -> this.entitySpeed.isShown()));
    public Setting<Float> vertical = add(new NumberSetting<>("Vertical", 10.0f, 0.0f, 20.0f).requiresSettings(this.flight, this.entitySpeed));
    public Setting<Boolean> glide = add(new BooleanSetting("Glide", false, v -> this.entitySpeed.isShown()));
    public Setting<Boolean> accelerate = add(new BooleanSetting("Accelerate", false, v -> this.entitySpeed.isShown()));
    public Setting<Boolean> copyYaw = add(new BooleanSetting("CopyYaw", false));
    public Setting<Boolean> horseJump = add(new BooleanSetting("HorseJump", true));
    public boolean flag;
    public long num;

    public EntityControl() {
        super("EntityControl", "Helps with riding entities.", Category.MOVEMENT, new String[0]);
        this.speed.setChangeCallback(() -> {
            this.num = System.currentTimeMillis();
        });
    }

    @Listen
    public void onTickPre(TickPreEvent tickPreEvent) {
        Entity vehicle;
        double d;
        if (isNotInGame()) {
            return;
        }
        if (!MovementUtils.is2181()) {
            this.num = System.currentTimeMillis();
        }
        if (minecraftClient.player.getVehicle() != null && (vehicle = minecraftClient.player.getVehicle()) != null) {
            if (this.copyYaw.getValue().booleanValue()) {
                vehicle.setYRot(minecraftClient.player.getYRot());
            }
            if (!(vehicle instanceof net.minecraft.world.entity.LivingEntity living) || !living.getItemBySlot(EquipmentSlot.SADDLE).isEmpty() || vehicle instanceof net.minecraft.world.entity.animal.equine.AbstractHorse) {
                double doubleValue = this.speed.getValue().doubleValue();
                double floatValue = this.vertical.getValue().floatValue();
                if (doubleValue >= 20.0d) {
                    doubleValue = 19.99d;
                }
                if (this.accelerate.getValue().booleanValue()) {
                    doubleValue = MovementUtils.computeAcceleratedSpeed(this.speed.getValue().doubleValue(), 0.1d, this.speed.getValue().doubleValue(), this.num);
                }
                if (!this.entitySpeed.getValue().booleanValue() || isVehicleColliding()) {
                    return;
                }
                double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, doubleValue);
                if (this.flight.getValue().booleanValue()) {
                    if (vehicle instanceof Boat) {
                        ((Boat) vehicle).setNoGravity(true);
                    }
                    if (minecraftClient.options.keyJump.isDown()) {
                        d = floatValue;
                    } else if (this.glide.getValue().booleanValue() || InputState.isKeyBindingActive(minecraftClient.options.keySprint)) {
                        d = -floatValue;
                    } else {
                        d = this.flag ? (-0.04125000163912773d) : 0.04125000163912773d;
                    }
                } else {
                    d = tickPreEvent.getY();
                }
                tickPreEvent.setMovement(new Vec3(doubleArray2508[0], d, doubleArray2508[1]));
                this.flag = !this.flag;
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            this.num = System.currentTimeMillis();
        }
    }

    public boolean isBoatOverride(Object obj) {
        return false;
    }

    public boolean isVehicleColliding() {
        return minecraftClient.player.getVehicle() != null && minecraftClient.player.getVehicle().horizontalCollision && this.noCollision.getValue().booleanValue();
    }
}