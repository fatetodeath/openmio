package me.mioclient.module.effects;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.render.hud.HudTextLine;

import me.mioclient.api.ClientAccessor;

import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class StatusEffectHudElement extends HudTextLine implements ClientAccessor {
    public final Holder<MobEffect> registryEntry;

    public StatusEffectHudElement(Holder<MobEffect> registryEntry, Supplier<Boolean> supplier) {
        super(() -> {
            MobEffectInstance statusEffect = minecraftClient.player.getEffect(registryEntry);
            if (statusEffect == null) {
                return Component.empty();
            }
            String string = MobEffectUtil.formatDuration(statusEffect, 1.0f, minecraftClient.level.tickRateManager().tickrate()).getString();
            if (string.startsWith("0")) {
                string = string.substring(1);
            }
            return Component.literal("%s %s%s%s".formatted(registryEntry.value().getDisplayName().getString(), statusEffect.getAmplifier() == 0 ? "" : new StringTemplate().arg(statusEffect.getAmplifier() + 1).format("\u0001 "), ChatFormatting.WHITE, string));
        }, supplier);
        this.registryEntry = registryEntry;
    }

    public Holder<MobEffect> getEffect() {
        return this.registryEntry;
    }
}