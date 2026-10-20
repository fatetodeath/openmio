package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.module.movement.obstaclepasser.ObstaclePasserMode;
import me.mioclient.util.math.Vec3dUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.movement.obstaclepasser.RateLimiter;
import me.mioclient.network.PacketSender;

import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.module.Module;
import me.mioclient.module.combat.AutoArmor;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import me.mioclient.mixin.ducks.DuckAbstractBlock;

public final class ObstaclePasser extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static AutoArmor autoArmor;
    private static ElytraFly elytraFly;

    public static AutoArmor getAutoArmor() {
        if (autoArmor == null && ClientServices.moduleInjector != null) {
            try {
                autoArmor = (AutoArmor) ClientServices.moduleInjector.getModule(AutoArmor.class);
            } catch (Exception e) {
            }
        }
        return autoArmor;
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
    public final RateLimiter obstaclePasserHelper_2;
    public Setting<Boolean> setting = add(new BooleanSetting("UseModePath", true));
    public Setting<Boolean> setting2 = add(new BooleanSetting("WaitChunks", true));
    public Setting<Integer> setting3 = add(new NumberSetting<>("TargetY", -1, -1, 320));
    public Setting<Integer> setting4 = add(new NumberSetting<>("Distance", 50, 6, 100));
    public final Stopwatch stopwatch;
    public float val;
    public BlockPos blockPos;
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public int num;
    public ObstaclePasserMode obstaclePasserMode;

    public ObstaclePasser() {
        super("ObstaclePasser", new StringTemplate().arg(String.valueOf(ChatFormatting.YELLOW)).format("Passes obstacles in your way. \n\u0001Requires either ElytraFly or Speed enabled."), Category.MOVEMENT, new String[0]);
        this.obstaclePasserHelper_2 = new RateLimiter(3);
        this.stopwatch = new Stopwatch();
        this.setting3.setHoleSnapConfig("~", HoleSnapMode.MIN);
        this.setting.setChangeCallback(() -> {
            this.obstaclePasserMode = null;
        });
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        if (isNotInGame()) {
            toggle();
            return;
        }
        this.flag = false;
        this.obstaclePasserMode = null;
        this.num = 0;
        this.val = Math.round(minecraftClient.player.getYRot());
        this.blockPos = minecraftClient.player.blockPosition();
        this.obstaclePasserHelper_2.do978();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        ClientServices.obstaclePasserHelper.cancelAll();
        AutoArmor autoArmor_lcl = getAutoArmor();
        if (this.flag && autoArmor_lcl != null && autoArmor_lcl.isToggled()) {
            autoArmor_lcl.elytra.setValue(true);
        }
        this.flag2 = false;
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (ClientServices.obstaclePasserHelper.isPathing()) {
            return;
        }
        minecraftClient.player.setYRot(this.val);
    }

    @Listen(get219= RenderConstants.num4)
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (areChunksLoaded()) {
            this.stopwatch.reset();
        }
        syncSneakState();
        if (this.obstaclePasserMode == null) {
            this.obstaclePasserMode = ObstaclePasserMode.getObstaclePasserMode478(this.val);
        }
        if (this.num > 0) {
            this.num--;
        }
        this.obstaclePasserHelper_2.recalculateLayout();
        processPathingTick();
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            this.obstaclePasserHelper_2.recordTimestamp();
        }
    }

    @Listen
    public void onInputTick(InputTickEvent tickEvent_2) {
        if ((this.setting2.getValue().booleanValue() && this.stopwatch.hasElapsedMs(750L)) || isPathing()) {
            return;
        }
        ElytraFly elytraFly_lcl = getElytraFly();
        if (shouldJump() || (!minecraftClient.player.isFallFlying() && elytraFly_lcl != null && elytraFly_lcl.isToggled())) {
            { net.minecraft.client.player.ClientInput _ci = tickEvent_2.getInput(); net.minecraft.world.entity.player.Input _p = _ci.keyPresses; _ci.keyPresses = new net.minecraft.world.entity.player.Input(_p.forward(), _p.backward(), _p.left(), _p.right(), true, _p.shift(), _p.sprint()); }
        }
        { net.minecraft.client.player.ClientInput _ci = tickEvent_2.getInput(); net.minecraft.world.entity.player.Input _p = _ci.keyPresses; _ci.keyPresses = new net.minecraft.world.entity.player.Input(true, _p.backward(), _p.left(), _p.right(), _p.jump(), _p.shift(), _p.sprint()); }
    }

    public void processPathingTick() {
        BlockPos blockPos710;
        if (ClientServices.obstaclePasserHelper.isPathing() && (blockPos710 = ClientServices.obstaclePasserHelper.getCurrentGoal()) != null) {
            if (Vec3dUtils.getHorizontalDistance(minecraftClient.player.position(), Vec3.atBottomCenterOf(blockPos710)) <= 1.0f) {
                ClientServices.obstaclePasserHelper.cancelAll();
                this.num = 10;
                ElytraFly elytraFly_lcl = getElytraFly();
                if (elytraFly_lcl != null && elytraFly_lcl.isToggled()) {
                    this.flag3 = true;
                }
            }
        }
        if (!shouldStartPathing() || ClientServices.obstaclePasserHelper.isPathing()) {
            return;
        }
        this.obstaclePasserHelper_2.do978();
        int y = this.setting3.getValue().intValue() == -1 ? this.blockPos.getY() : this.setting3.getValue().intValue();
        if (this.obstaclePasserMode != null && this.setting.getValue().booleanValue()) {
            BlockPos blockPos = null;
            for (int i = 5; i < this.setting4.getValue().intValue(); i++) {
                BlockPos base = this.obstaclePasserMode.function.apply(Integer.valueOf(i));
                blockPos = new BlockPos(base.getX(), y, base.getZ());
                if (((me.mioclient.mixin.ducks.DuckAbstractBlock)(Object) WorldUtils.getBlock(blockPos.below())).isCollidable()) {
                    break;
                }
            }
            ClientServices.obstaclePasserHelper.pathTo(blockPos);
            return;
        }
        Vec3 bottomCenterPos = Vec3.atBottomCenterOf(this.blockPos);
        Vec3 vec3d930 = getHorizontalDirection(this.val);
        BlockPos ofFloored = BlockPos.containing(bottomCenterPos.add(vec3d930.scale(Vec3dUtils.getHorizontalDistance(bottomCenterPos, minecraftClient.player.position()))));
        BlockPos blockPos2 = null;
        for (int i2 = 5; i2 < this.setting4.getValue().intValue(); i2++) {
            blockPos2 = ofFloored.offset(BlockPos.containing(vec3d930.scale(i2)));
            if (((me.mioclient.mixin.ducks.DuckAbstractBlock)(Object) WorldUtils.getBlock(blockPos2.below())).isCollidable()) {
                break;
            }
        }
        ClientServices.obstaclePasserHelper.pathTo(new BlockPos(blockPos2.getX(), y, blockPos2.getZ()));
    }

    public boolean shouldStartPathing() {
        if (this.num > 0) {
            return false;
        }
        ElytraFly elytraFly_lcl = getElytraFly();
        return minecraftClient.player.horizontalCollision || this.obstaclePasserHelper_2.isRateExceeded() || (!PlayerUtils.isReallyFallFlying() && ClientServices.antiPhaseHelper.getGroundTicks() > 3 && minecraftClient.player.onGround() && elytraFly_lcl != null && elytraFly_lcl.isToggled());
    }

    public void syncSneakState() {
        if (!ClientServices.obstaclePasserHelper.isPathing()) {
            if (this.flag) {
                this.flag = false;
            }
        } else {
            if (this.flag) {
                return;
            }
            this.flag = true;
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; PacketSender.sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), true, _cur.sprint()))); };
            { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; PacketSender.sendPacket(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), false, _cur.sprint()))); };
        }
    }

    public boolean hasElytra() {
        return minecraftClient.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
    }

    public boolean areChunksLoaded() {
        try {
            var chunkSource = minecraftClient.level.getChunkSource();
            return ChunkPos.rangeClosed(minecraftClient.player.chunkPosition(), 2).allMatch(chunkPos -> {
                return chunkSource.hasChunk(chunkPos.x(), chunkPos.z());
            }) || !this.setting2.getValue().booleanValue();
        } catch (Exception e) {
            return true;
        }
    }

    public boolean shouldJump() {
        ElytraFly elytraFly_lcl = getElytraFly();
        return this.flag3 && elytraFly_lcl != null && elytraFly_lcl.isToggled();
    }

    public boolean isPathing() {
        return ClientServices.obstaclePasserHelper.isPathing();
    }

    public boolean is929() {
        if (isToggled()) {
            return isPathing();
        }
        return false;
    }

    public Vec3 getHorizontalDirection(float f) {
        return Vec3.directionFromRotation(0.0f, f);
    }
}