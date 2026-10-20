package me.mioclient.module.render;

import me.mioclient.api.ClientServices;

import me.mioclient.render.shape.BoxRenderer;

import java.awt.Color;
import java.util.Iterator;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.render.DrawBlockOutlineEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.exploit.Reach;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class Highlight extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module
       registry was ready, leaving reach null and NPE-crashing ClickGui layout. */
    private static Reach reach;

    public static Reach getReach() {
        if (reach == null && ClientServices.moduleInjector != null) {
            try {
                reach = (Reach) ClientServices.moduleInjector.getModule(Reach.class);
            } catch (Exception e) {
            }
        }
        return reach;
    }
    public Setting<Boolean> complex = add(new BooleanSetting("Complex", false));
    public Setting<Boolean> unreachable = add(new BooleanSetting("Unreachable", false));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(1992137215, true)).setThemeSync(true));
    public Setting<Color> fillColor = add(new ColorSetting("FillColor", new Color(12425727, true)).setThemeSync(true));

    public Highlight() {
        super("Highlight", "Highlights the block you're looking at.", Category.RENDER, "blockhighlight");
        setDrawn(false);
        this.unreachable.addVisibilityPredicate(bool -> {
            Reach reach_lcl = getReach();
            return reach_lcl != null && reach_lcl.isToggled() && (reach_lcl.liquidPlace.getValue().booleanValue() || reach_lcl.airPlace.getValue().booleanValue());
        });
    }

    @Listen
    public void onDrawBlockOutline(DrawBlockOutlineEvent drawBlockOutlineEvent) {
        if (isUnreachableHighlightEnabled()) {
            return;
        }
        drawBlockOutlineEvent.cancel();
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        BlockPos blockPos406;
        if (isNotInGame() || (blockPos406 = getBlockPos406()) == null) {
            return;
        }
        BlockState blockState = minecraftClient.level.getBlockState(blockPos406);
        if (is408() || !(blockState.getBlock() instanceof AirBlock)) {
            VoxelShape outlineShape = blockState.getShape(minecraftClient.level, blockPos406);
            if (outlineShape.isEmpty()) {
                if (is408()) {
                    drawHighlightBox(inner_3.getMatrixStack(), Shapes.block().bounds(), blockPos406);
                }
            } else {
                if (isUnreachableHighlightEnabled()) {
                    return;
                }
                if (!this.complex.getValue().booleanValue()) {
                    drawHighlightBox(inner_3.getMatrixStack(), outlineShape.bounds(), blockPos406);
                    return;
                }
                Iterator<AABB> it = outlineShape.toAabbs().iterator();
                while (it.hasNext()) {
                    drawHighlightBox(inner_3.getMatrixStack(), it.next(), blockPos406);
                }
            }
        }
    }

    public BlockPos getBlockPos406() {
        BlockHitResult blockHitResult1232;
        Reach reach_lcl = getReach();
        if (reach_lcl != null && reach_lcl.isToggled() && reach_lcl.ghostHand.getValue().booleanValue() && (blockHitResult1232 = reach_lcl.getGhostHandHitResult()) != null && blockHitResult1232.getType() != HitResult.Type.MISS && blockHitResult1232.getBlockPos() != null) {
            return reach_lcl.getGhostHandHitResult().getBlockPos();
        }
        if (is408()) {
            BlockHitResult hitResult1229 = (Reach.getAirPlaceHitResult()) instanceof BlockHitResult ? (BlockHitResult) (Reach.getAirPlaceHitResult()) : null;
            if (hitResult1229 instanceof BlockHitResult) {
                return hitResult1229.getBlockPos();
            }
            return null;
        }
        BlockHitResult blockHitResult = (minecraftClient.hitResult) instanceof BlockHitResult ? (BlockHitResult) (minecraftClient.hitResult) : null;
        if (!(blockHitResult instanceof BlockHitResult)) {
            return null;
        }
        BlockHitResult blockHitResult2 = blockHitResult;
        if (blockHitResult2.getType() != HitResult.Type.MISS) {
            return blockHitResult2.getBlockPos();
        }
        return null;
    }

    public void drawHighlightBox(PoseStack matrixStack, AABB box, BlockPos blockPos) {
        BoxRenderer.drawFilledBox(matrixStack, box.move(blockPos).inflate(0.002d), this.fillColor.getValue());
        BoxRenderer.drawBoxOutline(matrixStack, box.move(blockPos).inflate(0.002d), this.color.getValue(), this.lineWidth.getValue().floatValue());
    }

    public boolean is408() {
        Reach reach_lcl = getReach();
        return reach_lcl != null && reach_lcl.isToggled() && reach_lcl.airPlace.getValue().booleanValue() && ((minecraftClient.player.getMainHandItem().getItem() instanceof BlockItem) || (minecraftClient.player.getOffhandItem().getItem() instanceof BlockItem));
    }

    public boolean isUnreachableHighlightEnabled() {
        return this.unreachable.getValue().booleanValue() && this.unreachable.isVisible();
    }
}