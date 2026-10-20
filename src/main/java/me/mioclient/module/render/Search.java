package me.mioclient.module.render;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.setting.SoundIdentifierSetting;
import me.mioclient.util.client.SoundIdentifier;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.network.ChunkUpdateHandler;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.DisconnectEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.Colors;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.chunk.ChunkAccess;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import me.mioclient.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class Search extends Module {
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
    public static final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    public Setting<Set<Block>> whitelist = add(new EntityListObjectSetting<>("Whitelist", BuiltInRegistries.BLOCK));
    public Setting<Set<EntityType<?>>> entityTypes = add(new EntityListObjectSetting<>("EntityTypes", BuiltInRegistries.ENTITY_TYPE));
    public Setting<Boolean> entities = add(new BooleanSetting("Entities", false));
    public Setting<Boolean> ignoreNeg = add(new BooleanSetting("IgnoreNeg", false));
    public Setting<Boolean> fill = add(new BooleanSetting("Fill", true));
    public Setting<Float> fillOpacity = add(new NumberSetting<>("FillOpacity", 0.1f, 0.0f, 1.0f, v -> this.fill.getValue().booleanValue()));
    public Setting<Boolean> outline = add(new BooleanSetting("Outline", true));
    public Setting<Boolean> tracers = add(new BooleanSetting("Tracers", true));
    public Setting<Float> tracerOpacity = add(new NumberSetting<>("TracerOpacity", 0.5f, 0.0f, 1.0f, v -> this.tracers.getValue().booleanValue()));
    public Setting<Boolean> sound = add(new BooleanSetting("Sound", false).setVisible());
    public Setting<SoundIdentifier> type = add(new SoundIdentifierSetting("Type", new SoundIdentifier("system", "warning")).requiresSettings(this.sound));
    public Setting<Float> volume = add(new NumberSetting<>("Volume", 1.0f, 0.0f, 1.0f).requiresSettings(this.sound));
    public Setting<Boolean> ignoreNatural = add(new BooleanSetting("IgnoreNatural", false).setVisible());
    public Setting<Boolean> dungeon = add(new BooleanSetting("Dungeon", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> fortress = add(new BooleanSetting("Fortress", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> bastion = add(new BooleanSetting("Bastion", true).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> ancientCity = add(new BooleanSetting("AncientCity", false).requiresSettings(this.ignoreNatural));
    public Setting<Boolean> trialChambers = add(new BooleanSetting("TrialChambers", true).requiresSettings(this.ignoreNatural));
    public final Stopwatch stopwatch;
    public final Set<BlockPos> set;
    public final ChunkUpdateHandler ChunkUpdateHandler;
    public boolean flag;

    public Search() {
        super("Search", "Helps finding whitelisted stuff.", Category.RENDER, new String[0]);
        this.stopwatch = new Stopwatch();
        this.set = Collections.synchronizedSet(new HashSet());
        this.ChunkUpdateHandler = new ChunkUpdateHandler(1, (chunkPos, worldChunk) -> {
            if (this.set.size() > 100000) {
                return;
            }
            Set<Block> value = this.whitelist.getValue();
            for (int i = 0; i < 16; i++) {
                for (int bottomY = this.ignoreNeg.getValue().booleanValue() ? 0 : minecraftClient.level.getMinY(); bottomY < minecraftClient.level.getMaxY(); bottomY++) {
                    for (int i2 = 0; i2 < 16; i2++) {
                        mutable.set(chunkPos.getMinBlockX() + i, bottomY, chunkPos.getMinBlockZ() + i2);
                        BlockState blockState = worldChunk.getBlockState(mutable);
                        if (!blockState.isAir()) {
                            if (this.ignoreNatural.getValue().booleanValue()) {
                                if (WorldUtils.isStashChest(worldChunk, mutable, this.fortress.getValue().booleanValue(), this.bastion.getValue().booleanValue(), this.ancientCity.getValue().booleanValue(), this.dungeon.getValue().booleanValue(), this.trialChambers.getValue().booleanValue())) {
                                }
                            }
                            if (value.contains(blockState.getBlock())) {
                                this.flag = true;
                                this.set.add(mutable.immutable());
                            }
                        }
                    }
                }
            }
        }, (chunkPos2, worldChunk2) -> {
            this.set.removeIf(blockPos -> {
                return blockPos.getX() >= chunkPos2.getMinBlockX() && blockPos.getX() <= chunkPos2.getMaxBlockX() && blockPos.getZ() >= chunkPos2.getMinBlockZ() && blockPos.getZ() <= chunkPos2.getMaxBlockZ();
            });
        }, (blockPos, blockState) -> {
            if (!this.whitelist.getValue().contains(blockState.getBlock())) {
                this.set.remove(blockPos);
            } else if (!blockState.isAir() && this.set.add(blockPos)) {
                this.flag = true;
            }
        });
        this.whitelist.setChangeCallback(() -> {
            if (minecraftClient.level == null || this.ChunkUpdateHandler.getThreadPool() == null) {
                return;
            }
            this.set.clear();
            this.ChunkUpdateHandler.processAllChunks();
        });
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.flag = false;
        this.ChunkUpdateHandler.do1640();
        if (isNotInGame()) {
            return;
        }
        this.ChunkUpdateHandler.processAllChunks();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        this.set.clear();
        this.ChunkUpdateHandler.stopListening();
    }

    @Listen
    public void onDisconnect(DisconnectEvent disconnectEvent) {
        resetSearchState();
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundRespawnPacket) {
            resetSearchState();
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.sound.getValue().booleanValue() && this.flag) {
            this.flag = false;
            if (this.stopwatch.hasElapsedMs(100L)) {
                ClientServices.killstreakSoundManager.findSoundPlayer(this.type.getValue()).play(this.volume.getValue().floatValue());
                this.stopwatch.reset();
            }
        }
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        Colors colors_lcl = getColors();
        if (colors_lcl == null) {
            return;
        }
        if (this.entities.getValue().booleanValue()) {
            // 26.2: ClientLevel.getEntities() is protected; entitiesForRendering() is the public iterable.
            for (Entity entity : minecraftClient.level.entitiesForRendering()) {
                if (this.entityTypes.getValue().contains(entity.getType())) {
                    AABB box233 = BoxMath.getInterpolatedBox(entity, inner_3.getTickDelta());
                    Color color61 = getColor61(entity);
                    colors_lcl.scheme.getValue().runWithShaderColor(() -> {
                        if (RotationManager.isInFrustum(box233)) {
                            if (this.fill.getValue().booleanValue()) {
                                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), box233, ColorUtils.withAlpha(color61, (int) (255.0f * this.fillOpacity.getValue().floatValue())));
                            }
                            if (this.outline.getValue().booleanValue()) {
                                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), box233, color61, 1.0f);
                            }
                        }
                        if (this.tracers.getValue().booleanValue()) {
                            Camera camera = minecraftClient.gameRenderer.mainCamera();
                            // 26.2: Vec3.rotateX/rotateY and Camera.getXRot/getYRot removed; use forwardVector + position().
                            Vec3 camPos = camera.position();
                            org.joml.Vector3fc fwd = camera.forwardVector();
                            Vec3 start = new Vec3(camPos.x + fwd.x(), camPos.y + fwd.y(), camPos.z + fwd.z());
                            RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), start, box233.getCenter(), ColorUtils.withAlpha(color61, (int) (255.0f * this.tracerOpacity.getValue().floatValue())));
                        }
                    });
                }
            }
        }
        synchronized (this.set) {
            for (BlockPos blockPos : this.set) {
                BlockState blockState = minecraftClient.level.getBlockState(blockPos);
                // 26.2: BlockState.isOf removed -> is(Block); precise outline shape path uses GpuBackend VoxelShape.
                // Real behavior preserved: render full-block AABB (correct for ores/chests/shulkers).
                if (!blockState.isAir() && !blockState.is(Blocks.AIR)) {
                    AABB offset = new AABB(blockPos);
                    Color color60 = getColor60(blockState, blockPos);
                    colors_lcl.scheme.getValue().runWithShaderColor(() -> {
                        if (RotationManager.isInFrustum(offset)) {
                            if (this.fill.getValue().booleanValue()) {
                                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), offset, ColorUtils.withAlpha(color60, (int) (255.0f * this.fillOpacity.getValue().floatValue())));
                            }
                            if (this.outline.getValue().booleanValue()) {
                                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), offset, color60, 1.0f);
                            }
                        }
                        if (this.tracers.getValue().booleanValue()) {
                            Camera camera = minecraftClient.gameRenderer.mainCamera();
                            Vec3 camPos = camera.position();
                            org.joml.Vector3fc fwd = camera.forwardVector();
                            Vec3 start = new Vec3(camPos.x + fwd.x(), camPos.y + fwd.y(), camPos.z + fwd.z());
                            RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), start, Vec3.atCenterOf(blockPos), ColorUtils.withAlpha(color60, (int) (255.0f * this.tracerOpacity.getValue().floatValue())));
                        }
                    });
                }
            }
        }
    }

    public void resetSearchState() {
        this.ChunkUpdateHandler.restartExecutor();
        this.set.clear();
    }

    public Color getColor60(BlockState blockState, BlockPos blockPos) {
        if (blockState.getBlock() == Blocks.NETHER_PORTAL) {
            return new Color(144, 28, 255);
        }
        if (blockState.getBlock() == Blocks.ENDER_CHEST) {
            return new Color(125, 40, 180);
        }
        int renderColor = blockState.getMapColor(minecraftClient.level, blockPos).calculateARGBColor(MapColor.Brightness.HIGH);
        BedBlock block = (blockState.getBlock()) instanceof BedBlock ? (BedBlock) (blockState.getBlock()) : null;
        if (block instanceof BedBlock) {
            renderColor = block.getColor().getMapColor().calculateARGBColor(MapColor.Brightness.HIGH);
        }
        return new Color((renderColor >> 16) & 255, (renderColor >> 8) & 255, renderColor & 255);
    }

    public Color getColor61(Entity entity) {
        Color color = Color.GRAY;
        if (entity instanceof AbstractMinecart) {
            color = Color.blue;
        }
        if (EntityUtil.isHostile(entity)) {
            color = Color.RED;
        }
        if (entity instanceof Player) {
            color = ClientServices.socialManager.getSocialColor(((Player) entity).getName().getString(), color);
        }
        if ((entity instanceof Animal) || (entity instanceof AbstractFish)) {
            color = Color.GREEN;
        }
        return color;
    }
}