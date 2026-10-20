package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.inventory.EnchantmentHelper;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.ContainerInput;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class AutoArmor extends Module {
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 50, 0, 500).setSuffix("ms"));
    public Setting<Boolean> preferences = add(new BooleanSetting("Preferences", false).setDependent());
    public Setting<Boolean> bindingCurse = add(new BooleanSetting("BindingCurse", false).requiresSettings(this.preferences));
    public Setting<Boolean> blast = add(new BooleanSetting("Blast", true).requiresSettings(this.preferences));
    public Setting<Boolean> thorns = add(new BooleanSetting("Thorns", false).requiresSettings(this.preferences));
    public Setting<Boolean> elytra = add(new BooleanSetting("Elytra", false).requiresSettings(this.preferences));
    public Setting<Boolean> turtleShell = add(new BooleanSetting("TurtleShell", false).requiresSettings(this.preferences));
    public Setting<Boolean> noHelmet = add(new BooleanSetting("NoHelmet", false).requiresSettings(this.preferences));
    public Setting<Boolean> onlySafe = add(new BooleanSetting("OnlySafe", false).requiresSettings(this.preferences, this.noHelmet));
    public Setting<Boolean> safe = add(new BooleanSetting("Safe", false).setVisible());
    public Setting<Integer> safeAmount = add(new NumberSetting<>("SafeAmount", 5, 1, 100, v -> this.safe.isShown()).setSuffix("%"));
    public Setting<Boolean> inRange = add(new BooleanSetting("InRange", false).setVisible());
    public Setting<Float> range = add(new NumberSetting<>("Range", 8.0f, 1.0f, 16.0f, v -> this.inRange.isShown()).setSuffix("m"));
    public Setting<Boolean> allowFriends = add(new BooleanSetting("AllowFriends", true, v -> this.inRange.isShown()));
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public final Stopwatch stopwatch3;
    public Inner inner;
    public boolean flag;

    
    
    public enum AutoArmorMode {
        HELMET(39, 3),
        CHESTPLATE(38, 2),
        LEGGINGS(37, 1),
        BOOTS(36, 0);

        public final int num;
        public final int num2;

        AutoArmorMode(int i, int i2) {
            this.num = i;
            this.num2 = i2;
        }

        public int getSlotIndex() {
            return this.num;
        }

        public int getArmorSlotIndex() {
            return this.num2;
        }

        public EquipmentSlot getEquipmentSlot() {
            return switch (this) {
                case HELMET -> EquipmentSlot.HEAD;
                case CHESTPLATE -> EquipmentSlot.CHEST;
                case LEGGINGS -> EquipmentSlot.LEGS;
                case BOOTS -> EquipmentSlot.FEET;
            };
        }
    }

    
    private class Inner {
        public boolean flag = false;
        public boolean flag2;
        public final AutoArmorMode autoArmorMode;
        public final int num;

        public Inner(AutoArmor autoArmor, AutoArmorMode autoArmorMode, int i) {
            this.autoArmorMode = autoArmorMode;
            this.num = i;
        }

        public void setFlag(boolean z) {
            this.flag2 = z;
        }

        public boolean isAllTasksComplete() {
            return this.flag;
        }

        public void runCallback() {
            if (!this.flag2) {
                int i = InventoryUtils.toNetworkSlot(this.num);
                ClientServices.fireworksHelper.setSyncBatching(true);
                InventoryUtils.swapStacks(i, 8 - this.autoArmorMode.getArmorSlotIndex());
                ClientServices.fireworksHelper.setSyncBatching(false);
                this.flag = true;
                return;
            }
            ClientAccessor.minecraftClient.gameMode.handleContainerInput(ClientAccessor.minecraftClient.player.containerMenu.containerId, 8 - this.autoArmorMode.num2, 0, ContainerInput.QUICK_MOVE, ClientAccessor.minecraftClient.player);
            this.flag = true;
        }
    }

    public AutoArmor() {
        super("AutoArmor", "Equips the best armor in your inventory for you.", Category.COMBAT, new String[0]);
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.stopwatch3 = new Stopwatch();
        this.inner = null;
        Runnable runnable = () -> {
            this.stopwatch.setTime(-1L);
        };
        this.blast.setChangeCallback(runnable);
        this.elytra.setChangeCallback(runnable);
        this.turtleShell.setChangeCallback(runnable);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.inner = null;
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.flag != ClientServices.holeSnapOverlayHelper.isInHole()) {
            this.stopwatch3.reset();
        }
        this.flag = ClientServices.holeSnapOverlayHelper.isInHole();
        if (isInventoryScreenSafe(minecraftClient.gui.screen())) {
            if (this.stopwatch.hasElapsedMs(this.delay.getValue().intValue())) {
                if (minecraftClient.player.isFallFlying()) {
                    this.stopwatch2.reset();
                }
                if (PlayerUtils.hasElytraEquipped(minecraftClient.player) && !minecraftClient.player.isFallFlying() && !EntityUtil.isOnGround(minecraftClient.player) && !this.stopwatch.hasElapsedMs(300L) && !this.stopwatch2.hasElapsedMs(500L)) {
                    PacketSender.sendStartFallFlying();
                    minecraftClient.player.startFallFlying();
                }
                boolean z = false;
                if (this.inRange.getValue().booleanValue()) {
                    for (AbstractClientPlayer abstractClientPlayerEntity : minecraftClient.level.players()) {
                        if (abstractClientPlayerEntity != minecraftClient.player && abstractClientPlayerEntity.distanceTo(minecraftClient.player) <= this.range.getValue().floatValue()) {
                            if (!ClientServices.socialManager.isFriend(abstractClientPlayerEntity.getGameProfile().name()) || !this.allowFriends.getValue().booleanValue()) {
                                z = true;
                            }
                        }
                    }
                } else {
                    z = true;
                }
                if (z) {
                    if (this.inner != null && !this.inner.isAllTasksComplete()) {
                        this.inner.runCallback();
                        this.stopwatch.reset();
                        return;
                    }
                    AutoArmorMode[] values = AutoArmorMode.values();
                    int length = values.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        AutoArmorMode autoArmorMode = values[i];
                        ItemStack stack = minecraftClient.player.getInventory().getItem(autoArmorMode.getSlotIndex());
                        if (!EnchantmentHelper.hasEnchantment(Enchantments.BINDING_CURSE, stack)) {
                            double d = getArmorProtectionScore(stack, autoArmorMode);
                            int i2 = autoArmorMode.getSlotIndex();
                            for (int i3 = 0; i3 < 36; i3++) {
                                double d2 = getArmorProtectionScore(minecraftClient.player.getInventory().getItem(i3), autoArmorMode);
                                if (d2 > d) {
                                    d = d2;
                                    i2 = i3;
                                }
                            }
                            if (i2 != autoArmorMode.getSlotIndex() && d != (-1.0d)) {
                                this.inner = new Inner(this, autoArmorMode, i2);
                                break;
                            }
                        }
                        i++;
                    }
                    if (shouldRemoveHelmet()) {
                        if (this.inner == null || this.inner.isAllTasksComplete()) {
                            ItemStack stack2 = minecraftClient.player.getInventory().getItem(AutoArmorMode.HELMET.getSlotIndex());
                            if (stack2.isEmpty() || EnchantmentHelper.hasEnchantment(Enchantments.BINDING_CURSE, stack2)) {
                                return;
                            }
                            this.inner = new Inner(this, AutoArmorMode.HELMET, -1);
                            this.inner.setFlag(true);
                        }
                    }
                }
            }
        }
    }

    public double getArmorProtectionScore(ItemStack itemStack, AutoArmorMode autoArmorMode) {
        float f = InventoryUtils.getDurabilityPercent(itemStack);
        if (this.safe.getValue().booleanValue() && f < this.safeAmount.getValue().intValue()) {
            return (-1.0d);
        }
        if (!this.bindingCurse.getValue().booleanValue() && EnchantmentHelper.hasEnchantment(Enchantments.BINDING_CURSE, itemStack)) {
            return (-1.0d);
        }
        EquipmentSlot slot = minecraftClient.player.getEquipmentSlotForItem(itemStack);
        if (slot == autoArmorMode.getEquipmentSlot()) {
            if (shouldRemoveHelmet() && slot == EquipmentSlot.HEAD) {
                return (-1.0d);
            }
            double armor = 0.0d;
            double toughness = 0.0d;
            ItemAttributeModifiers attributeModifiers = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (attributeModifiers != null) {
                for (ItemAttributeModifiers.Entry modifierEntry : attributeModifiers.modifiers()) {
                    if (modifierEntry.attribute().is(Attributes.ARMOR)) {
                        armor += modifierEntry.modifier().amount();
                    } else if (modifierEntry.attribute().is(Attributes.ARMOR_TOUGHNESS)) {
                        toughness += modifierEntry.modifier().amount();
                    }
                }
            }
            {
                double d = 0.0d;
                if (this.turtleShell.getValue().booleanValue() && itemStack.getItem() == Items.TURTLE_HELMET) {
                    d = 9999.0d;
                }
                double protection = d + (armor * 10) + (((int) toughness) * 10);
                if (itemStack.isEnchanted()) {
                    ResourceKey<Enchantment> registryKey = (autoArmorMode == AutoArmorMode.LEGGINGS || this.blast.getValue().booleanValue()) ? Enchantments.BLAST_PROTECTION : Enchantments.PROTECTION;
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemStack.getEnchantments().entrySet()) {
                        if (!entry.getKey().is(EnchantmentTags.CURSE)) {
                            protection = entry.getKey().is(registryKey) ? protection + (entry.getIntValue() * 3) : protection + (entry.getIntValue() * 0.1d);
                            if (entry.getKey().is(Enchantments.THORNS) && !this.thorns.getValue().booleanValue()) {
                                protection -= entry.getIntValue();
                            }
                        }
                    }
                }
                return protection;
            }
        }
        if (itemStack.getItem() != Items.ELYTRA || autoArmorMode != AutoArmorMode.CHESTPLATE) {
            return (-1.0d);
        }
        if (itemStack.getDamageValue() >= itemStack.getMaxDamage() - 1 || ExplosionDamageCalculator.getClampedDurabilityPercent(itemStack) <= 1) {
            return (-1.0d);
        }
        double longBitsToDouble = 1.0d;
        if (this.elytra.getValue().booleanValue()) {
            longBitsToDouble *= 9999.0d;
        }
        if (itemStack.isEnchanted()) {
            longBitsToDouble = longBitsToDouble + EnchantmentHelper.getEnchantmentLevel(Enchantments.UNBREAKING, itemStack) + EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, itemStack);
        }
        return longBitsToDouble;
    }

    public boolean isInventoryScreenSafe(Screen screen) {
        if (screen == null || (screen instanceof AbstractClickGuiScreen) || !(screen instanceof AbstractContainerScreen)) {
            return true;
        }
        return screen instanceof InventoryScreen;
    }

    public boolean shouldRemoveHelmet() {
        if (this.noHelmet.getValue().booleanValue()) {
            return !this.onlySafe.getValue().booleanValue() || (this.flag && this.stopwatch3.hasElapsedMs(500L));
        }
        return false;
    }
}