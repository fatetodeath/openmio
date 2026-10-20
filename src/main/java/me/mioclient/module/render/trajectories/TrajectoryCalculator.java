package me.mioclient.module.render.trajectories;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.MathConstants;

import me.mioclient.api.ClientAccessor;

import me.mioclient.ducks.DuckEntityCamera;

import me.mioclient.module.render.Trajectories;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import me.mioclient.manager.RotationManager;

public class TrajectoryCalculator implements ClientAccessor {
    public static Trajectories trajectories = (Trajectories) ClientServices.moduleInjector.getModule(Trajectories.class);
    public static final net.minecraft.world.phys.HitResult hitResult = new HitResult(Vec3.ZERO);
    public static final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    public static Vec3 vec3d = new Vec3(0.0d, 0.0d, 0.0d);
    public static Vec3 vec3d2 = new Vec3(0.0d, 0.0d, 0.0d);
    public final Vector3d vector3d = new Vector3d();
    public final Vector3d vector3d2 = new Vector3d();
    public double val;
    public double val2;
    public double val3;

    
    static class HitResult extends net.minecraft.world.phys.HitResult {
        public HitResult(Vec3 vec3d) {
            super(vec3d);
        }

        public HitResult.Type getType() {
            return HitResult.Type.MISS;
        }
    }

    public boolean is1867(Entity entity, ItemStack itemStack, double d, boolean z, double d2) {
        ChargedProjectiles chargedProjectilesComponent;
        Item item = itemStack.getItem();
        if ((item instanceof BowItem) && trajectories.bow.getValue().booleanValue()) {
            double lerp = Mth.lerp(d2, trajectories.getLastPullProgress(), BowItem.getPowerForTime(minecraftClient.player.getTicksUsingItem()));
            if (lerp <= 0.0d) {
                lerp = 1.0d;
            }
            calculateShotTrajectory(entity, 0.0d, lerp * 3.0d, d, 0.05000000074505806d, 0.6d, z, d2);
            return true;
        }
        if ((item instanceof CrossbowItem) && trajectories.xBow.getValue().booleanValue()) {
            if (!CrossbowItem.isCharged(itemStack) || (chargedProjectilesComponent = (ChargedProjectiles) itemStack.get(DataComponents.CHARGED_PROJECTILES)) == null) {
                return false;
            }
            if (chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET)) {
                calculateShotTrajectory(entity, 0.0d, chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? 1.6f : 3.15f, d, 0.0d, 0.6d, z, d2);
                return true;
            }
            calculateShotTrajectory(entity, 0.0d, chargedProjectilesComponent.contains(Items.FIREWORK_ROCKET) ? 1.6f : 3.15f, d, 0.05000000074505806d, 0.6d, z, d2);
            return true;
        }
        if ((item instanceof FishingRodItem) && trajectories.others.getValue().booleanValue()) {
            calculateFishingRodTrajectory(entity, d2);
            return true;
        }
        if ((item instanceof TridentItem) && trajectories.trident.getValue().booleanValue()) {
            calculateShotTrajectory(entity, 0.0d, 2.5d, d, 0.05000000074505806d, 0.99d, z, d2);
            return true;
        }
        if ((((item instanceof SnowballItem) || (item instanceof EggItem)) && trajectories.others.getValue().booleanValue()) || ((item instanceof EnderpearlItem) && trajectories.pearls.getValue().booleanValue())) {
            calculateShotTrajectory(entity, 0.0d, 1.5d, d, 0.03d, 0.8d, z, d2);
            return true;
        }
        if ((item instanceof ExperienceBottleItem) && trajectories.exp.getValue().booleanValue()) {
            calculateShotTrajectory(entity, (-20.0d), 0.7d, d, 0.07d, 0.8d, z, d2);
            return true;
        }
        if (!(item instanceof ThrowablePotionItem) || !trajectories.others.getValue().booleanValue()) {
            return false;
        }
        calculateShotTrajectory(entity, (-20.0d), MathConstants.val2, d, 0.05d, 0.8d, z, d2);
        return true;
    }

    public void calculateShotTrajectory(Entity entity, double d, double d2, double d3, double d4, double d5, boolean z, double d6) {
        double d7;
        double d8;
        double d9;
        this.vector3d.set(Mth.lerp(d6, entity.xo, entity.getX()), Mth.lerp(d6, entity.yo, entity.getY()) + entity.getEyeHeight(entity.getPose()), Mth.lerp(d6, entity.zo, entity.getZ()));
        double lerp = Mth.lerp(d6, entity.yRotO, entity.getYRot());
        double lerp2 = Mth.lerp(d6, entity.xRotO, entity.getXRot());
        if (entity == minecraftClient.player) {
            lerp = Mth.lerp(d6, ClientServices.RotationManager.getPrevYaw(), ClientServices.RotationManager.getYaw());
            lerp2 = Mth.lerp(d6, ClientServices.RotationManager.getPrevPitch(), ClientServices.RotationManager.getPitch());
        }
        if (d3 == 0.0d) {
            d7 = (-Math.sin(lerp * MathConstants.val4)) * Math.cos(lerp2 * MathConstants.val4);
            d8 = -Math.sin((lerp2 + d) * MathConstants.val4);
            d9 = Math.cos(lerp * MathConstants.val4) * Math.cos(lerp2 * MathConstants.val4);
        } else {
            Vec3 oppositeRotationVector = entity.getLookAngle().scale(-1.0d);
            Quaterniondc angleAxis = new Quaterniond().setAngleAxis(d3, oppositeRotationVector.x, oppositeRotationVector.y, oppositeRotationVector.z);
            Vec3 rotationVec = entity.getLookAngle();
            Vector3d vector3d = new Vector3d(rotationVec.x, rotationVec.y, rotationVec.z);
            vector3d.rotate(angleAxis);
            d7 = vector3d.x;
            d8 = vector3d.y;
            d9 = vector3d.z;
        }
        this.vector3d2.set(d7, d8, d9).normalize().mul(d2);
        if (z) {
            Vec3 velocity = entity.getDeltaMovement();
            if (minecraftClient.player == entity) {
                this.vector3d2.add(-Mth.lerp(d6, trajectories.getLastPlayerVelocity().x, velocity.x), entity.onGround() ? 0.0d : Mth.lerp(d6, trajectories.getLastPlayerVelocity().y, velocity.y), -Mth.lerp(d6, trajectories.getLastPlayerVelocity().z, velocity.z));
            } else {
                this.vector3d2.add(-velocity.x, entity.onGround() ? 0.0d : velocity.y, -velocity.z);
            }
        }
        this.val = d4;
        this.val2 = 0.99d;
        this.val3 = d5;
    }

    public boolean initAirborneProjectile(Entity entity, boolean z, double d) {
        if (!minecraftClient.level.noCollision(entity, entity.getBoundingBox().inflate(BoxMath.val))) {
            return false;
        }
        if (entity instanceof Arrow) {
            calculateAirborneTrajectory(entity, ((Arrow) entity).getDeltaMovement().length(), 0.05000000074505806d, 0.6d, z, d);
        } else if ((entity instanceof ThrownEnderpearl) || (entity instanceof Snowball) || (entity instanceof ThrownEgg)) {
            calculateAirborneTrajectory(entity, 1.5d, 0.03d, 0.8d, z, d);
            if (BlockPos.betweenClosedStream(entity.getBoundingBox().inflate(0.0d, 2.0d, 0.0d)).map((v0) -> {
                return new BlockPos(v0);
            }).anyMatch(blockPos -> {
                return minecraftClient.level.getBlockState(blockPos).getBlock() == Blocks.BUBBLE_COLUMN;
            })) {
                Vec3 velocity = entity.getDeltaMovement();
                Vec3 vec3d3 = new Vec3(velocity.x, (-999.0d), velocity.z);
                interpolateVelocity(entity, vec3d3, 1.0d, 1.5d);
            }
        } else if (entity instanceof ThrownTrident) {
            calculateAirborneTrajectory(entity, 2.5d, 0.05000000074505806d, 0.99d, z, d);
        } else if (entity instanceof ThrownExperienceBottle) {
            calculateAirborneTrajectory(entity, 0.7d, 0.07d, 0.8d, z, d);
        } else if (entity instanceof ThrowableProjectile) {
            calculateAirborneTrajectory(entity, MathConstants.val2, 0.05d, 0.8d, z, d);
        } else {
            if (!(entity instanceof WitherSkull) && !(entity instanceof Fireball) && !(entity instanceof DragonFireball)) {
                return false;
            }
            calculateAirborneTrajectory(entity, 0.95d, 0.0d, 0.8d, z, d);
        }
        if (!entity.isNoGravity()) {
            return true;
        }
        this.val = 0.0d;
        return true;
    }

    public void calculateAirborneTrajectory(Entity entity, double d, double d2, double d3, boolean z, double d4) {
        this.vector3d.set(Mth.lerp(d4, entity.xo, entity.getX()), Mth.lerp(d4, entity.yo, entity.getY()), Mth.lerp(d4, entity.zo, entity.getZ()));
        interpolateVelocity(entity, entity.getDeltaMovement(), d4, d);
        if (z) {
            Vec3 velocity = entity.getDeltaMovement();
            Vector3d vector3d = this.vector3d2;
            double d5 = velocity.x;
            double d6 = entity.onGround() ? 0.0d : velocity.y;
            vector3d.add(d5, d6, velocity.z);
        }
        this.val = d2;
        this.val2 = 0.99d;
        this.val3 = d3;
    }

    public void interpolateVelocity(Entity entity, Vec3 vec3d3, double d, double d2) {
        DuckEntityCamera cameraYawHelper = (DuckEntityCamera) entity;
        this.vector3d2.set(Mth.lerp(d, cameraYawHelper.mio$getPrevVelocity().x, vec3d3.x), Mth.lerp(d, cameraYawHelper.mio$getPrevVelocity().y, vec3d3.y), Mth.lerp(d, cameraYawHelper.mio$getPrevVelocity().z, vec3d3.z)).normalize().mul(d2);
    }

    public void calculateFishingRodTrajectory(Entity entity, double d) {
        double lerp = Mth.lerp(d, entity.yRotO, entity.getYRot());
        double lerp2 = Mth.lerp(d, entity.xRotO, entity.getXRot());
        double cos = Math.cos(((-lerp) * MathConstants.val4) - 3.1415927410125732d);
        double sin = Math.sin(((-lerp) * MathConstants.val4) - 3.1415927410125732d);
        double d2 = -Math.cos((-lerp2) * MathConstants.val4);
        double sin2 = Math.sin((-lerp2) * MathConstants.val4);
        this.vector3d.set(Mth.lerp(d, entity.xo, entity.getX()), Mth.lerp(d, entity.yo, entity.getY()) + entity.getEyeHeight(entity.getPose()), Mth.lerp(d, entity.zo, entity.getZ()));
        this.vector3d.sub(sin * 0.3d, 0.0d, cos * 0.3d);
        this.vector3d2.set(-sin, Mth.clamp(-(sin2 / d2), (-5.0d), 5.0d), -cos);
        double length = this.vector3d2.length();
        this.vector3d2.mul((0.6d / length) + MathConstants.val2, (0.6d / length) + MathConstants.val2, (0.6d / length) + MathConstants.val2);
        this.val = 0.03d;
        this.val2 = 0.92d;
        this.val3 = 0.0d;
    }

    public net.minecraft.world.phys.HitResult stepSimulation() {
        vec3d2 = new Vec3(this.vector3d.x, this.vector3d.y, this.vector3d.z);
        this.vector3d.add(this.vector3d2);
        this.vector3d2.mul(isInWater() ? this.val3 : this.val2);
        this.vector3d2.sub(0.0d, this.val, 0.0d);
        if (this.vector3d.y < minecraftClient.level.getMinY()) {
            return hitResult;
        }
        if (!minecraftClient.level.getChunkSource().hasChunk((int) (this.vector3d.x / 16.0d), (int) (this.vector3d.z / 16.0d))) {
            return hitResult;
        }
        vec3d = new Vec3(this.vector3d.x, this.vector3d.y, this.vector3d.z);
        net.minecraft.world.phys.HitResult hitResult1875 = raycastCollision();
        if (hitResult1875.getType() == HitResult.Type.MISS) {
            return null;
        }
        return hitResult1875;
    }

    public boolean isInWater() {
        mutable.set(this.vector3d.x, this.vector3d.y, this.vector3d.z);
        FluidState fluidState = minecraftClient.level.getFluidState(mutable);
        return fluidState.is(FluidTags.WATER) && this.vector3d.y - ((double) ((int) this.vector3d.y)) <= ((double) fluidState.getHeight(minecraftClient.level, mutable));
    }

    public net.minecraft.world.phys.HitResult raycastCollision() {
        Vec3 vec3d3 = vec3d2;
        net.minecraft.world.phys.HitResult raycast = minecraftClient.level.clip(new ClipContext(vec3d3, vec3d, ClipContext.Block.COLLIDER, this.val3 == 0.0d ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, minecraftClient.player));
        if (raycast.getType() != HitResult.Type.MISS) {
            vec3d3 = raycast.getLocation();
        }
        net.minecraft.world.phys.HitResult entityCollision = ProjectileUtil.getEntityHitResult(minecraftClient.level, minecraftClient.player, vec3d3, vec3d, new AABB(this.vector3d.x, this.vector3d.y, this.vector3d.z, this.vector3d.x, this.vector3d.y, this.vector3d.z).expandTowards(minecraftClient.player.getDeltaMovement()).inflate(1.0d), entity -> {
            return !entity.isSpectator() && entity.isAlive() && entity.isPickable();
        }, 0.0f);
        if (entityCollision != null) {
            raycast = entityCollision;
        }
        return raycast;
    }
}