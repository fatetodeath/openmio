package me.mioclient.module.render.breakhighlight;

import me.mioclient.util.time.Stopwatch;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public final class BreakProgressTracker {
    public final Stopwatch stopwatch = new Stopwatch();
    public final int entityId;
    public final BlockPos blockPos;
    public float val;
    public float val2;
    public int num;

    public BreakProgressTracker(int i, BlockPos blockPos) {
        this.entityId = i;
        this.blockPos = blockPos;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public Entity getEntity() {
        return Minecraft.getInstance().level.getEntity(this.entityId);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public float get2142() {
        return this.val;
    }

    public float get2814(float f) {
        return Mth.lerp(f, this.val2, this.val);
    }

    public void setMiningProgress(float f) {
        if (this.val == f) {
            return;
        }
        if (f == 0.0f) {
            this.val2 = 0.0f;
        } else {
            this.val2 = this.val;
        }
        resetTimer();
        this.val = Mth.clamp(f, 0.0f, 1.0f);
    }

    public int get2093() {
        return this.num;
    }

    public void setBreakerId(int i) {
        this.num = i;
    }

    public void resetTimer() {
        this.stopwatch.reset();
    }

    public boolean is2817() {
        Entity entity181;
        if ((this.val == 1.0f && this.stopwatch.hasElapsedMs(2000L)) || (entity181 = getEntity()) == null) {
            return true;
        }
        return entity181.getEyePosition().distanceToSqr(Vec3.atCenterOf(this.blockPos)) >= 64.0d;
    }
}