package me.mioclient.module.render;

import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.world.BlockPlacer;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class PhaseESP extends Module {
    public Setting<Color> safe = add(new ColorSetting("Safe", new Color(540373280, true)).setThemeSync(true));
    public Setting<Color> semiSafe = add(new ColorSetting("SemiSafe", new Color(637525248, true)).setThemeSync(true));
    public Setting<Color> unsafe = add(new ColorSetting("Unsafe", new Color(634257408, true)).setThemeSync(true));
    public Setting<Boolean> outline = add(new BooleanSetting("Outline", false).setVisible());
    public Setting<Float> alpha = add(new NumberSetting<>("Alpha", 1.0f, 0.0f, 1.0f).requiresSettings(this.outline));

    
    public static enum PhaseESPMode {
        phaseESPMode {
            @Override
            public Color getModeColor(PhaseESP phaseESP) {
                return phaseESP.safe.getValue();
            }
        },
        phaseESPMode2 {
            @Override
            public Color getModeColor(PhaseESP phaseESP) {
                return phaseESP.semiSafe.getValue();
            }
        },
        phaseESPMode3 {
            @Override
            public Color getModeColor(PhaseESP phaseESP) {
                return phaseESP.unsafe.getValue();
            }
        };

        public Color getModeColor(PhaseESP phaseESP) {
            return null;
        }
    }

    
    private static final class Record {
        public final BlockPos blockPos;
        public final PhaseESPMode phaseESPMode;

        public Record(BlockPos blockPos, PhaseESPMode phaseESPMode) {
            this.blockPos = blockPos;
            this.phaseESPMode = phaseESPMode;
        }

        public BlockPos getBlockPos() {
            return this.blockPos;
        }

        public PhaseESPMode getMode() {
            return this.phaseESPMode;
        }
    }

    public PhaseESP() {
        super("PhaseESP", "Highlights safe blocks to phase into.", Category.RENDER, new String[0]);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        AABB playerBox = minecraftClient.player.getBoundingBox();
        AABB expand = new AABB(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY, playerBox.maxZ).inflate(0.01d, 0.0d, 0.01d);
        if (BlockPlacer.isBlockCollidable(minecraftClient.player.blockPosition()) || minecraftClient.player.isSwimming()) {
            return;
        }
        BlockPos.betweenClosedStream(expand).filter(this::isPhaseableBlock).map(blockPos -> {
            return new Record(blockPos.immutable(), classifyPhaseMode(blockPos.immutable()));
        }).forEach(record -> {
            VoxelShape outlineShape = minecraftClient.level.getBlockState(record.blockPos).getShape(minecraftClient.level, record.blockPos);
            if (outlineShape.isEmpty()) {
                outlineShape = Shapes.box(0.0d, 0.0d, 0.0d, 1.0d, 1.0d, 1.0d);
            }
            AABB offset = outlineShape.bounds().move(record.blockPos);
            BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), offset.setMaxY(record.blockPos.getY()), record.phaseESPMode.getModeColor(this));
            if (this.outline.getValue().booleanValue()) {
                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), offset.setMaxY(record.blockPos.getY()), ColorUtils.withAlphaPercent(record.phaseESPMode.getModeColor(this), this.alpha.getValue().floatValue()), 1.0f);
            }
        });
    }

    public PhaseESPMode classifyPhaseMode(BlockPos blockPos) {
        boolean z = minecraftClient.level.getBlockState(blockPos).getBlock().getExplosionResistance() >= 600.0f;
        if (BlockPlacer.isBlockCollidable(blockPos.below()) && z) {
            if (minecraftClient.level.getBlockState(blockPos.below()).getBlock().getExplosionResistance() >= 600.0f) {
                return (isNotBedrock(blockPos) || isNotBedrock(blockPos.below())) ? PhaseESPMode.phaseESPMode2 : PhaseESPMode.phaseESPMode;
            }
        }
        return PhaseESPMode.phaseESPMode3;
    }

    public boolean isNotBedrock(BlockPos blockPos) {
        return BlockPlacer.getBlockAt(blockPos) != Blocks.BEDROCK;
    }

    public boolean isPhaseableBlock(BlockPos blockPos) {
        BlockState blockState = minecraftClient.level.getBlockState(blockPos);
        return blockState.getBlock().getExplosionResistance() >= 600.0f && BlockPlacer.isBlockCollidable(blockPos) && !blockState.canBeReplaced();
    }
}