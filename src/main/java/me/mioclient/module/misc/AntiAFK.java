package me.mioclient.module.misc;

import me.mioclient.event.events.input.InputTickEvent;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.mode.combat.KeyPearlMode;
import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.time.Stopwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.network.AddMessageEvent;
import me.mioclient.event.events.input.KeyEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.input.UpdateMouseEvent;
import me.mioclient.module.Module;
import net.minecraft.world.InteractionHand;
import me.mioclient.mixin.ducks.DuckMinecraftClient;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class AntiAFK extends Module {
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 15.0f, 1.0f, 60.0f).setSuffix("s"));
    public Setting<Boolean> autoReply = add(new BooleanSetting("AutoReply", false));
    public Setting<Boolean> jump = add(new BooleanSetting("Jump", false));
    public Setting<Boolean> sneak = add(new BooleanSetting("Sneak", false));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false));
    public Setting<Boolean> attack = add(new BooleanSetting("Attack", false));
    public Setting<Boolean> onlySwing = add(new BooleanSetting("OnlySwing", false, v -> this.attack.getValue().booleanValue()));
    public final Stopwatch stopwatch;
    public final Stopwatch stopwatch2;
    public boolean flag;
    public boolean flag2;

    public AntiAFK() {
        super("AntiAFK", "Prevents you from being kicked for AFK-ing.", Category.MISC, new String[0]);
        this.stopwatch = new Stopwatch();
        this.stopwatch2 = new Stopwatch();
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.stopwatch.reset();
        this.stopwatch2.reset();
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (isActionReady()) {
            if (minecraftClient.options.keyJump.isDown() && this.jump.getValue().booleanValue()) {
                minecraftClient.options.keyJump.setDown(false);
            }
            if (this.stopwatch2.hasElapsed(this.delay.getValue().floatValue(), TimeUnit.SECONDS)) {
                List<Runnable> list1582 = buildAntiAfkActions();
                Collections.shuffle(list1582);
                if (!list1582.isEmpty()) {
                    list1582.get(0).run();
                }
                this.stopwatch2.reset();
            }
        }
    }

    @Listen(get219= RenderConstants.num2)
    public void onAddMessage(AddMessageEvent addMessageEvent) {
        if (this.autoReply.getValue().booleanValue() && isActionReady() && addMessageEvent.getPhase() == KeyPearlMode.Pre && addMessageEvent.getText() != null && ChatUtils.isIncomingWhisper(addMessageEvent.getText().getString())) {
            minecraftClient.execute(() -> {
                minecraftClient.player.connection.sendCommand(new StringTemplate().arg((int) Math.floor(Math.random() * 10000.0d)).arg(minecraftClient.player.getName().getString()).format("r Hello! This is auto reply talking. \u0001 is currently AFK :'). [\u0001]"));
            });
        }
    }

    @Listen
    public void onUpdateMouse(UpdateMouseEvent updateMouseEvent) {
        this.stopwatch.reset();
    }

    @Listen
    public void onKey(KeyEvent keyEvent) {
        this.stopwatch.reset();
    }

    @Listen
    public void onInputTick(InputTickEvent tickEvent_2) {
        net.minecraft.client.player.ClientInput ci = tickEvent_2.getInput();
        if (this.flag) {
            net.minecraft.world.entity.player.Input cur = ci.keyPresses;
            ci.keyPresses = new net.minecraft.world.entity.player.Input(cur.forward(), cur.backward(), cur.left(), cur.right(), true, cur.shift(), cur.sprint());
        }
        if (this.flag2) {
            net.minecraft.world.entity.player.Input cur = ci.keyPresses;
            ci.keyPresses = new net.minecraft.world.entity.player.Input(cur.forward(), cur.backward(), cur.left(), cur.right(), cur.jump(), true, cur.sprint());
        }
        this.flag2 = false;
        this.flag = false;
    }

    public List<Runnable> buildAntiAfkActions() {
        ArrayList<Runnable> arrayList = new ArrayList<>();
        if (this.jump.getValue().booleanValue()) {
            arrayList.add(() -> {
                this.flag = true;
            });
        }
        if (this.sneak.getValue().booleanValue()) {
            arrayList.add(() -> {
                this.flag2 = true;
            });
        }
        if (this.rotate.getValue().booleanValue()) {
            Random random = new Random();
            arrayList.add(() -> {
                minecraftClient.player.setYRot(random.nextFloat(MathConstants.num3));
                minecraftClient.player.setXRot(random.nextFloat(180.0f) - MathConstants.num2);
            });
        }
        if (this.attack.getValue().booleanValue()) {
            arrayList.add(() -> {
                if (!this.onlySwing.getValue().booleanValue()) {
                    ((me.mioclient.mixin.ducks.DuckMinecraftClient) (Object) minecraftClient).attack();
                } else {
                    minecraftClient.player.swing(InteractionHand.MAIN_HAND);
                }
            });
        }
        return arrayList;
    }

    public boolean isActionReady() {
        return this.stopwatch.hasElapsedMs((long) (this.delay.getValue().floatValue() * 1000.0f));
    }
}