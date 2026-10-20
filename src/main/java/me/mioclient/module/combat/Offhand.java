package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.module.combat.offhand.OffhandMode;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.client.InputState;
import me.mioclient.util.combat.CrystalThreatDetector;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.network.PacketSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.InteractBlockEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.render.RenderEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.player.NoInteract;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import me.mioclient.module.Lag;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class Offhand extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static AutoCrystal autoCrystal;

    public static AutoCrystal getAutoCrystal() {
        if (autoCrystal == null && ClientServices.moduleInjector != null) {
            try {
                autoCrystal = (AutoCrystal) ClientServices.moduleInjector.getModule(AutoCrystal.class);
            } catch (Exception e) {
            }
        }
        return autoCrystal;
    }

    private static Offhand offhand;

    public static Offhand getOffhand() {
        if (offhand == null && ClientServices.moduleInjector != null) {
            try {
                offhand = (Offhand) ClientServices.moduleInjector.getModule(Offhand.class);
            } catch (Exception e) {
            }
        }
        return offhand;
    }

    private static NoInteract noInteract;

    public static NoInteract getNoInteract() {
        if (noInteract == null && ClientServices.moduleInjector != null) {
            try {
                noInteract = (NoInteract) ClientServices.moduleInjector.getModule(NoInteract.class);
            } catch (Exception e) {
            }
        }
        return noInteract;
    }

    private static AntiCheat antiCheat;

    public static AntiCheat getAntiCheat() {
        if (antiCheat == null && ClientServices.moduleInjector != null) {
            try {
                antiCheat = (AntiCheat) ClientServices.moduleInjector.getModule(AntiCheat.class);
            } catch (Exception e) {
            }
        }
        return antiCheat;
    }
    public Setting<OffhandMode> item = add(new EnumSetting<>("Item", OffhandMode.Totem));
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 25, 0, 500).setSuffix("ms"));
    public Setting<Float> health = add(new NumberSetting<>("Health", 16.0f, 2.0f, 36.0f).setSuffix("hp").setHoleSnapConfig("Adaptive", HoleSnapMode.MIN));
    public Setting<Boolean> lethal = add(new BooleanSetting("Lethal", true));
    public Setting<Boolean> f117 = add(new BooleanSetting("1.17+", true));
    public Setting<Boolean> containers = add(new BooleanSetting("Containers", true));
    public Setting<Boolean> gappleBind = add(new BooleanSetting("GappleBind", true).setVisible());
    public Setting<Float> safe = add(new NumberSetting<>("Safe", 8.0f, 0.0f, 36.0f, v -> this.gappleBind.isShown()).setSuffix("hp"));
    public Setting<Boolean> pickaxe = add(new BooleanSetting("Pickaxe", true, v -> this.gappleBind.isShown()));
    public Setting<Boolean> crystals = add(new BooleanSetting("Crystals", true, v -> this.gappleBind.isShown()));
    public Setting<Boolean> totems = add(new BooleanSetting("Totems", false).requiresSettings(this.gappleBind));
    public Setting<Boolean> crapple = add(new BooleanSetting("Crapple", false, v -> this.item.getValue() == OffhandMode.Gapple || this.gappleBind.isShown()));
    public Setting<Set<Item>> custom = add(new EntityListObjectSetting<>("Custom", BuiltInRegistries.ITEM));
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public final Stopwatch stopwatch3;
    public final Stopwatch stopwatch4;
    public final List<Long> list;
    public int num;
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public final CrystalThreatDetector mainhandHelper;
    public static boolean flag4;

    
    class Inner extends CrystalThreatDetector {
        public Inner() {
        }

        @Override // me.mioclient.util.combat.CrystalThreatDetector
        public boolean isEnabled() {
            if (minecraftClient.player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                return false;
            }
            return Offhand.this.lethal.getValue().booleanValue() || Offhand.this.health.isAtMinimum();
        }
    }

    public Offhand() {
        super("Offhand", "Manages your offhand automatically.", Category.COMBAT, "autototem");
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
        this.stopwatch3 = new Stopwatch();
        this.stopwatch4 = new Stopwatch();
        this.list = new ArrayList();
        this.num = -1;
        this.mainhandHelper = new Inner();
        this.health.setHoleSnapConfig("Adaptive", HoleSnapMode.MIN);
        this.lethal.addVisibilityPredicate(bool -> {
            return !this.health.isAtMinimum();
        });
        this.gappleBind.setConfigName("GappleBind");
        this.safe.setConfigName("SafeHealth");
        setupExclusiveItemCallback(this.custom);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return this.item.getValue().toString();
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.num = -1;
    }

    @Listen(get219= RenderConstants.num2)
    public void onRender(RenderEvent renderEvent) {
        this.flag2 = renderEvent.isThirdPerson();
        tickOffhand();
    }

    public void tickOffhand() {
        if (isNotInGame()) {
            return;
        }
        if (minecraftClient.player.isAlive()) {
            this.flag3 = false;
        }
        flag4 = false;
        this.mainhandHelper.recalculateLayout();
        if (!isForceSuicideTotemMode() && isOffhandSwapAllowed()) {
            if (this.stopwatch.hasElapsedMs(this.delay.getValue().intValue())) {
                this.list.removeIf(l -> {
                    return l == null || System.currentTimeMillis() > l.longValue() + 1000;
                });
                if (this.list.size() > 4) {
                    this.stopwatch2.reset();
                }
                do906();
            }
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundEntityEventPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundEntityEventPacket ? (ClientboundEntityEventPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundEntityEventPacket) {
            ClientboundEntityEventPacket entityStatusS2CPacket = packet904;
            if (!isForceSuicideTotemMode() && isOffhandSwapAllowed() && entityStatusS2CPacket.getEntity(minecraftClient.level) == minecraftClient.player && entityStatusS2CPacket.getEventId() == 35) {
                if (minecraftClient.player.getMainHandItem().getItem() != Items.TOTEM_OF_UNDYING) {
                    if (minecraftClient.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                        minecraftClient.player.getOffhandItem().shrink(1);
                    }
                }
                this.stopwatch4.reset();
                this.list.clear();
                do906();
            }
        }
        ClientboundAddEntityPacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundAddEntityPacket ? (ClientboundAddEntityPacket) (channelRead0Event.getPacket()) : null;
        if (packet9042 instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket entitySpawnS2CPacket = packet9042;
            if (this.mainhandHelper.isEnabled() && entitySpawnS2CPacket.getType() == EntityTypes.END_CRYSTAL) {
                if (this.mainhandHelper.isLethalExplosionAt(new Vec3(entitySpawnS2CPacket.getX(), entitySpawnS2CPacket.getY(), entitySpawnS2CPacket.getZ()))) {
                    this.stopwatch3.reset();
                }
            }
        }
        if (channelRead0Event.getPacket() instanceof ClientboundPlayerCombatKillPacket) {
            if (minecraftClient.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
                do2667();
            }
        }
    }

    @Listen
    public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
        AntiCheat antiCheat_lcl = getAntiCheat();
        if (interactBlockEvent.getHand() != InteractionHand.MAIN_HAND || resolveOffhandMode((OffhandMode) null) == null || (antiCheat_lcl != null && antiCheat_lcl.isF2b2t())) {
            return;
        }
        minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.OFF_HAND);
        interactBlockEvent.cancel();
    }

    public OffhandMode getOffhandMode2661() {
        AutoCrystal autoCrystal_lcl = getAutoCrystal();
        if (autoCrystal_lcl != null && autoCrystal_lcl.isToggled() && autoCrystal_lcl.forceSuicide.getValue().booleanValue() && this.item.getValue() != OffhandMode.Totem) {
            return OffhandMode.Crystal;
        }
        boolean booleanValue = this.crapple.getValue().booleanValue();
        OffhandMode offhandMode = OffhandMode.Totem;
        if (this.mainhandHelper.isThreatActive()) {
            return OffhandMode.Totem;
        }
        if (EntityUtil.getPlayerHealth() > this.health.getValue().floatValue() && minecraftClient.player.fallDistance < 16.0f && InventoryUtils.countItems(itemStack -> {
            return itemStack.getItem() == this.item.getValue().getOffhandItem(booleanValue);
        }) > 0) {
            offhandMode = this.item.getValue();
        }
        return resolveOffhandMode(offhandMode);
    }

    public boolean is2662(ItemStack itemStack) {
        if (itemStack.is(holder -> holder.is(ItemTags.SWORDS)) || (itemStack.getItem() instanceof AxeItem)) {
            return true;
        }
        if (itemStack.is(holder -> holder.is(ItemTags.PICKAXES)) && this.pickaxe.getValue().booleanValue()) {
            return true;
        }
        Item item = itemStack.getItem();
        if (item == Items.TOTEM_OF_UNDYING && this.totems.getValue().booleanValue()) {
            return true;
        }
        return item == Items.END_CRYSTAL && this.crystals.getValue().booleanValue();
    }

    public OffhandMode resolveOffhandMode(OffhandMode offhandMode) {
        if (offhandMode != null) {
            if (offhandMode.getOffhandItem(this.crapple.getValue().booleanValue()) instanceof ShieldItem) {
                return offhandMode;
            }
        }
        NoInteract noInteract_lcl = getNoInteract();
        if (WorldUtils.isInteractableHit(minecraftClient.hitResult) && !minecraftClient.player.isShiftKeyDown() && (noInteract_lcl == null || !noInteract_lcl.isToggled())) {
            return offhandMode;
        }
        float f = EntityUtil.getPlayerHealth();
        Item item = minecraftClient.player.getMainHandItem().getItem();
        boolean z = this.safe.getValue().floatValue() < f;
        if (item == Items.TOTEM_OF_UNDYING) {
            z = true;
        }
        return (this.gappleBind.getValue().booleanValue() && minecraftClient.options.keyUse.isDown() && z && is2662(minecraftClient.player.getMainHandItem()) && minecraftClient.gui.screen() == null && this.item.getValue() != OffhandMode.Gapple) ? OffhandMode.Gapple : offhandMode;
    }

    public void beginOffhandSwap() {
        this.list.add(Long.valueOf(System.currentTimeMillis()));
        ClientServices.fireworksHelper.setSyncBatching(true);
        if (ClientServices.holeSnapRenderHelper.isSprinting()) {
            PacketSender.sendClientCommand(minecraftClient.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING, 0);
            this.flag = true;
        }
    }

    public void endOffhandSwap() {
        ClientServices.fireworksHelper.setSyncBatching(false);
        if (!(minecraftClient.gui.screen() instanceof AbstractContainerScreen)) {
            ClientServices.fireworksHelper.close();
        }
        if (this.flag) {
            PacketSender.sendClientCommand(minecraftClient.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING, 0);
            this.flag = false;
        }
    }

    public void do906() {
        OffhandMode offhandMode2661 = getOffhandMode2661();
        int i = offhandMode2661.findOffhandItemSlot(this.crapple.getValue().booleanValue());
        if (this.num != -1 && (!(minecraftClient.gui.screen() instanceof AbstractContainerScreen) || (minecraftClient.gui.screen() instanceof InventoryScreen))) {
            if (minecraftClient.player.getInventory().getItem(this.num >= 36 ? this.num - 36 : this.num).getItem() == offhandMode2661.getOffhandItem(this.crapple.getValue().booleanValue()) && i != -1) {
                i = this.num;
            }
        }
        if (i == -1) {
            return;
        }
        if ((this.containers.getValue().booleanValue() || !((minecraftClient.gui.screen() instanceof ShulkerBoxScreen) || (minecraftClient.gui.screen() instanceof ContainerScreen))) && i < minecraftClient.player.containerMenu.slots.size()) {
            ItemStack cursorStack = minecraftClient.player.containerMenu.getCarried();
            ItemStack stack = minecraftClient.player.containerMenu.getSlot(i).getItem();
            if (!stack.has(DataComponents.FOOD) || this.flag2) {
                beginOffhandSwap();
                int i2 = minecraftClient.player.containerMenu.containerId;
                if (!cursorStack.isEmpty() && !InventoryUtils.isInventoryFull()) {
                    minecraftClient.gameMode.handleContainerInput(i2, InventoryUtils.findSlot(Items.AIR), 0, ContainerInput.PICKUP, minecraftClient.player);
                }
                if (this.f117.getValue().booleanValue() && minecraftClient.player.containerMenu.getCarried().isEmpty()) {
                    this.num = i;
                    minecraftClient.gameMode.handleContainerInput(i2, i, 40, ContainerInput.SWAP, minecraftClient.player);
                    this.stopwatch.reset();
                } else {
                    do438(i);
                    this.num = i;
                }
                flag4 = true;
                endOffhandSwap();
                if (InputState.isKeyBindingActive(minecraftClient.options.keyUse) && stack.has(DataComponents.FOOD)) {
                    minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.OFF_HAND);
                }
            }
        }
    }

    public void do438(int i) {
        if (this.stopwatch.hasElapsedMs(this.delay.getValue().intValue()) && minecraftClient.player.containerMenu.slots.size() > 45) {
            InventoryUtils.swapStacks(i, 45);
            this.stopwatch.reset();
        }
    }

    public void setupExclusiveItemCallback(Setting<Set<Item>> setting) {
        setting.setChangeCallback(() -> {
            EntityListObjectSetting entityListObjectSetting = (EntityListObjectSetting) setting;
            if (entityListObjectSetting.getLastAdded() == null) {
                return;
            }
            ((Set) setting.getValue()).clear();
            ((Set) setting.getValue()).add((Item) entityListObjectSetting.getLastAdded());
        });
    }

    public void do2667() {
        long j = this.stopwatch4.getElapsedTime();
        int i = ClientServices.holeSnapRenderHelper.getPing();
        if (this.flag3) {
            return;
        }
        Component append = Component.literal("Since last pop: %dms. Latency: %dms.".formatted(Long.valueOf(j), Integer.valueOf(i))).append(Component.literal(" Possible death reasons: Damage override/Lag spike.*").withStyle(style -> {
            return style.withHoverEvent(new HoverEvent.ShowText(Component.literal("More info in the game log.")));
        }));
        if (i > j) {
            ChatUtils.sendPrefixedMessage(append, ChatUtils.createSignature((Math.abs(getName().hashCode()) * (-1)) - 1), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode);
            System.out.println("*Death was most likely caused by you taking damage before the server receives the update slot packet, making you die holding a totem client-side.");
            this.flag3 = true;
        }
    }

    public boolean isForceSuicideTotemMode() {
        AutoCrystal autoCrystal_lcl = getAutoCrystal();
        return autoCrystal_lcl != null && autoCrystal_lcl.isToggled() && autoCrystal_lcl.forceSuicide.getValue().booleanValue() && this.item.getValue() == OffhandMode.Totem;
    }

    public boolean isOffhandSwapAllowed() {
        if (this.f117.getValue().booleanValue()) {
            return true;
        }
        return ((minecraftClient.gui.screen() instanceof CreativeModeInventoryScreen) || minecraftClient.player.inventoryMenu != minecraftClient.player.containerMenu || (minecraftClient.gui.screen() instanceof ContainerScreen)) ? false : true;
    }

    public static boolean is929() {
        Offhand offhand_lcl = getOffhand();
        return flag4 && offhand_lcl != null && offhand_lcl.isToggled();
    }
}