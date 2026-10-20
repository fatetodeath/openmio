package me.mioclient.module.render;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.module.render.esp.ESPRenderMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathUtils;

import me.mioclient.module.render.esp.EspBoxRenderer;
import me.mioclient.module.render.esp.EspOutlineRenderer;
import me.mioclient.module.render.esp.EspTargetFilter;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.mioclient.render.RenderConstants;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.Colors;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.biome.Biomes;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class ESP extends Module {
    /* Resolved lazily -- Colors registers after this module, so an eager static
       lookup ran before it was registered and left the ref null (NPE on render). */
    private static Colors colors;

    public static Colors getColors() {
        if (colors == null && ClientServices.moduleInjector != null) {
            try {
                colors = (Colors) ClientServices.moduleInjector.getModule(Colors.class);
            } catch (Exception e) {
            }
        }
        return colors;
    }
    public Setting<Boolean> targets = add(new BooleanSetting("Targets", true).setDependent());
    public Setting<Boolean> mobOwner = add(new BooleanSetting("MobOwner", false).requiresSettings(this.targets));
    public Setting<Boolean> light = add(new BooleanSetting("Light", false).requiresSettings(this.targets));
    public Setting<Boolean> hostiles = add(new BooleanSetting("Hostiles", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> fill5 = add(new ColorSetting("Fill", new Color(364747263, true)).setThemeSync(true).requiresSettings(this.hostiles, this.targets));
    public Setting<Color> outline2 = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.hostiles, this.targets));
    public Setting<Boolean> players = add(new BooleanSetting("Players", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> fill6 = add(new ColorSetting("Fill", new Color(364747263, true)).setThemeSync(true).requiresSettings(this.players, this.targets));
    public Setting<Color> outline4 = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.players, this.targets));
    public Setting<Boolean> animals = add(new BooleanSetting("Animals", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> fill2 = add(new ColorSetting("Fill", new Color(364747263, true)).setThemeSync(true).requiresSettings(this.animals, this.targets));
    public Setting<Color> outline6 = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.animals, this.targets));
    public Setting<Boolean> pearls = add(new BooleanSetting("Pearls", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> fill3 = add(new ColorSetting("Fill", new Color(364747263, true)).setThemeSync(true).requiresSettings(this.pearls, this.targets));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.pearls, this.targets));
    public Setting<Boolean> items = add(new BooleanSetting("Items", false).setVisible().requiresSettings(this.targets));
    public Setting<ESPRenderMode> mode = add(new EnumSetting<>("Mode", ESPRenderMode.BOX).requiresSettings(this.targets, this.items));
    public Setting<Integer> range = add(new NumberSetting<>("Range", 256, 1, 256).setSuffix("m").requiresSettings(this.targets, this.items));
    public Setting<Boolean> group = add(new BooleanSetting("Group", false));
    public Setting<Color> itemsText = add(new ColorSetting("ItemsText", new Color(-1, true), v -> (this.mode.getValue() == ESPRenderMode.TEXT || this.mode.getValue() == ESPRenderMode.BOTH) && this.targets.isShown() && this.items.isShown()).setThemeSync(true));
    public Setting<Color> background = add(new ColorSetting("Background", new Color(657930, true), v -> this.mode.getValue() == ESPRenderMode.TEXT && this.targets.isShown() && this.items.isShown()));
    public Setting<Float> scale = add(new NumberSetting<>("Scale", 1.0f, 0.5f, 2.0f, v -> this.mode.getValue() == ESPRenderMode.TEXT && this.targets.isShown() && this.items.isShown()));
    public Setting<Color> fill4 = add(new ColorSetting("Fill", new Color(364747263, true), v -> (this.mode.getValue() == ESPRenderMode.BOX || this.mode.getValue() == ESPRenderMode.BOTH) && this.targets.isShown() && this.items.isShown()).setThemeSync(true));
    public Setting<Color> outline3 = add(new ColorSetting("Outline", new Color(-910321153, true), v -> (this.mode.getValue() == ESPRenderMode.BOX || this.mode.getValue() == ESPRenderMode.BOTH) && this.targets.isShown() && this.items.isShown()).setThemeSync(true));
    public Setting<Boolean> exp = add(new BooleanSetting("Exp", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(364747263, true)).setThemeSync(true).requiresSettings(this.exp, this.targets));
    public Setting<Color> outline5 = add(new ColorSetting("Outline", new Color(-910321153, true)).setThemeSync(true).requiresSettings(this.exp, this.targets));
    public Setting<Boolean> chorus = add(new BooleanSetting("Chorus", false).setVisible().requiresSettings(this.targets));
    public Setting<Color> chorusText = add(new ColorSetting("ChorusText", new Color(-1, true)).setThemeSync(true).requiresSettings(this.chorus, this.targets));
    public Setting<Boolean> blocks = add(new BooleanSetting("Blocks", true).setDependent());
    public Setting<Boolean> chests = add(new BooleanSetting("Chests", true).requiresSettings(this.blocks));
    public Setting<Boolean> eChests = add(new BooleanSetting("EChests", true).requiresSettings(this.blocks));
    public Setting<Boolean> shulkers = add(new BooleanSetting("Shulkers", true).requiresSettings(this.blocks));
    public Setting<Boolean> beds = add(new BooleanSetting("Beds", false).requiresSettings(this.blocks));
    public Setting<Boolean> signs = add(new BooleanSetting("Signs", false).requiresSettings(this.blocks));
    public Setting<Boolean> dispensers = add(new BooleanSetting("Dispensers", false).requiresSettings(this.blocks));
    public Setting<Boolean> hoppers = add(new BooleanSetting("Hoppers", false).requiresSettings(this.blocks));
    public Setting<Boolean> furnaces = add(new BooleanSetting("Furnaces", false).requiresSettings(this.blocks));
    public Setting<Boolean> pots = add(new BooleanSetting("Pots", false).requiresSettings(this.blocks));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.0f, 4.0f));
    public boolean flag;
    public final EspOutlineRenderer eSPSearchHelper4_2;
    public final Map<Vec3, Long> map;

    public ESP() {
        super("ESP", "Highlights entities through walls.", Category.RENDER, new String[0]);
        this.eSPSearchHelper4_2 = new EspOutlineRenderer(this);
        this.map = Collections.synchronizedMap(new HashMap());
        this.fill2.setConfigName("AnimalsFill");
        this.outline6.setConfigName("AnimalsOutline");
        this.fill5.setConfigName("HostilesFill");
        this.outline2.setConfigName("HostilesOutline");
        this.fill6.setConfigName("PlayersFill");
        this.outline4.setConfigName("PlayersOutline");
        this.fill4.setConfigName("ItemsFill");
        this.outline3.setConfigName("ItemsOutline");
        this.fill.setConfigName("ExpFill");
        this.outline5.setConfigName("ExpOutline");
        this.mode.setConfigName("ItemsMode");
        this.range.setConfigName("ItemRange");
        setDrawn(false);
    }

    @Listen
    public void onRenderWorldPre(MatrixStackEvent.Inner_3 inner) {
        if (this.light.getValue().booleanValue()) {
            renderSpawnHighlights(inner);
        }
    }

    @Listen(get219= -99999999)
    public void onRenderWorldOutline(MatrixStackEvent.Inner_3 inner_3) {
        if (isNotInGame() || !this.eSPSearchHelper4_2.hasBlockEntityEsp()) {
            return;
        }
        Colors colors_lcl = getColors();
        if (colors_lcl == null) {
            return;
        }
        this.flag = true;
        EspBoxRenderer.disableEntityCulling();
        for (BlockEntity blockEntity : ClientServices.stashFinderHelper.getLoadedBlockEntities()) {
            if (this.eSPSearchHelper4_2.shouldHighlightBlockEntity(blockEntity)) {
                net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(blockEntity.getBlockPos());
                Color base = this.eSPSearchHelper4_2.getBlockEntityColor(blockEntity);
                Color color1596 = BoxRenderer.applyShaderColor(base, colors_lcl.scheme.getValue().getShaderColor());
                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), box, ColorUtils.withAlpha(color1596, 60));
                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), box, color1596, this.lineWidth.getValue().floatValue());
            }
        }
        EspBoxRenderer.restoreEntityCulling();
        this.flag = false;
    }

    @Listen(get219= RenderConstants.num5)
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (isNotInGame()) {
            return;
        }
        ArrayList<Entity> arrayList = new ArrayList();
        for (Entity entity : minecraftClient.level.entitiesForRendering()) {
            arrayList.add(entity);
        }
        arrayList.sort(Comparator.comparing(entity -> {
            return Integer.valueOf(-entity.tickCount);
        }));
        ArrayList arrayList2 = new ArrayList();
        for (Entity entity2 : arrayList) {
            if (this.eSPSearchHelper4_2.shouldRenderEntity(entity2)) {
                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), BoxMath.getInterpolatedBox(entity2, inner_3.getTickDelta()), this.eSPSearchHelper4_2.getEntityColor(entity2, true));
                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), BoxMath.getInterpolatedBox(entity2, inner_3.getTickDelta()), this.eSPSearchHelper4_2.getEntityColor(entity2, false), this.lineWidth.getValue().floatValue());
            }
            if (entity2 instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity) entity2;
                if (this.items.getValue().booleanValue() && (this.mode.getValue() == ESPRenderMode.BOTH || this.mode.getValue() == ESPRenderMode.TEXT)) {
                    Vec3 pos = minecraftClient.gameRenderer.mainCamera().position();
                    if (!(entity2 instanceof ItemEntity) || entity2.position().distanceTo(pos) <= this.range.getValue().intValue()) {
                        if (this.group.getValue().booleanValue()) {
                            boolean z = false;
                            Iterator it2 = arrayList2.iterator();
                            while (it2.hasNext()) {
                                if (((EspTargetFilter) it2.next()).addTrackedItem(itemEntity)) {
                                    z = true;
                                }
                            }
                            if (!z) {
                                arrayList2.add(new EspTargetFilter(itemEntity));
                            }
                        } else {
                            String string = itemEntity.getItem().getHoverName().getString();
                            if (itemEntity.getItem().getCount() > 1) {
                                string = new StringTemplate().arg(itemEntity.getItem().getCount()).arg(string).format("\u0001 x\u0001");
                            }
                            this.eSPSearchHelper4_2.renderBillboardLabel(inner_3, string, BoxMath.getInterpolatedBox(itemEntity, inner_3.getTickDelta()).getCenter().add(0.0d, 0.6000000238418579d, 0.0d), this.scale.getValue().floatValue(), this.itemsText.getValue(), this.background.getValue());
                        }
                    }
                }
            }
            String string1768 = this.eSPSearchHelper4_2.getOwnerName(entity2);
            if (string1768 != null) {
                Vec3 add = BoxMath.getInterpolatedBox(entity2, inner_3.getTickDelta()).getCenter().add(0.0d, 0.2d, 0.0d);
                if (!(entity2 instanceof ThrownEnderpearl)) {
                    add = add.add(0.0d, 1.3d, 0.0d);
                }
                this.eSPSearchHelper4_2.renderBillboardLabel(inner_3, string1768, add, 1.0f, Color.white, (Color) null);
            }
        }
        if (this.group.getValue().booleanValue()) {
            do1915(inner_3, arrayList2);
        }
        if (this.chorus.getValue().booleanValue()) {
            synchronized (this.map) {
                for (Map.Entry<Vec3, Long> entry : this.map.entrySet()) {
                    this.eSPSearchHelper4_2.renderBillboardLabel(inner_3, "Player teleport", entry.getKey(), 1.0f, this.chorusText.getValue(), (Color) null);
                }
            }
        }
    }

    public void do1915(MatrixStackEvent matrixStackEvent, List<EspTargetFilter> list) {
        for (EspTargetFilter eSPSearchHelper4 : list) {
            if (!this.group.getValue().booleanValue()) {
                return;
            }
            Vec3 center = eSPSearchHelper4.getMergedBox().getCenter();
            java.util.ArrayList<net.minecraft.network.chat.Component> tags = new java.util.ArrayList<>();
            for (Map.Entry<String, Integer> entry : eSPSearchHelper4.getItemCountMap().entrySet()) {
                tags.add(net.minecraft.network.chat.Component.literal(EspTargetFilter.formatItemCountLabel(entry.getKey(), entry.getValue().intValue())));
            }
            me.mioclient.render.shape.BoxRenderer.submitMultilineTag(matrixStackEvent.getMatrixStack(), center, 0.5d, 0.3d, tags, matrixStackEvent.cameraState, 1.0f);
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        ClientboundSoundPacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundSoundPacket ? (ClientboundSoundPacket) (channelRead0Event.getPacket()) : null;
        if (packet904 instanceof ClientboundSoundPacket) {
            ClientboundSoundPacket playSoundS2CPacket = packet904;
            if (playSoundS2CPacket.getSound().value().equals(SoundEvents.CHORUS_FRUIT_TELEPORT)) {
                this.map.put(new Vec3(playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ()), Long.valueOf(System.currentTimeMillis()));
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        this.map.entrySet().removeIf(entry -> {
            return System.currentTimeMillis() - ((Long) entry.getValue()).longValue() > 3500;
        });
    }

    public void drawGroupedItemBg(PoseStack matrixStack, EspTargetFilter eSPSearchHelper4, double d, float f) {
        RenderUtil.INSTANCE.drawBillboard(matrixStack, eSPSearchHelper4.getMergedBox().getCenter(), 0.0f, 0.0f, eSPSearchHelper4.getMaxLabelWidth() + 1.0f, f, d, this.background.getValue());
    }

    public void renderSpawnHighlights(MatrixStackEvent matrixStackEvent) {
        float intBitsToFloat = 0.75f;
        PoseStack matrixStack472 = matrixStackEvent.getMatrixStack();
        TrajectoriesVertexConsumer consumer = TrajectoriesVertexConsumer.createDefault();
        BufferBuilder begin = consumer.beginBuffer(PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        RenderUtil.INSTANCE.setupRenderState();
        for (BlockPos blockPos : WorldUtils.getBlocksInRadius(minecraftClient.player.getEyePosition(), 12.0f, true)) {
            BlockState blockState = minecraftClient.level.getBlockState(blockPos);
            if (WorldUtils.isAir(blockPos.above())) {
                if (minecraftClient.level.getMaxLocalRawBrightness(blockPos.above()) <= 7) {
                    boolean matchesKey = minecraftClient.level.getBiome(blockPos).is(Biomes.MUSHROOM_FIELDS);
                    if (blockState.isValidSpawn(minecraftClient.level, blockPos, EntityTypes.ZOMBIE) && !matchesKey) {
                        double distanceTo = minecraftClient.gameRenderer.mainCamera().position().distanceTo(Vec3.atCenterOf(blockPos));
                        float intBitsToFloat2 = 1.0f - ((float) Mth.clamp((distanceTo - 8.0d) / 2.0d, 0.0d, 1.0d));
                        int hashCode = Color.red.hashCode();
                        if (distanceTo >= 8.0d) {
                            hashCode = ColorUtils.toIntWithAlphaPercent(Color.red, intBitsToFloat2);
                        }
                        double baseY = blockPos.getY() + 1.02d;
                        Vec3 cornerA = new Vec3(blockPos.getX() + 1.0f - intBitsToFloat, baseY, blockPos.getZ() + 1.0f - intBitsToFloat);
                        Vec3 cornerB = new Vec3(blockPos.getX() + intBitsToFloat, baseY, blockPos.getZ() + intBitsToFloat);
                        Vec3 cornerC = new Vec3(blockPos.getX() + intBitsToFloat, baseY, blockPos.getZ() + 1.0f - intBitsToFloat);
                        Vec3 cornerD = new Vec3(blockPos.getX() + 1.0f - intBitsToFloat, baseY, blockPos.getZ() + intBitsToFloat);
                        RenderUtil.INSTANCE.addLineVertex(matrixStack472, begin, cornerA, cornerB, hashCode, hashCode);
                        RenderUtil.INSTANCE.addLineVertex(matrixStack472, begin, cornerC, cornerD, hashCode, hashCode);
                    }
                }
            }
        }
        consumer.drawBuffer();
        RenderUtil.INSTANCE.restoreRenderState();
    }

    public boolean is1918() {
        return this.flag;
    }
}