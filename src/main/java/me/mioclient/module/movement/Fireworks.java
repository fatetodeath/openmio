package me.mioclient.module.movement;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import java.util.concurrent.TimeUnit;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.InteractBlockEvent;
import me.mioclient.event.events.interaction.InteractItemEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.sound.PlayEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.mixin.ducks.DuckFireworkEntity;
import me.mioclient.module.Module;
import me.mioclient.module.exploit.RocketExtender;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public final class Fireworks extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static RocketExtender rocketExtender;
    private static ElytraFly elytraFly;

    public static RocketExtender getRocketExtender() {
        if (rocketExtender == null && ClientServices.moduleInjector != null) {
            try {
                rocketExtender = (RocketExtender) ClientServices.moduleInjector.getModule(RocketExtender.class);
            } catch (Exception e) {
            }
        }
        return rocketExtender;
    }

    public static ElytraFly getElytraFly() {
        if (elytraFly == null && ClientServices.moduleInjector != null) {
            try {
                elytraFly = (ElytraFly) ClientServices.moduleInjector.getModule(ElytraFly.class);
            } catch (Exception e) {
            }
        }
        return elytraFly;
    }
    public Setting<Boolean> antiWaste = add(new BooleanSetting("AntiWaste", true));
    public Setting<Boolean> boost = add(new BooleanSetting("Boost", false).setVisible());
    public Setting<Boolean> withElytraFly = add(new BooleanSetting("WithElytraFly", true).requiresSettings(this.boost));
    public Setting<Float> boostFactor = add(new NumberSetting<>("BoostFactor", 0.0f, 0.0f, 3.0f).requiresSettings(this.boost));
    public Setting<Boolean> autoLaunch = add(new BooleanSetting("AutoLaunch", false).setVisible());
    public Setting<Boolean> onlyElytraFly = add(new BooleanSetting("OnlyElytraFly", true).requiresSettings(this.autoLaunch));
    public Setting<Boolean> onlyFirst = add(new BooleanSetting("OnlyFirst", false).requiresSettings(this.autoLaunch));
    public Setting<Boolean> await = add(new BooleanSetting("Await", false).requiresSettings(this.autoLaunch));
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 0.0f, 0.0f, 5.0f).setSuffix("s").setHoleSnapConfig("Auto", HoleSnapMode.MIN).requiresSettings(this.autoLaunch));
    public Setting<Boolean> fastLaunch = add(new BooleanSetting("FastLaunch", false).setVisible());
    public Setting<Integer> ticks = add(new NumberSetting<>("Ticks", 10, 3, 40).requiresSettings(this.fastLaunch));
    public Setting<Boolean> flight = add(new BooleanSetting("Flight", false).setVisible().requiresSettings(this.fastLaunch));
    public Setting<Float> horizontal = add(new NumberSetting<>("Horizontal", 1.0f, 0.0f, 3.0f).requiresSettings(this.fastLaunch, this.flight));
    public Setting<Float> vertical = add(new NumberSetting<>("Vertical", 0.2f, 0.0f, 1.0f).requiresSettings(this.fastLaunch, this.flight));
    public final Stopwatch stopwatch;
    public boolean flag;
    public int num;
    public int num2;
    public boolean flag2;
    public boolean flag3;

    public Fireworks() {
        super("Fireworks", "Enhances the usage of firework rockets.", Category.MOVEMENT, "rockets");
        this.stopwatch = new Stopwatch();
        this.delay.setHoleSnapConfig("Auto", HoleSnapMode.MIN);
    }

    @Listen
    public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
        if (this.antiWaste.getValue().booleanValue()) {
            if (minecraftClient.player.getItemInHand(interactBlockEvent.getHand()).is(Items.FIREWORK_ROCKET)) {
                minecraftClient.gameMode.useItem(minecraftClient.player, interactBlockEvent.getHand());
                minecraftClient.player.swing(interactBlockEvent.getHand());
                interactBlockEvent.cancel();
            }
        }
    }

    @Listen
    public void onInteractItem(InteractItemEvent interactItemEvent) {
        if (shouldFastLaunch()) {
            if (minecraftClient.player.isHolding(Items.FIREWORK_ROCKET)) {
                this.num2 = 0;
                swapElytraAndFly();
                this.num = this.ticks.getValue().intValue();
                this.flag3 = true;
            }
        }
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (this.flag2) {
            minecraftClient.player.setShiftKeyDown(false);
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; PacketSender.sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), false, _cur.sprint()))); };
            this.flag2 = false;
            if (!PlayerUtils.hasElytraEquipped(minecraftClient.player)) {
                minecraftClient.player.stopFallFlying();
            }
        }
        if (this.num > 0) {
            swapElytraAndFly();
            this.num--;
            this.flag2 = this.num == 0;
        }
        if (this.autoLaunch.getValue().booleanValue()) {
            ElytraFly elytraFly_lcl = getElytraFly();
            RocketExtender rocketExtender_lcl = getRocketExtender();
            if ((elytraFly_lcl != null && elytraFly_lcl.isToggled()) || !this.onlyElytraFly.getValue().booleanValue()) {
                if (!minecraftClient.player.isFallFlying()) {
                    this.stopwatch.setTime(-1L);
                }
                boolean z = (this.stopwatch.hasElapsed((double) this.delay.getValue().floatValue(), TimeUnit.SECONDS) && (rocketExtender_lcl == null || !rocketExtender_lcl.isToggled() || rocketExtender_lcl.num == -1 || rocketExtender_lcl.blockPos == null)) ? false : true;
                if (hasActiveFirework()) {
                    z = true;
                }
                if (z) {
                    return;
                }
                if (!minecraftClient.player.isFallFlying()) {
                    this.flag = false;
                }
                if (this.flag && this.onlyFirst.getValue().booleanValue()) {
                    return;
                }
                autoUseFirework();
            }
        }
    }

    @Listen(get219= RenderConstants.num4)
    public void onMove(MoveEvent moveEvent) {
        if (this.flight.getValue().booleanValue() && this.num > 1 && hasActiveFirework()) {
            do138(moveEvent);
        }
        if (minecraftClient.player.isFallFlying() && this.await.getValue().booleanValue() && this.autoLaunch.getValue().booleanValue() && !hasActiveFirework()) {
            moveEvent.setMovement(new Vec3(0.0d, 0.0d, 0.0d));
        }
    }

    @Listen
    public void onPlay(PlayEvent playEvent) {
        if (this.num <= 0 || playEvent.getSoundInstance() == null || playEvent.getSoundInstance().getIdentifier() == null || !playEvent.getSoundInstance().getIdentifier().toString().contains("item.armor.equip")) {
            return;
        }
        playEvent.cancel();
    }

    public void do138(MoveEvent moveEvent) {
        float intBitsToFloat = (-0.001f);
        if (minecraftClient.player.input.keyPresses.jump()) {
            intBitsToFloat = this.vertical.getValue().floatValue();
        } else if (minecraftClient.player.input.keyPresses.shift()) {
            intBitsToFloat = -this.vertical.getValue().floatValue();
        }
        float floatValue = this.horizontal.getValue().floatValue();
        if (floatValue > 1.6d) {
            floatValue = Mth.clamp(1.6f + (this.num2 * 0.1f), 1.6f, floatValue);
            this.num2++;
        }
        double[] doubleArray2507 = MovementUtils.applyVelocity(moveEvent, floatValue);
        Vec3 vec3d = new Vec3(doubleArray2507[0], intBitsToFloat, doubleArray2507[1]);
        minecraftClient.player.setDeltaMovement(vec3d);
        moveEvent.setMovement(vec3d);
    }

    public void autoUseFirework() {
        if (this.autoLaunch.getValue().booleanValue() && minecraftClient.player.isFallFlying()) {
            InteractionHand hand450 = InventoryUtils.getHandWith(Items.FIREWORK_ROCKET);
            int i = minecraftClient.player.getInventory().getSelectedSlot();
            int i2 = InventoryUtils.findSlot(Items.FIREWORK_ROCKET);
            int i3 = InventoryUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (hand450 != null) {
                minecraftClient.gameMode.useItem(minecraftClient.player, hand450);
                this.stopwatch.reset();
            } else if (i2 != -1) {
                boolean z = i3 == -1;
                equipSlot(z ? i2 : i3, z);
                minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.MAIN_HAND);
                RocketExtender rocketExtender_lcl2 = getRocketExtender();
                if (rocketExtender_lcl2 != null) {
                    rocketExtender_lcl2.vec3d = minecraftClient.player.position();
                }
                equipSlot(z ? i2 : i, z);
                this.stopwatch.reset();
            }
            this.flag = true;
        }
    }

    public void equipSlot(int i, boolean z) {
        if (z) {
            InventoryUtils.swapToHotbar(i);
        } else {
            InventoryUtils.selectSlot(i);
        }
    }

    public boolean hasActiveFirework() {
        if (!this.stopwatch.hasElapsedMs(500L)) {
            return true;
        }
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if ((entity instanceof DuckFireworkEntity) && ((DuckFireworkEntity) entity).mio$getShooter() == minecraftClient.player) {
                return true;
            }
        }
        return false;
    }

    public float getBoostFactor(boolean z) {
        if (!isToggled() || !this.boost.getValue().booleanValue()) {
            return 0.0f;
        }
        if ((z || this.withElytraFly.getValue().booleanValue()) && hasActiveFirework()) {
            return this.boostFactor.getValue().floatValue();
        }
        return 0.0f;
    }

    public int getRemainingTicks() {
        return this.num;
    }

    public boolean shouldFastLaunch() {
        return (PlayerUtils.hasElytraEquipped(minecraftClient.player) || !isToggled() || !this.fastLaunch.getValue().booleanValue() || minecraftClient.player.onGround() || InventoryUtils.findSlot(Items.ELYTRA) == -1) ? false : true;
    }

    public void swapElytraAndFly() {
        int i = InventoryUtils.findInHotbar(Items.ELYTRA);
        boolean z = i == -1;
        if (i == -1) {
            i = InventoryUtils.findSlot(Items.ELYTRA);
        }
        if (i == -1) {
            return;
        }
        int i2 = i;
        if (z) {
            i = 0;
            minecraftClient.gameMode.handleContainerInput(0, i2, 0, ContainerInput.SWAP, minecraftClient.player);
        }
        minecraftClient.gameMode.handleContainerInput(0, 6, i, ContainerInput.SWAP, minecraftClient.player);
        PacketSender.sendStartFallFlying();
        minecraftClient.player.startFallFlying();
        minecraftClient.gameMode.handleContainerInput(0, 6, i, ContainerInput.SWAP, minecraftClient.player);
        if (z) {
            minecraftClient.gameMode.handleContainerInput(0, i2, 0, ContainerInput.SWAP, minecraftClient.player);
        }
    }
}