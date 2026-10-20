package me.mioclient.module.movement;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.math.Vec3dUtils;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.render.RenderUtil;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class LongJump extends Module {
    public Setting<Float> jumpSpeed = add(new NumberSetting<>("JumpSpeed", 1.0f, 0.0f, 5.0f));
    public Setting<Boolean> inLiquid = add(new BooleanSetting("InLiquid", false));
    public Setting<Boolean> useTimer = add(new BooleanSetting("UseTimer", true));
    public Setting<Boolean> autoDisable = add(new BooleanSetting("AutoDisable", true));
    public double val;
    public double val2;
    public int num;
    public boolean flag;

    
    public enum Mode implements NamedValue {
        PLAIN("Plain"),
        GRIMHOP("GrimHop");

        public final String name;

        Mode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public LongJump() {
        super("LongJump", "Makes you jump fast 'n far.", Category.MOVEMENT, new String[0]);
        this.num = 4;
    }

    @Listen
    
    public void applyMovement(MoveEvent moveEvent) {
        double d;
        double d2;
        if (hasHeadCollision() || isInLiquidBlocked() || minecraftClient.player.isFallFlying() || minecraftClient.player.isSpectator()) {
            return;
        }
        double d3 = moveEvent.getY();
        if (this.useTimer.getValue().booleanValue() && ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(250L)) {
            ClientServices.holeSnapHelper.requestTickRate(this, 1.0888f);
        } else if (this.useTimer.getValue().booleanValue()) {
            ClientServices.holeSnapHelper.releaseTickRate(this);
        }
        if (!ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(250L) && this.autoDisable.getValue().booleanValue()) {
            toggle();
        }
        double intBitsToFloat = 2.0f * this.jumpSpeed.getValue().floatValue();
        if (this.num == 1 && MovementUtils.is2181()) {
            this.val = (1.35d * MovementUtils.computeMoveSpeed(false, 0.2873d * intBitsToFloat)) - 0.01d;
        } else if (this.num == 2 && MovementUtils.is2181()) {
            d3 = 0.42d + MovementUtils.getJumpBoostBonus();
            this.val *= this.flag ? 1.6835d : 1.395d;
        } else if (this.num == 3) {
            this.val = this.val2 - (0.6600000262260437d * (this.val2 - MovementUtils.computeMoveSpeed(true, 0.2873d * intBitsToFloat)));
            this.flag = !this.flag;
        } else {
            if (!minecraftClient.player.verticalCollision) {
            }
            if (this.num > 0) {
                this.num = MovementUtils.is2181() ? 1 : 0;
            }
            this.val = this.val2 - (this.val2 / 159.0d);
        }
        this.val = Math.max(this.val, MovementUtils.computeMoveSpeed(false, 0.2873d * intBitsToFloat));
        if (MovementUtils.is2181()) {
            double[] doubleArray2508 = MovementUtils.computeVelocity(minecraftClient.player.getYRot(), minecraftClient.player.input, this.val);
            d = doubleArray2508[0];
            d2 = doubleArray2508[1];
        } else {
            d = 0.0d;
            d2 = 0.0d;
        }
        moveEvent.setMovement(new Vec3(d, d3, d2));
        if (MovementUtils.is2181()) {
            this.num++;
        }
    }

    @Listen(get219= RenderConstants.num2)
    public void onMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post) {
            return;
        }
        if (!MovementUtils.is2181()) {
            this.num = 4;
        }
        double x = minecraftClient.player.getX() - minecraftClient.player.xo;
        double z = minecraftClient.player.getZ() - minecraftClient.player.zo;
        this.val2 = Math.sqrt((x * x) + (z * z));
    }

    public boolean isInLiquidBlocked() {
        return PlayerUtils.isInLiquid(minecraftClient.player) && !this.inLiquid.getValue().booleanValue();
    }

    public boolean hasHeadCollision() {
        AABB boundingBox = minecraftClient.player.getBoundingBox();
        return !minecraftClient.level.noCollision(minecraftClient.player, new AABB(minecraftClient.player.blockPosition().getX(), boundingBox.minY, minecraftClient.player.blockPosition().getZ(), minecraftClient.player.blockPosition().getX() + 1.0d, boundingBox.maxY, minecraftClient.player.blockPosition().getZ() + 1.0d).deflate(1.0E-7d));
    }
}