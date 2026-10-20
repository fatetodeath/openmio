package me.mioclient.render.font;

import me.mioclient.api.ClientServices;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import javax.imageio.ImageIO;
import me.mioclient.module.client.Fonts;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.BufferUtils;

public class FontGlyphAtlas {
    /* Resolved lazily — eager static init ran before the module registry was ready. */
    private static Fonts fonts;

    public static Fonts getFonts() {
        if (fonts == null && ClientServices.moduleInjector != null) {
            try {
                fonts = (Fonts) ClientServices.moduleInjector.getModule(Fonts.class);
            } catch (Exception e) {
            }
        }
        return fonts;
    }
    public final Font font;
    public final boolean flag;
    public final boolean flag2;
    public int num;
    public BufferedImage bufferedImage;
    public AbstractTexture abstractTexture;
    public final HashMap<Character, Inner> hashMap = new HashMap<>();
    public int num2 = -1;
    /* 26.2 backend: the atlas lives in this vanilla-managed DynamicTexture. It must be
       registered under a fixed id so RenderTypes/blits can sample it through the GPU backend. */
    public static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("mio", "font_atlas");
    private static AbstractTexture registeredTexture;

    public static AbstractTexture getRegisteredTexture() {
        return registeredTexture;
    }

    static class Inner {
        public int x;
        public int y;
        public int num;
        public int num2;

        public Inner(int i, int i2, int i3, int i4) {
            this.x = i;
            this.y = i2;
            this.num = i3;
            this.num2 = i4;
        }

        public Inner() {
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getTotalWidth() {
            return this.num;
        }

        public int getFontHeight() {
            return this.num2;
        }
    }

    public FontGlyphAtlas(Font font, boolean z, boolean z2) {
        this.font = font;
        this.flag = z;
        this.flag2 = z2;
    }

    public void resetTrajectoryData() {
        this.abstractTexture.close();
        this.hashMap.clear();
    }

    public void buildGlyphAtlas(char[] cArr) {
        double longBitsToDouble = (-1.0d);
        double longBitsToDouble2 = (-1.0d);
        FontRenderContext fontRenderContext = new FontRenderContext(new AffineTransform(), this.flag, this.flag2);
        for (char c : cArr) {
            Rectangle2D stringBounds = this.font.getStringBounds(Character.toString(c), fontRenderContext);
            if (longBitsToDouble < stringBounds.getWidth()) {
                longBitsToDouble = stringBounds.getWidth();
            }
            if (longBitsToDouble2 < stringBounds.getHeight()) {
                longBitsToDouble2 = stringBounds.getHeight();
            }
        }
        double longBitsToDouble3 = longBitsToDouble + 2.0d;
        double longBitsToDouble4 = longBitsToDouble2 + 2.0d;
        this.num = ((int) Math.ceil(Math.max(Math.ceil(Math.sqrt((longBitsToDouble3 * longBitsToDouble3) * cArr.length) / longBitsToDouble3), Math.ceil(Math.sqrt((longBitsToDouble4 * longBitsToDouble4) * cArr.length) / longBitsToDouble4)) * Math.max(longBitsToDouble3, longBitsToDouble4))) + 1;
        this.bufferedImage = new BufferedImage(this.num, this.num, 2);
        Graphics2D createGraphics = this.bufferedImage.createGraphics();
        createGraphics.setFont(this.font);
        createGraphics.setColor(new Color(255, 255, 255, 0));
        createGraphics.fillRect(0, 0, this.num, this.num);
        createGraphics.setColor(Color.white);
        Fonts fonts_lcl = getFonts();
        if (fonts_lcl != null && fonts_lcl.antiAlias.getValue().booleanValue() && fonts_lcl.isToggled()) {
            createGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            createGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            createGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            createGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.flag2 ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            createGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.flag ? RenderingHints.VALUE_ANTIALIAS_OFF : RenderingHints.VALUE_ANTIALIAS_ON);
            createGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, this.flag ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        }
        FontMetrics fontMetrics = createGraphics.getFontMetrics();
        int i = 0;
        int i2 = 0;
        int i3 = 1;
        for (char c2 : cArr) {
            Inner inner = new Inner();
            Rectangle2D stringBounds2 = fontMetrics.getStringBounds(Character.toString(c2), createGraphics);
            inner.num = stringBounds2.getBounds().width + 8;
            inner.num2 = stringBounds2.getBounds().height;
            if (i2 + inner.num >= this.num) {
                i2 = 0;
                i3 += i;
                i = 0;
            }
            inner.x = i2;
            inner.y = i3;
            if (inner.num2 > this.num2) {
                this.num2 = inner.num2;
            }
            if (inner.num2 > i) {
                i = inner.num2;
            }
            createGraphics.drawString(Character.toString(c2), i2 + 2, i3 + fontMetrics.getAscent());
            i2 += inner.num;
            this.hashMap.put(Character.valueOf(c2), inner);
        }
        this.bufferedImage.flush();
        createGraphics.dispose();
    }

    public void uploadAtlasTexture() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(this.bufferedImage, "png", byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            ByteBuffer put = BufferUtils.createByteBuffer(byteArray.length).put(byteArray);
            put.flip();
            this.abstractTexture = new LinearFontTexture(() -> "mio-font-atlas", NativeImage.read(put));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bindAtlasTexture() {
        // 26.2: GPU-driven textures have no GL id to bind; the atlas lives in the DynamicTexture above.
    }

    public void unbindAtlasTexture() {
        // 26.2: no-op, see bindAtlasTexture.
    }

    public void ensureRegistered() {
        if (this.abstractTexture == null || registeredTexture == this.abstractTexture) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return;
            }
            TextureManager textureManager = mc.getTextureManager();
            if (textureManager == null) {
                return;
            }
            try {
                textureManager.release(ATLAS_ID);
            } catch (Exception ignored) {
            }
            textureManager.register(ATLAS_ID, this.abstractTexture);
            registeredTexture = this.abstractTexture;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public float renderGlyphQuad(PoseStack matrixStack, VertexConsumer vertexConsumer, char c, float f, float f2, float f3, float f4, float f5, float f6) {
        Inner inner = this.hashMap.get(Character.valueOf(c));
        if (inner == null) {
            return 0.0f;
        }
        float f11 = inner.x / (float) this.num;
        float f12 = inner.y / (float) this.num;
        float f13 = inner.num / (float) this.num;
        float f14 = inner.num2 / (float) this.num;
        float f15 = inner.num;
        float f16 = inner.num2;
        PoseStack.Pose pose = matrixStack.last();
        vertexConsumer.addVertex(pose, f, f2 + f16, ClientTextRenderer.getCurrentShadowOffset()).setColor(f3, f5, f4, f6).setUv(f11, f12 + f14);
        vertexConsumer.addVertex(pose, f + f15, f2 + f16, ClientTextRenderer.getCurrentShadowOffset()).setColor(f3, f5, f4, f6).setUv(f11 + f13, f12 + f14);
        vertexConsumer.addVertex(pose, f + f15, f2, ClientTextRenderer.getCurrentShadowOffset()).setColor(f3, f5, f4, f6).setUv(f11 + f13, f12);
        vertexConsumer.addVertex(pose, f, f2, ClientTextRenderer.getCurrentShadowOffset()).setColor(f3, f5, f4, f6).setUv(f11, f12);
        return f15 - 8.0f;
    }

    public float getCharacterWidth(char c) {
        if (!this.hashMap.containsKey(Character.valueOf(c))) {
            c = ' ';
        }
        return this.hashMap.get(Character.valueOf(c)).num;
    }

    public int getMaxCharHeight() {
        return this.num2;
    }

    public boolean isAntiAliased() {
        return this.flag;
    }

    public boolean hasFractionalMetrics() {
        return this.flag2;
    }
}
