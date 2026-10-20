package me.mioclient.module.movement.speed;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.player.MovementUtils;

import me.mioclient.command.FakePlayerCommand;
import me.mioclient.event.events.player.MoveEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.movement.Speed;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;

public class GrimSpeedHandler extends SpeedModeHandler {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static AntiCheat antiCheat;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }
    public AABB box;

    public GrimSpeedHandler(Speed speed) {
        super(speed);
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void applyMovement(MoveEvent moveEvent) {
        if (this.box != null && MovementUtils.is2181()) {
            int i = 0;
        for (BlockEntity blockEntity : WorldUtils.getLoadedBlockEntities()) {
                if (blockEntity instanceof ShulkerBoxBlockEntity) {
                    ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity) blockEntity;
                    // 26.2: AABB.expand -> inflate, AABB.offset -> move, BlockEntity.position -> getBlockPos,
                    // ShulkerBoxBlockEntity.getAnimationStage/AnimationStage -> getAnimationStatus/AnimationStatus
                    AntiCheat antiCheat_lcl = getAntiCheat();
                    boolean isF2b2t = antiCheat_lcl != null && antiCheat_lcl.isF2b2t();
                    if (this.box.inflate(0.5d).intersects(shulkerBoxBlockEntity.getBoundingBox(Blocks.SHULKER_BOX.defaultBlockState()).move(blockEntity.getBlockPos())) && shulkerBoxBlockEntity.getAnimationStatus() != ShulkerBoxBlockEntity.AnimationStatus.CLOSED) {
                        i += isF2b2t ? 0 : 4;
                    }
                }
            }
            AntiCheat antiCheat_lcl2 = getAntiCheat();
            float intBitsToFloat = (antiCheat_lcl2 != null && antiCheat_lcl2.isF2b2t()) ? 0.0f : 1.0f;
            // 26.2: ClientLevel.getEntities() is protected -> entitiesForRendering()
            for (Entity entity : minecraftClient.level.entitiesForRendering()) {
                if (!(entity instanceof FakePlayerCommand.RemotePlayer) && entity != minecraftClient.player && entity.isPushable()) {
                    // 26.2: AABB.expand -> inflate
                    if (entity.getBoundingBox().intersects(this.box.inflate(intBitsToFloat))) {
                        if (entity instanceof Boat) {
                            i += 4;
                        }
                        i++;
                    }
                }
            }
            if (i == 0) {
                return;
            }
            // 26.2: Vec3.multiply(double) -> scale(double)
            moveEvent.setMovement(moveEvent.getMovement().add(moveEvent.getMovement().normalize().with(Direction.Axis.Y, 0.0d).scale(0.08d * i)));
        }
    }

    @Override // me.mioclient.module.movement.speed.SpeedModeHandler
    public void applyAntiKickMotion(MotionEvent motionEvent) {
        if (motionEvent.getPhase() == KeyPearlMode.Post) {
            this.box = minecraftClient.player.getBoundingBox();
        }
    }
}