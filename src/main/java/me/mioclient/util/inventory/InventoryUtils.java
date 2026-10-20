package me.mioclient.util.inventory;

import me.mioclient.api.ClientServices;

import me.mioclient.api.ClientAccessor;

import java.util.Iterator;
import me.mioclient.module.player.ItemSaver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;

public class InventoryUtils implements ClientAccessor {
    public static ItemSaver itemSaver = (ItemSaver) ClientServices.moduleInjector.getModule(ItemSaver.class);

    public static void do438(int i) {
        if (i < 0 || i > 8) {
            return;
        }
        minecraftClient.player.getInventory().setSelectedSlot(i);
        
    }

    public static void swapToHotbar(int i) {
        if (i < 0) {
            return;
        }
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, toNetworkSlot(i), minecraftClient.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, minecraftClient.player);
    }

    public static void swapSlots(int i, int i2) {
        if (i < 0 || i2 < 0) {
            return;
        }
        int i3 = minecraftClient.player.containerMenu.containerId;
        minecraftClient.gameMode.handleContainerInput(i3, i, i2, ContainerInput.SWAP, minecraftClient.player);
    }

    public static void swapStacks(int i, int i2) {
        if (i == -1 || i2 == -1) {
            return;
        }
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, i, 0, ContainerInput.PICKUP, minecraftClient.player);
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, i2, 0, ContainerInput.PICKUP, minecraftClient.player);
        if (minecraftClient.player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        minecraftClient.gameMode.handleContainerInput(minecraftClient.player.containerMenu.containerId, i, 0, ContainerInput.PICKUP, minecraftClient.player);
    }

    public static int findSlot(Item item, boolean z) {
        return findSlot(item, itemStack -> {
            return true;
        }, z);
    }

    public static int findSlot(Item item) {
        return findSlot(item, itemStack -> {
            return true;
        }, false);
    }

    public static int findSlot(Item item, java.util.function.Predicate<ItemStack> predicate, boolean z) {
        if (!ClientServices.fireworksHelper.hasPendingTotemPop() && item.equals(minecraftClient.player.getOffhandItem().getItem())) {
            if (predicate.test(minecraftClient.player.getOffhandItem())) {
                return -1;
            }
        }
        int i = 36;
        while (i >= 0) {
            ItemStack stack = minecraftClient.player.getInventory().getItem(i);
            if (stack.getItem() == item && predicate.test(stack)) {
                if (i < 9) {
                    i += 36;
                }
                return i;
            }
            i--;
        }
        if (!z) {
            return -1;
        }
        for (int i2 = 0; i2 <= 3; i2++) {
            ItemStack itemStack = (ItemStack) minecraftClient.player.inventoryMenu.getInputGridSlots().get(i2).getItem();
            if (itemStack.getItem() == item && predicate.test(itemStack)) {
                return i2 + 1;
            }
        }
        return -1;
    }

    public static int findInInventory(java.util.function.Predicate<ItemStack> predicate, boolean z) {
        int i = 36;
        while (true) {
            if (i < (z ? 9 : 0)) {
                return -1;
            }
            if (predicate.test(minecraftClient.player.getInventory().getItem(i))) {
                return i;
            }
            i--;
        }
    }

    public static int findInHotbarAny(Item... itemArr) {
        return findInHotbar(itemStack -> {
            for (Item item : itemArr) {
                if (item.equals(itemStack.getItem())) {
                    return true;
                }
            }
            return false;
        });
    }

    public static int findInHotbar(Item item) {
        return findInHotbar(itemStack -> {
            return itemStack.getItem().equals(item);
        });
    }

    public static int findInHotbar(java.util.function.Predicate<ItemStack> predicate) {
        for (int i = 0; i < 9; i++) {
            if (predicate.test(minecraftClient.player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    public static int countItems(java.util.function.Predicate<ItemStack> predicate) {
        int i = 0;
        for (int i2 = 40; i2 >= 0; i2--) {
            ItemStack stack = minecraftClient.player.getInventory().getItem(i2);
            if (predicate.test(stack)) {
                i += stack.getCount();
            }
        }
        for (int i3 = 0; i3 <= 3; i3++) {
            ItemStack itemStack = (ItemStack) minecraftClient.player.inventoryMenu.getInputGridSlots().get(i3).getItem();
            if (predicate.test(itemStack)) {
                i += itemStack.getCount();
            }
        }
        return i;
    }

    public static InteractionHand getHandWith(Item item) {
        if (minecraftClient.player.getOffhandItem().getItem() == item) {
            return InteractionHand.OFF_HAND;
        }
        if (minecraftClient.player.getMainHandItem().getItem() == item) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    public static float getArmorDurability(Player playerEntity) {
        float intBitsToFloat = 100.0f;
        java.util.List<ItemStack> armorStacks = java.util.Arrays.asList(playerEntity.getItemBySlot(EquipmentSlot.FEET), playerEntity.getItemBySlot(EquipmentSlot.LEGS), playerEntity.getItemBySlot(EquipmentSlot.CHEST), playerEntity.getItemBySlot(EquipmentSlot.HEAD));
        Iterator it = armorStacks.iterator();
        while (it.hasNext()) {
            float f = getDurabilityPercent((ItemStack) it.next());
            if (f < intBitsToFloat) {
                intBitsToFloat = f;
            }
        }
        return intBitsToFloat;
    }

    public static float getDurabilityPercent(ItemStack itemStack) {
        return 100 - ((int) ((1.0f - ((itemStack.getMaxDamage() - itemStack.getDamageValue()) / (float) itemStack.getMaxDamage())) * 100.0f));
    }

    public static int toNetworkSlot(int i) {
        return (i <= -1 || i >= 9) ? i : 36 + i;
    }

    public static boolean isInventoryFull() {
        for (int i = 36; i >= 0; i--) {
            if (minecraftClient.player.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isHotbarFull() {
        for (int i = 0; i < 9; i++) {
            if (minecraftClient.player.getInventory().getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void selectSlot(int i) {
        if (i == -1) {
            return;
        }
        minecraftClient.player.getInventory().setSelectedSlot(i);
        
    }

    public static boolean fitsEquipmentSlot(EquipmentSlot equipmentSlot, ItemStack itemStack) {
        if (equipmentSlot == null) {
            return false;
        }
        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        return equippable.slot() == equipmentSlot;
    }

    public static EquipmentSlot getEquipmentSlot(int i) {
        switch (i) {
            case 36:
                return EquipmentSlot.FEET;
            case 37:
                return EquipmentSlot.LEGS;
            case 38:
                return EquipmentSlot.CHEST;
            case 39:
                return EquipmentSlot.HEAD;
            default:
                return null;
        }
    }

    public static int getBestToolSlot(BlockPos blockPos, boolean z) {
        double longBitsToDouble = (-1.0d);
        int i = -1;
        BlockState blockState = minecraftClient.level.getBlockState(blockPos);
        int i2 = 0;
        while (true) {
            if (i2 >= (!z ? 36 : 9)) {
                return i;
            }
            ItemStack stack = minecraftClient.player.getInventory().getItem(i2);
            if (itemSaver.isToggled()) {
                ItemSaver itemSaver2 = itemSaver;
                if (!ItemSaver.isToolSafe(stack)) {
                    i2++;
                }
            }
            double miningSpeedMultiplier = stack.getDestroySpeed(blockState);
            if (miningSpeedMultiplier > longBitsToDouble) {
                longBitsToDouble = miningSpeedMultiplier;
                i = i2;
            } else if (i2 == minecraftClient.player.getInventory().getSelectedSlot() && miningSpeedMultiplier == longBitsToDouble) {
                i = i2;
            }
            i2++;
        }
    }

    public static boolean isShulkerBox(Item item) {
        return (item instanceof BlockItem) && (((BlockItem) item).getBlock() instanceof ShulkerBoxBlock);
    }
}
