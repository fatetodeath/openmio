package me.mioclient.module.player.speedmine;

import me.mioclient.util.world.WorldUtils;

import me.mioclient.render.shape.BoxRenderer;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.mioclient.event.events.render.MatrixStackEvent;

public final class SpeedMineTarget implements ClientAccessor {
    public final BlockPos blockPos;
    public float val;
    public float val2;

    public SpeedMineTarget(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public void do2141(SpeedMine speedMine, MatrixStackEvent matrixStackEvent) {
        VoxelShape outlineShape = minecraftClient.level.getBlockState(this.blockPos).getShape(minecraftClient.level, this.blockPos);
        if (outlineShape.isEmpty() || !WorldUtils.isBreakable(this.blockPos)) {
            return;
        }
        float f = get2142();
        Color[] colorArray1773 = speedMine.colorMode.getValue().getRenderColors(speedMine, f);
        AABB offset = speedMine.renderMode.getValue().getRenderBox(speedMine, outlineShape.bounds(), f).move(this.blockPos);
        BoxRenderer.drawFilledBox(matrixStackEvent.getMatrixStack(), offset, colorArray1773[0]);
        BoxRenderer.drawBoxOutline(matrixStackEvent.getMatrixStack(), offset, colorArray1773[1], speedMine.lineWidth.getValue().floatValue());
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public float get2142() {
        return Mth.clamp(Mth.lerp(RenderUtil.getPartialTick(), this.val, this.val2), 0.0f, 1.0f);
    }

    public void addMiningProgress(float f) {
        setMiningProgress(this.val2 + f);
    }

    public void setMiningProgress(float f) {
        this.val = this.val2;
        this.val2 = f;
    }
}