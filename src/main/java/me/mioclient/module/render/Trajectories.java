package me.mioclient.module.render;

import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.module.render.trajectories.TrajectoryCalculator;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.module.Module;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL32C;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Trajectories extends Module {
    public final TrajectoryCalculator trajectoriesSearchHelper4;
    public Setting<Boolean> onlyOwn = add(new BooleanSetting("OnlyOwn", true));
    public Setting<Boolean> predict = add(new BooleanSetting("Predict", true));
    public Setting<Boolean> airborne = add(new BooleanSetting("Airborne", false));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-1312974337, true)).setThemeSync(true));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", false).setDependent());
    public Setting<Boolean> bow = add(new BooleanSetting("Bow", true).requiresSettings(this.targets));
    public Setting<Boolean> xBow = add(new BooleanSetting("XBow", true).requiresSettings(this.targets));
    public Setting<Boolean> trident = add(new BooleanSetting("Trident", true).requiresSettings(this.targets));
    public Setting<Boolean> pearls = add(new BooleanSetting("Pearls", true).requiresSettings(this.targets));
    public Setting<Boolean> exp = add(new BooleanSetting("Exp", true).requiresSettings(this.targets));
    public Setting<Boolean> others = add(new BooleanSetting("Others", true).requiresSettings(this.targets));
    public BufferBuilder bufferBuilder;
    public Vec3 vec3d;
    public float val;

    
    private class Inner {
        public final List<Vec3> list = new ArrayList();
        public boolean flag;
        public AABB box;
        public Entity entity;
        public Entity entity2;

        public Inner() {
        }

        public void resetTrajectoryData() {
            this.list.clear();
            this.flag = false;
            this.entity = null;
        }

        public void setSourceEntity(Entity entity) {
            this.entity2 = entity;
        }

        public void simulatePath() {
            recordPosition();
            for (int i = 0; i < 2000; i++) {
                HitResult hitResult1873 = Trajectories.this.trajectoriesSearchHelper4.stepSimulation();
                if (hitResult1873 != null) {
                    handleHit(hitResult1873);
                    return;
                }
                recordPosition();
            }
        }

        public void recordPosition() {
            this.list.add(new Vec3(Trajectories.this.trajectoriesSearchHelper4.vector3d.x, Trajectories.this.trajectoriesSearchHelper4.vector3d.y, Trajectories.this.trajectoriesSearchHelper4.vector3d.z));
        }

        public void handleHit(HitResult hitResult) {
            if (hitResult.getType() != HitResult.Type.BLOCK) {
                if (hitResult.getType() == HitResult.Type.ENTITY) {
                    this.entity = ((EntityHitResult) hitResult).getEntity();
                    return;
                }
                return;
            }
            BlockHitResult blockHitResult = (BlockHitResult) hitResult;
            this.flag = true;
            this.box = new AABB(blockHitResult.getLocation(), blockHitResult.getLocation());
            if (blockHitResult.getDirection() == Direction.UP || blockHitResult.getDirection() == Direction.DOWN) {
                this.box = this.box.inflate(0.25d, 0.0d, 0.25d);
            } else if (blockHitResult.getDirection() == Direction.NORTH || blockHitResult.getDirection() == Direction.SOUTH) {
                this.box = this.box.inflate(0.25d, 0.25d, 0.0d);
            } else {
                this.box = this.box.inflate(0.0d, 0.25d, 0.25d);
            }
            this.list.add(hitResult.getLocation());
        }

        public void renderTrajectory(MatrixStackEvent.Inner_3 inner_3) {
            Vec3 vec3d = null;
            for (Vec3 vec3d2 : this.list) {
                if (vec3d != null) {
                    RenderUtil.INSTANCE.addLineVertex(inner_3.getMatrixStack(), Trajectories.this.bufferBuilder, vec3d, vec3d2, Trajectories.this.color.getValue().getRGB(), Trajectories.this.color.getValue().getRGB());
                }
                vec3d = vec3d2;
            }
            if (this.flag) {
                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), this.box, ColorUtils.withAlpha(Trajectories.this.color.getValue(), 60));
                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), this.box, Trajectories.this.color.getValue(), Trajectories.this.lineWidth.getValue().floatValue());
            }
            if (this.entity == null || this.entity2 != ClientAccessor.minecraftClient.player) {
                return;
            }
            BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), BoxMath.getInterpolatedBox(this.entity, inner_3.getTickDelta()), ColorUtils.withAlpha(Trajectories.this.color.getValue(), 60));
        }
    }

    public Trajectories() {
        super("Trajectories", "Draws trajectories for projectiles and bows.", Category.RENDER, new String[0]);
        this.trajectoriesSearchHelper4 = new TrajectoryCalculator();
        this.vec3d = Vec3.ZERO;
        setDrawn(false);
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        this.vec3d = minecraftClient.player.getDeltaMovement();
        this.val = BowItem.getPowerForTime(minecraftClient.player.getTicksUsingItem());
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            if (shouldTrackEntity(entity)) {
                Inner inner = new Inner();
                if (entity instanceof Projectile) {
                    if (this.trajectoriesSearchHelper4.initAirborneProjectile((Projectile) entity, true, inner_3.getTickDelta())) {
                        inner.simulatePath();
                        inner.setSourceEntity(entity);
                        inner.renderTrajectory(inner_3);
                        inner.resetTrajectoryData();
                    }
                } else {
                    if (entity instanceof Player) {
                        Entity entity2 = (Player) entity;
                        if (!this.trajectoriesSearchHelper4.is1867(entity2, ((Player) entity2).getItemInHand(((Player) entity2).getUsedItemHand()), 0.0d, true, inner_3.getTickDelta())) {
                        }
                    }
                    inner.simulatePath();
                    inner.setSourceEntity(entity);
                    inner.renderTrajectory(inner_3);
                    inner.resetTrajectoryData();
                }
            }
        }
    }

    public boolean shouldTrackEntity(Entity entity) {
        if (entity instanceof WitherSkull) {
            return false;
        }
        return ((entity instanceof Player) && this.predict.getValue().booleanValue()) ? entity == minecraftClient.player || !this.onlyOwn.getValue().booleanValue() : (entity instanceof Projectile) && this.airborne.getValue().booleanValue();
    }

    public Vec3 getLastPlayerVelocity() {
        return this.vec3d;
    }

    public float getLastPullProgress() {
        return this.val;
    }
}