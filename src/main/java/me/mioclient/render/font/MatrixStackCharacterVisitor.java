package me.mioclient.render.font;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class MatrixStackCharacterVisitor {
    public final float val;
    public final FontGlyphAtlas fontHelper;
    public final int num;
    public final PoseStack matrixStack;
    public final GuiGraphicsExtractor graphics;
    public float val2;
    public float val3;

    public MatrixStackCharacterVisitor(PoseStack matrixStack, int i, float f, FontGlyphAtlas fontHelper) {
        this.num = i;
        this.val = f;
        this.fontHelper = fontHelper;
        this.matrixStack = matrixStack;
        this.graphics = null;
    }

    public MatrixStackCharacterVisitor(int i, float f, FontGlyphAtlas fontHelper) {
        this.num = i;
        this.val = f;
        this.fontHelper = fontHelper;
        this.matrixStack = new PoseStack();
        this.graphics = null;
    }

    public int renderText(FormattedCharSequence orderedText, float f, float f2) {
        if (orderedText == null) {
            return 0;
        }
        this.val2 = f * 2.0f;
        this.val3 = f2 * 2.0f;
        visitCharacters(this.matrixStack, orderedText);
        return (int) (this.val2 / 4.0f);
    }

    public void visitCharacters(PoseStack matrixStack, FormattedCharSequence orderedText) {
        matrixStack.pushPose();
        matrixStack.translate(ClientTextRenderer.getTranslateOffset(), ClientTextRenderer.getShiftOffset(), 0.0f);
        matrixStack.scale(0.5f, 0.5f, 1.0f);
        if (!AdvanceGlyph.trajectoriesVertexConsumer.isBuilding()) {
            AdvanceGlyph.trajectoriesVertexConsumer.beginBuffer(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        orderedText.accept(this::accept);
        matrixStack.popPose();
    }

    public boolean accept(int i, Style style, int i2) {
        float intBitsToFloat;
        float intBitsToFloat2;
        float intBitsToFloat3;
        TextColor color = style.getColor();
        float intBitsToFloat4 = ((this.num >> 24) & 255) / 255.0f;
        if (color != null) {
            int rgb = color.getValue();
            intBitsToFloat = ((rgb >> 16) & 255) / 255.0f;
            intBitsToFloat2 = ((rgb >> 8) & 255) / 255.0f;
            intBitsToFloat3 = (rgb & 255) / 255.0f;
        } else {
            intBitsToFloat = ((this.num >> 16) & 255) / 255.0f;
            intBitsToFloat2 = ((this.num >> 8) & 255) / 255.0f;
            intBitsToFloat3 = (this.num & 255) / 255.0f;
        }
        float f = intBitsToFloat * this.val;
        float f2 = intBitsToFloat2 * this.val;
        float f3 = intBitsToFloat3 * this.val;
        this.fontHelper.bindAtlasTexture();
        this.val2 += this.fontHelper.renderGlyphQuad(this.matrixStack, AdvanceGlyph.trajectoriesVertexConsumer, Character.toString(i2).charAt(0), this.val2, this.val3, f, f3, f2, intBitsToFloat4);
        return true;
    }
}
