package me.mioclient.module.combat;

import me.mioclient.util.math.MathConstants;

import me.mioclient.render.RenderUtil;

import java.util.Comparator;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class AimAssist extends Module {
    public Setting<Float> setting;
    public Setting<Float> setting2;
    public Setting<Float> setting3;
    public Setting<Boolean> setting4;
    public Setting<Boolean> setting5;
    public Setting<Boolean> setting6;
    public Setting<Boolean> setting7;
    public Setting<Boolean> setting8;
    public AbstractClientPlayer abstractClientPlayerEntity;

    public AimAssist() {
        super("AimAssist", "Helps you aiming at players.", Category.COMBAT, new String[0]);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (this.setting == null || this.setting2 == null || this.setting3 == null || this.setting4 == null || this.setting5 == null || this.setting6 == null || this.setting7 == null || this.setting8 == null) {
            return;
        }
        if (minecraftClient.gui.screen() != null) {
            return;
        }
        if ((minecraftClient.player.getMainHandItem().getItem() instanceof AxeItem) || minecraftClient.player.getMainHandItem().is(holder -> holder.is(ItemTags.SWORDS)) || !this.setting4.getValue().booleanValue()) {
            if (this.abstractClientPlayerEntity == null || !this.setting6.getValue().booleanValue()) {
                this.abstractClientPlayerEntity = (AbstractClientPlayer) minecraftClient.level.players().stream().sorted(Comparator.comparing(abstractClientPlayerEntity -> {
                    return Float.valueOf(minecraftClient.player.distanceTo((Entity) abstractClientPlayerEntity));
                })).filter((v1) -> {
                    return meetsTargetCriteria(v1);
                }).findFirst().orElse(null);
            }
            if (!meetsTargetCriteria(this.abstractClientPlayerEntity)) {
                this.abstractClientPlayerEntity = null;
                return;
            }
            if (minecraftClient.crosshairPickEntity == this.abstractClientPlayerEntity) {
                return;
            }
            if (this.setting8.getValue().booleanValue()) {
                float[] floatArray2484 = RotationManager.getRotationsTo(this.abstractClientPlayerEntity.getBoundingBox().getCenter());
                minecraftClient.player.setYRot(floatArray2484[0]);
                minecraftClient.player.setXRot(floatArray2484[1]);
                return;
            }
            double d = getAimYawDelta(this.abstractClientPlayerEntity);
            if (d > 1.0d || d < (-1.0d)) {
                minecraftClient.player.setYRot(minecraftClient.player.getYRot(RenderUtil.getPartialTick()) - ((float) ((d * this.setting2.getValue().floatValue()) * 0.009999999776482582d)));
            }
        }
    }

    
    public boolean meetsTargetCriteria(Entity entity) {
        if ((entity instanceof Player) && entity != minecraftClient.player && entity.isAlive() && (!this.setting5.getValue().booleanValue() || !entity.isInvisible())) {
            if (isWithinFovAngle(entity, this.setting3.getValue().floatValue()) && minecraftClient.player.distanceTo(entity) <= this.setting.getValue().floatValue()) {
                if (this.setting7.getValue().booleanValue()) {
                }
                if (hasLineOfSight(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasLineOfSight(Entity entity) {
        Vec3 vec3d = new Vec3(minecraftClient.player.getX(), minecraftClient.player.getY() + minecraftClient.player.getEyeHeight(), minecraftClient.player.getZ());
        if (minecraftClient.level.clip(new ClipContext(vec3d, new Vec3(entity.getX(), entity.getY(), entity.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS) {
            return true;
        }
        return minecraftClient.level.clip(new ClipContext(vec3d, new Vec3(entity.getX(), entity.getY() + ((double) entity.getEyeHeight()), entity.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS;
    }

    public static double getAimYawDelta(Entity entity) {
        return ((((minecraftClient.player.getYRot(RenderUtil.getPartialTick()) - getEntityYawAngle(entity)) % MathConstants.num3) + 540.0d) % MathConstants.num3) - 180.0d;
    }

    public static boolean isWithinFovAngle(Entity entity, double d) {
        double d2 = (float) (d * MathConstants.val2);
        double yaw = ((((minecraftClient.player.getYRot(RenderUtil.getPartialTick()) - getEntityYawAngle(entity)) % MathConstants.num3) + 540.0d) % MathConstants.num3) - 180.0d;
        return (yaw > 0.0d && yaw < d2) || ((-d2) < yaw && yaw < 0.0d);
    }

    public static float getEntityYawAngle(Entity entity) {
        return (float) (Math.atan2(entity.getX() - minecraftClient.player.getX(), entity.getZ() - minecraftClient.player.getZ()) * 57.2957795d * (-1.0d));
    }
}