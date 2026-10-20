package me.mioclient.setting;

import me.mioclient.gui.widget.PanelComponent;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.mode.render.Direction;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.math.GridPoint;
import me.mioclient.util.math.MathConstants;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.glfw.GLFW;
import me.mioclient.render.RenderConstants;

public class GameStopwatchPresetSettingButton extends StopwatchPresetSettingButton {
    public Direction mode_3;
    public Direction mode_32;
    public final List<GridPoint> list;
    public GridPoint positionData;
    public int num;
    public boolean flag;

    public GameStopwatchPresetSettingButton(ClickGuiPanel presetEnumSettingHelper, int i) {
        super(presetEnumSettingHelper, i);
        this.mode_3 = Direction.UP;
        this.mode_32 = null;
        this.list = new ArrayList();
        do1640();
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton
    public void onTickElapsed() {
        if (this.flag) {
            return;
        }
        if (this.mode_32 != null) {
            this.mode_3 = this.mode_32;
            this.mode_32 = null;
        }
        if (this.list.isEmpty()) {
            return;
        }
        GridPoint positionData1221 = ((GridPoint) this.list.getFirst()).addPoint(this.mode_3.getPositionData244());
        if (positionData1221.get476() > this.num - 1) {
            positionData1221 = new GridPoint(0, positionData1221.getGridY());
        } else if (positionData1221.get476() < 0) {
            positionData1221 = new GridPoint(this.num - 1, positionData1221.getGridY());
        } else if (positionData1221.getGridY() > this.num2 - 1) {
            positionData1221 = new GridPoint(positionData1221.get476(), 0);
        } else if (positionData1221.getGridY() < 0) {
            positionData1221 = new GridPoint(positionData1221.get476(), this.num2 - 1);
        }
        if (this.list.contains(positionData1221)) {
            this.flag = true;
            return;
        }
        this.list.addFirst(positionData1221);
        if (!((GridPoint) this.list.getFirst()).equals(this.positionData)) {
            this.list.removeLast();
        } else {
            this.positionData = findUnusedGridPosition();
            this.num++;
        }
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
    public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
        super.render(drawContext, matrixStack, d, d2);
        drawGridCell(matrixStack, this.positionData.get476(), this.positionData.getGridY(), Color.red);
        boolean z = false;
        for (GridPoint positionData : this.list) {
            if (z) {
                drawGridCell(matrixStack, positionData.get476(), positionData.getGridY(), this.flag ? Color.GRAY : new Color(0, 150, 0));
            } else {
                drawGridCell(matrixStack, positionData.get476(), positionData.getGridY(), Color.GREEN);
                z = true;
            }
        }
        if (!this.flag) {
            ClientTextRenderer.INSTANCE.drawScaledTextShadow(drawContext, String.valueOf(this.num), getPanel().getX() + 1.5f, getPanel().getY() + this.num + 1, 0.75f, Color.white);
        } else {
            String string = new StringTemplate().arg(this.num).format("Score \u0001. Press R.");
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string, (float) ((getPanel().getX() + (getPanel().getTotalWidth() * MathConstants.val2)) - (ClientTextRenderer.INSTANCE.getStringWidth(string) * MathConstants.val2)), (float) (getPanel().getY() + this.num + (getFontHeight() * MathConstants.val2)), Color.white);
        }
    }

    @Override // me.mioclient.gui.GuiComponent
    public void keyPressed(int i) {
        Direction mode_3 = null;
        switch (i) {
            case 82:
                do1640();
                break;
            case 262:
                mode_3 = Direction.RIGHT;
                break;
            case 263:
                mode_3 = Direction.LEFT;
                break;
            case 264:
                mode_3 = Direction.DOWN;
                break;
            case 265:
                mode_3 = Direction.UP;
                break;
        }
        if (mode_3 == null || mode_3 == this.mode_3.getOpposite()) {
            return;
        }
        this.mode_32 = mode_3;
    }

    @Override // me.mioclient.setting.StopwatchPresetSettingButton
    public int getTickDelay() {
        if (GLFW.glfwGetKey(minecraftClient.getWindow().handle(), 32) == 1) {
            return RenderConstants.num;
        }
        return 500;
    }

    @Override // me.mioclient.gui.GuiComponent
    public int getFontHeight() {
        return getPanel().getTotalWidth();
    }

    public void do1640() {
        this.list.clear();
        this.list.add(new GridPoint(11, 11));
        this.positionData = findUnusedGridPosition();
        this.num = 0;
        this.flag = false;
    }

    public GridPoint findUnusedGridPosition() {
        GridPoint positionData = (GridPoint) this.list.getFirst();
        while (true) {
            GridPoint positionData2 = positionData;
            if (!this.list.contains(positionData2)) {
                return positionData2;
            }
            positionData = getPositionData3123();
        }
    }

    public GridPoint getPositionData3123() {
        return new GridPoint((int) (Math.random() * this.num), (int) (Math.random() * this.num2));
    }
}
