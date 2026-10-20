package me.mioclient.module.movement;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.client.InventorySyncEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.gui.minigame.MinigamePanel;
import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.screen.ClickGuiScreen;
import me.mioclient.gui.screen.HudEditorScreen;
import me.mioclient.gui.widget.ChestSearchOverlay;
import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.anim.InterpolatedFloat;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.network.PacketSender;

import java.util.Iterator;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.InteractBlockEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.player.TickMovementEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.AntiCheat;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;

public class NoSlow extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving antiCheat null (NPE). */
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
    public Setting<Boolean> guiMove = add(new BooleanSetting("GuiMove", true));
    public Setting<Boolean> portals = add(new BooleanSetting("Portals", true));
    public Setting<Boolean> items = add(new BooleanSetting("Items", true));
    public Setting<Boolean> sneak = add(new BooleanSetting("Sneak", false));
    public Setting<Boolean> crawl = add(new BooleanSetting("Crawl", false));
    public Setting<Boolean> strict = add(new BooleanSetting("Strict", true).setDependent());
    public Setting<NoSlowMode> mode = add(new EnumSetting<>("Mode", NoSlowMode.NONE).requiresSettings(this.strict));
    public Setting<Boolean> strictInventory = add(new BooleanSetting("StrictInventory", false));
    public Setting<Boolean> multiTask = add(new BooleanSetting("MultiTask", true).requiresSettings(this.strict));
    public Setting<Boolean> blocks = add(new BooleanSetting("Blocks", false).setDependent());
    public Setting<Boolean> ice = add(new BooleanSetting("Ice", false, v -> this.blocks.isShown()));
    public Setting<Boolean> slime = add(new BooleanSetting("Slime", false, v -> this.blocks.isShown()));
    public Setting<Boolean> noSlimeBounce = add(new BooleanSetting("NoSlimeBounce", true, v -> this.blocks.isShown() && this.slime.getValue().booleanValue()));
    public Setting<Boolean> soulSand = add(new BooleanSetting("SoulSand", false, v -> this.blocks.isShown()));
    public Setting<Boolean> honey = add(new BooleanSetting("Honey", false, v -> this.blocks.isShown()));
    public Setting<Boolean> berryBush = add(new BooleanSetting("BerryBush", false, v -> this.blocks.isShown()));
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public final Stopwatch stopwatch;
    public final InterpolatedFloat hUDHelper_2;
    public final InterpolatedFloat hUDHelper_22;

    
    public enum NoSlowMode implements NamedValue {
        NONE("None"),
        NCP("NCP"),
        GRIM("Grim"),
        GRIMV3("Slow"),
        CONSTANTIAM("Constantiam");

        public final String name;

        NoSlowMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public NoSlow() {
        super("NoSlow", "Cancels several things that may slow you down.", Category.MOVEMENT, new String[0]);
        this.stopwatch = new Stopwatch();
        this.hUDHelper_2 = new InterpolatedFloat(2.0f, true);
        this.hUDHelper_22 = new InterpolatedFloat(2.0f, true);
        setDrawn(false);
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (!(sendImmediatelyEvent.getPacket() instanceof ServerboundMovePlayerPacket pkt)) {
            return;
        }
        ItemStack stackInHand = minecraftClient.player.getItemInHand(minecraftClient.player.getUsedItemHand());
        if (minecraftClient.player.isUsingItem() && this.items.getValue().booleanValue() && !minecraftClient.player.isPassenger() && !minecraftClient.player.isFallFlying() && stackInHand.has(DataComponents.FOOD) && isNcpMode()) {
            minecraftClient.player.connection.send(new ServerboundSetCarriedItemPacket(minecraftClient.player.getInventory().getSelectedSlot()));
        }
        if (this.guiMove.getValue().booleanValue() && this.strictInventory.getValue().booleanValue() && !isGrimMode() && this.flag && !minecraftClient.player.isPassenger() && minecraftClient.player.onGround() && this.stopwatch.hasElapsedMs(200L) && !minecraftClient.player.isFallFlying() && this.mode.getValue() != NoSlowMode.GRIMV3) {
            // 26.2 port: immutable packet - cancel + send airborne replacement.
            sendImmediatelyEvent.cancel();
            boolean hc = pkt.horizontalCollision();
            net.minecraft.client.player.LocalPlayer pl = minecraftClient.player;
            if (pkt instanceof ServerboundMovePlayerPacket.PosRot) {
                pl.connection.send(new ServerboundMovePlayerPacket.PosRot(pkt.getX(pl.getX()), pkt.getY(pl.getY()), pkt.getZ(pl.getZ()), pkt.getYRot(pl.getYRot()), pkt.getXRot(pl.getXRot()), false, hc));
            } else if (pkt instanceof ServerboundMovePlayerPacket.Pos) {
                pl.connection.send(new ServerboundMovePlayerPacket.Pos(pkt.getX(pl.getX()), pkt.getY(pl.getY()), pkt.getZ(pl.getZ()), false, hc));
            } else if (pkt instanceof ServerboundMovePlayerPacket.Rot) {
                pl.connection.send(new ServerboundMovePlayerPacket.Rot(pkt.getYRot(pl.getYRot()), pkt.getXRot(pl.getXRot()), false, hc));
            } else {
                pl.connection.send(new ServerboundMovePlayerPacket.StatusOnly(false, hc));
            }
            this.flag = false;
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.flag2 = false;
        this.flag3 = false;
        if (minecraftClient.player.onGround()) {
            return;
        }
        this.stopwatch.reset();
    }

    @Listen
    public void onInteractBlock(InteractBlockEvent interactBlockEvent) {
        if (this.mode.getValue() == NoSlowMode.CONSTANTIAM && this.items.getValue().booleanValue() && !minecraftClient.player.isFallFlying()) {
            if (minecraftClient.player.getItemInHand(interactBlockEvent.getHand()).has(DataComponents.FOOD) && !BlockPlacer.needsSneakPlace(interactBlockEvent.getHitResult().getBlockPos())) {
                minecraftClient.gameMode.useItem(minecraftClient.player, InteractionHand.MAIN_HAND);
                interactBlockEvent.cancel();
            }
        }
    }

    @Listen
    public void onInventorySync(InventorySyncEvent keyPearlModeEvent) {
        if (keyPearlModeEvent.getPhase() != KeyPearlMode.Pre || !this.strictInventory.getValue().booleanValue() || isGrimMode() || !this.guiMove.getValue().booleanValue()) {
            if (keyPearlModeEvent.getPhase() == KeyPearlMode.Post && MovementUtils.is2181() && isNcpMode() && this.guiMove.getValue().booleanValue()) {
                if (minecraftClient.player.isSprinting()) {
                    minecraftClient.player.connection.send(new ServerboundPlayerCommandPacket(minecraftClient.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                }
                if (minecraftClient.player.isShiftKeyDown()) {
                    { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), true, _cur.sprint()))); };
                    return;
                }
                return;
            }
            return;
        }
        if (!this.multiTask.getValue().booleanValue() && !this.flag2) {
            minecraftClient.gameMode.releaseUsingItem(minecraftClient.player);
            this.flag2 = true;
        }
        if (MovementUtils.is2181() && this.mode.getValue() != NoSlowMode.GRIMV3) {
            if (ClientServices.antiPhaseHelper.getGroundTicks() > 1 && !this.flag3) {
                PacketSender.sendPosition(minecraftClient.player.getX(), minecraftClient.player.getY() + 0.0626d, minecraftClient.player.getZ(), false);
                this.flag3 = true;
                this.flag = true;
            }
            if (minecraftClient.player.isSprinting()) {
                minecraftClient.player.connection.send(new ServerboundPlayerCommandPacket(minecraftClient.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
            if (minecraftClient.player.isShiftKeyDown()) {
                { net.minecraft.world.entity.player.Input _cur = minecraftClient.player.input.keyPresses; minecraftClient.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(_cur.forward(), _cur.backward(), _cur.left(), _cur.right(), _cur.jump(), false, _cur.sprint()))); };
            }
        }
    }

    @Listen
    public void onTickMovement(TickMovementEvent tickMovementEvent) {
        if (this.items.getValue().booleanValue() && minecraftClient.player.isUsingItem() && !minecraftClient.player.isPassenger()) {
            boolean z = true;
            if (this.mode.getValue() == NoSlowMode.GRIMV3) {
                z = minecraftClient.player.tickCount % 3 == 0 || minecraftClient.player.tickCount % 4 == 0;
                if (minecraftClient.player.tickCount % 12 == 0) {
                    z = false;
                }
                if (minecraftClient.player.isFallFlying()) {
                    z = false;
                }
            }
            if (z) {
                // 26.2: ClientInput uses boolean keyPresses, movement speed is handled via tick movement multipliers elsewhere.
            }
            if (isGrimMode()) {
                do3102(minecraftClient.player.getUsedItemHand());
            }
        }
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (!isOffhandSwapAllowed() || !this.guiMove.getValue().booleanValue() || (minecraftClient.gui.screen() instanceof ClickGuiScreen) || (minecraftClient.gui.screen() instanceof HudEditorScreen)) {
            this.hUDHelper_22.setImmediate(minecraftClient.player.getYRot());
            this.hUDHelper_2.setImmediate(minecraftClient.player.getXRot());
            return;
        }
        if (isKeyPressed(264)) {
            this.hUDHelper_2.animate(Math.min(Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.xRotO, minecraftClient.player.getXRot()) + 8.0f, MathConstants.num2));
        } else if (isKeyPressed(265)) {
            this.hUDHelper_2.animate(Math.max(Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.xRotO, minecraftClient.player.getXRot()) - 8.0f, -MathConstants.num2));
        }
        if (isKeyPressed(262)) {
            this.hUDHelper_22.animate(Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.yRotO, minecraftClient.player.getYRot()) + 8.0f);
        } else if (isKeyPressed(263)) {
            this.hUDHelper_22.animate(Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.yRotO, minecraftClient.player.getYRot()) - 8.0f);
        }
        if (is3104()) {
            minecraftClient.player.setXRot(this.hUDHelper_2.getValue());
            minecraftClient.player.setYRot(this.hUDHelper_22.getValue());
        }
    }

    public void do3102(InteractionHand hand) {
        InteractionHand hand2015 = PlayerUtils.getOppositeHand(hand);
        if (!isSlowdownItem(minecraftClient.player.getItemInHand(hand2015))) {
            PacketSender.sendInteractItem(hand2015);
            return;
        }
        if (hand2015 == InteractionHand.MAIN_HAND) {
            int i = minecraftClient.player.getInventory().getSelectedSlot();
            InventoryUtils.do438((i + 1) % 9);
            PacketSender.sendInteractItem(hand2015);
            InventoryUtils.do438(i);
            return;
        }
        int i2 = InventoryUtils.toNetworkSlot(InventoryUtils.findInInventory(itemStack -> {
            return !isSlowdownItem(itemStack);
        }, true));
        minecraftClient.gameMode.handleContainerInput(0, i2, 40, ContainerInput.SWAP, minecraftClient.player);
        PacketSender.sendInteractItem(hand2015);
        minecraftClient.gameMode.handleContainerInput(0, i2, 40, ContainerInput.SWAP, minecraftClient.player);
    }

    public boolean isSlowdownItem(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD) || itemStack.is(Items.BOW) || itemStack.is(Items.SHIELD) || itemStack.is(Items.ENDER_PEARL);
    }

    public boolean isOffhandSwapAllowed() {
        if (minecraftClient.gui.screen() instanceof ClickGuiScreen) {
            Iterator<ClickGuiPanel> it = ClientServices.getClickGui().getGuiPanels().iterator();
            while (it.hasNext()) {
                ClickGuiPanel next = it.next();
                if ((next instanceof MinigamePanel) && next.isShown()) {
                    return false;
                }
            }
            if (ClientServices.getClickGui().textFieldWidget.isFocused() || !ClientServices.getClickGui().textFieldWidget.getValue().isBlank()) {
                return false;
            }
        }
        return (minecraftClient.gui.screen() == null || (minecraftClient.gui.screen() instanceof ChatScreen) || (minecraftClient.gui.screen() instanceof SignEditScreen) || (minecraftClient.gui.screen() instanceof AnvilScreen) || (minecraftClient.gui.screen() instanceof AbstractCommandBlockEditScreen) || (minecraftClient.gui.screen() instanceof StructureBlockEditScreen) || (ChestSearchOverlay.textFieldWidget != null && ChestSearchOverlay.textFieldWidget.isActive())) ? false : true;
    }

    public boolean is3104() {
        return isKeyPressed(264) || isKeyPressed(265) || isKeyPressed(262) || isKeyPressed(263);
    }

    public boolean isKeyPressed(int i) {
        return GLFW.glfwGetKey(minecraftClient.getWindow().handle(), i) == 1;
    }

    public boolean isNcpMode() {
        return this.mode.getValue() == NoSlowMode.NCP;
    }

    public boolean isGrimMode() {
        return this.mode.getValue() == NoSlowMode.GRIM;
    }

    public boolean shouldApply() {
        return isToggled() && !this.multiTask.getValue().booleanValue() && minecraftClient.player.isUsingItem() && !minecraftClient.player.isPassenger() && minecraftClient.player.getUsedItemHand() == InteractionHand.MAIN_HAND;
    }
}