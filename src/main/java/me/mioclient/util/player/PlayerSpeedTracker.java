package me.mioclient.util.player;

import me.mioclient.api.ClientServices;

import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.ArrayDeque;
import java.util.Iterator;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;

public class PlayerSpeedTracker implements ClientAccessor {
    public double val;
    public double val2;
    public final ArrayDeque<Double> arrayDeque = new ArrayDeque<>();
    public Stopwatch stopwatch = new Stopwatch();

    public PlayerSpeedTracker() {
        eventBus.register(this);
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        this.val = 0.0d;
        this.val2 = 0.0d;
        this.arrayDeque.clear();
        this.stopwatch.reset();
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        double d = getInstantSpeed();
        if (d > 0.0d || minecraftClient.player.tickCount % 4 == 0) {
            this.arrayDeque.add(Double.valueOf(d * ClientServices.holeSnapHelper.getTickRate()));
        } else {
            this.arrayDeque.pollFirst();
        }
        while (!this.arrayDeque.isEmpty() && this.arrayDeque.size() > 9) {
            this.arrayDeque.poll();
        }
        double d2 = 0.0d;
        Iterator<Double> it = this.arrayDeque.iterator();
        while (it.hasNext()) {
            d2 += it.next().doubleValue();
        }
        this.val = this.arrayDeque.size() == 0 ? 0.0d : d2 / this.arrayDeque.size();
        if (this.val < 21.0d) {
            this.val2 = this.val;
        } else if (this.stopwatch.hasElapsedMs(50L)) {
            this.val2 = this.val;
            this.stopwatch.reset();
        }
    }

    public double getEffectiveSpeed() {
        return this.val;
    }

    public double getSmoothedSpeed() {
        return this.val2;
    }

    public double getInstantSpeed() {
        return Math.hypot(minecraftClient.player.isPassenger() ? minecraftClient.player.getVehicle().getX() - minecraftClient.player.getVehicle().xo : minecraftClient.player.getX() - minecraftClient.player.xo, minecraftClient.player.isPassenger() ? minecraftClient.player.getVehicle().getZ() - minecraftClient.player.getVehicle().zo : minecraftClient.player.getZ() - minecraftClient.player.zo) * 20.0d * 3.6d;
    }
}
