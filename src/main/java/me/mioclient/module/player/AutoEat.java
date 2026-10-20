package me.mioclient.module.player;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.EntityUtil;

import java.util.Iterator;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AutoEat extends Module {
    public Setting<Integer> health = add(new NumberSetting<>("Health", 16, 0, 36).setHoleSnapConfig("Off", HoleSnapMode.MIN));
    public Setting<Integer> hunger = add(new NumberSetting<>("Hunger", 6, -1, 20).setHoleSnapConfig("Off", HoleSnapMode.MIN));
    public Setting<Boolean> pauseBaritone = add(new BooleanSetting("PauseBaritone", false));
    public Setting<Boolean> autoSwap = add(new BooleanSetting("AutoSwap", true).setVisible());
    public Setting<AutoEatMode> sort = add(new EnumSetting<>("Sort", AutoEatMode.autoEatMode2).requiresSettings(this.autoSwap));
    public Setting<Boolean> swapBack = add(new BooleanSetting("SwapBack", false).requiresSettings(this.autoSwap));
    public Setting<Boolean> autoEscape = add(new BooleanSetting("AutoEscape", false).setVisible());
    public Setting<Boolean> autoDisable = add(new BooleanSetting("AutoDisable", false).requiresSettings(this.autoEscape));
    public int num;
    public boolean flag;
    public int num2;

    

    
    public static enum AutoEatMode implements NamedValue {
        autoEatMode("Hunger") {
            @Override
            public double scoreFood(FoodProperties foodComponent) {
                return foodComponent.nutrition();
            }
        },
        autoEatMode2("Saturation") {
            @Override
            public double scoreFood(FoodProperties foodComponent) {
                return foodComponent.saturation();
            }
        },
        autoEatMode3("Health") {
            @Override
            public double scoreFood(FoodProperties foodComponent) {
                return 0.0d;
            }
        };

        public final String name;

        AutoEatMode(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public double scoreFood(FoodProperties foodComponent) {
            return 0.0d;
        }
    }

    public AutoEat() {
        super("AutoEat", "Eats your food for you.", Category.PLAYER, new String[0]);
        this.pauseBaritone.requireBaritone();
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        setEating(false);
        this.num = -1;
        this.num2 = -1;
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        minecraftClient.options.keyUse.setDown(false);
        ClientServices.obstaclePasserHelper.removePauseRequest(this);
    }

    @Listen
    
    public void onResetEvent(MotionEvent motionEvent) {
        int i;
        if (motionEvent.getPhase() == KeyPearlMode.Post) {
            return;
        }
        ClientServices.obstaclePasserHelper.removePauseRequest(this);
        if (!this.autoSwap.getValue().booleanValue() && this.flag) {
            if (!minecraftClient.player.getMainHandItem().has(DataComponents.FOOD)) {
                minecraftClient.gameMode.releaseUsingItem(minecraftClient.player);
                setEating(false);
                return;
            }
        }
        int i2 = get2085();
        if (i2 != -1) {
        }
        if (isInDangerousProximity() && (i = InventoryUtils.findSlot(Items.CHORUS_FRUIT)) != -1) {
            i2 = -1;
            this.num2 = i;
            do438(this.num2);
        }
        if (is2088() && !isInDangerousProximity()) {
            if (this.flag) {
                minecraftClient.gameMode.releaseUsingItem(minecraftClient.player);
                setEating(false);
                if (this.autoSwap.getValue().booleanValue() && this.swapBack.getValue().booleanValue() && this.num != -1) {
                    InventoryUtils.selectSlot(this.num);
                }
                if (this.num2 != -1) {
                    do438(this.num2);
                    this.num2 = -1;
                }
                if (this.autoDisable.getValue().booleanValue() && this.autoEscape.getValue().booleanValue()) {
                    disable();
                }
                this.num = -1;
                return;
            }
            return;
        }
        boolean z = i2 == 40;
        if (this.autoSwap.getValue().booleanValue() && !z && i2 != -1 && minecraftClient.player.getInventory().getSelectedSlot() != i2) {
            this.num = minecraftClient.player.getInventory().getSelectedSlot();
            InventoryUtils.selectSlot(i2);
        }
        ItemStack mainHandStack = minecraftClient.player.getMainHandItem();
        if (!minecraftClient.player.isUsingItem() && (z || mainHandStack.has(DataComponents.FOOD))) {
            minecraftClient.gameMode.useItem(minecraftClient.player, z ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            setEating(true);
        }
        if (this.pauseBaritone.getValue().booleanValue() && minecraftClient.player.isUsingItem()) {
            ClientServices.obstaclePasserHelper.addPauseRequest(this);
        }
    }

    public int get2085() {
        double longBitsToDouble = (-1.0d);
        int i = -1;
        int i2 = 0;
        while (true) {
            if (i2 >= 10) {
                break;
            }
            if (i2 == 9) {
                i2 = 40;
            }
            ItemStack stack = minecraftClient.player.getInventory().getItem(i2);
            FoodProperties foodComponent = stack.get(DataComponents.FOOD);
            if (foodComponent != null && (foodComponent.canAlwaysEat() || minecraftClient.player.getFoodData().getFoodLevel() < 20)) {
                double d = this.sort.getValue().scoreFood(foodComponent);
                if (d > longBitsToDouble || (d == longBitsToDouble && i2 == minecraftClient.player.getInventory().getSelectedSlot())) {
                    longBitsToDouble = d;
                    i = i2;
                }
                if (stack.getItem() == Items.CHORUS_FRUIT && isInDangerousProximity()) {
                    i = i2;
                    break;
                }
            }
            i2++;
        }
        return i;
    }

    public boolean is2086() {
        return this.flag;
    }

    public boolean isInDangerousProximity() {
        if (!this.autoEscape.getValue().booleanValue()) {
            return false;
        }
        LocalPlayer clientPlayerEntity = null;
        Iterator it = minecraftClient.level.players().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LocalPlayer clientPlayerEntity2 = (LocalPlayer)((Player) it.next());
            if (((Player) clientPlayerEntity2).distanceTo(minecraftClient.player) <= 12.0f && !ClientServices.socialManager.isFriendEntity((Player) clientPlayerEntity2) && clientPlayerEntity2 != minecraftClient.player) {
                clientPlayerEntity = clientPlayerEntity2;
                break;
            }
        }
        return clientPlayerEntity != null && minecraftClient.player.tickCount <= 50;
    }

    public boolean is2088() {
        return (this.hunger.getValue().intValue() == -1 || minecraftClient.player.getFoodData().getFoodLevel() > this.hunger.getValue().intValue()) && (this.health.getValue().intValue() == 0 || EntityUtil.getPlayerHealth() > ((float) this.health.getValue().intValue()));
    }

    public void setEating(boolean z) {
        if (this.flag == z) {
            return;
        }
        this.flag = z;
    }

    public void do438(int i) {
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, i, minecraftClient.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, minecraftClient.player);
    }
}