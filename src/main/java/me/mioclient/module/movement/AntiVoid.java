package me.mioclient.module.movement;

import me.mioclient.api.NamedValue;

import me.mioclient.render.font.ClientTextRenderer;

import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AntiVoid extends Module {
    public Setting<AntiVoidMode> mode = add(new EnumSetting<>("Mode", AntiVoidMode.CANCEL));
    public Setting<Integer> height = add(new NumberSetting<>("Height", 30, 1, 100).setSuffix("m"));
    public Setting<Boolean> forceGround = add(new BooleanSetting("ForceGround", false, v -> this.mode.getValue() == AntiVoid.AntiVoidMode.CANCEL));

    
    public enum AntiVoidMode implements NamedValue {
        TELEPORT("Teleport"),
        CANCEL("Cancel");

        public final String name;

        AntiVoidMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public AntiVoid() {
        super("AntiVoid", "Prevents you from falling into the void.", Category.MOVEMENT, new String[0]);
        setDrawn(false);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return ClientTextRenderer.capitalize(this.mode.getValue());
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.player.onGround() || minecraftClient.player.fallDistance <= 0.0f) {
            return;
        }
        if (minecraftClient.level.clip(new ClipContext(minecraftClient.player.position(), new Vec3(minecraftClient.player.getX(), minecraftClient.level.getMinY() - 1, minecraftClient.player.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraftClient.player)).getType() == HitResult.Type.MISS && minecraftClient.level.getMinY() + this.height.getValue().intValue() >= minecraftClient.player.getY()) {
            if (this.mode.getValue() == AntiVoidMode.TELEPORT) {
                minecraftClient.player.setDeltaMovement(0.0d, 10.0d, 0.0d);
            } else {
                minecraftClient.player.setDeltaMovement(minecraftClient.player.getDeltaMovement().with(Direction.Axis.Y, 0.0d));
                if (this.forceGround.getValue().booleanValue()) {
                    minecraftClient.player.setOnGround(true);
                }
            }
        }
    }
}