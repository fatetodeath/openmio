package me.mioclient.module.player.freecam;

import me.mioclient.api.ClientServices;

import me.mioclient.api.ClientAccessor;

import me.mioclient.mixin.ducks.DuckKeyBinding;
import me.mioclient.module.movement.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Input;
import org.lwjgl.glfw.GLFW;

/* 26.2 port: Yarn Input(tick(boolean,float), movementForward/Sideways) -> MojMap KeyboardInput(tick(), keyPresses record). */
public class FreecamInput extends KeyboardInput implements ClientAccessor {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving noslow null. */
    private static NoSlow noslow;

    public static NoSlow getNoslow() {
        if (noslow == null && ClientServices.moduleInjector != null) {
            try {
                noslow = (NoSlow) ClientServices.moduleInjector.getModule(NoSlow.class);
            } catch (Exception e) {
            }
        }
        return noslow;
    }

    public FreecamInput(Options options) {
        super(options);
    }

    @Override
    public void tick() {
        boolean fwd = isKeyBindingActive(minecraftClient.options.keyUp);
        boolean back = isKeyBindingActive(minecraftClient.options.keyDown);
        boolean left = isKeyBindingActive(minecraftClient.options.keyLeft);
        boolean right = isKeyBindingActive(minecraftClient.options.keyRight);
        boolean jump = isKeyBindingActive(minecraftClient.options.keyJump);
        boolean shift = isKeyBindingActive(minecraftClient.options.keyShift);
        boolean sprint = isKeyBindingActive(minecraftClient.options.keySprint);
        this.keyPresses = new Input(fwd, back, left, right, jump, shift, sprint);
    }

    public void tick(boolean z, float f) {
        tick();
    }

    public float getKeyMovementValue(boolean z, boolean z2) {
        if (z == z2) {
            return 0.0f;
        }
        return z ? 1.0f : (-1.0f);
    }

    public boolean isKeyBindingActive(KeyMapping keyBinding) {
        NoSlow noslow_lcl = getNoslow();
        return (noslow_lcl != null && noslow_lcl.isToggled() && noslow_lcl.isOffhandSwapAllowed() && noslow_lcl.guiMove.getValue().booleanValue()) ? GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), ((DuckKeyBinding) keyBinding).getKey().getValue()) == 1 : keyBinding.isDown();
    }
}