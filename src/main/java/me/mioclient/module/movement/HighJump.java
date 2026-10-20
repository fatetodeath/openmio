package me.mioclient.module.movement;

import me.mioclient.util.player.MovementUtils;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.world.entity.ai.attributes.Attributes;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class HighJump extends Module {
    public static final float val = 0.42f;
    public Setting<Float> strength = add(new NumberSetting<>("Strength", 1.0f, 0.0f, 5.0f));
    public Setting<Boolean> inMovement = add(new BooleanSetting("InMovement", true));

    public HighJump() {
        super("HighJump", "Makes you jump higher.", Category.MOVEMENT, new String[0]);
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        if (isNotInGame()) {
            return;
        }
        reset();
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (!this.inMovement.getValue().booleanValue() && MovementUtils.is2181()) {
            reset();
        } else {
            setJumpStrength(this.strength.getValue().floatValue());
        }
    }

    public void reset() {
        setJumpStrength(0.42f);
    }

    public void setJumpStrength(float f) {
        minecraftClient.player.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(f);
    }
}