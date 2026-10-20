package me.mioclient.gui.screen;

import me.mioclient.api.ClientServices;
import me.mioclient.gui.widget.PanelComponent;
import me.mioclient.util.client.SoundIdentifier;

import me.mioclient.gui.panel.ClickGuiPanel;
import me.mioclient.render.font.ClientTextRenderer;

import me.mioclient.manager.ConfigManager;
import me.mioclient.render.RenderUtil;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import me.mioclient.api.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import me.mioclient.setting.PresetPresetSettingButton;

public class SoundSelectScreen extends AbstractClickGuiScreen {
    public Setting<SoundIdentifier> setting;
    public final Screen screen;

    
    
    public class Inner extends PresetPresetSettingButton {
        public final SoundIdentifier searchIdentifier;

        public Inner(ClickGuiPanel presetEnumSettingHelper, SoundIdentifier searchIdentifier) {
            super(presetEnumSettingHelper, searchIdentifier.getName());
            this.searchIdentifier = searchIdentifier;
        }

        @Override // me.mioclient.setting.PresetPresetSettingButton, me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void render(GuiGraphicsExtractor drawContext, PoseStack matrixStack, double d, double d2) {
            if (SoundSelectScreen.this.setting.getValue().equals(this.searchIdentifier)) {
                RenderUtil.INSTANCE.drawFilledRectColor(matrixStack, this.presetEnumSettingHelper.getX() + 1, this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 0.5f, getUI().color.getValue());
            } else {
                RenderUtil.INSTANCE.drawFilledRectColor(matrixStack, this.presetEnumSettingHelper.getX() + 1, this.presetEnumSettingHelper.getY() + this.num + 0.5f, (this.presetEnumSettingHelper.getX() + this.presetEnumSettingHelper.getTotalWidth()) - 1, ((this.presetEnumSettingHelper.getY() + this.num) + getFontHeight()) - 0.5f, getUI().bgButton.getValue());
            }
            super.render(drawContext, matrixStack, d, d2);
        }

        @Override // me.mioclient.gui.widget.PanelComponent, me.mioclient.gui.GuiComponent
        public void mouseClicked(double d, double d2, int i) {
            if (containsPoint(d, d2)) {
                if (i == 0) {
                    SoundSelectScreen.this.setting.setValue(this.searchIdentifier);
                    SoundSelectScreen.this.requestClose();
                } else if (i == 1) {
                    ClientServices.killstreakSoundManager.playSound(this.searchIdentifier, 1.0f);
                }
            }
        }
    }

    public SoundSelectScreen(Setting<SoundIdentifier> setting, Screen screen) {
        this.setting = setting;
        this.screen = screen;
        rebuildSoundPanels();
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public void requestClose() {
        minecraftClient.gui.setScreen(this.screen);
    }

    @Override // me.mioclient.gui.screen.AbstractClickGuiScreen
    public Screen getParentScreen() {
        Screen screen = this.screen;
        return screen instanceof AbstractClickGuiScreen ? ((AbstractClickGuiScreen) screen).getParentScreen() : this.screen;
    }

    public void filesDragged(List<Path> list) {
        for (Path path : list) {
            if (path.toFile().getName().endsWith(".ogg")) {
                Path resolve = ConfigManager.path8.resolve("custom");
                resolve.toFile().mkdirs();
                try {
                    Files.copy(path, resolve.resolve(path.getFileName()), new CopyOption[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        ClientServices.killstreakSoundManager.reloadWorldRenderer();
        rebuildSoundPanels();
    }

    public void rebuildSoundPanels() {
        getGuiPanels().clear();
        synchronized (ClientServices.killstreakSoundManager.getSoundIdentifiers()) {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            ClientServices.killstreakSoundManager.getSoundIdentifiers().stream().map(searchIdentifier -> {
                return searchIdentifier.getSoundSource();
            }).filter(str -> {
                return !str.isEmpty();
            }).distinct().forEach(str2 -> {
                ClickGuiPanel presetEnumSettingHelper = new ClickGuiPanel(ClientTextRenderer.capitalize(str2));
                getGuiPanels().add(presetEnumSettingHelper);
                presetEnumSettingHelper.setX(presetEnumSettingHelper.getX() + ((presetEnumSettingHelper.getTotalWidth() + 3) * atomicInteger.getAndIncrement()));
            });
            for (SoundIdentifier searchIdentifier2 : ClientServices.killstreakSoundManager.getSoundIdentifiers()) {
                Iterator<ClickGuiPanel> it = getGuiPanels().iterator();
                while (true) {
                    if (it.hasNext()) {
                        ClickGuiPanel next = it.next();
                        if (next.getName().equalsIgnoreCase(searchIdentifier2.getSoundSource())) {
                            // JADX emitted an orphaned lambda here (never assigned/invoked); removed.
                            next.getChildren().add(new Inner(next, searchIdentifier2));
                            break;
                        }
                    }
                }
            }
        }
    }
}
