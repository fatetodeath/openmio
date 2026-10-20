package me.mioclient.module.render;

import me.mioclient.api.ClientServices;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathUtils;

import me.mioclient.module.render.breakhighlight.BreakProgressTracker;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;

public class BreakHighlight extends Module {
    public Setting<Double> range = add(new NumberSetting<>("Range", 10.0, 1.0, 25.0).setSuffix("m"));
    public Setting<Boolean> friends = add(new BooleanSetting("Friends", true));
    public Setting<Boolean> enemyColor = add(new BooleanSetting("EnemyColor", true));
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(279542783, true)).setThemeSync(true));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(12425727, true)).setThemeSync(true));

    public BreakHighlight() {
        super("BreakHighlight", "Highlights the blocks that are being broken.", Category.RENDER, new String[0]);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        for (BreakProgressTracker entityIdHelper : ClientServices.breakHighlightHelper.getList1517()) {
            if (MathUtils.getCameraDistanceToBlock(entityIdHelper.getBlockPos()) <= this.range.getValue().doubleValue() && RotationManager.isInFrustum(new AABB(entityIdHelper.getBlockPos())) && WorldUtils.isBreakable(entityIdHelper.getBlockPos())) {
                Color value = this.fill.getValue();
                Color value2 = this.outline.getValue();
                VoxelShape outlineShape = minecraftClient.level.getBlockState(entityIdHelper.getBlockPos()).getShape(minecraftClient.level, entityIdHelper.getBlockPos());
                float f = entityIdHelper.get2814(inner_3.getTickDelta());
                Player entity181 = (entityIdHelper.getEntity()) instanceof Player ? (Player) (entityIdHelper.getEntity()) : null;
                AABB offset = scaleBoxByProgress(outlineShape.isEmpty() ? new AABB(0.0d, 0.0d, 0.0d, 1.0d, 1.0d, 1.0d) : outlineShape.bounds(), f).move(entityIdHelper.getBlockPos());
                if (entity181 instanceof Player) {
                    Player playerEntity = entity181;
                    Color color530 = ClientServices.socialManager.getSocialColor(playerEntity.getGameProfile().name(), (Color) null);
                    if (this.friends.getValue().booleanValue() || !ClientServices.socialManager.isFriendEntity(playerEntity)) {
                        if (color530 != null) {
                            if (!ClientServices.socialManager.isEnemy(playerEntity.getGameProfile().name()) || this.enemyColor.getValue().booleanValue()) {
                                value = ColorUtils.withAlpha(color530, this.fill.getValue().getAlpha());
                                value2 = ColorUtils.withAlpha(color530, this.outline.getValue().getAlpha());
                            }
                        }
                    }
                }
                BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), scaleBoxByProgress(offset, f), value);
                BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), scaleBoxByProgress(offset, f), value2, 1.0f);
            }
        }
    }

    public AABB scaleBoxByProgress(AABB box, float f) {
        float clamp = Mth.clamp(f, 0.0f, 1.0f);
        double abs = Math.abs(box.getXsize());
        double abs2 = Math.abs(box.getYsize());
        double abs3 = Math.abs(box.getZsize());
        Vec3 center = box.getCenter();
        return new AABB(center.subtract((clamp * abs) / 2.0d, (clamp * abs2) / 2.0d, (clamp * abs3) / 2.0d), center.add((clamp * abs) / 2.0d, (clamp * abs2) / 2.0d, (clamp * abs3) / 2.0d));
    }
}