package me.mioclient.module.combat;

import me.mioclient.api.ClientServices;

import me.mioclient.mode.combat.BlockerOffsetMode;

import java.util.ArrayList;
import java.util.List;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.module.Delay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import me.mioclient.module.Module;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Blocker extends Delay {
    public Setting<BlockerOffsetMode> offsets = add(new EnumSetting<>("Offsets", BlockerOffsetMode.SURROUND));
    public Setting<Boolean> corners = add(new BooleanSetting("Corners", false, v -> this.offsets.getValue() != BlockerOffsetMode.CEV));
    public Setting<Float> blockDamage = add(new NumberSetting<>("BlockDamage", 0.8f, 0.1f, 1.0f));

    public Blocker() {
        super("Blocker", "Blocks certain offsets to prevent crystal damage.", Category.COMBAT, new String[0]);
        this.setting9.setDefaultValue(false);
    }

    @Override // me.mioclient.module.Module
    public String getInfo() {
        return this.offsets.getValue().getName();
    }

    @Override // me.mioclient.module.Delay
    public List<BlockPos> getPlacePositions() {
        ArrayList<BlockPos> arrayList = new ArrayList();
        for (Vec3i vec3i : this.offsets.getValue().getOffsets()) {
            BlockPos add = BlockPos.containing(minecraftClient.player.position()).offset(vec3i);
            if (ClientServices.breakHighlightHelper.getBreakProgress(add) >= this.blockDamage.getValue().floatValue()) {
                arrayList.add(add);
            }
        }
        ArrayList arrayList2 = new ArrayList();
        for (BlockPos blockPos : arrayList) {
            Vec3i[] vec3iArray2842 = BlockerOffsetMode.SURROUND.getOffsets();
            int length = vec3iArray2842.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                Vec3i vec3i2 = vec3iArray2842[i];
                if (blockPos.getY() != ((int) minecraftClient.player.position().y)) {
                    break;
                }
                Vec3i ofFloored = BlockPos.containing(minecraftClient.player.position());
                if (!this.corners.getValue().booleanValue()) {
                    if (blockPos.subtract(ofFloored).equals(vec3i2)) {
                        arrayList2.add(blockPos.offset(vec3i2));
                        break;
                    }
                } else if (!blockPos.offset(vec3i2).equals(ofFloored)) {
                    arrayList2.add(blockPos.offset(vec3i2));
                }
                i++;
            }
            if (blockPos.getY() == minecraftClient.player.getY() + 3.0d) {
                arrayList2.add(blockPos.below());
            } else {
                arrayList2.add(blockPos.above());
            }
        }
        return arrayList2;
    }
}