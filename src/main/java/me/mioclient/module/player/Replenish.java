package me.mioclient.module.player;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.player.ScaffoldMode_2;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.time.Stopwatch;

import java.util.List;
import java.util.Set;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.combat.AutoCrystal;
import me.mioclient.module.exploit.XCarry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.core.NonNullList;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class Replenish extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving xCarry/autoCrystal null. */
    private static XCarry xCarry;
    private static AutoCrystal autoCrystal;

    public static XCarry getXCarry() {
        if (xCarry == null && ClientServices.moduleInjector != null) {
            try {
                xCarry = (XCarry) ClientServices.moduleInjector.getModule(XCarry.class);
            } catch (Exception e) {
            }
        }
        return xCarry;
    }

    public static AutoCrystal getAutoCrystal() {
        if (autoCrystal == null && ClientServices.moduleInjector != null) {
            try {
                autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
            } catch (Exception e) {
            }
        }
        return autoCrystal;
    }
    public Setting<Set<Item>> items = add(new EntityListObjectSetting<>("Items", BuiltInRegistries.ITEM));
    public Setting<ScaffoldMode_2> selection = add(new EnumSetting<>("Selection", ScaffoldMode_2.ANY));
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 50, 0, 500).setSuffix("ms"));
    public Setting<Integer> threshold = add(new NumberSetting<>("Threshold", 32, 16, 63));
    public Setting<Boolean> sort = add(new BooleanSetting("Sort", false));
    public Setting<Boolean> unstackable = add(new BooleanSetting("Unstackable", false));
    public Setting<Boolean> limit = add(new BooleanSetting("Limit", true).setDependent());
    public Setting<Boolean> motion = add(new BooleanSetting("Motion", false, v -> this.limit.isShown()));
    public Setting<Boolean> falling = add(new BooleanSetting("Falling", false, v -> this.limit.isShown()));
    public final List<Item> list;
    public final Stopwatch stopwatch;

    public Replenish() {
        super("Replenish", "Replenishes your hotbar.", Category.PLAYER, new String[0]);
        this.list = NonNullList.withSize(9, Items.AIR);
        this.stopwatch = new Stopwatch();
        setDrawn(false);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.stopwatch.hasElapsedMs(this.delay.getValue().intValue()) && minecraftClient.player.containerMenu.getCarried().isEmpty()) {
            if (MovementUtils.is2181() && this.motion.getValue().booleanValue()) {
                return;
            }
            if (minecraftClient.player.getDeltaMovement().y <= 0.0d || !this.falling.getValue().booleanValue()) {
                boolean z = minecraftClient.gui.screen() instanceof AbstractContainerScreen;
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = minecraftClient.player.getInventory().getItem(i);
                    Item item = this.list.get(i);
                    if (this.unstackable.getValue().booleanValue() && stack.isEmpty() && item != Items.AIR && !z) {
                        if (shouldReplenishItem(item, i)) {
                            this.stopwatch.reset();
                            return;
                        }
                    }
                    this.list.set(i, stack.getItem());
                }
                if (!z || (minecraftClient.gui.screen() instanceof InventoryScreen)) {
                    for (int i2 = 0; i2 < 9; i2++) {
                        if (is2852(i2)) {
                            this.stopwatch.reset();
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean shouldReplenishItem(Item item, int i) {
        if (!this.selection.getValue().isAllowed(item, this.items.getValue())) {
            return false;
        }
        AutoCrystal autoCrystal_lcl = getAutoCrystal();
        boolean z = autoCrystal_lcl != null && autoCrystal_lcl.isToggled() && autoCrystal_lcl.forceSuicide.getValue().booleanValue();
        int i2 = InventoryUtils.findSlot(item, itemStack -> {
            return (itemStack == minecraftClient.player.getOffhandItem() || (z && itemStack.getItem() == Items.TOTEM_OF_UNDYING)) ? false : true;
        }, true);
        if (i2 == -1 && item == Items.TOTEM_OF_UNDYING && !z) {
            i2 = InventoryUtils.findInInventory(itemStack2 -> {
                return is2851(itemStack2.getItem()) && itemStack2 != minecraftClient.player.getOffhandItem();
            }, true);
        }
        if (i2 >= 36 || i2 == -1) {
            return false;
        }
        if (!is2850(i)) {
            InventoryUtils.swapStacks(i2, InventoryUtils.toNetworkSlot(i));
            return true;
        }
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, i2, 0, ContainerInput.QUICK_MOVE, minecraftClient.player);
        return true;
    }

    public boolean is2850(int i) {
        for (int i2 = 0; i2 < 9; i2++) {
            if (i2 != i) {
                if (minecraftClient.player.getInventory().getItem(i2).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean is2851(Item item) {
        return item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE;
    }

    public boolean is2852(int i) {
        ItemStack stack = minecraftClient.player.getInventory().getItem(i);
        if (!this.selection.getValue().isAllowed(stack.getItem(), this.items.getValue())) {
            return false;
        }
        float intValue = this.threshold.getValue().intValue() / 64.0f;
        if (stack.isEmpty() || stack.getItem() == Items.AIR || !stack.isStackable() || stack.getCount() / stack.getMaxStackSize() >= intValue) {
            return false;
        }
        int i2 = -1;
        int i3 = 0;
        for (int i4 = 9; i4 < 36; i4++) {
            ItemStack stack2 = minecraftClient.player.getInventory().getItem(i4);
            boolean areItemsAndComponentsEqual = ItemStack.isSameItemSameComponents(stack2, stack);
            if (!stack2.isEmpty() && areItemsAndComponentsEqual && stack2.getCount() > i3) {
                i2 = i4;
                i3 = stack2.getCount();
            }
        }
        XCarry xCarry_lcl = getXCarry();
        if (xCarry_lcl != null && xCarry_lcl.isToggled() && !(minecraftClient.gui.screen() instanceof InventoryScreen)) {
            for (int i5 = 0; i5 <= 3; i5++) {
                ItemStack itemStack = minecraftClient.player.containerMenu.getSlot(i5).getItem();
                boolean areItemsAndComponentsEqual2 = ItemStack.isSameItemSameComponents(itemStack, stack);
                if (!itemStack.isEmpty() && areItemsAndComponentsEqual2 && itemStack.getCount() > i3) {
                    i2 = i5 + 1;
                    i3 = itemStack.getCount();
                }
            }
        }
        if (i2 == -1) {
            return false;
        }
        int i6 = minecraftClient.player.containerMenu.containerId;
        if (!this.sort.getValue().booleanValue() || i3 == 1 || stack.getMaxStackSize() - stack.getCount() <= i3) {
            if (!InventoryUtils.isHotbarFull()) {
                InventoryUtils.swapStacks(i2, InventoryUtils.toNetworkSlot(i));
                return true;
            }
            minecraftClient.gameMode.handleContainerInput(i6, i2, 0, ContainerInput.QUICK_MOVE, minecraftClient.player);
            return true;
        }
        ClientServices.fireworksHelper.setSyncBatching(true);
        minecraftClient.gameMode.handleContainerInput(i6, i2, 0, ContainerInput.PICKUP, minecraftClient.player);
        minecraftClient.gameMode.handleContainerInput(i6, i2, 1, ContainerInput.PICKUP, minecraftClient.player);
        minecraftClient.gameMode.handleContainerInput(i6, InventoryUtils.toNetworkSlot(i), 0, ContainerInput.PICKUP, minecraftClient.player);
        ClientServices.fireworksHelper.setSyncBatching(false);
        return true;
    }
}