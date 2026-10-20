package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.util.combat.ExplosionDamageCalculator;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import java.text.DecimalFormat;
import java.util.Iterator;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.render.RenderEvent;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.exploit.IllegalDisconnect;
import me.mioclient.module.misc.AutoReconnect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class AutoLog extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static IllegalDisconnect illegaldisconnect;

    public static IllegalDisconnect getIllegaldisconnect() {
        if (illegaldisconnect == null && ClientServices.moduleInjector != null) {
            try {
                illegaldisconnect = (IllegalDisconnect) ClientServices.moduleInjector.getModule(IllegalDisconnect.class);
            } catch (Exception e) {
            }
        }
        return illegaldisconnect;
    }

    private static AutoReconnect autoreconnect;

    public static AutoReconnect getAutoreconnect() {
        if (autoreconnect == null && ClientServices.moduleInjector != null) {
            try {
                autoreconnect = (AutoReconnect) ClientServices.moduleInjector.getModule(AutoReconnect.class);
            } catch (Exception e) {
            }
        }
        return autoreconnect;
    }
    public Setting<Integer> health = add(new NumberSetting<>("Health", 12, 1, 20).setSuffix("hp"));
    public Setting<Float> invincTimeout = add(new NumberSetting<>("InvincTimeout", 0.0f, 0.0f, 4.0f).setSuffix("s"));
    public Setting<Boolean> autoDisable = add(new BooleanSetting("AutoDisable", true));
    public Setting<Boolean> gearedOnly = add(new BooleanSetting("GearedOnly", false));
    public Setting<Boolean> unfocusedOnly = add(new BooleanSetting("UnfocusedOnly", false));
    public Setting<Boolean> onRender = add(new BooleanSetting("OnRender", false));
    public Setting<Boolean> crystal = add(new BooleanSetting("Crystal", false));
    public Setting<Boolean> tNTMinecart = add(new BooleanSetting("TNTMinecart", false));
    public Setting<Boolean> totems = add(new BooleanSetting("Totems", false).setVisible());
    public Setting<Integer> totemCount = add(new NumberSetting<>("TotemCount", 1, 0, 16, v -> this.totems.isShown()));
    public Setting<Boolean> elytra = add(new BooleanSetting("Elytra", false).setVisible());
    public Setting<Integer> durability = add(new NumberSetting<>("Durability", 5, 1, 100).setSuffix("%").requiresSettings(this.elytra));
    public Setting<Boolean> assumeInventory = add(new BooleanSetting("AssumeInventory", false).requiresSettings(this.elytra));
    public Setting<Boolean> yLevel = add(new BooleanSetting("YLevel", false).setVisible());
    public Setting<Float> y = add(new NumberSetting<>("Y", 64.0f, -64.0f, 300.0f).setSuffix("m").requiresSettings(this.yLevel));
    public Runnable runnable;
    public final Stopwatch stopwatch;

    public AutoLog() {
        super("AutoLog", "Logs you out so you don't have to fight anyone.", Category.COMBAT, new String[0]);
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.runnable = null;
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return new DecimalFormat("0.0").format(this.health.getValue());
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (isNotInGame() || minecraftClient.player.isSpectator() || minecraftClient.player.isCreative()) {
            return;
        }
        if (!this.gearedOnly.getValue().booleanValue() || PlayerUtils.hasArmor(minecraftClient.player)) {
            if (this.elytra.getValue().booleanValue() && isElytraLowDurability()) {
                triggerDisconnect("Your elytra durability is too low!");
                return;
            }
            if (this.yLevel.getValue().booleanValue() && minecraftClient.player.getY() < this.y.getValue().floatValue()) {
                triggerDisconnect(new StringTemplate().arg(String.valueOf(this.y.getValue())).format("You are below Y level \u0001"));
                return;
            }
            if (minecraftClient.player.tickCount / 20.0f < this.invincTimeout.getValue().floatValue()) {
                return;
            }
            if (this.onRender.getValue().booleanValue()) {
                for (Player playerEntity : minecraftClient.level.players()) {
                    if (!ClientServices.socialManager.isFriend(playerEntity.getName().getString()) && playerEntity != minecraftClient.player) {
                        triggerDisconnect(new StringTemplate().arg(playerEntity.getName().getString()).format("\u0001 has entered your render distance!"));
                    }
                }
            }
            if (!minecraftClient.level.getEntities(EntityTypeTest.forClass(MinecartTNT.class), minecraftClient.player.getBoundingBox().inflate(4.0d, 4.0d, 4.0d), (v0) -> {
                return v0.isPrimed();
            }).isEmpty() && this.tNTMinecart.getValue().booleanValue()) {
                triggerDisconnect("You are at risk of getting blown up by a TNT minecart!");
            }
            if (this.crystal.getValue().booleanValue()) {
                minecraftClient.level.getEntities(EntityTypeTest.forClass(EndCrystal.class), minecraftClient.player.getBoundingBox().inflate(12.0d, 12.0d, 12.0d), (v0) -> {
                    return true;
                }).forEach(entity -> {
                    if (!(entity instanceof EndCrystal) || minecraftClient.player.distanceToSqr(entity) > 128.0d) {
                        return;
                    }
                    Vec3 pos = entity.position();
                    LivingEntity livingEntity = minecraftClient.player;
                    if (ExplosionDamageCalculator.getExplosionDamage(pos, livingEntity, minecraftClient.player.getBoundingBox(), 6.0d, true, (BlockPos) null, (BlockPos) null) >= EntityUtil.getPlayerHealth()) {
                        triggerDisconnect("You are at risk of getting blown up by an end crystal!");
                    }
                });
            }
            int i = (int) EntityUtil.getPlayerHealth();
            if (minecraftClient.player.isRemoved() || i == 0) {
                return;
            }
            if (minecraftClient.isWindowActive() && this.unfocusedOnly.getValue().booleanValue()) {
                return;
            }
            int sum = minecraftClient.player.getInventory().getNonEquipmentItems().stream().filter(itemStack -> {
                return itemStack.getItem() == Items.TOTEM_OF_UNDYING;
            }).mapToInt((v0) -> {
                return v0.getCount();
            }).sum();
            if (minecraftClient.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                sum += minecraftClient.player.getOffhandItem().getCount();
            }
            boolean z = sum <= this.totemCount.getValue().intValue();
            if (!this.totems.getValue().booleanValue()) {
                z = true;
            }
            if (i > this.health.getValue().intValue() || !z) {
                return;
            }
            triggerDisconnect(new StringTemplate().arg(i).format("Your health is too low! (\u0001)."));
        }
    }

    @Listen
    public void onRender(RenderEvent renderEvent) {
        if (isNotInGame()) {
            this.runnable = null;
        }
        if (this.runnable == null || !this.stopwatch.hasElapsedMs(1500L)) {
            return;
        }
        this.runnable.run();
        this.runnable = null;
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundDisconnectPacket) {
            this.runnable = null;
        }
    }

    public boolean isElytraLowDurability() {
        ItemStack armorStack = minecraftClient.player.getItemBySlot(EquipmentSlot.CHEST);
        if (!armorStack.is(Items.ELYTRA) || !armorStack.isDamageableItem()) {
            return false;
        }
        if (InventoryUtils.getDurabilityPercent(armorStack) > ((float) this.durability.getValue().intValue())) {
            return false;
        }
        if (!this.assumeInventory.getValue().booleanValue()) {
            return true;
        }
        Iterator it = minecraftClient.player.getInventory().getNonEquipmentItems().iterator();
        while (it.hasNext()) {
            ItemStack itemStack = (ItemStack) it.next();
            if (itemStack.is(Items.ELYTRA) && (!itemStack.isDamageableItem() || InventoryUtils.getDurabilityPercent(itemStack) > this.durability.getValue().intValue())) {
                return false;
            }
        }
        return true;
    }

    public void triggerDisconnect(String str) {
        if (this.autoDisable.getValue().booleanValue()) {
            disable();
        }
        Connection connection = minecraftClient.player.connection.getConnection();
        if (connection != null) {
            ClientServices.holeSnapRenderHelper.flag3 = true;
            AutoReconnect autoreconnect_lcl = getAutoreconnect();
            if (autoreconnect_lcl != null) {
                autoreconnect_lcl.disable();
            }
            Runnable runnable = () -> {
                connection.disconnect(Component.empty().append(Component.literal(new StringTemplate().arg(str).arg(String.valueOf(ChatFormatting.RED)).arg(String.valueOf(ChatFormatting.GRAY)).format("\u0001[AutoLog] \u0001\u0001"))));
            };
            IllegalDisconnect illegaldisconnect_lcl = getIllegaldisconnect();
            boolean z = (illegaldisconnect_lcl != null && illegaldisconnect_lcl.ignoreProxy.getValue().booleanValue() && IllegalDisconnect.isKnownProxy()) ? false : true;
            if (illegaldisconnect_lcl == null || !illegaldisconnect_lcl.isToggled() || !z) {
                runnable.run();
                return;
            }
            IllegalDisconnect.sendDisconnectPacket();
            if (this.runnable == null) {
                this.stopwatch.reset();
            }
            this.runnable = runnable;
        }
    }
}