package me.mioclient.util.client;

import me.mioclient.api.ClientAccessor;

import net.fabricmc.loader.api.FabricLoader;

public class GuiScaleOverride implements ClientAccessor {
    public static double val;
    public static boolean flag = FabricLoader.getInstance().isModLoaded("advanced-ui-scale");

    public static void injectModuleFields() {
        applyScale(1.0f);
    }

    public static void applyScale(float f) {
        if (isScaleModLoaded()) {
            return;
        }
        val = minecraftClient.getWindow().getGuiScale();
        minecraftClient.getWindow().setGuiScale(Math.max(1, (int) (getBaseScaleFactor() * f)));
    }

    public static void do604() {
        if (isScaleModLoaded()) {
            return;
        }
        if (val == (-1.0d)) {
            throw new UnsupportedOperationException();
        }
        minecraftClient.getWindow().setGuiScale((int) val);
        val = (-1.0d);
    }

    public static float getBaseScaleFactor() {
        return minecraftClient.getWindow().calculateScale((Integer) minecraftClient.options.guiScale().get(), (Boolean) minecraftClient.options.forceUnicodeFont().get());
    }

    public static double getScaleMultiplier(float f) {
        return isScaleModLoaded() ? 1.0d : (getBaseScaleFactor() / minecraftClient.getWindow().getGuiScale()) * f;
    }

    public static boolean isScaleModLoaded() {
        return flag;
    }
}
