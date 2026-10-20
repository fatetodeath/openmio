package me.mioclient.module;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.client.ModuleListMode;
import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.mode.world.StashFinderMode;
import me.mioclient.render.hud.HudTextLine;
import me.mioclient.render.hud.TextHudElement;
import me.mioclient.util.math.GridPoint;
import me.mioclient.util.math.MathUtils;

import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.api.Setting;
import me.mioclient.module.player.Freecam;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class Position extends me.mioclient.module.HudModule {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving freecam null (NPE). */
    private static Freecam freecam;

    public static Freecam getFreecam() {
        if (freecam == null && ClientServices.moduleInjector != null) {
            try {
                freecam = (Freecam) ClientServices.moduleInjector.getModule(Freecam.class);
            } catch (Exception e) {
            }
        }
        return freecam;
    }
    public Setting<Float> setting;
    public Setting<Boolean> setting2;
    public Setting<Boolean> setting3;
    public GridPoint positionData;

    public Position() {
        super("Position", "coordinates");
        this.setting = add(new NumberSetting("SafeRange", Float.valueOf(0.0f), Float.valueOf(0.0f), Float.valueOf(100.0f)).setSuffix("k"));
        this.setting2 = add(new BooleanSetting("Nether", true));
        this.setting3 = add(new BooleanSetting("Fake", false));
        this.setting.setHoleSnapConfig("None", HoleSnapMode.MIN);
        this.setting3.setChangeCallback(() -> {
            ClientServices.tooltipsHelper.schedule(() -> {
                boolean isChanceHit = MathUtils.isChanceHit(50);
                boolean is3802 = MathUtils.isChanceHit(50);
                int longBitsToDouble = ((int) (((-minecraftClient.player.getX()) * 2.0d) + (Math.random() * minecraftClient.player.getX() * 4.0d))) + 13;
                int longBitsToDouble2 = ((int) (((-minecraftClient.player.getZ()) * 2.0d) + ((Math.random() * minecraftClient.player.getZ()) * 4.0d))) - 37;
                if (isChanceHit) {
                    longBitsToDouble = -longBitsToDouble;
                }
                if (is3802) {
                    longBitsToDouble2 = -longBitsToDouble2;
                }
                this.positionData = new GridPoint(longBitsToDouble, longBitsToDouble2);
            }, 0);
        });
        setElement(new TextHudElement(this, new HudTextLine(() -> {
            if (!isNotInGame()) {
                if (this.setting.getValue().floatValue() > 0.0f) {
                    if (minecraftClient.player.position().distanceTo(Vec3.ZERO) > this.setting.getValue().floatValue() * 1000.0f) {
                    }
                }
                boolean contains = minecraftClient.level.dimension().identifier().getPath().contains("nether");
                Vec3 pos = minecraftClient.player.position();
                Freecam freecam_lcl = getFreecam();
                if (freecam_lcl != null && freecam_lcl.isToggled()) {
                    pos = minecraftClient.gameRenderer.mainCamera().position().subtract(0.0d, minecraftClient.player.getBbHeight(), 0.0d);
                }
                if (this.setting3.getValue().booleanValue() && this.positionData != null) {
                    if (this.positionData.get476() == 0 && this.positionData.getGridY() == 0) {
                        this.setting3.fireChangeCallback();
                    }
                    pos = pos.add(this.positionData.get476(), 0.0d, this.positionData.getGridY());
                }
                BlockPos ofFloored = BlockPos.containing(pos.x, pos.y, pos.z);
                BlockPos ofFloored2 = contains ? BlockPos.containing(pos.multiply(8.0d, 1.0d, 8.0d)) : BlockPos.containing(pos.multiply(0.125d, 1.0d, 0.125d));
                String string2921 = new StringTemplate().arg(String.valueOf(ChatFormatting.WHITE)).format("XYZ: \u0001");
                String string29212 = new StringTemplate().arg(ofFloored.getZ()).arg(ofFloored.getY()).arg(ofFloored.getX()).format("\u0001, \u0001, \u0001.");
                String string29213 = new StringTemplate().arg(ofFloored2.getZ()).arg(ofFloored2.getY()).arg(ofFloored2.getX()).format(" (\u0001, \u0001, \u0001.)");
                if (this.setting2.getValue().booleanValue() && WorldUtils.getDimension() != StashFinderMode.THE_END) {
                    string29212 = new StringTemplate().arg(string29213).arg(string29212).format("\u0001\u0001");
                }
                return Component.literal(new StringTemplate().arg(string29212).arg(string2921).format("\u0001\u0001"));
            }
            return Component.literal("XYZ: %sREDACTED".formatted(ChatFormatting.WHITE));
        }, () -> {
            return true;
        })));
        getElement().setAnchor(ModuleListMode.BOTTOM_LEFT);
    }
}