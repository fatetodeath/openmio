package me.mioclient.render.font;

import me.mioclient.mode.client.FontStyle;
import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import me.mioclient.render.shape.Shape2DRenderer;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

public class AdvanceGlyph {
    public final TextHandler textHandler = new TextHandler(this);
    public final FontGlyphAtlas fontHelper;
    public float val;
    public float val2;
    public static final TrajectoriesVertexConsumer trajectoriesVertexConsumer = TrajectoriesVertexConsumer.createDefault();
    public static final TrajectoriesVertexConsumer trajectoriesVertexConsumer2 = TrajectoriesVertexConsumer.createDefault();
    public static final int[] intArr = new int[32];

    public AdvanceGlyph(FontGlyphAtlas fontHelper) {
        this.fontHelper = fontHelper;
    }

    public static AdvanceGlyph createForFontStyle(String str, int i, FontStyle fontStyle) {
        FontGlyphAtlas fontHelper;
        char[] cArr = new char[1501];
        for (int i2 = 0; i2 < cArr.length - 1; i2++) {
            cArr[i2] = (char) i2;
        }
        cArr[cArr.length - 1] = 8734;
        switch (fontStyle) {
            case PLAIN:
                fontHelper = new FontGlyphAtlas(new java.awt.Font(str, 0, i * 2), true, true);
                break;
            case BOLD:
                fontHelper = new FontGlyphAtlas(new java.awt.Font(str, 1, i * 2), true, true);
                break;
            case ITALIC:
                fontHelper = new FontGlyphAtlas(new java.awt.Font(str, 2, i * 2), true, true);
                break;
            case BOLDITALIC:
                fontHelper = new FontGlyphAtlas(new java.awt.Font(str, 3, i * 2), true, true);
                break;
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
        FontGlyphAtlas fontHelper2 = fontHelper;
        fontHelper2.buildGlyphAtlas(cArr);
        fontHelper2.uploadAtlasTexture();
        return new AdvanceGlyph(fontHelper2);
    }

    public int drawStringShadowFloat(PoseStack matrixStack, String str, float f, float f2, int i) {
        return drawStringWithShadow(matrixStack, str, f, f2, i, true);
    }

    public int drawStringShadowDouble(PoseStack matrixStack, String str, double d, double d2, int i) {
        return drawStringWithShadow(matrixStack, str, (float) d, (float) d2, i, true);
    }

    public int drawStringPlainFloat(PoseStack matrixStack, String str, float f, float f2, int i) {
        return drawStringWithShadow(matrixStack, str, f, f2, i, false);
    }

    public int drawStringPlainDouble(PoseStack matrixStack, String str, double d, double d2, int i) {
        return drawStringWithShadow(matrixStack, str, (float) d, (float) d2, i, false);
    }

    public int drawCenteredString(PoseStack matrixStack, String str, double d, double d2, int i) {
        return drawStringWithShadow(matrixStack, str, ((float) d) - (measureStringWidth(str) / 2.0f), (float) d2, i, false);
    }

    public int drawCenteredStringShadow(PoseStack matrixStack, String str, double d, double d2, int i) {
        return drawStringWithShadow(matrixStack, str, ((float) d) - (measureStringWidth(str) / 2.0f), (float) d2, i, true);
    }

    public int renderOrderedText(GuiGraphicsExtractor graphics, FormattedCharSequence orderedText, float f, float f2, int i, boolean z) {
        /* 26.2 backend: glyphs are collected into one batched run (a single glyph render
           node) sampled from the registered atlas. Falls back to per-glyph blits if the
           atlas has no GPU view yet. Geometry matches the previous 0.5-scale layout. */
        if (orderedText == null || graphics == null) {
            return 0;
        }
        if (((i >> 24) & 255) == 0) {
            return 0;
        }
        this.fontHelper.ensureRegistered();
        float x = f + ClientTextRenderer.getTranslateOffset();
        float y = f2 + ClientTextRenderer.getShiftOffset();
        FontGlyphRun run = new FontGlyphRun();
        run.atlasSize = this.fontHelper.num;
        float end;
        if (z) {
            int shadow = ((i & 16579836) >> 2) | (i & (-16777216));
            collectGlyphs(run, orderedText, x + ClientTextRenderer.getShadowOffset(), y + ClientTextRenderer.getShadowOffset(), shadow);
            end = collectGlyphs(run, orderedText, x, y, i);
        } else {
            end = collectGlyphs(run, orderedText, x, y, i);
        }
        if (!run.isEmpty()) {
            boolean submitted = false;
            if (run.textureView() != null) {
                try {
                    net.minecraft.client.gui.navigation.ScreenRectangle scissor = graphics.scissorStack.peek();
                    if (scissor == null) {
                        scissor = new net.minecraft.client.gui.navigation.ScreenRectangle(0, 0, graphics.guiWidth(), graphics.guiHeight());
                    }
                    graphics.guiRenderState.addGlyphToCurrentLayer(new net.minecraft.client.renderer.state.gui.GlyphRenderState(graphics.pose(), run, scissor));
                    submitted = true;
                } catch (Exception ignored) {
                }
            }
            if (!submitted) {
                drawBlitFallback(graphics, run);
            }
        }
        return (int) (end - x);
    }

    /* Atlas glyphs are rasterized at 2x (see createForFontStyle); screen geometry uses
       quarter-scale metrics so on-screen size is unchanged but sharper. Cell padding
       (8px) is excluded from advances. */
    public static final float GLYPH_SCALE = 0.25f;
    public static final float GLYPH_CELL_PADDING = 8.0f;

    private float collectGlyphs(FontGlyphRun run, FormattedCharSequence orderedText, float x, float y, int color) {
        float baseAlpha = ((color >> 24) & 255);
        float baseRed = ((color >> 16) & 255);
        float baseGreen = ((color >> 8) & 255);
        float baseBlue = (color & 255);
        float spaceAdvance = (this.fontHelper.getCharacterWidth(' ') - GLYPH_CELL_PADDING) * GLYPH_SCALE;
        float[] cursor = new float[]{x};
        boolean[] skipFormat = new boolean[]{false};
        int[] legacyRgb = new int[]{-1};
        orderedText.accept((index, style, codepoint) -> {
            char c = (char) codepoint;
            /* Legacy '§' color codes arrive here as plain codepoints when the
               source was Component.literal("§e..."). Consume the marker and its
               format char so no stray glyph is drawn and width matches
               measureStringWidth (which already skips them). */
            if (skipFormat[0]) {
                skipFormat[0] = false;
                int formatIndex = "0123456789abcdef".indexOf(Character.toLowerCase(c));
                if (formatIndex >= 0) {
                    legacyRgb[0] = intArr[formatIndex];
                } else if (c == 'r' || c == 'R') {
                    legacyRgb[0] = -1;
                }
                return true;
            }
            if (c == 167) {
                skipFormat[0] = true;
                return true;
            }
            FontGlyphAtlas.Inner glyph = this.fontHelper.hashMap.get(Character.valueOf(c));
            if (glyph == null) {
                cursor[0] += spaceAdvance;
                return true;
            }
            float red = baseRed;
            float green = baseGreen;
            float blue = baseBlue;
            TextColor textColor = style.getColor();
            if (textColor != null) {
                int rgb = textColor.getValue();
                red = ((rgb >> 16) & 255);
                green = ((rgb >> 8) & 255);
                blue = (rgb & 255);
            } else if (legacyRgb[0] >= 0) {
                red = ((legacyRgb[0] >> 16) & 255);
                green = ((legacyRgb[0] >> 8) & 255);
                blue = (legacyRgb[0] & 255);
            }
            int argb = (((int) baseAlpha) << 24) | ((((int) red) & 255) << 16) | ((((int) green) & 255) << 8) | (((int) blue) & 255);
            int gw = glyph.num;
            int gh = glyph.num2;
            float dw = Math.max(1, Math.round(gw * GLYPH_SCALE));
            float dh = Math.max(1, Math.round(gh * GLYPH_SCALE));
            run.addQuad(cursor[0], y, dw, dh, glyph.x, glyph.y, gw, gh, argb);
            cursor[0] += (gw - GLYPH_CELL_PADDING) * GLYPH_SCALE;
            return true;
        });
        return cursor[0];
    }

    public int renderOrderedText(PoseStack matrixStack, FormattedCharSequence orderedText, float f, float f2, int i, boolean z) {
        MatrixStackCharacterVisitor visitor = new MatrixStackCharacterVisitor(matrixStack, i, 1.0f, this.fontHelper);
        return z ? Math.max(visitor.renderText(orderedText, f + ClientTextRenderer.getShadowOffset(), f2 + ClientTextRenderer.getShadowOffset()), visitor.renderText(orderedText, f, f2)) : visitor.renderText(orderedText, f, f2);
    }

    private void drawBlitFallback(GuiGraphicsExtractor graphics, FontGlyphRun run) {
        int atlasSize = run.atlasSize <= 0 ? this.fontHelper.num : run.atlasSize;
        if (atlasSize <= 0) {
            return;
        }
        for (FontGlyphRun.Quad quad : run.quads) {
            try {
                graphics.blit(RenderPipelines.GUI_TEXTURED, FontGlyphAtlas.ATLAS_ID, Math.round(quad.x), Math.round(quad.y), quad.u, quad.v, Math.max(1, Math.round(quad.w)), Math.max(1, Math.round(quad.h)), Math.round(quad.gw), Math.round(quad.gh), atlasSize, atlasSize, quad.color);
            } catch (Exception ignored) {
            }
        }
    }

    public int drawStringWithShadow(PoseStack matrixStack, String str, float f, float f2, int i, boolean z) {
        return z ? Math.max(renderStringWithColor(matrixStack, str, f + ClientTextRenderer.getShadowOffset(), f2 + ClientTextRenderer.getShadowOffset(), i, true), renderStringWithColor(matrixStack, str, f, f2, i, false)) : renderStringWithColor(matrixStack, str, f, f2, i, false);
    }

    public int renderStringWithColor(PoseStack matrixStack, String str, float f, float f2, int i, boolean z) {
        if (str == null) {
            return 0;
        }
        if ((i & (-67108864)) == 0) {
            i |= -16777216;
        }
        if (z) {
            i = ((i & 16579836) >> 2) | (i & (-16777216));
        }
        this.val = f * 2.0f;
        this.val2 = f2 * 2.0f;
        renderStringCharacters(matrixStack, str, z, i);
        return (int) (this.val / 4.0f);
    }

    public void renderStringCharacters(PoseStack matrixStack, String str, boolean z, int i) {
        float intBitsToFloat = ((i >> 24) & 255) / 255.0f;
        float intBitsToFloat2 = ((i >> 16) & 255) / 255.0f;
        float intBitsToFloat3 = ((i >> 8) & 255) / 255.0f;
        float intBitsToFloat4 = (i & 255) / 255.0f;
        matrixStack.pushPose();
        matrixStack.translate(ClientTextRenderer.getTranslateOffset(), ClientTextRenderer.getShiftOffset(), 0.0f);
        matrixStack.scale(0.5f, 0.5f, 0.5f);
        if (!trajectoriesVertexConsumer.isBuilding()) {
            trajectoriesVertexConsumer.beginBuffer(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        int i2 = 0;
        while (i2 < str.length()) {
            char charAt = str.charAt(i2);
            if (charAt != 167 || i2 + 1 >= str.length()) {
                this.fontHelper.bindAtlasTexture();
                advanceCursor(this.fontHelper.renderGlyphQuad(matrixStack, trajectoriesVertexConsumer, charAt, this.val, this.val2, intBitsToFloat2, intBitsToFloat4, intBitsToFloat3, intBitsToFloat), this.fontHelper);
            } else {
                int indexOf = "0123456789abcdefklmnor".indexOf(str.toLowerCase(java.util.Locale.ENGLISH).charAt(i2 + 1));
                if (indexOf < 16) {
                    if (indexOf < 0) {
                        indexOf = 15;
                    }
                    if (z) {
                        indexOf += 16;
                    }
                    int i3 = intArr[indexOf];
                    intBitsToFloat2 = ((i3 >> 16) & 255) / 255.0f;
                    intBitsToFloat3 = ((i3 >> 8) & 255) / 255.0f;
                    intBitsToFloat4 = (i3 & 255) / 255.0f;
                } else {
                    intBitsToFloat2 = ((i >> 16) & 255) / 255.0f;
                    intBitsToFloat3 = ((i >> 8) & 255) / 255.0f;
                    intBitsToFloat4 = (i & 255) / 255.0f;
                }
                i2++;
            }
            i2++;
        }
        matrixStack.popPose();
    }

    public void advanceCursor(float f, FontGlyphAtlas fontHelper) {
        this.val += f;
    }

    public FontGlyphAtlas getFontAtlas() {
        return this.fontHelper;
    }

    public int getHalfLineHeight() {
        return this.fontHelper.getMaxCharHeight() / 4;
    }

    public int measureStringWidth(String str) {
        if (str == null) {
            return 0;
        }
        int i = 0;
        int length = str.length();
        int i2 = 0;
        while (i2 < length) {
            if (str.charAt(i2) != 167 || i2 + 1 >= length) {
                i += (int) ((getFontAtlas().getCharacterWidth(str.charAt(i2)) - GLYPH_CELL_PADDING) * GLYPH_SCALE);
            } else {
                i2++;
            }
            i2++;
        }
        return i + 1;
    }

    public float getCharacterAdvance(char c) {
        return (getFontAtlas().getCharacterWidth(c) - GLYPH_CELL_PADDING) * GLYPH_SCALE;
    }

    public void resetShadowOffset() {
        if (trajectoriesVertexConsumer.isBuilding()) {
            FontGlyphAtlas fontHelper2770 = getFontAtlas();
            fontHelper2770.bindAtlasTexture();
            TrajectoriesVertexConsumer.flush(trajectoriesVertexConsumer);
            fontHelper2770.unbindAtlasTexture();
        }
        flushPendingText();
    }

    public static void flushPendingText() {
        if (trajectoriesVertexConsumer2.isBuilding()) {
            TrajectoriesVertexConsumer.flush(trajectoriesVertexConsumer2);
        }
    }

    public TextHandler getTextHandler() {
        return this.textHandler;
    }

    public float getAdvance() {
        return 0.0f;
    }

    static {
        for (int i = 0; i < 32; i++) {
            int i2 = ((i >> 3) & 1) * 85;
            int i3 = (((i >> 2) & 1) * 170) + i2;
            int i4 = (((i >> 1) & 1) * 170) + i2;
            int i5 = ((i & 1) * 170) + i2;
            if (i == 6) {
                i3 += 85;
            }
            if (i >= 16) {
                i3 /= 4;
                i4 /= 4;
                i5 /= 4;
            }
            intArr[i] = ((i3 & 255) << 16) | ((i4 & 255) << 8) | (i5 & 255);
        }
    }
}
