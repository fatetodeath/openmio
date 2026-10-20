package me.mioclient.module.render;

import java.util.Set;
import me.mioclient.ducks.DuckSimpleOption;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EntityListObjectSetting;
import net.minecraft.core.registries.BuiltInRegistries;

public class Xray extends Module {
    public static Xray xray;
    public Setting<Boolean> bypass = add(new BooleanSetting("Bypass", false));
    public Setting<Set<Block>> whitelist = add(new EntityListObjectSetting<>("Whitelist", BuiltInRegistries.BLOCK));
    public double val;

    public Xray() {
        super("Xray", "Wallhack on ores.", Category.RENDER, new String[0]);
        xray = this;
        this.whitelist.setChangeCallback(this::reloadWorldRenderer);
        this.bypass.setChangeCallback(this::reloadWorldRenderer);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.val = ((Double) minecraftClient.options.gamma().get()).doubleValue();
        ((DuckSimpleOption<Double>)(Object) minecraftClient.options.gamma()).forceSetValue(Double.valueOf(1000.0d));
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        ((DuckSimpleOption<Double>)(Object) minecraftClient.options.gamma()).forceSetValue(Double.valueOf(this.val));
    }

    @Override // me.mioclient.module.Module
    public void onToggle() {
        reloadWorldRenderer();
    }

    public boolean shouldRenderBlock(BlockPos blockPos, Block block) {
        return (block == null || blockPos == null || isNotInGame() || !this.whitelist.getValue().contains(block) || (this.bypass.getValue().booleanValue() && !isBlockExposed(blockPos))) ? false : true;
    }

    public boolean isBlockExposed(BlockPos blockPos) {
        for (Direction direction : Direction.values()) {
            // 26.2: BlockState.isOpaque() was removed; isSolidRender() is the full-cube opaque check.
            if (!minecraftClient.level.getBlockState(blockPos.relative(direction)).isSolidRender()) {
                return true;
            }
        }
        return false;
    }

    public void reloadWorldRenderer() {
        if (minecraftClient.level != null) {
            // 26.2: LevelRenderer.reload() was removed; LevelExtractor owns chunk rebuilds now.
            minecraftClient.levelExtractor.allChanged();
        }
    }

    public static Xray getInstance() {
        return xray;
    }
}