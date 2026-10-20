package me.mioclient.module.render;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.module.Module;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class Animations extends Module {
    public Setting<Boolean> crystals = add(new BooleanSetting("Crystals", false).setVisible());
    public Setting<Float> floatFactor = add(new NumberSetting<>("FloatFactor", 1.0f, 0.0f, 1.0f).requiresSettings(this.crystals));
    public Setting<Float> rotationSpeed = add(new NumberSetting<>("RotationSpeed", 1.0f, 0.0f, 6.0f).requiresSettings(this.crystals));
    public Setting<Float> crystalScale = add(new NumberSetting<>("CrystalScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.crystals));
    public Setting<Boolean> parts = add(new BooleanSetting("Parts", false).setDependent().requiresSettings(this.crystals));
    public Setting<Boolean> inner = add(new BooleanSetting("Inner", true).requiresSettings(this.crystals, this.parts));
    public Setting<Boolean> outer = add(new BooleanSetting("Outer", true).requiresSettings(this.crystals, this.parts));
    public Setting<Boolean> core = add(new BooleanSetting("Core", true).requiresSettings(this.crystals, this.parts));
    public Setting<Boolean> bottom = add(new BooleanSetting("Bottom", true).requiresSettings(this.crystals, this.parts));
    public Setting<Boolean> players = add(new BooleanSetting("Players", false).setVisible());
    public Setting<Boolean> static_ = add(new BooleanSetting("Static", false).requiresSettings(this.players));
    public Setting<Boolean> sneak = add(new BooleanSetting("Sneak", false).requiresSettings(this.players));
    public Setting<Float> playerScale = add(new NumberSetting<>("PlayerScale", 1.0f, 0.0f, 2.0f).requiresSettings(this.players));

    public Animations() {
        super("Animations", "Modifies entity animations.", Category.RENDER, new String[0]);
    }

    public boolean isPlayerScaleModified() {
        return isToggled() && this.players.getValue().booleanValue() && this.playerScale.getValue().floatValue() != 1.0f;
    }

    public boolean isStaticAnimationEnabled() {
        return isToggled() && this.players.getValue().booleanValue() && this.static_.getValue().booleanValue();
    }

    public boolean isSneakAnimationEnabled() {
        return isToggled() && this.players.getValue().booleanValue() && this.sneak.getValue().booleanValue();
    }

    public boolean isCrystalsEnabled() {
        return isToggled() && this.crystals.getValue().booleanValue();
    }

    public boolean isCrystalPartHidden(int i) {
        switch (i) {
            case 0:
                return isBottomPartHidden();
            case 1:
                return isOuterPartHidden();
            case 2:
                return isInnerPartHidden();
            case 3:
                return isCorePartHidden();
            default:
                return false;
        }
    }

    public boolean isInnerPartHidden() {
        return isCrystalsEnabled() && !this.inner.getValue().booleanValue();
    }

    public boolean isOuterPartHidden() {
        return isCrystalsEnabled() && !this.outer.getValue().booleanValue();
    }

    public boolean isCorePartHidden() {
        return isCrystalsEnabled() && !this.core.getValue().booleanValue();
    }

    public boolean isBottomPartHidden() {
        return isCrystalsEnabled() && !this.bottom.getValue().booleanValue();
    }
}