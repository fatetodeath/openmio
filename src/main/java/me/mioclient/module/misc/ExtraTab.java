package me.mioclient.module.misc;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import net.minecraft.network.chat.Component;

import me.mioclient.mode.render.NameTagsHelperMode;

import java.awt.Color;
import java.util.Comparator;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.module.Module;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.level.GameType;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class ExtraTab extends Module {
    public Setting<MixinPlayerListHudMode> latency = add(new EnumSetting<>("Latency", MixinPlayerListHudMode.VANILLA));
    public Setting<Float> scale = add(new NumberSetting<>("Scale", 1.0f, 0.5f, 1.5f));
    public Setting<Boolean> betterSkin = add(new BooleanSetting("BetterSkin", true));
    public Setting<Boolean> highlight = add(new BooleanSetting("Highlight", true).setVisible());
    public Setting<Color> self = add(new ColorSetting("Self", new Color(-65281, true), v -> this.highlight.isShown()).setThemeSync(true));
    public Setting<Boolean> social = add(new BooleanSetting("Social", true, v -> this.highlight.isShown()));
    public Setting<Boolean> sort = add(new BooleanSetting("Sort", false).setVisible());
    public Setting<ExtraTabMode> mode = add(new EnumSetting<>("Mode", ExtraTabMode.ALPHABET).requiresSettings(this.sort));
    public Setting<Boolean> reverse = add(new BooleanSetting("Reverse", false).requiresSettings(this.sort));

    
    public enum ExtraTabMode implements NamedValue {
        ALPHABET("Alphabet"),
        SOCIALS("Socials"),
        LATENCY("Latency"),
        LENGTH("Length");

        public final String name;

        ExtraTabMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    
    public enum MixinPlayerListHudMode implements NamedValue {
        VANILLA("Vanilla"),
        TEXT("Component"),
        NONE("None");

        public final String name;

        MixinPlayerListHudMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public ExtraTab() {
        super("ExtraTab", "Allows you to customize your TAB-screen.", Category.MISC, new String[0]);
        setDrawn(false);
    }

    public float getTabScale() {
        return !isToggled() ? 1.0f : this.scale.getValue().floatValue();
    }

    public int getSortWeight(PlayerInfo playerListEntry) {
        switch (this.mode.getValue()) {
            case ALPHABET:
                return 0;
            case SOCIALS:
                NameTagsHelperMode nameTagsHelperMode525 = ClientServices.socialManager.getRole(playerListEntry.getProfile().name());
                int i = 0;
                if (nameTagsHelperMode525 == NameTagsHelperMode.FRIEND) {
                    i = -1;
                }
                if (nameTagsHelperMode525 == NameTagsHelperMode.ENEMY) {
                    i = -2;
                }
                return i;
            case LATENCY:
                return -playerListEntry.getLatency();
            case LENGTH:
                // 26.2: Font#width(String) measures the rendered name width.
                return -minecraftClient.font.width(playerListEntry.getProfile().name());
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }

    public Comparator<PlayerInfo> getComparator() {
        Comparator<PlayerInfo> thenComparing = Comparator.comparingInt((PlayerInfo playerListEntry) -> {
            int i = playerListEntry.getGameMode() == GameType.SPECTATOR ? 1 : 0;
            if (this.reverse.getValue().booleanValue()) {
                i *= -1;
            }
            return i;
        }).thenComparing(playerListEntry2 -> {
            var team = playerListEntry2.getTeam();
            return team == null ? "" : team.getName();
        }).thenComparing(playerListEntry3 -> {
            return Integer.valueOf(getSortWeight(playerListEntry3));
        }).thenComparing(playerListEntry4 -> {
            return playerListEntry4.getProfile().name();
        }, (v0, v1) -> {
            return v0.compareToIgnoreCase(v1);
        });
        if (this.reverse.getValue().booleanValue()) {
            thenComparing = thenComparing.reversed();
        }
        return thenComparing;
    }
}