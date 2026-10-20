package me.mioclient.module.combat;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.anim.ValueAnimator;
import me.mioclient.util.inventory.EnchantmentHelper;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.Comparator;
import java.util.function.Predicate;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.network.PacketSender;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import me.mioclient.module.combat.Criticals;
import me.mioclient.module.movement.FastSwim;
import me.mioclient.module.movement.Flight;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.TridentItem;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import me.mioclient.module.render.Crosshair;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Aura extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static Criticals criticals;

    public static Criticals getCriticals() {
        if (criticals == null && ClientServices.moduleInjector != null) {
            try {
                criticals = (Criticals) ClientServices.moduleInjector.getModule(Criticals.class);
            } catch (Exception e) {
            }
        }
        return criticals;
    }

    private static FastSwim fastSwim;

    public static FastSwim getFastSwim() {
        if (fastSwim == null && ClientServices.moduleInjector != null) {
            try {
                fastSwim = (FastSwim) ClientServices.moduleInjector.getModule(FastSwim.class);
            } catch (Exception e) {
            }
        }
        return fastSwim;
    }

    private static Flight flight;

    public static Flight getFlight() {
        if (flight == null && ClientServices.moduleInjector != null) {
            try {
                flight = (Flight) ClientServices.moduleInjector.getModule(Flight.class);
            } catch (Exception e) {
            }
        }
        return flight;
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
    public Setting<AuraMode_3> sorting = add(new EnumSetting<>("Sorting", AuraMode_3.DISTANCE));
    public Setting<Float> fov = add(new NumberSetting<>("Fov", 180.0f, 1.0f, 180.0f));
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 1.0f, 0.0f, 1.0f));
    public Setting<Float> range = add(new NumberSetting<>("Range", 6.0f, 1.0f, 6.5f).setSuffix("m"));
    public Setting<Float> wallRange = add(new NumberSetting<>("WallRange", 6.0f, 0.0f, 6.5f).setSuffix("m"));
    public Setting<Boolean> tPSSync = add(new BooleanSetting("TPSSync", false));
    public Setting<Boolean> pauseBaritone = add(new BooleanSetting("PauseBaritone", false));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false).setVisible());
    public Setting<Float> limit = add(new NumberSetting<>("Limit", 0.0f, 0.0f, 1.0f).setHoleSnapConfig("None", HoleSnapMode.MIN).requiresSettings(this.rotate));
    public Setting<Boolean> render = add(new BooleanSetting("Render", false).setVisible());
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(1191182335, true), v -> this.render.isShown()).setThemeSync(true));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(-1, true), v -> this.render.isShown()).setThemeSync(true));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 2.0f, v -> this.render.isShown()));
    public Setting<Boolean> weapons = add(new BooleanSetting("Weapons", false).setDependent());
    public Setting<AuraPredicateMode> weapon = add(new EnumSetting<>("Weapon", AuraPredicateMode.REQUIRE).requiresSettings(this.weapons));
    public Setting<AuraMode> swap = add(new EnumSetting<>("Swap", AuraMode.NORMAL));
    public Setting<AuraMode_2> mace = add(new EnumSetting<>("Mace", AuraMode_2.NONE));
    public Setting<Boolean> lagBack = add(new BooleanSetting("LagBack", false));
    public Setting<Boolean> pauseEating = add(new BooleanSetting("PauseEating", false));
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", true).setDependent());
    public Setting<Boolean> players = add(new BooleanSetting("Players", true, v -> this.targets.isShown()).setVisible());
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", false, v -> this.targets.isShown() && this.players.isShown()));
    public Setting<Boolean> animals = add(new BooleanSetting("Animals", false, v -> this.targets.isShown()));
    public Setting<Boolean> hostiles = add(new BooleanSetting("Hostiles", true, v -> this.targets.isShown()));
    public Setting<Boolean> neutrals = add(new BooleanSetting("Neutrals", false).requiresSettings(this.targets));
    public Setting<Boolean> crystals = add(new BooleanSetting("Crystals", false, v -> this.targets.isShown()));
    public Setting<Boolean> projectiles = add(new BooleanSetting("Projectiles", false).requiresSettings(this.targets));
    public Setting<Boolean> ignoreNamed = add(new BooleanSetting("IgnoreNamed", false, v -> this.targets.isShown()));
    public final ValueAnimator zoomHelper;
    public int num;
    public boolean flag;
    public boolean flag2;
    public Entity entity;
    public Entity entity2;

    
    public enum AuraMode implements NamedValue {
        NORMAL("Normal"),
        SWAPBACK("SwapBack"),
        SILENT("Silent");

        public final String name;

        AuraMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum AuraMode_2 implements NamedValue {
        NONE("None", itemStack -> {
            return false;
        }),
        BREACH("Breach", itemStack2 -> {
            return EnchantmentHelper.hasEnchantment(Enchantments.BREACH, itemStack2);
        }),
        DENSITY("Density", itemStack3 -> {
            return EnchantmentHelper.hasEnchantment(Enchantments.DENSITY, itemStack3) && ClientAccessor.minecraftClient.player.fallDistance > 3.0f;
        }),
        SMART("Smart", itemStack4 -> {
            return (ClientAccessor.minecraftClient.player.fallDistance > 3.0f || ClientServices.antiPhaseHelper.getGroundTicks() <= 1) ? EnchantmentHelper.hasEnchantment(Enchantments.DENSITY, itemStack4) : EnchantmentHelper.hasEnchantment(Enchantments.BREACH, itemStack4);
        });

        public final Predicate<ItemStack> predicate;
        public final String name;

        AuraMode_2(String str, Predicate<ItemStack> predicate) {
            this.predicate = predicate;
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public boolean matchesItem(ItemStack itemStack) {
            return this.predicate.test(itemStack);
        }
    }

    
    public enum AuraMode_3 implements NamedValue {
        DISTANCE("Distance", Comparator.comparing((Entity entity) -> {
            return Float.valueOf(entity.distanceTo(ClientAccessor.minecraftClient.player));
        })),
        CROSSHAIR("Crosshair", Comparator.comparing((Entity entity2) -> {
            return Float.valueOf(Mth.degreesDifferenceAbs(ClientAccessor.minecraftClient.player.getYRot(), RotationManager.getRotationsTo(entity2)[0]));
        })),
        HEALTH("Health", Comparator.comparing(EntityUtil::getEntityHealth));

        public final String name;
        public final Comparator<Entity> comparator;

        AuraMode_3(String str, Comparator comparator) {
            this.name = str;
            this.comparator = comparator;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public Comparator<Entity> getComparator() {
            return this.comparator;
        }
    }

    
    public enum AuraPredicateMode implements NamedValue {
        NONE("None"),
        REQUIRE("Require"),
        SWAP("Swap");

        public final String name;

        AuraPredicateMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Aura() {
        super("Aura", "Attacks entities nearby.", Category.COMBAT, "killaura", "forcefield", "ka");
        this.zoomHelper = new ValueAnimator();
        this.num = -1;
        this.limit.setHoleSnapConfig("None", HoleSnapMode.MIN);
        this.pauseBaritone.requireBaritone();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        if (isNotInGame()) {
            this.num = -1;
        } else {
            swapBackToOriginalSlot();
        }
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        this.entity = null;
        this.zoomHelper.setImmediate(0.0f);
        ClientServices.obstaclePasserHelper.removePauseRequest(this);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        if (this.entity == null || !(this.entity instanceof Player)) {
            return null;
        }
        return this.entity.getName().getString();
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        boolean z = (this.entity == null || this.entity.isAlive()) ? false : true;
        if ((!isValidWeapon(minecraftClient.player.getMainHandItem()) && this.weapon.getValue() == AuraPredicateMode.REQUIRE) || z) {
            this.entity = null;
            return;
        }
        AntiCheat antiCheat_lcl = getAntiCheat();
        if (minecraftClient.player.containerMenu == minecraftClient.player.inventoryMenu || antiCheat_lcl == null || !antiCheat_lcl.isF2b2t()) {
            this.entity = WorldUtils.getNearestEntity(this.players.getValue().booleanValue(), this.neutrals.getValue().booleanValue(), this.animals.getValue().booleanValue(), this.hostiles.getValue().booleanValue(), this.crystals.getValue().booleanValue(), this.projectiles.getValue().booleanValue(), this.wallRange.getValue().floatValue() + 0.5f, this.range.getValue().floatValue() + 0.5f, this.ignoreNamed.getValue().booleanValue(), this.ignoreNakeds.getValue().booleanValue(), this.sorting.getValue().getComparator(), this::isEntityInFOV);
            ClientServices.obstaclePasserHelper.removePauseRequest(this);
            if (this.pauseBaritone.getValue().booleanValue() && this.entity != null) {
                ClientServices.obstaclePasserHelper.addPauseRequest(this);
            }
            if (this.entity == null) {
                swapBackToOriginalSlot();
                return;
            }
            if (!RotationManager.is724() || canAttack(this.entity) || minecraftClient.player.isFallFlying()) {
                do906();
                if (this.rotate.getValue().booleanValue()) {
                    if (RotationManager.is724() || this.limit.getValue().floatValue() == 0.0f || this.limit.getValue().floatValue() * this.delay.getValue().floatValue() <= getAttackCooldown()) {
                        ClientServices.RotationManager.requestRotations(RotationManager.limitYawSpeed(RotationManager.getRotationsTo(this.entity), ClientServices.RotationManager.getYawStep()), 5);
                    }
                }
            }
        }
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        ServerboundMovePlayerPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundMovePlayerPacket ? (ServerboundMovePlayerPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundMovePlayerPacket) {
            ServerboundMovePlayerPacket playerMoveC2SPacket = packet904;
            if (minecraftClient.player.fallDistance > 3.0f) {
                if (minecraftClient.level.noCollision(minecraftClient.player.getBoundingBox().expandTowards(0.0d, minecraftClient.player.getDeltaMovement().y * 1.1d, 0.0d)) || !this.flag) {
                    return;
                }
                sendImmediatelyEvent.cancel();
                double newY = playerMoveC2SPacket.getY(0.0d) + 2.0d;
                boolean onGround = playerMoveC2SPacket.isOnGround();
                boolean horizontalCollision = playerMoveC2SPacket.horizontalCollision();
                if (playerMoveC2SPacket.hasPosition() && playerMoveC2SPacket.hasRotation()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.PosRot(new Vec3(playerMoveC2SPacket.getX(0.0d), newY, playerMoveC2SPacket.getZ(0.0d)), playerMoveC2SPacket.getYRot(0.0f), playerMoveC2SPacket.getXRot(0.0f), onGround, horizontalCollision));
                } else if (playerMoveC2SPacket.hasPosition()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.Pos(new Vec3(playerMoveC2SPacket.getX(0.0d), newY, playerMoveC2SPacket.getZ(0.0d)), onGround, horizontalCollision));
                } else if (playerMoveC2SPacket.hasRotation()) {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.Rot(playerMoveC2SPacket.getYRot(0.0f), playerMoveC2SPacket.getXRot(0.0f), onGround, horizontalCollision));
                } else {
                    PacketSender.sendPacket(new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision));
                }
                minecraftClient.player.setPos(minecraftClient.player.getX(), minecraftClient.player.getY() + 3.0d, minecraftClient.player.getZ());
                this.flag = false;
            }
        }
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (this.render.getValue().booleanValue()) {
            this.zoomHelper.animateTo(this.entity == null ? 0.0f : 1.0f, 250L);
            float f = this.zoomHelper.getValue();
            AABB box1747 = getClusterBox();
            if (box1747 == null || f == 0.0f) {
                return;
            }
            Color color816 = ColorUtils.withAlpha(this.fill.getValue(), (int) (this.fill.getValue().getAlpha() * f));
            Color color8162 = ColorUtils.withAlpha(this.outline.getValue(), (int) (this.outline.getValue().getAlpha() * f));
            BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), box1747, color816);
            BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), box1747, color8162, this.lineWidth.getValue().floatValue());
        }
    }

    public AABB getClusterBox() {
        AABB box = null;
        Entity entity = this.entity;
        if (entity == null) {
            entity = this.entity2;
        }
        if (entity != null) {
            box = BoxMath.getInterpolatedBox(entity, RenderUtil.getPartialTick());
            this.entity2 = entity;
        }
        return box;
    }

    public void do906() {
        if (this.entity != null && canAttack(this.entity)) {
            if (WorldUtils.getEntityDistance(this.entity) > (RaycastUtils.hasLineOfSight(this.entity) ? this.range.getValue() : this.wallRange.getValue()).floatValue()) {
                return;
            }
            boolean z = minecraftClient.player.fallDistance <= 0.0f && !minecraftClient.player.onGround();
            if (minecraftClient.player.onGround() && ClientServices.antiPhaseHelper.getGroundTicks() <= 1) {
                z = true;
            }
            if (minecraftClient.player.getVehicle() != null) {
                z = false;
            }
            Flight flight_lcl = getFlight();
            Criticals criticals_lcl = getCriticals();
            boolean flightToggled = flight_lcl != null && flight_lcl.isToggled();
            boolean criticalsBlocksFall = criticals_lcl != null && criticals_lcl.isToggled() && criticals_lcl.mode.getValue() == Criticals.CriticalsMode.GRIM;
            if (!flightToggled && !minecraftClient.player.getAbilities().flying && !minecraftClient.player.isFallFlying() && !PlayerUtils.isInLiquid(minecraftClient.player) && !minecraftClient.player.onClimbable() && (this.entity instanceof LivingEntity) && z && !criticalsBlocksFall) {
                if (!minecraftClient.player.hasEffect(MobEffects.LEVITATION)) {
                    return;
                }
            }
            int i = minecraftClient.player.getInventory().getSelectedSlot();
            int i2 = InventoryUtils.findInHotbar(itemStack -> {
                return (itemStack.getItem() instanceof AxeItem) || itemStack.is(holder -> holder.is(ItemTags.SWORDS));
            });
            boolean z2 = this.swap.getValue() == AuraMode.SILENT && isWeaponSwapMode();
            if (isWeaponSwapMode()) {
                if ((!z2 || this.pauseEating.getValue().booleanValue()) && i2 != i && minecraftClient.player.isUsingItem() && minecraftClient.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
                    return;
                }
                InventoryUtils.selectSlot(i2);
                if (i != i2) {
                    this.num = i;
                }
            }
            boolean isSprinting = minecraftClient.player.isSprinting();
            if (isSprinting) {
                minecraftClient.player.connection.send(new ServerboundPlayerCommandPacket(minecraftClient.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
            int i3 = findMaceSlot();
            LivingEntity livingEntity = (this.entity) instanceof LivingEntity ? (LivingEntity) (this.entity) : null;
            if ((livingEntity instanceof LivingEntity) && !PlayerUtils.hasArmor(livingEntity)) {
                i3 = -1;
            }
            if (i3 != -1) {
                InventoryUtils.selectSlot(i3);
            }
            BlockPlacer.attackEntity(this.entity);
            minecraftClient.player.resetAttackStrengthTicker();
            if (isSprinting) {
                minecraftClient.player.connection.send(new ServerboundPlayerCommandPacket(minecraftClient.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
            }
            FastSwim fastSwim_lcl = getFastSwim();
            if (fastSwim_lcl != null) {
                fastSwim_lcl.flag = false;
            }
            if (i3 != -1) {
                if (this.lagBack.getValue().booleanValue()) {
                    this.flag = EnchantmentHelper.hasEnchantment(Enchantments.DENSITY, minecraftClient.player.getInventory().getItem(i3));
                }
                InventoryUtils.selectSlot(i);
            }
            if (!z2 || i2 == -1) {
                return;
            }
            InventoryUtils.selectSlot(i);
        }
    }

    public boolean canAttack(Entity entity) {
        if (!(this.entity instanceof Player) && (entity instanceof LivingEntity)) {
            LivingEntity livingEntity = (LivingEntity) entity;
            if (EntityUtil.getEntityHealth((Entity) livingEntity) <= 1.0d && livingEntity.getMaxHealth() <= 4.0f) {
                return true;
            }
        }
        return ((double) getAttackCooldown()) >= ((double) this.delay.getValue().floatValue());
    }

    public float getAttackCooldown() {
        double d = MathConstants.val2;
        if (this.tPSSync.getValue().booleanValue()) {
            d -= 20.0f - ClientServices.holeSnapRenderHelper.getSmoothedTickRate();
        }
        return minecraftClient.player.getAttackStrengthScale((float) d);
    }

    public boolean isValidWeapon(ItemStack itemStack) {
        return itemStack.is(holder -> holder.is(ItemTags.SWORDS)) || (itemStack.getItem() instanceof TridentItem) || (itemStack.getItem() instanceof AxeItem) || (itemStack.getItem() instanceof MaceItem) || (itemStack.is(holder -> holder.is(ItemTags.PICKAXES)) && InventoryUtils.findInHotbar(itemStack2 -> {
            return itemStack2.is(holder -> holder.is(ItemTags.SWORDS)) || (itemStack2.getItem() instanceof AxeItem);
        }) == -1);
    }

    public void swapBackToOriginalSlot() {
        if (isWeaponSwapMode() && this.swap.getValue() == AuraMode.SWAPBACK && this.num != -1) {
            InventoryUtils.selectSlot(this.num);
            this.num = -1;
        }
    }

    public int findMaceSlot() {
        if (this.swap.getValue() == AuraMode.SILENT && isWeaponSwapMode()) {
            return -1;
        }
        int i = InventoryUtils.findInHotbar(itemStack -> {
            return itemStack.getItem() == Items.MACE && this.mace.getValue().matchesItem(itemStack);
        });
        return (this.mace.getValue() == AuraMode_2.SMART && minecraftClient.player.fallDistance > 3.0f && i == -1) ? InventoryUtils.findInHotbar(itemStack2 -> {
            return itemStack2.getItem() == Items.MACE;
        }) : i;
    }

    public boolean isEntityInFOV(Entity entity) {
        float[] floatArray2483 = RotationManager.getRotationsTo(entity);
        return Mth.degreesDifferenceAbs(floatArray2483[0], minecraftClient.player.getYRot()) <= this.fov.getValue().floatValue() && Math.abs(minecraftClient.player.getXRot() - floatArray2483[1]) <= this.fov.getValue().floatValue();
    }

    public boolean isWeaponSwapMode() {
        return this.weapon.getValue() == AuraPredicateMode.SWAP;
    }
}