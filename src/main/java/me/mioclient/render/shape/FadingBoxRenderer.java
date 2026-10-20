package me.mioclient.render.shape;

import me.mioclient.util.math.ColorUtils;

import java.awt.Color;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import me.mioclient.manager.RotationManager;

public class FadingBoxRenderer {
    public AABB box;
    public long num;
    public float val = 1.0f;

    public void startFadeOut() {
        this.num = System.currentTimeMillis() + 150;
    }

    public void setBlockPos(BlockPos blockPos) {
        this.box = new AABB(blockPos);
    }

    public void setBoundingBox(AABB box) {
        this.box = box;
    }

    public void setStrokeWidth(float f) {
        this.val = f;
    }

    public void renderFadingBox(PoseStack matrixStack, Color color, Color color2, float f, boolean z) {
        if (this.box == null || !RotationManager.isInFrustum(this.box)) {
            return;
        }
        float f2 = getFadeProgress(f);
        BoxRenderer.drawFilledBox(matrixStack, this.box, z ? ColorUtils.withAlpha(color, (int) (color.getAlpha() * f2)) : color);
        BoxRenderer.drawBoxOutline(matrixStack, this.box, z ? ColorUtils.withAlpha(color2, (int) (color2.getAlpha() * f2)) : color2, this.val);
        if (f2 == 0.0f) {
            this.box = null;
        }
    }

    public float getFadeProgress(float f) {
        return Mth.clamp(1.0f - (((float) Math.max(System.currentTimeMillis() - this.num, 0L)) / f), 0.0f, 1.0f);
    }

    public AABB getBox() {
        return this.box;
    }
}
