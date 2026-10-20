package me.mioclient.gui.screen;

import me.mioclient.api.ClientServices;
import me.mioclient.module.HudModule;

import me.mioclient.gui.GuiComponent;

import me.mioclient.gui.minigame.MinigamePanel;
import me.mioclient.gui.panel.CategoryPanel;
import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.gui.widget.ModuleButton;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;
import me.mioclient.manager.ConfigManager;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import me.mioclient.api.Category;
import me.mioclient.module.Module;
import me.mioclient.module.client.HUD;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.lwjgl.glfw.GLFW;
import me.mioclient.module.render.Search;
import me.mioclient.setting.SettingPresetSettingButton;

public class ClickGuiScreen extends AbstractClickGuiScreen implements ClientAccessor {
    public static final int[] intArr = {265, 265, 264, 264, 263, 262, 263, 262, 66, 65};
    public boolean flag2;
    public EditBox textFieldWidget;
    public String string;
    public SettingPresetSettingButton<?> settingPresetSettingButton = null;
    public int current = 0;
    public final Stopwatch stopwatch = new Stopwatch();
    public List<ModuleButton> list = new ArrayList();
    public boolean flag = ConfigManager.path9.toFile().exists();

    public ClickGuiScreen() {
        int i = 10;
        for (Category category : Category.values()) {
            if (category != Category.HUD) {
                CategoryPanel categorySearchHelper4 = new CategoryPanel(category);
                categorySearchHelper4.setX(i);
                this.arrayList.add(categorySearchHelper4);
                i += categorySearchHelper4.getTotalWidth() + 3;
            }
        }
        if (this.flag) {
            MinigamePanel game = new MinigamePanel();
            game.setX(i);
            game.setExpanded(false);
            this.arrayList.add(game);
        }
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void init() {
        super.init();
        this.string = "";
        /* No visible box: the field lives off-screen and stays invisible; the query
           itself is drawn floating and centered in renderCustomContent. */
        this.textFieldWidget = new EditBox(minecraftClient.font, -1000, -1000, 120, 12, Component.literal("Search"));
        this.textFieldWidget.setVisible(false);
        this.textFieldWidget.setEditable(true);
        this.textFieldWidget.setBordered(false);
        this.textFieldWidget.setMaxLength(64);
        this.textFieldWidget.setResponder(str -> {
            if (!str.isEmpty()) {
                for (Module module : ClientServices.moduleRegistry.getRegistry()) {
                    if (!(module instanceof HUD) && !(module instanceof HudModule) && module.getName().toLowerCase(Locale.ROOT).startsWith(str.toLowerCase(Locale.ROOT))) {
                        /* Remainder of the first match past what was typed. */
                        int prefix = Math.min(str.length(), module.getName().length());
                        this.string = module.getName().substring(prefix);
                        this.textFieldWidget.setSuggestion(this.string);
                        return;
                    }
                }
            }
            this.string = "";
            this.textFieldWidget.setSuggestion("");
        });
        addRenderableWidget(this.textFieldWidget);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void renderCustomContent(GuiGraphicsExtractor drawContext, int i, int i2, float f) {
        if (this.list.size() >= 3) {
            ((ModuleButton) this.list.getFirst()).flag = false;
        }
        this.list.removeIf((v0) -> {
            return v0.isClosed();
        });
        int sw = minecraftClient.getWindow().getGuiScaledWidth();
        int sh = minecraftClient.getWindow().getGuiScaledHeight();
        String hint = "Ctrl + F to activate search.";
        if (!this.textFieldWidget.isFocused()) {
            if (this.textFieldWidget.getValue().isEmpty()) {
                if (ClientServices.welcomerHelper.getRank() != 8) {
                    ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, hint, sw - ClientTextRenderer.INSTANCE.getStringWidth(hint) - 4, sh - ClientTextRenderer.INSTANCE.getFontHeight() - 2, ColorUtils.getGradientColor(Color.GRAY, Color.WHITE, 2000.0d, 250.0d));
                }
                return;
            }
        }
        /* Floating search readout (the EditBox is hidden off-screen): the typed value
           in white, the autocomplete remainder in gray and a blinking cursor,
           measured and centered as a single line. */
        String value = this.textFieldWidget.getValue();
        String suggestion = this.string == null ? "" : this.string;
        String cursor = getBlinkingCursor();
        float valueW = ClientTextRenderer.INSTANCE.getStringWidth(value);
        float suggW = suggestion.isEmpty() ? 0.0f : ClientTextRenderer.INSTANCE.getStringWidth(suggestion);
        float cursorW = cursor.isEmpty() ? 0.0f : ClientTextRenderer.INSTANCE.getStringWidth(cursor);
        float totalW = valueW + suggW + cursorW;
        float cx = sw / 2.0f - totalW / 2.0f;
        float cy = sh - ClientTextRenderer.INSTANCE.getFontHeight() - 14;
        drawContext.fill((int) (cx - 6), (int) (cy - 4), (int) (cx + totalW + 6), (int) (cy + ClientTextRenderer.INSTANCE.getFontHeight() + 4), new Color(10, 10, 10, 140).getRGB());
        ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, value, cx, cy, Color.WHITE);
        if (!suggestion.isEmpty()) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, suggestion, cx + valueW, cy, Color.GRAY);
        }
        if (!cursor.isEmpty()) {
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, cursor, cx + valueW + suggW, cy, Color.WHITE);
        }
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int i = event.key();
        super.keyPressed(event);
        if (this.flag || intArr[this.current] != i) {
            this.current = 0;
        } else {
            this.current++;
            if (this.current == intArr.length) {
                try {
                    ConfigManager.path9.toFile().createNewFile();
                } catch (Exception e) {
                }
                this.flag = true;
                this.current = 0;
                MinigamePanel game = new MinigamePanel();
                game.setExpanded(true);
                game.setX((minecraftClient.getWindow().getGuiScaledWidth() / 2) - (game.getTotalWidth() / 2));
                game.setY((minecraftClient.getWindow().getGuiScaledHeight() / 2) - (game.getFontHeight() / 2));
                this.arrayList.add(game);
            }
        }
        switch (i) {
            case 70:
                if (GLFW.glfwGetKey(minecraftClient.getWindow().handle(), 341) != 1) {
                    return true;
                }
                if (this.textFieldWidget.isFocused()) {
                    reset();
                    return true;
                }
                setInitialFocus(this.textFieldWidget);
                setFocused(this.textFieldWidget);
                this.textFieldWidget.setFocused(true);
                return true;
            case 257:
                Iterator<ClickGuiPanel> it = this.arrayList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        Iterator<GuiComponent> it2 = it.next().getChildren().iterator();
                        while (it2.hasNext()) {
                            GuiComponent next = it2.next();
                            if (next instanceof ModuleButton) {
                                ModuleButton arrayListPresetHelper2 = (ModuleButton) next;
                                if (arrayListPresetHelper2.getModule().getName().equalsIgnoreCase(this.textFieldWidget.getValue() + this.string)) {
                                    arrayListPresetHelper2.flag = true;
                                    arrayListPresetHelper2.flashHighlight();
                                }
                            }
                        }
                    } else {
                        break;
                    }
                }
                reset();
                return true;
            case 258:
                if (!this.textFieldWidget.isFocused()) {
                    return true;
                }
                this.textFieldWidget.setValue(this.textFieldWidget.getValue() + this.string);
                return true;
            default:
                return true;
        }
    }

    public void filesDragged(List<Path> list) {
        me.mioclient.gui.screen.CategorySelectScreen.tryOpenForDroppedFiles(this, list);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void reset() {
        super.reset();
        setFocused(null);
        this.textFieldWidget.setFocused(false);
        this.textFieldWidget.setValue("");
        this.textFieldWidget.setSuggestion("");
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void requestClose() {
        super.requestClose();
        executorService.submit(() -> {
            ClientServices.presetHelper.do41();
        });
    }

    public EditBox getSearchField() {
        return this.textFieldWidget;
    }

    public String getBlinkingCursor() {
        if (this.stopwatch.hasElapsedMs(500L)) {
            this.flag2 = !this.flag2;
            this.stopwatch.reset();
        }
        return this.flag2 ? "_" : "";
    }

    public SettingPresetSettingButton<?> getActiveSettingButton() {
        return this.settingPresetSettingButton;
    }

    public void setActiveSetting(SettingPresetSettingButton<?> settingPresetSettingButton) {
        this.settingPresetSettingButton = settingPresetSettingButton;
    }
}
