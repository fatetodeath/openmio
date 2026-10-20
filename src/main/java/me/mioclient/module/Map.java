package me.mioclient.module;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.module.exploit.newchunks.NewChunkEntry;
import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.math.ColorUtils;

import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.Iterator;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.manager.ScissorStackManager;
import me.mioclient.api.Setting;
import me.mioclient.module.exploit.NewChunks;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class Map extends me.mioclient.module.HudModule {
    public Setting<Mode> setting;
    public Setting<Float> setting2;
    public Setting<Float> setting3;
    public Setting<Color> setting4;
    public Setting<Color> setting5;
    public Setting<Color> setting6;
    public static final float val2 = 8.0f;
    public static final float val = 16.0f;
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving newChunks null (NPE). */
    private static NewChunks newChunks;

    public static NewChunks getNewChunks() {
        if (newChunks == null && ClientServices.moduleInjector != null) {
            try {
                newChunks = (NewChunks) ClientServices.moduleInjector.getModule(NewChunks.class);
            } catch (Exception e) {
            }
        }
        return newChunks;
    }
    public static final Identifier identifier = Identifier.fromNamespaceAndPath("mio", "textures/nav.png");

    
    
    public enum Mode implements NamedValue {
        NONE("None"),
        ARROW("Arrow"),
        DOT("Dot");

        public final String name;

        Mode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Map() {
        super("Map", "minimap");
        this.setting = add(new EnumSetting("Pointer", Mode.ARROW));
        this.setting2 = add(new NumberSetting("Width", Float.valueOf(76.0f), Float.valueOf(50.0f), Float.valueOf(500.0f)));
        this.setting3 = add(new NumberSetting("Height", Float.valueOf(76.0f), Float.valueOf(50.0f), Float.valueOf(250.0f)));
        this.setting4 = add(new ColorSetting("Pointer", Color.white));
        this.setting5 = add(new ColorSetting("Background", new Color(10, 10, 10, 50)));
        this.setting6 = add(new ColorSetting("Outline", new Color(10, 10, 10, 100)));
        this.setting4.setConfigName("PointerColor");
        this.setting4.setDisabled();
        setDescription(new StringTemplate().arg(String.valueOf(ChatFormatting.YELLOW)).format("\u0001Primarily used for NewChunks"));
        ResizableHudElement size = new ResizableHudElement(this);
        size.enableScaling(this);
        setElement(size);
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        // 26.2: GpuBackend removed immediate mode; draw the minimap with GuiGraphicsExtractor#fill rects.
        if (minecraftClient.level == null || minecraftClient.player == null) {
            return;
        }
        float w = this.setting2.getValue().floatValue();
        float h = this.setting3.getValue().floatValue();
        int bg = this.setting5.getValue().getRGB();
        int outline = this.setting6.getValue().getRGB();
        drawContext.fill(0, 0, (int) w, (int) h, bg);
        drawContext.fill(0, 0, (int) w, 1, outline);
        drawContext.fill(0, (int) h - 1, (int) w, (int) h, outline);
        drawContext.fill(0, 0, 1, (int) h, outline);
        drawContext.fill((int) w - 1, 0, (int) w, (int) h, outline);
        ChunkPos playerChunk = minecraftClient.player.chunkPosition();
        int range = 4;
        int cell = Math.max(2, (int) (Math.min(w, h) / (range * 2 + 1)));
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                ChunkPos cp = new ChunkPos(playerChunk.x() + dx, playerChunk.z() + dz);
                if (!isChunkInRange(cp)) {
                    continue;
                }
                NewChunks newChunks_lcl = getNewChunks();
                boolean isNew = newChunks_lcl != null && !newChunks_lcl.getEntriesForChunk(cp).isEmpty();
                java.awt.Color c = isNew ? java.awt.Color.RED : new java.awt.Color(60, 60, 60, 120);
                int cx = (int) (w / 2 + dx * cell - cell / 2);
                int cz = (int) (h / 2 + dz * cell - cell / 2);
                drawContext.fill(cx, cz, cx + cell - 1, cz + cell - 1, c.getRGB());
            }
        }
        do2055(drawContext, w / 2, h / 2);
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        return new float[]{this.setting2.getValue().floatValue(), this.setting3.getValue().floatValue()};
    }

    public boolean isChunkInRange(ChunkPos chunkPos) {
        // 26.2: ChunkPos is a record(x, z); range-check against the player chunk directly.
        if (minecraftClient.player == null) {
            return false;
        }
        ChunkPos playerChunk = minecraftClient.player.chunkPosition();
        int dx = chunkPos.x() - playerChunk.x();
        int dz = chunkPos.z() - playerChunk.z();
        return dx * dx + dz * dz <= 25;
    }

    public void drawChunkRect(GuiGraphicsExtractor drawContext, ChunkPos chunkPos, Color color) {
        if (minecraftClient.player == null) {
            return;
        }
        ChunkPos playerChunk = minecraftClient.player.chunkPosition();
        float w = this.setting2.getValue().floatValue();
        float h = this.setting3.getValue().floatValue();
        int range = 4;
        int cell = Math.max(2, (int) (Math.min(w, h) / (range * 2 + 1)));
        int cx = (int) (w / 2 + (chunkPos.x() - playerChunk.x()) * cell - cell / 2);
        int cz = (int) (h / 2 + (chunkPos.z() - playerChunk.z()) * cell - cell / 2);
        drawContext.fill(cx, cz, cx + cell - 1, cz + cell - 1, color.getRGB());
    }

    public void do2055(GuiGraphicsExtractor drawContext, float f, float f2) {
        // 26.2: player pointer drawn as a small cross via fills (no GlStateManager).
        int c = this.setting4.getValue().getRGB();
        int x = (int) f;
        int y = (int) f2;
        Mode pointer = this.setting.getValue();
        if (pointer == Mode.NONE) {
            return;
        }
        if (pointer == Mode.DOT) {
            drawContext.fill(x - 2, y - 2, x + 2, y + 2, c);
            return;
        }
        float yaw = minecraftClient.player == null ? 0.0f : minecraftClient.player.getYRot();
        double rad = Math.toRadians(yaw);
        int dx = (int) Math.round(-Math.sin(rad) * 5);
        int dz = (int) Math.round(Math.cos(rad) * 5);
        drawContext.fill(x - 1, y - 1, x + 1, y + 1, c);
        drawContext.fill(x, y, x + dx, y + dz, c);
    }
}