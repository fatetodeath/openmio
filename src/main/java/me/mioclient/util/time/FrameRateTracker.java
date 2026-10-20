package me.mioclient.util.time;

import me.mioclient.api.ClientAccessor;

import me.mioclient.event.EventBus;

import java.util.LinkedList;
import net.minecraft.util.Mth;

public final class FrameRateTracker implements ClientAccessor {
    public final LinkedList<Long> linkedList = new LinkedList<>();
    public int num;

    public FrameRateTracker() {
        eventBus.register(this);
    }

    public void updateFrameCount() {
        long nanoTime = System.nanoTime();
        this.linkedList.add(Long.valueOf(nanoTime));
        while (nanoTime - this.linkedList.getFirst().longValue() > 1000000000) {
            this.linkedList.remove();
        }
        this.num = this.linkedList.size();
    }

    public int getFrameRate() {
        return this.num;
    }

    public float getFrameDelta(float f) {
        return Mth.clamp(1.0f / (this.num * f), 0.0f, 1.0f);
    }
}
