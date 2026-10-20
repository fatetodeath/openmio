package me.mioclient.module.render;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.module.render.tooltips.ByteCountingOutput;
import me.mioclient.module.render.tooltips.ShulkerPreviewScreen;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.module.render.tooltips.ContainerPreview;
import me.mioclient.render.tooltip.HeightTooltipComponent;
import me.mioclient.render.tooltip.ShulkerContentsTooltip;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.screen.RenderWithTooltipEvent;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
import me.mioclient.mixin.ducks.DuckHandledScreen;
import me.mioclient.module.Position;
import me.mioclient.setting.BooleanSetting;

public class Tooltips extends Module {
    public Setting<Boolean> colored = add(new BooleanSetting("Colored", true));
    public Setting<Boolean> majorityItem = add(new BooleanSetting("MajorityItem", false));
    public Setting<Boolean> keepText = add(new BooleanSetting("KeepText", true));
    public Setting<Boolean> shulkerContent = add(new BooleanSetting("ShulkerContent", true));
    public Setting<Boolean> beeNest = add(new BooleanSetting("BeeNest", false));
    public Setting<Boolean> mapOverlay = add(new BooleanSetting("MapOverlay", true));
    public Setting<Boolean> size = add(new BooleanSetting("Size", false));
    public Color color;
    public final List<ContainerPreview> list;

    public Tooltips() {
        super("Tooltips", new StringTemplate().arg(String.valueOf(ChatFormatting.YELLOW)).arg(String.valueOf(ChatFormatting.YELLOW)).format("Draws advanced tool tips on items.\n\u0001Middle Click a shulker to open the preview screen.\n\u0001Hold Left Alt when hovering over a shulker to inspect it's items."), Category.RENDER, new String[0]);
        this.list = Collections.synchronizedList(new ArrayList());
        setDrawn(false);
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundContainerSetContentPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundContainerSetContentPacket ? (ClientboundContainerSetContentPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundContainerSetContentPacket) {
            ClientboundContainerSetContentPacket inventoryS2CPacket = packet904;
            if (this.majorityItem.getValue().booleanValue()) {
                minecraftClient.execute(() -> {
                    this.list.clear();
                    Iterator it = inventoryS2CPacket.items().iterator();
                    while (it.hasNext()) {
                        ContainerPreview tooltipsData2241 = ContainerPreview.fromShulkerBox((ItemStack) it.next());
                        if (tooltipsData2241 != null) {
                            this.list.add(tooltipsData2241);
                        }
                    }
                });
            }
        }
        if ((channelRead0Event.getPacket() instanceof ClientboundContainerSetSlotPacket) && this.majorityItem.getValue().booleanValue()) {
            ClientServices.tooltipsHelper.schedule(() -> {
                if (minecraftClient.player.containerMenu == null) {
                    return;
                }
                this.list.clear();
                Iterator it = minecraftClient.player.containerMenu.slots.iterator();
                while (it.hasNext()) {
                    ContainerPreview tooltipsData2241 = ContainerPreview.fromShulkerBox(((Slot) it.next()).getItem());
                    if (tooltipsData2241 != null) {
                        this.list.add(tooltipsData2241);
                    }
                }
            }, 0);
        }
    }

    @Listen
    public void onRenderWithTooltip(RenderWithTooltipEvent renderWithTooltipEvent) {
        Slot mio$getFocusedSlot;
        BlockPos blockPos;
        if ((renderWithTooltipEvent.getScreen() instanceof AbstractContainerScreen) && (mio$getFocusedSlot = ((me.mioclient.mixin.ducks.DuckHandledScreen) renderWithTooltipEvent.getScreen()).mio$getFocusedSlot()) != null) {
            ItemStack stack = mio$getFocusedSlot.getItem();
            List<ItemStack> ofSize = NonNullList.withSize(27, ItemStack.EMPTY);
            BlockItem item = (stack.getItem()) instanceof BlockItem ? (BlockItem) (stack.getItem()) : null;
            if (item instanceof BlockItem) {
                ShulkerBoxBlock block = (item.getBlock()) instanceof ShulkerBoxBlock ? (ShulkerBoxBlock) (item.getBlock()) : null;
                if (block instanceof ShulkerBoxBlock) {
                    ShulkerBoxBlock shulkerBoxBlock = block;
                    if (shulkerBoxBlock.getColor() != null) {
                        Color color = new Color(shulkerBoxBlock.getColor().getMapColor().col, false);
                        float[] fArr = new float[3];
                        color.getColorComponents(fArr);
                        setShulkerTintColor(new Color(fArr[0], fArr[1], fArr[2], 0.5f));
                    }
                }
            }
            ItemContainerContents containerComponent = stack.get(DataComponents.CONTAINER);
            if (containerComponent != null) {
                List list = containerComponent.allItemsCopyStream().toList();
                for (int i = 0; i < list.size() && i < 27; i++) {
                    ItemStack itemStack = (ItemStack) list.get(i);
                    if (itemStack.getCount() == 0) {
                        itemStack.setCount(69);
                    }
                    ofSize.set(i, itemStack);
                }
            }
            do584(stack, ofSize);
            if (!ofSize.stream().allMatch((v0) -> {
                return v0.isEmpty();
            }) && this.shulkerContent.getValue().booleanValue()) {
                if (GLFW.glfwGetMouseButton(minecraftClient.getWindow().handle(), 2) == 1 && !(renderWithTooltipEvent.getScreen() instanceof ShulkerPreviewScreen)) {
                    minecraftClient.setScreenAndShow(new ShulkerPreviewScreen(new ShulkerBoxMenu(1337, minecraftClient.player.getInventory(), new SimpleContainer((ItemStack[]) ofSize.toArray(new ItemStack[27]))), minecraftClient.player.getInventory(), stack.getHoverName(), renderWithTooltipEvent.getScreen()));
                }
                if (this.keepText.getValue().booleanValue()) {
                    renderWithTooltipEvent.getComponents().add(new ShulkerContentsTooltip(ofSize));
                } else {
                    renderWithTooltipEvent.getComponents().clear();
                    renderWithTooltipEvent.getComponents().addAll(List.of(ClientTooltipComponent.create(stack.getHoverName().getVisualOrderText()), new ShulkerContentsTooltip(ofSize)));
                }
            }
            if (stack.getItem() == Items.FILLED_MAP) {
                MapId mapIdComponent = stack.get(DataComponents.MAP_ID);
                int id = mapIdComponent == null ? -1 : mapIdComponent.id();
                MapItemSavedData mapState2653 = ClientServices.tooltipsSearchHelper4.lookupMapState(stack, id);
                if (mapState2653 != null) {
                    if (this.keepText.getValue().booleanValue()) {
                        renderWithTooltipEvent.getComponents().add(new HeightTooltipComponent(id, mapState2653));
                    } else {
                        renderWithTooltipEvent.getComponents().clear();
                        renderWithTooltipEvent.getComponents().addAll(List.of(ClientTooltipComponent.create(stack.getHoverName().getVisualOrderText()), new HeightTooltipComponent(id, mapState2653)));
                    }
                    if (this.mapOverlay.getValue().booleanValue()) {
                        renderWithTooltipEvent.getComponents().add(ClientTooltipComponent.create(Component.literal("Hold Left-Alt to hide item amount.").withStyle(style -> {
                            return style.withColor(ChatFormatting.YELLOW);
                        }).getVisualOrderText()));
                    }
                }
            }
            Bees beesComponent = stack.get(DataComponents.BEES);
            if (beesComponent != null && this.beeNest.getValue().booleanValue()) {
                for (net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant occupant : beesComponent.bees()) {
                    CompoundTag copyNbt = occupant.entityData().getUnsafe();
                    if (copyNbt != null && copyNbt.contains("FlowerPos")) {
                        try {
                            int[] arr = copyNbt.getIntArray("FlowerPos").orElse(new int[0]);
                            if (arr.length >= 3) {
                                BlockPos flowerPos = new BlockPos(arr[0], arr[1], arr[2]);
                                renderWithTooltipEvent.getComponents().add(ClientTooltipComponent.create(Component.literal(new StringTemplate().arg(flowerPos.toShortString()).format("Position \u0001")).getVisualOrderText()));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            if (this.size.getValue().booleanValue()) {
                try {
                    int byteCount = stack.toString().length();
                    renderWithTooltipEvent.getComponents().add(ClientTooltipComponent.create(Component.literal(getString585(byteCount)).getVisualOrderText()));
                } catch (Throwable th) {
                }
            }
        }
    }

    public void do584(ItemStack itemStack, List<ItemStack> list) {
        CustomData nbtComponent = itemStack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent == null || !nbtComponent.copyTag().contains("BlockEntityTag")) {
            return;
        }
        CompoundTag compound = nbtComponent.copyTag().getCompoundOrEmpty("BlockEntityTag");
        if (compound.contains("Items")) {
            ListTag list2 = compound.getListOrEmpty("Items");
            for (int i = 0; i < list2.size(); i++) {
                CompoundTag compound2 = list2.getCompoundOrEmpty(i);
                ItemStack itemStack2 = null;
                try {
                    itemStack2 = ItemStack.CODEC.parse(minecraftClient.player.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), compound2).result().orElse(null);
                } catch (Exception ignored) {
                }
                byte b = compound2.getByteOr("Count", (byte) -1);
                if (itemStack2 != null && b == 0) {
                    itemStack2.setCount(69);
                    list.set(i, itemStack2);
                }
            }
        }
    }

    public String getString585(long j) {
        long j2 = j / 1024;
        long j3 = j2 / 1024;
        return j3 > 0 ? String.format("%,d MB", Long.valueOf(j3)) : j2 > 0 ? String.format("%,d KB", Long.valueOf(j2)) : String.format("%,d B", Long.valueOf(j));
    }

    public Color getTintColor() {
        if (this.colored.getValue().booleanValue()) {
            return this.color;
        }
        return null;
    }

    public void setShulkerTintColor(Color color) {
        this.color = color;
    }
}