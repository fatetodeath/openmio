package me.mioclient.gui.minigame;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.minigame.TetrominoPiece;
import me.mioclient.gui.minigame.TetrominoShape;
import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathConstants;

import java.awt.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.setting.StopwatchPresetSettingButton;

public class TetrisGame extends StopwatchPresetSettingButton {
    public boolean flag;
    public boolean flag2;
    public boolean flag3;
    public int num;
    public int num2;
    public int num3;
    public TetrominoPiece randomHelper;
    public TetrominoShape[] mode_7Arr;

    public TetrisGame(ClickGuiPanel presetEnumSettingHelper, int i) {
        super(presetEnumSettingHelper, i, 22, 22);
        this.flag = false;
        this.flag2 = false;
        this.flag3 = false;
        this.num = 0;
        this.num2 = 0;
        this.num3 = 0;
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton
    public void onTickElapsed() {
        if (this.randomHelper == null) {
            do1640();
        }
        if (this.flag2) {
            return;
        }
        if (!this.flag) {
            moveDownOneLine();
        } else {
            this.flag = false;
            spawnNewPiece();
        }
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton
    public int getTickDelay() {
        return 500;
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        super.render(drawContext, matrixStack, d, d2);
        for (int i = 0; i < this.num2; i++) {
            for (int i2 = 0; i2 < this.num; i2++) {
                TetrominoShape mode_71641 = getShapeAt(i2, (this.num2 - i) - 1);
                if (mode_71641 != TetrominoShape.NoShape) {
                    drawGridCell(matrixStack, i2, i, mode_71641.getColor());
                }
            }
        }
        if (this.randomHelper.getShape() != TetrominoShape.NoShape) {
            TetrominoPiece randomHelper = new TetrominoPiece();
            randomHelper.setShape(this.randomHelper.getShape());
            for (int i3 = 0; i3 < 4; i3++) {
                randomHelper.setBlockX(i3, this.randomHelper.getBlockX(i3));
                randomHelper.setBlockY(i3, this.randomHelper.getBlockY(i3));
                drawGridCell(matrixStack, this.num2 + this.randomHelper.getBlockX(i3), (this.num2 - (this.num3 - this.randomHelper.getBlockY(i3))) - 1, this.randomHelper.getShape().getColor());
            }
            int i4 = this.num3;
            while (i4 > 0 && canPlacePiece(randomHelper, this.num2, i4 - 1)) {
                i4--;
            }
            for (int i5 = 0; i5 < 4; i5++) {
                drawGridCell(matrixStack, this.num2 + randomHelper.getBlockX(i5), (this.num2 - (i4 - randomHelper.getBlockY(i5))) - 1, ColorUtils.withAlpha(this.randomHelper.getShape().getColor(), 50));
            }
        }
        if (!this.flag3) {
            ClientTextRenderer.INSTANCE.drawScaledTextShadow(drawContext, String.valueOf(this.num), getPanel().getX() + 1.5f, getPanel().getY() + this.num + 1, 0.75f, Color.white);
            return;
        }
        String string = new StringTemplate().arg(this.num).format("Score \u0001. Press R.");
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string, (float) ((getPanel().getX() + (getPanel().getTotalWidth() * MathConstants.val2)) - (ClientTextRenderer.INSTANCE.getStringWidth(string) * MathConstants.val2)), (float) (getPanel().getY() + this.num + (getFontHeight() * MathConstants.val2)), Color.white);
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        switch (i) {
            case 32:
                dropPiece();
                return;
            case 68:
                moveDownOneLine();
                return;
            case 82:
                do1640();
                return;
            case 262:
                tryPlacePiece(this.randomHelper, this.num2 + 1, this.num3);
                return;
            case 263:
                tryPlacePiece(this.randomHelper, this.num2 - 1, this.num3);
                return;
            case 264:
                tryPlacePiece(this.randomHelper.rotateCounterClockwise(), this.num2, this.num3);
                return;
            case 265:
                tryPlacePiece(this.randomHelper.rotateClockwise(), this.num2, this.num3);
                return;
            default:
                return;
        }
    }

    public void do1640() {
        this.flag3 = false;
        this.randomHelper = new TetrominoPiece();
        this.mode_7Arr = new TetrominoShape[this.num * this.num2];
        clearBoard();
        spawnNewPiece();
    }

    public TetrominoShape getShapeAt(int i, int i2) {
        return this.mode_7Arr[(i2 * this.num) + i];
    }

    public void dropPiece() {
        for (int i = this.num3; i > 0 && tryPlacePiece(this.randomHelper, this.num2, i - 1); i--) {
        }
        lockPiece();
    }

    public void moveDownOneLine() {
        if (tryPlacePiece(this.randomHelper, this.num2, this.num3 - 1)) {
            return;
        }
        lockPiece();
    }

    public void clearBoard() {
        this.num = 0;
        for (int i = 0; i < this.num2 * this.num; i++) {
            this.mode_7Arr[i] = TetrominoShape.NoShape;
        }
    }

    public void lockPiece() {
        for (int i = 0; i < 4; i++) {
            this.mode_7Arr[((this.num3 - this.randomHelper.getBlockY(i)) * this.num) + this.num2 + this.randomHelper.getBlockX(i)] = this.randomHelper.getShape();
        }
        removeFullLines();
        if (this.flag) {
            return;
        }
        spawnNewPiece();
    }

    public void spawnNewPiece() {
        this.randomHelper.randomizeShape();
        this.num2 = (this.num / 2) + 1;
        this.num3 = (this.num2 - 1) + this.randomHelper.getMinY();
        if (tryPlacePiece(this.randomHelper, this.num2, this.num3)) {
            return;
        }
        this.randomHelper.setShape(TetrominoShape.NoShape);
        this.flag3 = true;
    }

    public boolean canPlacePiece(TetrominoPiece randomHelper, int i, int i2) {
        for (int i3 = 0; i3 < 4; i3++) {
            int i4 = i + randomHelper.getBlockX(i3);
            int i5 = i2 - randomHelper.getBlockY(i3);
            if (i4 < 0 || i4 >= this.num || i5 < 0 || i5 >= this.num2 || getShapeAt(i4, i5) != TetrominoShape.NoShape) {
                return false;
            }
        }
        return true;
    }

    public boolean tryPlacePiece(TetrominoPiece randomHelper, int i, int i2) {
        if (!canPlacePiece(randomHelper, i, i2)) {
            return false;
        }
        this.randomHelper = randomHelper;
        this.num2 = i;
        this.num3 = i2;
        return true;
    }

    public void removeFullLines() {
        int i = 0;
        for (int i2 = this.num2 - 1; i2 >= 0; i2--) {
            boolean z = true;
            int i3 = 0;
            while (true) {
                if (i3 >= this.num) {
                    break;
                }
                if (getShapeAt(i3, i2) == TetrominoShape.NoShape) {
                    z = false;
                    break;
                }
                i3++;
            }
            if (z) {
                i++;
                for (int i4 = i2; i4 < this.num2 - 1; i4++) {
                    for (int i5 = 0; i5 < this.num; i5++) {
                        this.mode_7Arr[(i4 * this.num) + i5] = getShapeAt(i5, i4 + 1);
                    }
                }
            }
        }
        if (i > 0) {
            this.num += i;
            this.flag = true;
            this.randomHelper.setShape(TetrominoShape.NoShape);
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        return getPanel().getTotalWidth();
    }
}
