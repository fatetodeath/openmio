package me.mioclient.util.anim;

import me.mioclient.api.ClientServices;

import java.util.function.Supplier;
import net.minecraft.util.Mth;

public class AnimationProgress extends InterpolatedFloat {
    public AnimationProgress(float f, boolean z) {
        super(f, z);
    }

    public AnimationProgress(float f) {
        super(f);
    }

    public AnimationProgress(Supplier<Float> supplier, boolean z) {
        super(supplier, z);
    }

    @Override // me.mioclient.util.anim.InterpolatedFloat
    public float getValue() {
        float f = ClientServices.hitmarkerHelper.getFrameDelta(0.5f) * this.supplier.get().floatValue();
        if (this.flag) {
            double longBitsToDouble = 1.0d;
            float intBitsToFloat = f - 1.0f;
            f = (float) (longBitsToDouble - (intBitsToFloat * Math.pow(intBitsToFloat, 3.0d)));
        }
        this.val = this.val3 + ((this.val - this.val3) * f);
        if (this.val == this.val2 || Math.abs(this.val2 - this.val) < 0.01d) {
            setImmediate(this.val2);
        }
        if (ClientServices.hitmarkerHelper.getFrameRate() <= 15) {
            this.val = this.val2;
        }
        return Mth.clamp(this.val - 1.0f, 0.0f, 1.0f);
    }

    @Override // me.mioclient.util.anim.InterpolatedFloat
    public void animate(float f) {
        super.animate(f + 1.0f);
    }

    public float getRawValue() {
        return this.val;
    }

    public void animateToggle(boolean z) {
        animate(z ? 1.0f : 0.0f);
    }

    public void setToggledImmediate(boolean z) {
        setImmediate(z ? 2.0f : 1.0f);
    }

    public void reset() {
        animate(0.0f);
    }
}
