package me.mioclient.module.render;

import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.time.Stopwatch;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.network.ChunkUpdateHandler;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Direction;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Tunnels extends Module {
    public Setting<Boolean> verticals = add(new BooleanSetting("Verticals", true));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    public Setting<Float> height = add(new NumberSetting<>("Height", 0.1f, -1.0f, 2.0f).setSuffix("m"));
    public Setting<Integer> minLength = add(new NumberSetting<>("MinLength", 4, 1, 16).setSuffix("m"));
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(1358888960, true)).setThemeSync(true));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(-65536, true)).setThemeSync(true));
    public static final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    public final Stopwatch stopwatch;
    public final List<Inner> list;
    public final Queue<Inner> queue;
    public final ObjectSet<Inner> objectSet;
    public final ChunkUpdateHandler ChunkUpdateHandler;

    
    
    public static class Inner {
        public final boolean flag;
        public AABB box;

        public Inner(BlockPos blockPos, boolean z) {
            this(new AABB(blockPos), z);
        }

        public Inner(AABB box, boolean z) {
            this.box = box;
            this.flag = z;
        }

        public double get696() {
            return Math.max(Math.max(this.box.getXsize(), this.box.getZsize()), this.box.getYsize());
        }

        public boolean isAdjacentTo(Inner inner) {
            if (this.flag != inner.flag) {
                return false;
            }
            AABB box = inner.box;
            if (!this.flag) {
                if (box.minY != this.box.minY) {
                    return false;
                }
                return (this.box.minX > box.maxX ? 1 : (this.box.minX == box.maxX ? 0 : -1)) == 0 || (this.box.maxX > box.minX ? 1 : (this.box.maxX == box.minX ? 0 : -1)) == 0 ? this.box.minZ == box.minZ && this.box.maxZ == box.maxZ : ((this.box.minZ > box.maxZ ? 1 : (this.box.minZ == box.maxZ ? 0 : -1)) == 0 || (this.box.maxZ > box.minZ ? 1 : (this.box.maxZ == box.minZ ? 0 : -1)) == 0) && this.box.minX == box.minX && this.box.maxX == box.maxX;
            }
            if (box.minY == this.box.maxY || box.maxY == this.box.minY) {
                return ((this.box.minX > box.minX ? 1 : (this.box.minX == box.minX ? 0 : -1)) == 0 && (this.box.maxX > box.maxX ? 1 : (this.box.maxX == box.maxX ? 0 : -1)) == 0) && ((this.box.minZ > box.minZ ? 1 : (this.box.minZ == box.minZ ? 0 : -1)) == 0 && (this.box.maxZ > box.maxZ ? 1 : (this.box.maxZ == box.maxZ ? 0 : -1)) == 0);
            }
            return false;
        }

        public void expandBoundingBox(BlockPos blockPos) {
            this.box = this.box.minmax(new AABB(blockPos));
        }

        public int hashCode() {
            return Objects.hash(this.box, Boolean.valueOf(this.flag));
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Inner)) {
                return false;
            }
            Inner inner = (Inner) obj;
            return inner.box.equals(this.box) && this.flag == inner.flag;
        }
    }

    public Tunnels() {
        super("Tunnels", "Highlights dug-out tunnels.", Category.RENDER, new String[0]);
        this.stopwatch = new Stopwatch();
        this.list = Collections.synchronizedList(new ArrayList());
        this.queue = new ArrayDeque();
        this.objectSet = new ObjectOpenHashSet();
        this.ChunkUpdateHandler = new ChunkUpdateHandler(1, (chunkPos, worldChunk) -> {
            detectTunnelsInChunk(chunkPos);
        }, (chunkPos2, worldChunk2) -> {
            this.list.removeIf(inner -> {
                return is1982(BlockPos.containing(inner.box.getCenter()), chunkPos2);
            });
        }, null);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.ChunkUpdateHandler.do1640();
        if (isNotInGame()) {
            return;
        }
        this.ChunkUpdateHandler.processAllChunks();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        this.list.clear();
        this.ChunkUpdateHandler.stopListening();
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (isNotInGame()) {
            return;
        }
        synchronized (this.list) {
            if ((!this.queue.isEmpty()) && this.stopwatch.hasElapsedMs(1000L)) {
                this.stopwatch.reset();
                executorService.submit(() -> {
                    do1978(new ArrayList(this.list));
                });
            }
            this.list.removeIf(inner -> {
                return Math.sqrt(minecraftClient.player.getEyePosition().distanceToSqr(inner.box.getCenter())) > 256.0d;
            });
        }
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (isNotInGame()) {
            return;
        }
        synchronized (this.list) {
            for (Inner inner : this.list) {
                if (inner.get696() >= this.minLength.getValue().intValue()) {
                    float floatValue = this.height.getValue().floatValue();
                    if (inner.flag) {
                        floatValue = (float) inner.box.getYsize();
                    }
                    AABB withMaxY = inner.box.setMaxY(inner.box.minY + floatValue);
                    if (RotationManager.isInFrustum(withMaxY)) {
                        BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), withMaxY, this.fill.getValue());
                        BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), withMaxY, this.outline.getValue(), this.lineWidth.getValue().floatValue());
                    }
                }
            }
        }
    }

    public void do1978(List<Inner> list) {
        Inner poll;
        while (!this.queue.isEmpty() && (poll = this.queue.poll()) != null) {
            boolean z = false;
            Iterator<Inner> it = list.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                Inner next = it.next();
                if (next.isAdjacentTo(poll)) {
                    next.expandBoundingBox(BlockPos.containing(poll.box.getCenter()));
                    z = true;
                    break;
                }
            }
            if (!z) {
                list.add(poll);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            for (int i2 = i; i2 < list.size(); i2++) {
                Inner inner = list.get(i);
                Inner inner2 = list.get(i2);
                if (inner.isAdjacentTo(inner2)) {
                    this.objectSet.add(inner2);
                }
            }
        }
        Iterator it2 = this.objectSet.iterator();
        while (it2.hasNext()) {
            list.remove((Inner) it2.next());
        }
        this.objectSet.clear();
        synchronized (this.list) {
            this.list.clear();
            this.list.addAll(list);
            list.clear();
        }
    }

    public void detectTunnelsInChunk(ChunkPos chunkPos) {
        boolean is1981;
        for (int i = 0; i < 16; i++) {
            for (int bottomY = minecraftClient.level.getMinY(); bottomY < minecraftClient.level.getMaxY(); bottomY++) {
                for (int i2 = 0; i2 < 16; i2++) {
                    mutable.set(chunkPos.getMinBlockX() + i, bottomY, chunkPos.getMinBlockZ() + i2);
                    if (isNonSolid(mutable) && ((is1981 = is1981(mutable)) || isTunnelCorridor(mutable))) {
                        if (is1981) {
                            this.queue.add(new Inner((BlockPos) mutable, true));
                        } else {
                            Iterator it = Direction.Plane.HORIZONTAL.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    if (isTunnelCorridor(mutable.relative((Direction) it.next()))) {
                                        this.queue.add(new Inner((BlockPos) mutable, false));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isTunnelCorridor(BlockPos blockPos) {
        if (!isNonSolid(blockPos.above()) || isNonSolid(blockPos.below()) || isNonSolid(blockPos.above().above())) {
            return false;
        }
        return (isNonSolid(blockPos.north()) && isNonSolid(blockPos.south()) && isNonSolid(blockPos.above().north()) && isNonSolid(blockPos.above().south())) ? (isNonSolid(blockPos.east()) || isNonSolid(blockPos.west()) || isNonSolid(blockPos.above().east()) || isNonSolid(blockPos.above().west())) ? false : true : isNonSolid(blockPos.east()) && isNonSolid(blockPos.west()) && isNonSolid(blockPos.above().east()) && isNonSolid(blockPos.above().west()) && !isNonSolid(blockPos.north()) && !isNonSolid(blockPos.south()) && !isNonSolid(blockPos.above().north()) && !isNonSolid(blockPos.above().south());
    }

    public boolean is1981(BlockPos blockPos) {
        if (!this.verticals.getValue().booleanValue()) {
            return false;
        }
        Iterator it = Direction.Plane.HORIZONTAL.iterator();
        while (it.hasNext()) {
            BlockState blockState = minecraftClient.level.getBlockState(blockPos.relative((Direction) it.next()));
            if (!blockState.isSolid() || blockState.is(Blocks.BAMBOO_BLOCK) || blockState.is(Blocks.BASALT)) {
                return false;
            }
        }
        Iterator it2 = Direction.Plane.VERTICAL.iterator();
        while (it2.hasNext()) {
            Direction direction = (Direction) it2.next();
            BlockPos offset = blockPos.relative(direction);
            if (!isNonSolid(offset)) {
                return false;
            }
            if (!isNonSolid(offset.relative(direction))) {
                return false;
            }
        }
        return true;
    }

    public boolean is1982(BlockPos blockPos, ChunkPos chunkPos) {
        return blockPos.getX() >= chunkPos.getMinBlockX() && blockPos.getX() <= chunkPos.getMaxBlockX() && blockPos.getZ() >= chunkPos.getMinBlockZ() && blockPos.getZ() <= chunkPos.getMaxBlockZ();
    }

    public boolean isNonSolid(BlockPos blockPos) {
        return !minecraftClient.level.getBlockState(blockPos).isSolid();
    }
}