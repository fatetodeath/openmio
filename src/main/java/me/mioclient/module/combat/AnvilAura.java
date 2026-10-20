package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.module.combat.anvilaura.AnvilAuraMode;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.world.BlockPlacer;

import me.mioclient.module.combat.anvilaura.AnvilTarget;

import java.util.ArrayList;
import java.util.List;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.module.Delay;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class AnvilAura extends Delay {
    public static final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    public Setting<Float> targetRange = add(new NumberSetting<>("TargetRange", 4.0f, 1.0f, 6.0f).setSuffix("m"));
    public Setting<Boolean> ignoreNakeds = add(new BooleanSetting("IgnoreNakeds", false));
    public boolean flag;

    public AnvilAura() {
        super("AnvilAura", "Drops anvils on your enemies.", Category.COMBAT, "autoanvil");
        unregister((Setting<?>) this.setting10);
    }

    @Override // me.mioclient.module.Delay
    public List<BlockPos> getPlacePositions() {
        ArrayList arrayList = new ArrayList();
        this.flag = false;
        AnvilTarget anvilAuraData1909 = getAnvilTarget();
        if (anvilAuraData1909 == null) {
            return arrayList;
        }
        if (anvilAuraData1909.getAuraMode() == AnvilAuraMode.CAGE) {
            for (int i = 0; i < 3; i++) {
                arrayList.add(PlayerUtils.getRoundedBlockPos(anvilAuraData1909.getPlayerEntity()).relative(anvilAuraData1909.getDirection()).above(i));
            }
        } else {
            arrayList.add(PlayerUtils.getRoundedBlockPos(anvilAuraData1909.getPlayerEntity()).above(2));
            this.flag = true;
        }
        return arrayList;
    }

    @Override // me.mioclient.module.Delay
    public int getSlotIndex() {
        return this.flag ? InventoryUtils.findInHotbar(Items.ANVIL) : super.getSlotIndex();
    }

    public AnvilTarget getAnvilTarget() {
        AnvilTarget anvilAuraData;
        double longBitsToDouble = 1.7976931348623157E308d;
        AnvilTarget anvilAuraData2 = null;
        for (LivingEntity livingEntity : minecraftClient.level.players()) {
            if (((AbstractClientPlayer) livingEntity).isAlive() && minecraftClient.player != livingEntity && !ClientServices.socialManager.isFriendEntity((Player) livingEntity) && (!this.ignoreNakeds.getValue().booleanValue() || PlayerUtils.hasArmor(livingEntity))) {
                BlockPos up = PlayerUtils.getRoundedBlockPos(livingEntity).above(2);
                double distanceTo = minecraftClient.player.getEyePosition().distanceTo(((AbstractClientPlayer) livingEntity).position());
                if (distanceTo <= this.targetRange.getValue().floatValue()) {
                    if (minecraftClient.level.getBlockState(up.below()).canBeReplaced() && BlockPlacer.isSpaceFree(up, false)) {
                        if (BlockPlacer.findSupportSide(up, this.setting4.getValue().booleanValue()) != null) {
                            anvilAuraData = new AnvilTarget((Player) livingEntity, AnvilAuraMode.ANVIL, null);
                        } else {
                            Direction direction1910 = getDirection1910(up);
                            if (direction1910 != null) {
                                anvilAuraData = new AnvilTarget((Player) livingEntity, AnvilAuraMode.CAGE, direction1910);
                            } else {
                                continue;
                            }
                        }
                        if (distanceTo < longBitsToDouble) {
                            anvilAuraData2 = anvilAuraData;
                            longBitsToDouble = distanceTo;
                        }
                    }
                }
            }
        }
        return anvilAuraData2;
    }

    public Direction getDirection1910(BlockPos blockPos) {
        List<Direction> list3031 = BlockPlacer.getInteractableSides(blockPos);
        int i = 1337;
        Direction direction = null;
        for (Direction direction2 : Direction.values()) {
            if (list3031.contains(direction2.getOpposite()) || !this.setting4.getValue().booleanValue()) {
                int i2 = 0;
                int i3 = 0;
                while (true) {
                    if (i3 >= 3) {
                        break;
                    }
                    mutable.set(blockPos.getX() + direction2.getStepX(), blockPos.getY() - i3, blockPos.getZ() + direction2.getStepZ());
                    if (!minecraftClient.level.getBlockState(mutable).canBeReplaced()) {
                        i2 = i3;
                        break;
                    }
                    i2 = -1;
                    i3++;
                }
                if (i2 != -1 && i2 < i) {
                    i = i2;
                    direction = direction2;
                }
            }
        }
        return direction;
    }
}