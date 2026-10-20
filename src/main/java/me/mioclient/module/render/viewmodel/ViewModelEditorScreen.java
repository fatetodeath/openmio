package me.mioclient.module.render.viewmodel;

import me.mioclient.gui.screen.AbstractClickGuiScreen;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.client.GuiScaleOverride;
import me.mioclient.util.client.InputState;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.time.Stopwatch;

import java.awt.Color;
import java.util.LinkedList;
import java.util.List;
import me.mioclient.api.Setting;
import me.mioclient.module.render.ViewModel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

public class ViewModelEditorScreen extends AbstractClickGuiScreen {
    public static final Color color = new Color(255, 255, 255, 100);
    public final ViewModel viewmodel;
    public final LinkedList<ViewModelTransform> linkedList = new LinkedList<>();
    public final Stopwatch stopwatch = new Stopwatch();
    public ViewModelTransformMode mode_11;
    public ViewModelTransform data_2;
    public double val;
    public double val2;
    public boolean flag;

    public ViewModelEditorScreen(ViewModel viewModel) {
        this.viewmodel = viewModel;
        this.data_2 = ViewModelTransform.fromViewModel(viewModel);
        this.linkedList.add(this.data_2);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void renderCustomContent(GuiGraphicsExtractor drawContext, int i, int i2, float f) {
        ViewModelTransform data_2 = this.data_2;
        if (getActiveTransformMode() == ViewModelTransformMode.NONE) {
            return;
        }
        float f2 = ((float) MathUtils.floorToDecimals((this.val - i) * 0.004999999888241291d, 2)) * (this.flag ? -1 : 1);
        float f3 = (float) MathUtils.floorToDecimals((this.val2 - i2) * 0.004999999888241291d, 2);
        if (InputState.isLeftShiftDown()) {
            if (Math.abs(f2) > Math.abs(f3)) {
                f3 = 0.0f;
            } else {
                f2 = 0.0f;
            }
        }
        if (getActiveTransformMode() == ViewModelTransformMode.TRANSLATE) {
            if (!InputState.isLeftCtrlDown()) {
                this.viewmodel.offX.setValue(Float.valueOf(data_2.getOffX() + f2));
                this.viewmodel.offY.setValue(Float.valueOf(data_2.getOffY() + f3));
                this.viewmodel.mainX.setValue(Float.valueOf(data_2.getMainX() + f2));
                this.viewmodel.mainY.setValue(Float.valueOf(data_2.getMainY() + f3));
                return;
            }
            if (this.flag) {
                this.viewmodel.offX.setValue(Float.valueOf(data_2.getOffX() + f2));
                this.viewmodel.offY.setValue(Float.valueOf(data_2.getOffY() + f3));
                return;
            } else {
                this.viewmodel.mainX.setValue(Float.valueOf(data_2.getMainX() + f2));
                this.viewmodel.mainY.setValue(Float.valueOf(data_2.getMainY() + f3));
                return;
            }
        }
        if (getActiveTransformMode() == ViewModelTransformMode.SCALE) {
            if (InputState.isLeftShiftDown()) {
                f2 = Math.abs(Math.max(f2, f3)) * Math.signum(f2);
                f3 = Math.abs(Math.max(f2, f3)) * Math.signum(f3);
            }
            float f4 = -f2;
            if (!InputState.isLeftCtrlDown()) {
                this.viewmodel.offScaleX.setValue(Float.valueOf(data_2.getOffScaleX() + f4));
                this.viewmodel.offScaleY.setValue(Float.valueOf(data_2.getOffTranslationY() + f3));
                this.viewmodel.mainScaleX.setValue(Float.valueOf(data_2.getMainScaleX() + f4));
                this.viewmodel.mainScaleY.setValue(Float.valueOf(data_2.getOffScaleY() + f3));
                return;
            }
            if (this.flag) {
                this.viewmodel.offScaleX.setValue(Float.valueOf(data_2.getOffScaleX() + f4));
                this.viewmodel.offScaleY.setValue(Float.valueOf(data_2.getOffTranslationY() + f3));
                return;
            } else {
                this.viewmodel.mainScaleX.setValue(Float.valueOf(data_2.getMainScaleX() + f4));
                this.viewmodel.mainScaleY.setValue(Float.valueOf(data_2.getOffScaleY() + f3));
                return;
            }
        }
        if (getActiveTransformMode() == ViewModelTransformMode.ROTATE) {
            float intBitsToFloat = f2 / 0.005f;
            float intBitsToFloat2 = f3 / 0.005f;
            if (!InputState.isLeftCtrlDown()) {
                this.viewmodel.offRotateX.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getOffRotationX() + intBitsToFloat2)));
                this.viewmodel.offRotateY.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getOffRotateY() + intBitsToFloat)));
                this.viewmodel.mainRotateX.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getMainRotateX() + intBitsToFloat2)));
                this.viewmodel.mainRotateY.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getMainRotateY() + intBitsToFloat)));
                return;
            }
            if (this.flag) {
                this.viewmodel.offRotateX.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getOffRotationX() + intBitsToFloat2)));
                this.viewmodel.offRotateY.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getOffRotateY() + intBitsToFloat)));
            } else {
                this.viewmodel.mainRotateX.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getMainRotateX() + intBitsToFloat2)));
                this.viewmodel.mainRotateY.setValue(Float.valueOf(MathUtils.getWrappedDegrees(data_2.getMainRotateY() + intBitsToFloat)));
            }
        }
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void handleMouseClicked(double d, double d2, int i) {
        setActiveTransformMode(buttonToTransformMode(i));
        if (getActiveTransformMode() != ViewModelTransformMode.NONE) {
            this.data_2 = ViewModelTransform.fromViewModel(this.viewmodel);
            this.linkedList.add(this.data_2);
            this.flag = d * GuiScaleOverride.getScaleMultiplier((float) this.val) < ((double) (((float) minecraftClient.getWindow().getGuiScaledWidth()) / 2.0f));
            this.val = d;
            this.val2 = d2;
        }
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void handleMouseReleased(double d, double d2, int i) {
        setActiveTransformMode(ViewModelTransformMode.NONE);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        if (d4 > (-1.0d) && d4 < 1.0d) {
            return super.mouseScrolled(d, d2, d3, d4);
        }
        boolean z = d * GuiScaleOverride.getScaleMultiplier((float) this.val) < ((double) (((float) minecraftClient.getWindow().getGuiScaledWidth()) / 2.0f));
        float longBitsToDouble = (float) (d4 * (-0.10000000149011612d));
        if (this.stopwatch.hasElapsedMs(100L)) {
            this.linkedList.add(ViewModelTransform.fromViewModel(this.viewmodel));
        }
        this.stopwatch.reset();
        if (!InputState.isLeftShiftDown()) {
            if (InputState.is2609()) {
                applyZDelta(this.viewmodel.mainScaleZ, this.viewmodel.offScaleZ, z, longBitsToDouble);
                return super.mouseScrolled(d, d2, d3, d4);
            }
            applyZDelta(this.viewmodel.mainZ, this.viewmodel.offZ, z, longBitsToDouble);
            return super.mouseScrolled(d, d2, d3, d4);
        }
        float f = MathUtils.getWrappedDegrees(this.viewmodel.offRotateZ.getValue().floatValue() + (((float) d4) * 10.0f));
        float f2 = MathUtils.getWrappedDegrees(this.viewmodel.mainRotateZ.getValue().floatValue() + (((float) d4) * 10.0f));
        if (!InputState.isLeftCtrlDown()) {
            this.viewmodel.offRotateZ.setValue(Float.valueOf(f));
            this.viewmodel.mainRotateZ.setValue(Float.valueOf(f2));
        } else if (z) {
            this.viewmodel.offRotateZ.setValue(Float.valueOf(f));
        } else {
            this.viewmodel.mainRotateZ.setValue(Float.valueOf(f2));
        }
        return super.mouseScrolled(d, d2, d3, d4);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean keyPressed(KeyEvent event) {
        int i = event.key();
        if (InputState.isLeftCtrlDown() && i == 90 && !this.linkedList.isEmpty()) {
            this.linkedList.getLast().applyTo(this.viewmodel);
            if (this.linkedList.size() != 1) {
                this.linkedList.removeLast();
            }
        }
        if (i == 261) {
            this.data_2 = ViewModelTransform.identity();
            this.data_2.applyTo(this.viewmodel);
            this.linkedList.add(this.data_2);
        }
        return super.keyPressed(event);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void renderOverlay(GuiGraphicsExtractor drawContext) {
        float scaledWindowWidth = drawContext.guiWidth();
        float scaledWindowHeight = drawContext.guiHeight();
        drawContext.fill((int) ((scaledWindowWidth / 2.0f) - 1.0f), 0, (int) ((scaledWindowWidth / 2.0f) + 1.0f), 50, color.getRGB());
        int i = 2;
        for (String str : List.of("Left Click - Rotate", "Right Click - Move", "Middle Click - Scale", "MouseHandler Scroll - Move by Z", "MouseHandler Scroll + Shift - Rotate by Z", "MouseHandler Scroll + Alt - Scale by Z", "Hold Ctrl - Modify one", "Hold Shift - Lock axis", "Ctrl + Z - Undo", "Delete - Reset to defaults")) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, str, (scaledWindowWidth - ClientTextRenderer.INSTANCE.getStringWidth(str)) / 2.0f, 50 + i, ColorUtils.withAlpha(Color.white, 180));
            i += ClientTextRenderer.INSTANCE.getFontHeight() + 1;
        }
        drawContext.fill((int) ((scaledWindowWidth / 2.0f) - 1.0f), i + 52, (int) ((scaledWindowWidth / 2.0f) + 1.0f), (int) scaledWindowHeight, color.getRGB());
    }

    public ViewModelTransformMode getActiveTransformMode() {
        return this.mode_11;
    }

    public ViewModelTransformMode buttonToTransformMode(int i) {
        switch (i) {
            case 0:
                return ViewModelTransformMode.ROTATE;
            case 1:
                return ViewModelTransformMode.TRANSLATE;
            case 2:
                return ViewModelTransformMode.SCALE;
            default:
                return ViewModelTransformMode.NONE;
        }
    }

    public void applyZDelta(Setting<Float> setting, Setting<Float> setting2, boolean z, float f) {
        if (!InputState.isLeftCtrlDown()) {
            setting.setValue(Float.valueOf(setting.getValue().floatValue() + f));
            setting2.setValue(Float.valueOf(setting2.getValue().floatValue() + f));
        } else if (z) {
            setting2.setValue(Float.valueOf(setting2.getValue().floatValue() + f));
        } else {
            setting.setValue(Float.valueOf(setting.getValue().floatValue() + f));
        }
    }

    public void setActiveTransformMode(ViewModelTransformMode mode_11) {
        this.mode_11 = mode_11;
    }
}