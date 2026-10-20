package me.mioclient.util.math;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.runtime.ObjectMethods;

public final class GridPoint {
    public final int num;
    public final int num2;

    public GridPoint(int i, int i2) {
        this.num = i;
        this.num2 = i2;
    }

    public double getSquaredDistanceTo(GridPoint positionData) {
        return Math.pow(this.num - positionData.num, 2.0d) + Math.pow(this.num2 - positionData.num2, 2.0d);
    }

    public double getDistanceTo(GridPoint positionData) {
        return Math.sqrt(getSquaredDistanceTo(positionData));
    }

    public GridPoint addPoint(GridPoint positionData) {
        return new GridPoint(this.num + positionData.num, this.num2 + positionData.num2);
    }

    public int get476() {
        return this.num;
    }

    public int getGridY() {
        return this.num2;
    }
}
