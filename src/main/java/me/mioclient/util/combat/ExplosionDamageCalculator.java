package me.mioclient.util.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.util.inventory.EnchantmentHelper;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.world.BlockRaycastFilter;
import me.mioclient.util.world.CustomRaycastContext;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.api.ClientAccessor;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import me.mioclient.module.combat.AutoCrystal;
import me.mioclient.module.combat.AutoMine;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Explosion;

public class ExplosionDamageCalculator implements ClientAccessor {
    public static AutoCrystal autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
    public static AutoMine autoMine = (AutoMine) ClientServices.moduleInjector.getModule(AutoMine.class);

    
    
    public static /* synthetic */ class Inner {
        public static final /* synthetic */ int[] intArr = new int[Difficulty.values().length];

        static {
            try {
                intArr[Difficulty.PEACEFUL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                intArr[Difficulty.EASY.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                intArr[Difficulty.HARD.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    public static float getCrystalDamage(Vec3 vec3d, LivingEntity livingEntity) {
        return getExplosionDamage(vec3d, livingEntity, 6.0d, true, (BlockPos) null);
    }

    public static float getCrystalDamage(Vec3 vec3d, LivingEntity livingEntity, AABB box) {
        return getExplosionDamage(vec3d, livingEntity, box, 6.0d, true, (BlockPos) null, (BlockPos) null);
    }

    public static float getBedDamage(Vec3 vec3d, LivingEntity livingEntity) {
        return getBedDamage(vec3d, livingEntity, livingEntity.getBoundingBox());
    }

    public static float getBedDamage(Vec3 vec3d, LivingEntity livingEntity, AABB box) {
        return getExplosionDamage(vec3d, livingEntity, box, 5.0d, true, (BlockPos) null, (BlockPos) null);
    }

    public static float getExplosionDamage(Vec3 vec3d, LivingEntity livingEntity, double d, boolean z, BlockPos blockPos) {
        return getExplosionDamage(vec3d, livingEntity, livingEntity.getBoundingBox(), d, z, blockPos, (BlockPos) null);
    }

    public static float getExplosionDamage(Vec3 vec3d, LivingEntity livingEntity, AABB box, double d, boolean z, BlockPos blockPos, BlockPos blockPos2) {
        if (livingEntity.equals(minecraftClient.player) && minecraftClient.player.isCreative()) {
            return 0.0f;
        }
        Vec3 vec3d2 = new Vec3(Mth.lerp(MathConstants.val2, box.minX, box.maxX), box.minY, Mth.lerp(MathConstants.val2, box.minZ, box.maxZ));
        double longBitsToDouble = d * 2.0d;
        double distanceTo = vec3d2.distanceTo(vec3d) / longBitsToDouble;
        if (distanceTo > 1.0d) {
            return 0.0f;
        }
        double longBitsToDouble2 = (1.0d - distanceTo) * getExposure(vec3d, (Entity) livingEntity, box, blockPos, blockPos2, z);
        return Math.max(getDamageAfterProtection(livingEntity, getDifficultyScaledDamage((float) (((((longBitsToDouble2 * longBitsToDouble2) + longBitsToDouble2) / 2.0d) * 7.0d * longBitsToDouble) + 1.0d))), 0.0f);
    }

    public static float getExposure(Vec3 vec3d, Entity entity, BlockPos blockPos, boolean z) {
        return getExposure(vec3d, entity, entity.getBoundingBox(), blockPos, (BlockPos) null, z);
    }

    public static float getExposure(Vec3 vec3d, Entity entity, AABB box, BlockPos blockPos, BlockPos blockPos2, boolean z) {
        double longBitsToDouble = 1.0d / (((box.maxX - box.minX) * 2.0d) + 1.0d);
        double longBitsToDouble2 = 1.0d / (((box.maxY - box.minY) * 2.0d) + 1.0d);
        double longBitsToDouble3 = 1.0d / (((box.maxZ - box.minZ) * 2.0d) + 1.0d);
        double longBitsToDouble4 = (1.0d - (Math.floor(1.0d / longBitsToDouble) * longBitsToDouble)) / 2.0d;
        double longBitsToDouble5 = (1.0d - (Math.floor(1.0d / longBitsToDouble3) * longBitsToDouble3)) / 2.0d;
        if (longBitsToDouble < 0.0d || longBitsToDouble2 < 0.0d || longBitsToDouble3 < 0.0d) {
            return 0.0f;
        }
        int i = 0;
        int i2 = 0;
        double d = 0.0d;
        while (true) {
            double d2 = d;
            if (d2 > 1.0d) {
                return (float) i / (float) i2;   // çˆ†ç‚¸æš´éœ²çŽ‡(å‘½ä¸­æ ·æœ¬/æ€»æ ·æœ¬), å¿…é¡»æµ®ç‚¹é™¤æ³•; åç¼–è¯‘ä¸¢äº†è½¬åž‹å¯¼è‡´æ•´æ•°é™¤æ³•â†’æ’0â†’æ°´æ™¶ä¼¤å®³â‰ˆ0â†’AutoCrystalä¸æ”¾ç½®
            }
            double d3 = 0.0d;
            while (true) {
                double d4 = d3;
                if (d4 <= 1.0d) {
                    double d5 = 0.0d;
                    while (true) {
                        double d6 = d5;
                        if (d6 <= 1.0d) {
                            CustomRaycastContext.Inner inner1603 = new CustomRaycastContext.Inner(new Vec3(Mth.lerp(d2, box.minX, box.maxX) + longBitsToDouble4, Mth.lerp(d4, box.minY, box.maxY), Mth.lerp(d6, box.minZ, box.maxZ) + longBitsToDouble5), vec3d).setEntity(entity);
                            BlockRaycastFilter[] holeSnapHelperArr = new BlockRaycastFilter[3];
                            BlockPos[] blockPosArr = new BlockPos[2];
                            blockPosArr[0] = blockPos;
                            blockPosArr[1] = blockPos == null ? null : autoMine.speedMineHelper.getBlockPos();
                            holeSnapHelperArr[0] = BlockRaycastFilter.createIgnoreFilter(Arrays.asList(blockPosArr));
                            holeSnapHelperArr[1] = BlockRaycastFilter.getHoleSnapHelper1676(blockPos2);
                            holeSnapHelperArr[2] = z ? BlockRaycastFilter.holeSnapHelper2 : BlockRaycastFilter.holeSnapHelper;
                            if (RaycastUtils.performCustomBlockRaycast(inner1603.setCombinedFilter(holeSnapHelperArr).build()).getType() == HitResult.Type.MISS) {
                                i++;
                            }
                            i2++;
                            d5 = d6 + longBitsToDouble3;
                        } else {
                            break;
                        }
                    }
                    d3 = d4 + longBitsToDouble2;
                } else {
                    break;
                }
            }
            d = d2 + longBitsToDouble;
        }
    }

    public static float getDamageAfterProtection(LivingEntity livingEntity, float f) {
        // 26.2 port: DamageUtil removed; inline vanilla armor+toughness reduction.
        float armor = (float) livingEntity.getArmorValue();
        float toughness = (float) livingEntity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float clamped = Mth.clamp(armor - f / (2.0f + toughness / 4.0f), armor * 0.2f, 20.0f);
        float damageLeft = f * (1.0f - clamped / 25.0f);
        if (livingEntity.hasEffect(MobEffects.RESISTANCE)) {
            damageLeft = (damageLeft * (25 - ((livingEntity.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5))) / 25.0f;
        }
        int i = 0;
        // 26.2 port: iterate armor slots via getItemBySlot.
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            Map<ResourceKey<Enchantment>, Integer> map1419 = EnchantmentHelper.getEnchantmentMap(livingEntity.getItemBySlot(slot));
            i += (map1419.getOrDefault(Enchantments.BLAST_PROTECTION, 0).intValue() * 2) + map1419.getOrDefault(Enchantments.PROTECTION, 0).intValue();
        }
        if (i > 20 || (autoCrystal.isToggled() && autoCrystal.assumeBestArmor.getValue().booleanValue())) {
            i = 20;
        }
        float intBitsToFloat = damageLeft * (1.0f - (i / 25.0f));
        if (intBitsToFloat < 0.0f) {
            return 0.0f;
        }
        return intBitsToFloat;
    }

    public static float getDifficultyScaledDamage(float f) {
        switch (Inner.intArr[minecraftClient.level.getDifficulty().ordinal()]) {
            case 1:
                return 0.0f;
            case 2:
                return Math.min((f / 2.0f) + 1.0f, f);
            case 3:
                return (f * 3.0f) / 2.0f;
            default:
                return f;
        }
    }

    public static int getClampedDurabilityPercent(ItemStack itemStack) {
        return Mth.clamp(100 - ((int) Math.ceil((1.0f - ((itemStack.getMaxDamage() - itemStack.getDamageValue()) / (float) itemStack.getMaxDamage())) * 100.0f)), 1, 100);
    }
}
