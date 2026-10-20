package me.mioclient.module.movement;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.player.SpeedMine;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class FastSwim extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving speedmine null (NPE). */
    private static SpeedMine speedmine;

    public static SpeedMine getSpeedmine() {
        if (speedmine == null && ClientServices.moduleInjector != null) {
            try {
                speedmine = (SpeedMine) ClientServices.moduleInjector.getModule(SpeedMine.class);
            } catch (Exception e) {
            }
        }
        return speedmine;
    }
    public Setting<Boolean> water = add(new BooleanSetting("Water", false).setVisible());
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.1f, 3.0f).requiresSettings(this.water));
    public Setting<Boolean> lava = add(new BooleanSetting("Lava", true).setVisible());
    public Setting<Float> speed2 = add(new NumberSetting<>("Speed", 1.0f, 0.1f, 3.0f).requiresSettings(this.lava));
    public Setting<Boolean> vertical = add(new BooleanSetting("Vertical", false).setVisible());
    public Setting<Float> vSpeed = add(new NumberSetting<>("VSpeed", 1.0f, 0.1f, 2.0f).requiresSettings(this.vertical));
    public Setting<Float> glide = add(new NumberSetting<>("Glide", 0.05f, 0.0f, 5.0f));
    public Setting<Boolean> accelerate = add(new BooleanSetting("Accelerate", false).setVisible());
    public Setting<Float> accelMin = add(new NumberSetting<>("AccelMin", 1.0f, 0.0f, 5.0f).requiresSettings(this.accelerate));
    public Setting<Float> accelTime = add(new NumberSetting<>("AccelTime", 1.0f, 0.1f, 10.0f).setSuffix("s").requiresSettings(this.accelerate));
    public long num;
    public boolean flag;

    public FastSwim() {
        super("FastSwim", "Makes you move faster while in liquids.", Category.MOVEMENT, new String[0]);
        this.speed.setConfigName("WaterSpeed");
        this.speed2.setConfigName("LavaSpeed");
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (((minecraftClient.player.isInLava() && this.lava.getValue().booleanValue()) || (PlayerUtils.isInWater(minecraftClient.player) && this.water.getValue().booleanValue())) && !minecraftClient.player.isFallFlying() && minecraftClient.player.onGround()) {
            minecraftClient.player.setDeltaMovement(0.0d, minecraftClient.player.getDeltaMovement().y, 0.0d);
        }
    }

    @Listen
    public void onMove(MoveEvent moveEvent) {
        SpeedMine speedmine_lcl = getSpeedmine();
        boolean z = speedmine_lcl != null && speedmine_lcl.isToggled() && ((double) speedmine_lcl.getMiningProgress()) + speedmine_lcl.getCurrentMiningSpeed() >= ((double) speedmine_lcl.damage.getValue().floatValue()) - 0.1d && speedmine_lcl.getBreakingPos() != null && WorldUtils.isBreakable(speedmine_lcl.getBreakingPos());
        if (!((minecraftClient.player.isInLava() && this.lava.getValue().booleanValue()) || (PlayerUtils.isInWater(minecraftClient.player) && this.water.getValue().booleanValue())) || minecraftClient.player.isFallFlying() || !ClientServices.holeSnapRenderHelper.getSetbackStopwatch().hasElapsedMs(500L) || z) {
            this.num = System.currentTimeMillis();
            return;
        }
        if (!MovementUtils.is2181()) {
            this.num = System.currentTimeMillis();
        }
        float intBitsToFloat = this.vertical.getValue().booleanValue() ? 0.12f * this.vSpeed.getValue().floatValue() : 0.0f;
        float intBitsToFloat2 = 0.12f * this.accelMin.getValue().floatValue();
        double intBitsToFloat3 = 0.12f * this.speed2.getValue().floatValue();
        if (this.water.getValue().booleanValue() && PlayerUtils.isInWater(minecraftClient.player)) {
            intBitsToFloat3 = 0.12f * this.speed.getValue().floatValue();
        }
        if (this.accelerate.getValue().booleanValue() && intBitsToFloat2 < intBitsToFloat3) {
            intBitsToFloat3 = Math.min(intBitsToFloat2 + ((intBitsToFloat3 - intBitsToFloat2) * Mth.clamp(((float) (System.currentTimeMillis() - this.num)) / (this.accelTime.getValue().floatValue() * 1000.0f), 0.0f, 1.0f)), intBitsToFloat3);
        }
        if (minecraftClient.player.input.keyPresses.jump() && !this.flag) {
            moveEvent.setY(intBitsToFloat);
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, intBitsToFloat));
        } else if (minecraftClient.player.input.keyPresses.shift()) {
            moveEvent.setY(-intBitsToFloat);
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, -intBitsToFloat));
            intBitsToFloat3 /= 2.0d;
        } else {
            moveEvent.setY(0.0d);
            minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, 0.0d));
            if (!minecraftClient.player.verticalCollision && this.glide.getValue().floatValue() != 0.0f) {
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().add(0.0d, -this.glide.getValue().floatValue(), 0.0d));
                moveEvent.setY(minecraftClient.player.getDeltaMovement().y);
            }
        }
        double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, intBitsToFloat3);
        minecraftClient.player.setDeltaMovement(doubleArray2507[0], minecraftClient.player.getDeltaMovement().y, doubleArray2507[1]);
    }
}