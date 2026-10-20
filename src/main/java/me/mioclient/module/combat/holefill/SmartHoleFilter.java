package me.mioclient.module.combat.holefill;
import net.minecraft.world.phys.Vec3;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.math.Vec3dUtils;
import me.mioclient.util.player.MovementUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.HoleData;

import me.mioclient.module.combat.HoleFill;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class SmartHoleFilter extends RangeHoleFilter {
    public SmartHoleFilter(HoleFill holeFill) {
        super(holeFill);
    }

    @Override // me.mioclient.module.combat.holefill.RangeHoleFilter, me.mioclient.module.combat.holefill.HoleFilter
    public boolean isWithinRange(HoleData holeSnapData) {
        for (LivingEntity livingEntity : minecraftClient.level.players()) {
            if (!this.holeFill.ignoreNakeds.getValue().booleanValue() || PlayerUtils.hasArmor(livingEntity)) {
                AABB box1109 = this.holeFill.extrapolation.getValue().booleanValue() ? ClientServices.mainhandHelper.getPredictedBox((Player) livingEntity, BoxMath.getSnapDivisions((Player) livingEntity, this.holeFill.ticks)) : BoxMath.getServerBox(livingEntity);
                boolean z = !MovementUtils.isPlayerMoving((Player) livingEntity) || MovementUtils.isEntityMovingToward((Player) livingEntity, Vec3.atCenterOf(holeSnapData.getBlockPos()));
                if (Vec3dUtils.getHorizontalDistance(Vec3.atCenterOf(holeSnapData.getBlockPos()), BoxMath.getBottomCenter(box1109)) <= this.holeFill.smartRange.getValue().floatValue() && ((AbstractClientPlayer) livingEntity).getY() > holeSnapData.getBlockPos().getY() && box1109.minY - holeSnapData.getBlockPos().getY() <= 4.0d && !((AbstractClientPlayer) livingEntity).isRemoved() && minecraftClient.player != livingEntity) {
                    if (ClientServices.socialManager.isFriend(((AbstractClientPlayer) livingEntity).getGameProfile().name())) {
                        continue;
                    } else if (!ClientServices.holeSnapOverlayHelper.isHoleAt(BlockPos.containing(((AbstractClientPlayer) livingEntity).position())) && z) {
                        if (this.holeFill.selfSafety.getValue().booleanValue()) {
                            if (!ClientServices.holeSnapOverlayHelper.isHoleAt(BlockPos.containing(minecraftClient.player.position()))) {
                                if (minecraftClient.player.position().distanceTo(Vec3.atCenterOf(holeSnapData.getBlockPos())) < 2.0d) {
                                    return false;
                                }
                            }
                        }
                        return super.isWithinRange(holeSnapData);
                    }
                }
            }
        }
        return false;
    }
}