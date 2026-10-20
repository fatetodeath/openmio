package me.mioclient.module.render.esp;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.math.BoxMath;

import me.mioclient.api.ClientAccessor;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EspTargetFilter implements ClientAccessor {
    public final Map<String, Integer> map = new TreeMap(Comparator.comparing(obj -> {
        return (String) obj;
    }).reversed());
    public final AABB box;
    public AABB box2;
    public float val;

    public EspTargetFilter(ItemEntity itemEntity) {
        float tickDelta = minecraftClient.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        this.box = AABB.ofSize(itemEntity.getPosition(tickDelta), 5.0d, 5.0d, 5.0d);
        this.box2 = BoxMath.getInterpolatedBox((Entity) itemEntity, tickDelta);
        addTrackedItem(itemEntity);
    }

    public AABB getClusterBox() {
        return this.box;
    }

    public AABB getMergedBox() {
        return this.box2;
    }

    public boolean addTrackedItem(ItemEntity itemEntity) {
        if (!this.box.intersects(itemEntity.getBoundingBox())) {
            return false;
        }
        float tickDelta = minecraftClient.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        String string = itemEntity.getName().getString();
        this.map.compute(string, (str, num) -> {
            return Integer.valueOf(itemEntity.getItem().getCount() + (num == null ? 0 : num.intValue()));
        });
        this.val = Math.max(ClientTextRenderer.INSTANCE.getStringWidth(formatItemCountLabel(string, this.map.get(string).intValue())), this.val);
        if (new Vec3(itemEntity.getX() - itemEntity.xo, itemEntity.getY() - itemEntity.yo, itemEntity.getZ() - itemEntity.zo).lengthSqr() >= 1.0E-4d) {
            return true;
        }
        this.box2 = this.box2.minmax(BoxMath.getInterpolatedBox((Entity) itemEntity, tickDelta));
        return true;
    }

    public Map<String, Integer> getItemCountMap() {
        return this.map;
    }

    public float getMaxLabelWidth() {
        return this.val;
    }

    public static String formatItemCountLabel(String str, int i) {
        return i <= 1 ? str : new StringTemplate().arg(i).arg(str).format("\u0001 x\u0001");
    }
}