package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;

import java.util.ArrayList;
import java.util.List;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.mixin.ducks.DuckMinecraftClient;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.exploit.MultiTask;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AutoClicker extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static MultiTask multitask;

    public static MultiTask getMultitask() {
        if (multitask == null && ClientServices.moduleInjector != null) {
            try {
                multitask = (MultiTask) ClientServices.moduleInjector.getModule(MultiTask.class);
            } catch (Exception e) {
            }
        }
        return multitask;
    }
    public Setting<AutoClickerMode> mode = add(new EnumSetting<>("Mode", AutoClickerMode.LEFT));
    public Setting<Integer> cps = add(new NumberSetting<>("CPS", 8, 1, 20));
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 1.0f, 1.0f, 3.0f).setSuffix("x").setHoleSnapConfig("Off", HoleSnapMode.MIN));
    public Setting<Boolean> friendProtect = add(new BooleanSetting("FriendProtect", true));
    public final Stopwatch stopwatch;
    public float val;
    public final List<Long> list;

    
    public enum AutoClickerMode implements NamedValue {
        LEFT("Left"),
        RIGHT("Right");

        public final String name;

        AutoClickerMode(String str) {
            this.name = str;
        }

        public void executeClickAction() {
            if (this == LEFT) {
                ((DuckMinecraftClient)(Object) ClientAccessor.minecraftClient).attack();
            } else {
                ((DuckMinecraftClient)(Object) ClientAccessor.minecraftClient).interact();
            }
        }

        public KeyMapping getKeyBinding2026() {
            return this == LEFT ? ClientAccessor.minecraftClient.options.keyAttack : ClientAccessor.minecraftClient.options.keyUse;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public AutoClicker() {
        super("AutoClicker", "Spams attack as you hold down the attack button.", Category.COMBAT, new String[0]);
        this.stopwatch = new Stopwatch();
        this.val = 1.0f;
        this.list = new ArrayList();
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return String.valueOf(this.list.size());
    }

    @Listen
    public void onRenderWorldPre(MatrixStackEvent.Inner_3 inner) {
        this.list.removeIf(l -> {
            return System.currentTimeMillis() - l.longValue() >= 1000;
        });
        long intValue = 1000 / this.cps.getValue().intValue();
        if (!this.delay.isAtMinimum()) {
            intValue = (long) (((float) intValue) * this.val);
        }
        if (this.stopwatch.hasElapsedMs(intValue)) {
            performClick();
            this.list.add(Long.valueOf(System.currentTimeMillis()));
            this.stopwatch.reset();
            this.val = MathUtils.randomFloat(1.0f, this.delay.getValue().floatValue());
        }
    }

    public boolean is1073(Entity entity) {
        return isToggled() && this.friendProtect.getValue().booleanValue() && (entity instanceof Player) && ClientServices.socialManager.isFriendEntity((Player) entity);
    }

    public void performClick() {
        MultiTask multitask_lcl = getMultitask();
        if (multitask_lcl == null || !multitask_lcl.isToggled() || (!minecraftClient.player.isUsingItem() && minecraftClient.gui.screen() == null)) {
            KeyMapping.click(this.mode.getValue().getKeyBinding2026().getDefaultKey());
        } else {
            this.mode.getValue().executeClickAction();
        }
    }
}