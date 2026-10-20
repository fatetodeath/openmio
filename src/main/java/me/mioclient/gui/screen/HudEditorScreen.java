package me.mioclient.gui.screen;

import me.mioclient.api.ClientServices;
import me.mioclient.setting.StringSettingRow;

import me.mioclient.gui.panel.CategoryPanel;
import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.widget.ModuleButton;
import me.mioclient.mode.client.ModuleListMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.hud.DraggableHudElement;
import me.mioclient.render.hud.ModuleListRenderer;
import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.render.hud.TextRenderState;
import me.mioclient.render.shape.Shape2DRenderer;
import me.mioclient.util.anim.AnimationProgress;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.GridPoint;
import me.mioclient.util.math.Tuple;

import me.mioclient.api.ClientAccessor;
import me.mioclient.module.client.hud.HudEdgeDragHandler;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import me.mioclient.api.Category;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class HudEditorScreen extends AbstractClickGuiScreen implements ClientAccessor {
    public static final List<ModuleListRenderer> list = Collections.synchronizedList(new ArrayList());
    public static final TextRenderState helper_22 = new TextRenderState();
    public final HudEdgeDragHandler zoomHelper_2 = new HudEdgeDragHandler();
    public final AnimationProgress progress = new AnimationProgress(2.0f, true);
    public boolean isDraggingModule;
    public float dragOffsetX;
    public float dragOffsetY;
    public float lastMouseX;
    public float lastMouseY;

    public HudEditorScreen() {
        for (ModuleListMode moduleListMode : ModuleListMode.values()) {
            list.add(new ModuleListRenderer(moduleListMode));
        }
        CategoryPanel categorySearchHelper4 = new CategoryPanel(Category.HUD);
        categorySearchHelper4.getChildren().sort(Comparator.comparing(presetHelper_5 -> {
            return presetHelper_5 instanceof ModuleButton ? ((ModuleButton) presetHelper_5).getModule().getName() : "";
        }));
        categorySearchHelper4.getTotalWidth();
        categorySearchHelper4.setX(85);
        categorySearchHelper4.setY(10);
        this.arrayList.add(categorySearchHelper4);
    }

    /* 26.2 port: Screen.render() is gone — extractRenderState is the draw path. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int i, int i2, float f) {
        helper_22.setActive(false);
        this.lastMouseX = i;
        this.lastMouseY = i2;
        float scaledWindowWidth = drawContext.guiWidth() / 2.0f;
        float scaledWindowHeight = drawContext.guiHeight() / 2.0f;
        Color color817 = ColorUtils.withAlphaPercent(Color.white, getAlpha() * 0.4f);
        drawContext.fill((int) (scaledWindowWidth - 0.5f), 0, (int) (scaledWindowWidth + 0.5f), (int) (scaledWindowHeight * 2.0f), color817.getRGB());
        drawContext.fill(0, (int) (scaledWindowHeight - 0.5f), (int) (scaledWindowWidth - 0.5f), (int) (scaledWindowHeight + 0.5f), color817.getRGB());
        drawContext.fill((int) (scaledWindowWidth + 0.5f), (int) (scaledWindowHeight - 0.5f), (int) (scaledWindowWidth * 2.0f), (int) (scaledWindowHeight + 0.5f), color817.getRGB());
        this.zoomHelper_2.do1010(drawContext, i, i2, f);
        renderAll(drawContext, i, i2);
        if (this.isDraggingModule) {
            DraggableHudElement object1989 = getRenderer(ModuleListMode.NONE).getElements().getLast();
            float[] floatArray2950 = object1989.getMeasuredSize();
            object1989.setRelativeX(Math.max(0.0f, Math.min(drawContext.guiWidth() - floatArray2950[0], i - this.dragOffsetX)), true);
            object1989.setRelativeY(Math.max(0.0f, Math.min(drawContext.guiHeight() - floatArray2950[1], i2 - this.dragOffsetY)), true);
            object1989.computeSides();
            if (Math.abs((object1989.getX() + (floatArray2950[0] / 2.0f)) - scaledWindowWidth) <= 5.0f) {
                float intBitsToFloat = scaledWindowWidth - (floatArray2950[0] / 2.0f);
                object1989.setRelativeX(intBitsToFloat, true);
                drawSnapGuide(drawContext, intBitsToFloat - 0.5d, true);
                drawSnapGuide(drawContext, intBitsToFloat + floatArray2950[0], true);
            }
            if (Math.abs((object1989.getY() + (floatArray2950[1] / 2.0f)) - scaledWindowHeight) <= 5.0f) {
                float intBitsToFloat2 = scaledWindowHeight - (floatArray2950[1] / 2.0f);
                object1989.setRelativeY(intBitsToFloat2, true);
                drawSnapGuide(drawContext, intBitsToFloat2 - 0.5d, false);
                drawSnapGuide(drawContext, intBitsToFloat2 + floatArray2950[1], false);
            }
        }
        float f2 = this.progress.getValue();
        this.progress.animateToggle(helper_22.isActive());
        if (helper_22.isActive() || f2 > 0.1d) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, helper_22.getText(), helper_22.getX() + (10.0f * (1.0f - f2) * helper_22.getSide()), helper_22.getY(), ColorUtils.withAlpha(Color.white, (int) (f2 * 255.0f)));
        }
        super.extractRenderState(drawContext, i, i2, f);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double d = event.x();
        double d2 = event.y();
        int i = event.button();
        Tuple holeSnapData_2 = null;
        for (ModuleListRenderer ModuleListRenderer : list) {
            for (int size = ModuleListRenderer.getElements().size() - 1; size >= 0; size--) {
                DraggableHudElement moduleListSearchHelper4 = ModuleListRenderer.getElements().get(size);
                if (moduleListSearchHelper4.isVisible() && i == 0 && holeSnapData_2 == null && ((!(moduleListSearchHelper4 instanceof ResizableHudElement) || !((ResizableHudElement) moduleListSearchHelper4).isOverResizeHandle(d, d2)) && moduleListSearchHelper4.containsPoint(d, d2))) {
                    holeSnapData_2 = new Tuple(ModuleListRenderer.getAnchor(), moduleListSearchHelper4);
                    this.dragOffsetX = (int) (d - moduleListSearchHelper4.getX());
                    this.dragOffsetY = (int) (d2 - moduleListSearchHelper4.getY());
                    this.isDraggingModule = true;
                    moduleListSearchHelper4.setDragging(true);
                }
            }
        }
        if (holeSnapData_2 != null) {
            getRenderer((ModuleListMode) holeSnapData_2.getLeft()).getElements().remove(holeSnapData_2.getRight());
            getRenderer(ModuleListMode.NONE).getElements().addLast((DraggableHudElement) holeSnapData_2.getRight());
            ((DraggableHudElement) holeSnapData_2.getRight()).setAnchor(ModuleListMode.NONE);
        } else {
            this.zoomHelper_2.mouseClicked(d, d2, i);
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean mouseReleased(MouseButtonEvent event) {
        double d = event.x();
        double d2 = event.y();
        int i = event.button();
        this.zoomHelper_2.mouseReleased(d, d2, i);
        if (this.isDraggingModule) {
            this.isDraggingModule = false;
            ModuleListRenderer ModuleListRenderer200 = getRenderer(ModuleListMode.NONE);
            DraggableHudElement object1989 = ModuleListRenderer200.getElements().getLast();
            object1989.setDragging(false);
            ModuleListRenderer200.getElements().remove(object1989);
            for (ModuleListRenderer ModuleListRenderer : list) {
                Iterator<DraggableHudElement> it = ModuleListRenderer.getElements().iterator();
                while (it.hasNext()) {
                    if (it.next().getData_32936().overlaps(object1989.getData_32936())) {
                        getRenderer(ModuleListRenderer.getAnchor()).getElements().addLast(object1989);
                        object1989.setAnchor(ModuleListRenderer.getAnchor());
                        return true;
                    }
                }
            }
            for (ModuleListMode moduleListMode : ModuleListMode.values()) {
                if (moduleListMode.getData_32936().overlaps(object1989.getData_32936().expand(5.0f))) {
                    getRenderer(moduleListMode).getElements().addLast(object1989);
                    object1989.setAnchor(moduleListMode);
                    return true;
                }
            }
        }
        return super.mouseReleased(event);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean keyPressed(KeyEvent event) {
        int i = event.key();
        int i2 = event.scancode();
        boolean z = false;
        Iterator<ClickGuiPanel> it = this.arrayList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            if (it.next().containsPoint(this.lastMouseX, this.lastMouseY)) {
                z = true;
                break;
            }
        }
        if (!z) {
            DraggableHudElement moduleListSearchHelper4202 = getHoveredElement();
            if (getArrowPositionData(i) == null || moduleListSearchHelper4202 == null || !moduleListSearchHelper4202.isVisible()) {
                switch (i) {
                    case 73:
                        if (!(ClientServices.getClickGui().getActiveSettingButton() instanceof StringSettingRow)) {
                            swapLists(ModuleListMode.BOTTOM_LEFT, ModuleListMode.BOTTOM_RIGHT);
                            swapLists(ModuleListMode.TOP_LEFT, ModuleListMode.TOP_RIGHT);
                            break;
                        }
                        break;
                    case 259:
                    case 261:
                        if (moduleListSearchHelper4202 != null && moduleListSearchHelper4202.isVisible()) {
                            moduleListSearchHelper4202.getHudModule().setToggled(false);
                            break;
                        }
                        break;
                }
            } else {
                GridPoint positionData = getArrowPositionData(i);
                moduleListSearchHelper4202.setRelativeX(moduleListSearchHelper4202.getX() + positionData.get476(), true);
                moduleListSearchHelper4202.setRelativeY(moduleListSearchHelper4202.getY() + positionData.getGridY(), true);
                moduleListSearchHelper4202.computeSides();
            }
        }
        return super.keyPressed(event);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean mouseScrolled(double d, double d2, double d3, double d4) {
        return super.mouseScrolled(d, d2, d3, d4);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void close() {
        super.close();
        if (this.isDraggingModule) {
            getRenderer(ModuleListMode.NONE).getElements().getLast().setDragging(false);
            this.isDraggingModule = false;
        }
    }

    public void addModuleToList(ModuleListMode moduleListMode, DraggableHudElement moduleListSearchHelper4) {
        getRenderer(moduleListMode).getElements().add(moduleListSearchHelper4);
    }

    public void addModuleDefault(DraggableHudElement moduleListSearchHelper4) {
        addModuleToList(ModuleListMode.NONE, moduleListSearchHelper4);
    }

    public void removeModuleFromAll(DraggableHudElement moduleListSearchHelper4) {
        Iterator<ModuleListRenderer> it = getAllRenderers().iterator();
        while (it.hasNext() && !it.next().getElements().remove(moduleListSearchHelper4)) {
        }
    }

    public List<ModuleListRenderer> getAllRenderers() {
        return list;
    }

    public void renderAll(GuiGraphicsExtractor drawContext, double d, double d2) {
        Iterator<ModuleListRenderer> it = getAllRenderers().iterator();
        while (it.hasNext()) {
            it.next().render(drawContext, new PoseStack(), d, d2);
        }
        Shape2DRenderer.resetShadowOffset();
    }

    public ModuleListRenderer getRenderer(ModuleListMode moduleListMode) {
        for (ModuleListRenderer ModuleListRenderer : getAllRenderers()) {
            if (ModuleListRenderer.getAnchor() == moduleListMode) {
                return ModuleListRenderer;
            }
        }
        return null;
    }

    public void swapLists(ModuleListMode moduleListMode, ModuleListMode moduleListMode2) {
        ModuleListRenderer ModuleListRenderer200 = getRenderer(moduleListMode);
        ModuleListRenderer ModuleListRenderer2002 = getRenderer(moduleListMode2);
        ArrayList arrayList = new ArrayList(ModuleListRenderer200.getElements());
        ModuleListRenderer200.getElements().clear();
        Iterator<DraggableHudElement> it = ModuleListRenderer2002.getElements().iterator();
        while (it.hasNext()) {
            ModuleListRenderer200.getElements().add(it.next());
        }
        ModuleListRenderer2002.getElements().clear();
        Iterator it2 = arrayList.iterator();
        while (it2.hasNext()) {
            ModuleListRenderer2002.getElements().add((DraggableHudElement) it2.next());
        }
    }

    public DraggableHudElement getHoveredElement() {
        Iterator<ModuleListRenderer> it = list.iterator();
        while (it.hasNext()) {
            Iterator<DraggableHudElement> it2 = it.next().getElements().iterator();
            while (it2.hasNext()) {
                DraggableHudElement next = it2.next();
                if (next.containsPoint(this.lastMouseX, this.lastMouseY)) {
                    return next;
                }
            }
        }
        return null;
    }

    public void drawSnapGuide(GuiGraphicsExtractor drawContext, double d, boolean z) {
        int f = (int) d;
        int w = (int) 0.5f;
        if (z) {
            drawContext.fill(f, 0, f + w, drawContext.guiHeight(), Color.yellow.getRGB());
        } else {
            drawContext.fill(0, f, drawContext.guiWidth(), f + w, Color.yellow.getRGB());
        }
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void renderOverlay(GuiGraphicsExtractor drawContext) {
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean shouldRenderBackground() {
        return false;
    }

    public GridPoint getArrowPositionData(int i) {
        switch (i) {
            case 262:
                return new GridPoint(1, 0);
            case 263:
                return new GridPoint(-1, 0);
            case 264:
                return new GridPoint(0, 1);
            case 265:
                return new GridPoint(0, -1);
            default:
                return null;
        }
    }
}
