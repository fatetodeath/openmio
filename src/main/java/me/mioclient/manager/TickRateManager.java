package me.mioclient.manager;

import me.mioclient.util.player.PlayerTickDriver;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.concurrent.ConcurrentHashMap;
import me.mioclient.event.events.client.EnableEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.render.RenderConstants;

public class TickRateManager implements ClientAccessor {
    public final ConcurrentHashMap<Module, Boolean> concurrentHashMap = new ConcurrentHashMap<>();
    public float val = 1.0f;

    
    public static class Inner extends TickRateManager {
        @Listen(get219= RenderConstants.num5)
        public void onTick(TickEvent tickEvent) {
            if (getTickRate() == 1.0f) {
                return;
            }
            for (int i = 1; i <= getTickRate() - 1.0f; i++) {
                PlayerTickDriver.tickOffhand();
            }
        }
    }

    public TickRateManager() {
        eventBus.register(this);
    }

    @Listen
    public void onEnable(EnableEvent enableEvent) {
        if (enableEvent.getModule().isToggled()) {
            return;
        }
        releaseTickRate(enableEvent.getModule());
    }

    public void releaseTickRate(Module module) {
        this.concurrentHashMap.compute(module, (module2, bool) -> {
            if (Boolean.FALSE.equals(bool)) {
                setTickRate(1.0f);
            }
            return true;
        });
    }

    public void requestTickRate(Module module, float f) {
        this.concurrentHashMap.compute(module, (module2, bool) -> {
            return false;
        });
        setTickRate(f);
    }

    public float getTickRate() {
        return this.val;
    }

    public void setTickRate(float f) {
        this.val = f;
    }
}
