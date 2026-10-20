package me.mioclient.module.combat;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.inventory.PotionResolver;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.StopUsingItemEvent;
import me.mioclient.event.events.client.TickPostEvent;
import me.mioclient.module.Module;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import me.mioclient.ducks.DuckStatusEffectInstance;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class Arrows extends Module {
    public static final AtomicBoolean atomicBoolean = new AtomicBoolean();
    public Setting<Boolean> autoShoot = add(new BooleanSetting("AutoShoot", false));
    public Setting<Float> fov = add(new NumberSetting<>("Fov", 20.0f, 10.0f, 45.0f, v -> !this.autoShoot.getValue().booleanValue()));
    public Setting<Integer> assumeDuration = add(new NumberSetting<>("AssumeDuration", 50, 0, 100).setSuffix("%"));
    public final Map<MobEffect, Long> map;
    public final Stopwatch stopwatch;
    public int num;
    public Potion potion;

    public Arrows() {
        super("Arrows", "Will swap between effect arrows in your inventory.", Category.COMBAT, "projectiles");
        this.map = new HashMap();
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        if (atomicBoolean.get()) {
            disable();
        }
    }

    @Listen
    
    public void onStopUsingItem(StopUsingItemEvent stopUsingItemEvent) {
        Player playerEntity1934 = findClosestTargetInFov();
        InteractionHand activeHand = minecraftClient.player.getUsedItemHand();
        if (activeHand == null || !(minecraftClient.player.getItemInHand(activeHand).getItem() instanceof ProjectileWeaponItem)) {
            return;
        }
        if (playerEntity1934 != null) {
            if (this.num != -1) {
            }
            int i = minecraftClient.player.getInventory().getSelectedSlot();
            InventoryUtils.selectSlot((i + 1) % 9);
            PacketSender.sendPlayerAction(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN);
            InventoryUtils.selectSlot(i);
            stopUsingItemEvent.cancel();
            return;
        }
        if (this.stopwatch.hasElapsedMs(50L)) {
            for (int i2 = 0; i2 < minecraftClient.player.getInventory().getContainerSize(); i2++) {
                if (minecraftClient.player.getInventory().getItem(i2).getItem() instanceof ArrowItem) {
                    if (i2 != this.num) {
                        this.stopwatch.reset();
                        ClientServices.fireworksHelper.setSyncBatching(true);
                        InventoryUtils.swapStacks(InventoryUtils.toNetworkSlot(this.num), InventoryUtils.toNetworkSlot(i2));
                        ClientServices.fireworksHelper.setSyncBatching(false);
                        this.num = -1;
                    }
                    if (this.potion == null) {
                        return;
                    }
                    Iterator it = this.potion.getEffects().iterator();
                    while (it.hasNext()) {
                        this.map.put((MobEffect) ((MobEffectInstance) it.next()).getEffect().value(), Long.valueOf(System.currentTimeMillis()));
                    }
                    return;
                }
            }
        }
    }

    @Listen
    public void onTickPost(TickPostEvent tickPostEvent) {
        if (minecraftClient.player.getMainHandItem().getItem() instanceof ProjectileWeaponItem) {
            LocalPlayer playerEntity1934 = (LocalPlayer)(findClosestTargetInFov());
            if (playerEntity1934 == null) {
                playerEntity1934 = minecraftClient.player;
            }
            this.map.entrySet().removeIf(entry -> {
                return ((Long) entry.getValue()).longValue() + 750 < System.currentTimeMillis();
            });
            int i = -1;
            int i2 = -1;
            for (int i3 = 0; i3 < minecraftClient.player.getInventory().getContainerSize(); i3++) {
                int i4 = scoreArrow(minecraftClient.player.getInventory().getItem(i3), playerEntity1934);
                if (i4 >= 0 && i4 > i) {
                    i2 = i3;
                    i = i4;
                }
            }
            this.num = i2;
            if (this.num != -1) {
                this.potion = PotionResolver.resolvePotion(minecraftClient.player.getInventory().getItem(this.num));
            }
        }
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        if (this.autoShoot.getValue().booleanValue()) {
            if (this.num != -1) {
                if (scoreArrow(minecraftClient.player.getInventory().getItem(this.num), minecraftClient.player) > 0) {
                    shootBow();
                    return;
                }
            }
            disable();
        }
    }

    public Player findClosestTargetInFov() {
        if (!this.autoShoot.getValue().booleanValue() && minecraftClient.player.getXRot() >= (-85.0f)) {
            AbstractClientPlayer abstractClientPlayerEntity = null;
            double longBitsToDouble = 3.4028234663852886E38d;
            for (AbstractClientPlayer abstractClientPlayerEntity2 : minecraftClient.level.players()) {
                if (minecraftClient.player != abstractClientPlayerEntity2 && !ClientServices.socialManager.isFriendEntity((Player) abstractClientPlayerEntity2)) {
                    float[] floatArray2483 = RotationManager.getRotationsTo((Entity) abstractClientPlayerEntity2);
                    float angleBetween = Mth.degreesDifferenceAbs(minecraftClient.player.getYRot(), floatArray2483[0]) - (abstractClientPlayerEntity2.getDimensions(abstractClientPlayerEntity2.getPose()).width() * 0.5f);
                    float angleBetween2 = Mth.degreesDifferenceAbs(minecraftClient.player.getXRot(), floatArray2483[1]) - (abstractClientPlayerEntity2.getDimensions(abstractClientPlayerEntity2.getPose()).height() * 0.5f);
                    if (angleBetween <= this.fov.getValue().floatValue() && angleBetween2 <= this.fov.getValue().floatValue()) {
                        double hypot = Math.hypot(angleBetween, angleBetween2);
                        if (hypot < longBitsToDouble) {
                            abstractClientPlayerEntity = abstractClientPlayerEntity2;
                            longBitsToDouble = hypot;
                        }
                    }
                }
            }
            return abstractClientPlayerEntity;
        }
        return minecraftClient.player;
    }

    public int scoreArrow(ItemStack itemStack, Object obj) {
        if (!(itemStack.getItem() instanceof ArrowItem) || !(obj instanceof Player)) {
            return -1;
        }
        Player playerEntity = (Player) obj;
        Potion potion1565 = PotionResolver.resolvePotion(itemStack);
        boolean z = playerEntity == minecraftClient.player || ClientServices.socialManager.isFriendEntity(playerEntity);
        int i = potion1565.getEffects().isEmpty() ? z ? -1 : 1 : 0;
        for (MobEffectInstance statusEffectInstance : potion1565.getEffects()) {
            Holder<MobEffect> effectType = statusEffectInstance.getEffect();
            boolean hasStatusEffect = playerEntity.hasEffect(effectType);
            if (hasStatusEffect && ((me.mioclient.ducks.DuckStatusEffectInstance)(Object) playerEntity.getEffect(effectType)).mio$getDurationRation() * 100.0f < this.assumeDuration.getValue().intValue()) {
                hasStatusEffect = false;
            }
            if (!hasStatusEffect) {
                if (!this.map.containsKey(effectType.value())) {
                    int amplifier = (!((MobEffect) effectType.value()).isInstantaneous() ? statusEffectInstance.getAmplifier() : 0) + 1;
                    if (isBeneficialEffect(effectType) != z) {
                        amplifier *= -1;
                    }
                    i += amplifier;
                }
            }
        }
        return i;
    }

    public boolean isBeneficialEffect(Holder<MobEffect> registryEntry) {
        if (registryEntry == MobEffects.SLOW_FALLING) {
            return false;
        }
        return ((MobEffect) registryEntry.value()).isBeneficial();
    }

    public void shootBow() {
        int i = minecraftClient.player.getInventory().getSelectedSlot();
        int i2 = InventoryUtils.findInHotbar(Items.BOW);
        ClientServices.RotationManager.requestRotations(new float[]{minecraftClient.player.getYRot(), (-90.0f)}, 999);
        if (i2 == -1 || minecraftClient.player.isUsingItem()) {
            return;
        }
        InventoryUtils.do438(i2);
        minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.MAIN_HAND);
        if (atomicBoolean.get()) {
            return;
        }
        atomicBoolean.set(true);
        ClientServices.tooltipsHelper.schedule(() -> {
            minecraftClient.gameMode.releaseUsingItem(minecraftClient.player);
            InventoryUtils.do438(i);
            atomicBoolean.set(false);
            disable();
        }, 3);
    }
}