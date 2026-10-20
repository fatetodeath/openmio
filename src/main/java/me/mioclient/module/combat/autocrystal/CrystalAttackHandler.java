package me.mioclient.module.combat.autocrystal;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.ScaffoldHelperMode;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;
import me.mioclient.network.PacketSender;

import me.mioclient.event.EventBus;

import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.combat.AutoCrystal;
import me.mioclient.module.misc.Swing;
import me.mioclient.module.render.Hitmarker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;

public class CrystalAttackHandler implements ClientAccessor {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving swing/ac/hitmarker null. */
    private static Swing swing;
    private static AutoCrystal ac;
    private static Hitmarker hitmarker;

    public static Swing getSwing() {
        if (swing == null && ClientServices.moduleInjector != null) {
            try {
                swing = (Swing) ClientServices.moduleInjector.getModule(Swing.class);
            } catch (Exception e) {
            }
        }
        return swing;
    }

    public static AutoCrystal getAc() {
        if (ac == null && ClientServices.moduleInjector != null) {
            try {
                ac = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
            } catch (Exception e) {
            }
        }
        return ac;
    }

    public static Hitmarker getHitmarker() {
        if (hitmarker == null && ClientServices.moduleInjector != null) {
            try {
                hitmarker = (Hitmarker) ClientServices.moduleInjector.getModule(Hitmarker.class);
            } catch (Exception e) {
            }
        }
        return hitmarker;
    }
    public final Stopwatch stopwatch = new Stopwatch();
    public long num;
    public int current;

    public CrystalAttackHandler() {
        eventBus.register(this);
    }

    @Listen(get219= -300)
    public void onTick(TickEvent tickEvent) {
        this.current = -1;
    }

    public void setCurrentTarget(int i) {
        this.current = i;
    }

    public boolean tryAttackCrystal(int i) {
        return sendAttackPacket(i, false);
    }

    public boolean sendAttackPacket(int i, boolean z) {
        if (!isInstantAttackEnabled() && z) {
            return false;
        }
        if (this.current != -1 && !z) {
            return false;
        }
        int i2 = minecraftClient.player.getInventory().getSelectedSlot();
        boolean isWeaknessSwapActive = isWeaknessSwapActive();
        this.current = i;
        PacketSender.sendInteractEntity(this.current);
        Entity entityById = minecraftClient.level.getEntity(i);
        Hitmarker hitmarker_lcl = getHitmarker();
        if (entityById != null && entityById.level() != null && entityById != minecraftClient.player && entityById.level().isClientSide() && hitmarker_lcl != null && hitmarker_lcl.shouldTrackEntity(entityById) && (entityById instanceof EndCrystal)) {
            hitmarker_lcl.num = System.currentTimeMillis();
            hitmarker_lcl.num2 = 255;
            hitmarker_lcl.flag = true;
        }
        InteractionHand hand = minecraftClient.player.getOffhandItem().getItem() == Items.END_CRYSTAL ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(entityById instanceof EndCrystal) && entityById != null) {
            hand = InteractionHand.MAIN_HAND;
        }
        do1117(hand);
        if (z) {
            PacketSender.sendHandSwing(InteractionHand.MAIN_HAND);
        } else {
            minecraftClient.player.swing(hand);
        }
        AutoCrystal ac_lcl = getAc();
        if (ac_lcl == null || !isWeaknessSwapActive || ac_lcl.weakness.getValue() != ScaffoldHelperMode.SILENT) {
            return true;
        }
        ac_lcl.incrementSwapCounter();
        InventoryUtils.selectSlot(i2);
        return true;
    }

    public boolean isInstantAttackEnabled() {
        AutoCrystal ac_lcl = getAc();
        if (ac_lcl == null) {
            return false;
        }
        return ac_lcl.instant.getValue() != AutoCrystalMode.NONE;
    }

    public int getAttackDelay() {
        AutoCrystal ac_lcl = getAc();
        if (ac_lcl == null) {
            return 0;
        }
        return ac_lcl.delay2.getValue().intValue();
    }

    public int get1116() {
        AutoCrystal ac_lcl = getAc();
        if (ac_lcl == null) {
            return 0;
        }
        return ac_lcl.ticksExisted.getValue().intValue();
    }

    public void do1117(InteractionHand hand) {
        Swing swing_lcl = getSwing();
        if (swing_lcl == null) {
            return;
        }
        if (!this.stopwatch.hasElapsedAndResetMs(200 + this.num) || hand != InteractionHand.MAIN_HAND || swing_lcl.hand.getValue() == Swing.ScaffoldHelperMode.scaffoldHelperMode2 || swing_lcl.type.getValue() == Swing.MixinLivingEntityMode.VANILLA) {
            return;
        }
        minecraftClient.player.resetAttackStrengthTicker();
        this.num = (long) (Math.random() * 300.0d);
    }

    public boolean isWeaknessSwapActive() {
        int i;
        AutoCrystal ac_lcl = getAc();
        if (ac_lcl == null || ac_lcl.weakness.getValue() == ScaffoldHelperMode.NONE) {
            return false;
        }
        if (!minecraftClient.player.hasEffect(MobEffects.WEAKNESS) || minecraftClient.player.getMainHandItem().has(DataComponents.TOOL) || (i = InventoryUtils.findInHotbar(itemStack -> {
            return itemStack.is(ItemTags.SWORDS);
        })) == -1) {
            return false;
        }
        if (ac_lcl.weakness.getValue() == ScaffoldHelperMode.SILENT) {
            ac_lcl.incrementSwapCounter();
        }
        InventoryUtils.selectSlot(i);
        return true;
    }
}