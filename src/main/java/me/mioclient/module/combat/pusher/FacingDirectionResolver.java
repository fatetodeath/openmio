package me.mioclient.module.combat.pusher;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

public final class FacingDirectionResolver {
    public Direction resolveFacingDirection(float[] fArr) {
        float intBitsToFloat = fArr[1] * 0.017453292f;
        float intBitsToFloat2 = (-fArr[0]) * 0.017453292f;
        float sin = Mth.sin(intBitsToFloat);
        float cos = Mth.cos(intBitsToFloat);
        float sin2 = Mth.sin(intBitsToFloat2);
        float cos2 = Mth.cos(intBitsToFloat2);
        boolean z = sin2 > 0.0f;
        boolean z2 = sin < 0.0f;
        boolean z3 = cos2 > 0.0f;
        float f = z ? sin2 : -sin2;
        float f2 = z2 ? -sin : sin;
        float f3 = z3 ? cos2 : -cos2;
        float f4 = f * cos;
        float f5 = f3 * cos;
        Direction direction = z ? Direction.EAST : Direction.WEST;
        Direction direction2 = z2 ? Direction.UP : Direction.DOWN;
        return f > f3 ? f2 > f4 ? direction2 : direction : f2 > f5 ? direction2 : z3 ? Direction.SOUTH : Direction.NORTH;
    }
}