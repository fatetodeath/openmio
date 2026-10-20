package me.mioclient.util.player;

import me.mioclient.api.ClientAccessor;

import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.animal.wolf.Wolf;

public class EntityUtil implements ClientAccessor {
    public static float getPlayerHealth() {
        return getEntityHealth(minecraftClient.player);
    }

    public static float getEntityHealth(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return 2.0f;
        }
        LivingEntity livingEntity = (LivingEntity) entity;
        return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
    }

    public static boolean isHostile(Entity entity) {
        // 26.2 port: neutral-mob APIs (Angerable/PiglinActivity/WolfVariant textures) were removed;
        // keep stable checks only.
        if (entity instanceof EnderMan) {
            return ((EnderMan) entity).isCreepy();
        }
        if (entity instanceof Wolf) {
            return false;
        }
        if (entity instanceof PolarBear) {
            return true;
        }
        return entity instanceof Monster;
    }

    public static boolean isPassive(Entity entity) {
        return (entity instanceof AbstractFish) || (entity instanceof Animal) || (entity instanceof Squid) || (entity instanceof Dolphin);
    }

    public static boolean isOnGround(LivingEntity livingEntity) {
        return !minecraftClient.level.noCollision(livingEntity, livingEntity.getBoundingBox().expandTowards(0.0d, (-0.1d), 0.0d));
    }

    public static boolean isFirstPersonView(Entity entity) {
        return minecraftClient.options.getCameraType() == CameraType.FIRST_PERSON && entity != minecraftClient.player && minecraftClient.getCameraEntity() == entity;
    }

    public static void setStepHeight(LivingEntity livingEntity, float f) {
        livingEntity.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(f);
    }
}
