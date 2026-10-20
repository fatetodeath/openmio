package me.mioclient.module.combat;

import me.mioclient.event.events.interaction.InteractItemPacketEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.inventory.PotionResolver;
import me.mioclient.util.math.MathConstants;

import java.util.Iterator;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.core.Holder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;

public class BowAim extends Module {
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", true).setDependent());
    public Setting<Boolean> players = add(new BooleanSetting("Players", true, v -> this.targets.isShown()).setVisible());
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", false, v -> this.targets.isShown() && this.players.isShown()));
    public Setting<Boolean> animals = add(new BooleanSetting("Animals", false, v -> this.targets.isShown()));
    public Setting<Boolean> hostiles = add(new BooleanSetting("Hostiles", true, v -> this.targets.isShown()));
    public Setting<Boolean> neutrals = add(new BooleanSetting("Neutrals", false).requiresSettings(this.targets));
    public float[] floatArr;

    public BowAim() {
        super("BowAim", "Helps aiming with bows.", Category.COMBAT, new String[0]);
    }

    @Listen
    public void onMotion(MotionEvent motionEvent) {
        Item item = minecraftClient.player.getMainHandItem().getItem();
        boolean z = (item instanceof ProjectileWeaponItem) || (item instanceof TridentItem);
        boolean isUsingItem = minecraftClient.player.isUsingItem();
        if (CrossbowItem.isCharged(minecraftClient.player.getMainHandItem())) {
            isUsingItem = true;
        }
        if (z && isUsingItem && motionEvent.getPhase() == KeyPearlMode.Pre) {
            Iterator it = getPotion1955().getEffects().iterator();
            while (it.hasNext()) {
                Holder effectType = ((MobEffectInstance) it.next()).getEffect();
                if (((MobEffect) effectType.value()).isBeneficial() && effectType != MobEffects.SLOW_FALLING && effectType != MobEffects.JUMP_BOOST) {
                    return;
                }
            }
            Entity entity2440 = WorldUtils.getNearestEntity(this.players.getValue().booleanValue(), this.neutrals.getValue().booleanValue(), this.animals.getValue().booleanValue(), this.hostiles.getValue().booleanValue(), false, false, 0.0f, 128.0f, false, this.ignoreNakeds.getValue().booleanValue());
            if (entity2440 == null || (entity2440 instanceof EnderMan)) {
                return;
            }
            AABB boundingBox = ((Entity) entity2440).getBoundingBox();
            if (entity2440 instanceof Player) {
                boundingBox = ClientServices.mainhandHelper.getPredictedBox((Player) entity2440, 5);
            }
            Vec3 center = boundingBox.getCenter();
            if (RotationManager.isInFrustum(boundingBox) && WorldUtils.isPositionVisible(center)) {
                float[] floatArray1956 = getFloatArray1956((Entity) entity2440, true, 2.0f);
                if (Float.isNaN(floatArray1956[0]) || Float.isNaN(floatArray1956[1])) {
                    return;
                }
                this.floatArr = floatArray1956;
                ClientServices.RotationManager.requestRotations(floatArray1956, 5, true);
            }
        }
    }

    @Listen
    public void onInteractItemPacket(InteractItemPacketEvent interactItemEvent_2) {
        ServerboundUseItemPacket playerInteractItemC2SPacket1816 = interactItemEvent_2.getInteractPacket();
        if (!minecraftClient.player.getItemInHand(playerInteractItemC2SPacket1816.getHand()).is(Items.CROSSBOW) || this.floatArr == null) {
            return;
        }
        interactItemEvent_2.setRotation(this.floatArr);
    }

    public Potion getPotion1955() {
        for (int i = 0; i < minecraftClient.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraftClient.player.getInventory().getItem(i);
            if (stack.getItem() instanceof ArrowItem) {
                return PotionResolver.resolvePotion(stack);
            }
        }
        return PotionResolver.potion;
    }

    public static float[] getFloatArray1956(Entity entity, boolean z, float f) {
        LocalPlayer clientPlayerEntity = minecraftClient.player;
        double x = (entity.getX() + (z ? (entity.getX() - entity.xo) * f : 0.0d)) - (clientPlayerEntity.getX() + (z ? clientPlayerEntity.getX() - clientPlayerEntity.xo : 0.0d));
        double eyeHeight = ((((entity.getBoundingBox().minY + (z ? (entity.getBoundingBox().minY - entity.yo) * f : 0.0d)) + entity.getEyeHeight(entity.getPose())) - 0.15d) - (clientPlayerEntity.getBoundingBox().minY + (z ? clientPlayerEntity.getY() - clientPlayerEntity.yo : 0.0d))) - clientPlayerEntity.getEyeHeight(clientPlayerEntity.getPose());
        double z2 = (entity.getZ() + (z ? (entity.getZ() - entity.zo) * f : 0.0d)) - (clientPlayerEntity.getZ() + (z ? clientPlayerEntity.getZ() - clientPlayerEntity.zo : 0.0d));
        double hypot = Math.hypot(x, z2);
        float itemUseTime = clientPlayerEntity.getTicksUsingItem() / 20.0f;
        if (clientPlayerEntity.isHolding(Items.CROSSBOW)) {
            itemUseTime = 1.0f;
        }
        float intBitsToFloat = ((itemUseTime * itemUseTime) + (itemUseTime * 2.0f)) / 3.0f;
        if (intBitsToFloat > 1.0f) {
            intBitsToFloat = 1.0f;
        }
        return new float[]{((float) ((Math.atan2(z2, x) * 180.0d) / MathConstants.val)) - MathConstants.num2, (float) (-Math.toDegrees(Math.atan(((intBitsToFloat * intBitsToFloat) - Math.sqrt((((intBitsToFloat * intBitsToFloat) * intBitsToFloat) * intBitsToFloat) - (0.006000000052154064d * ((0.006000000052154064d * (hypot * hypot)) + ((2.0d * eyeHeight) * (intBitsToFloat * intBitsToFloat)))))) / (0.006000000052154064d * hypot))))};
    }
}