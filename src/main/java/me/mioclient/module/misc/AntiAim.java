package me.mioclient.module.misc;

import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.util.math.MathConstants;
import me.mioclient.util.world.BlockPlacer;
import me.mioclient.util.world.RaycastUtils;

import me.mioclient.api.ClientAccessor;

import me.mioclient.render.RenderConstants;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AntiAim extends Module {
    public Setting<Boolean> yaw2 = add(new BooleanSetting("Yaw", false).setVisible());
    public Setting<AntiAimPredicateMode> yaw = add(new EnumSetting<>("Yaw", AntiAimPredicateMode.antiAimPredicateMode).requiresSettings(this.yaw2));
    public Setting<Float> speed = add(new NumberSetting<>("Speed", 1.0f, 0.1f, 90.0f));
    public Setting<Float> static_2 = add(new NumberSetting<>("Static", 0.0f, 0.0f, 360.0f));
    public Setting<Float> flip = add(new NumberSetting<>("Flip", 0.0f, 0.0f, 90.0f));
    public Setting<Float> jitterRange = add(new NumberSetting<>("JitterRange", 0.0f, 0.0f, 180.0f));
    public Setting<Float> base = add(new NumberSetting<>("Base", 0.0f, 0.0f, 360.0f));
    public Setting<Boolean> pitch = add(new BooleanSetting("Pitch", false).setVisible());
    public Setting<AntiAimMode> pitch2 = add(new EnumSetting<>("Pitch", AntiAimMode.antiAimMode).requiresSettings(this.pitch));
    public Setting<Float> static_ = add(new NumberSetting<>("Static", 0.0f, -90.0f, 90.0f));
    public Setting<Boolean> enderman = add(new BooleanSetting("Enderman", false).setVisible());
    public Setting<AntiAimMode_2> look = add(new EnumSetting<>("Look", AntiAimMode_2.AVOID).requiresSettings(this.enderman));
    public boolean flag;
    public float val;
    public int num;

    

    
    public enum AntiAimMode implements NamedValue {
        antiAimMode("Static") {
            @Override
            public float computePitchAngle(AntiAim antiAim, float f) {
                return antiAim.static_.getValue().floatValue();
            }
        },
        antiAimMode2("Random") {
            @Override
            public float computePitchAngle(AntiAim antiAim, float f) {
                return (float) ((Math.random() * 180.0d) - MathConstants.num2);
            }
        };

        public final String name;

        AntiAimMode(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public float computePitchAngle(AntiAim antiAim, float f) {
            return 0.0f;
        }
    }

    
    public enum AntiAimMode_2 implements NamedValue {
        AVOID("Avoid"),
        STARE("Stare");

        public final String name;

        AntiAimMode_2(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    

    
    public enum AntiAimPredicateMode implements NamedValue {
        antiAimPredicateMode("Jitter") {
            @Override
            public float computeYawAngle(AntiAim antiAim, float f) {
                return (float) Mth.wrapDegrees((((Math.random() * antiAim.jitterRange.getValue().floatValue()) * 2.0d) - antiAim.jitterRange.getValue().floatValue()) + antiAim.base.getValue().floatValue());
            }
        },
        antiAimPredicateMode2("Spin") {
            @Override
            public float computeYawAngle(AntiAim antiAim, float f) {
                antiAim.val += antiAim.speed.getValue().floatValue();
                return Mth.wrapDegrees(antiAim.val);
            }
        },
        RANDOMIRECTION("Flip") {
            @Override
            public float computeYawAngle(AntiAim antiAim, float f) {
                return Mth.wrapDegrees(antiAim.base.getValue().floatValue() + (ClientAccessor.minecraftClient.player.tickCount % 2 == 0 ? -antiAim.flip.getValue().floatValue() : antiAim.flip.getValue().floatValue()));
            }
        },
        antiAimPredicateMode4("Static") {
            @Override
            public float computeYawAngle(AntiAim antiAim, float f) {
                return antiAim.static_2.getValue().floatValue();
            }
        },
        antiAimPredicateMode5("Random") {
            @Override
            public float computeYawAngle(AntiAim antiAim, float f) {
                return (float) (Math.random() * MathConstants.num3);
            }
        };

        public final String name;

        AntiAimPredicateMode(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public float computeYawAngle(AntiAim antiAim, float f) {
            return 0.0f;
        }
    }

    public AntiAim() {
        super("AntiAim", "Sets your rotations server-side.", Category.MISC, new String[0]);
        this.yaw.setConfigName("YawMode");
        this.pitch2.setConfigName("PitchMode");
    }

    @Listen(get219= RenderConstants.num4)
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        EnderMan endermanEntity852;
        this.flag = false;
        if (ClientServices.RotationManager.getActiveRequest() != null) {
            return;
        }
        if (this.num > 0) {
            this.num--;
            return;
        }
        float f = holeSnapEvent.getYaw();
        float f2 = holeSnapEvent.getPitch();
        if (this.yaw2.getValue().booleanValue()) {
            f = this.yaw.getValue().computeYawAngle(this, minecraftClient.player.getYRot());
            markRotationModified();
        }
        if (this.pitch.getValue().booleanValue()) {
            f2 = this.pitch2.getValue().computePitchAngle(this, minecraftClient.player.getXRot());
            markRotationModified();
        }
        if (this.enderman.getValue().booleanValue() && (endermanEntity852 = getEndermanEntity852()) != null) {
            if (this.look.getValue() == AntiAimMode_2.AVOID) {
                f2 = 90.0f;
                markRotationModified();
            }
            if (this.look.getValue() == AntiAimMode_2.STARE) {
                float[] floatArray2484 = RotationManager.getRotationsTo(endermanEntity852.getEyePosition());
                f = floatArray2484[0];
                f2 = floatArray2484[1];
                markRotationModified();
            }
        }
        if (this.flag) {
            ClientServices.RotationManager.requestRotations(new float[]{f, f2}, -999, true);
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        Item item = minecraftClient.player.getMainHandItem().getItem();
        if (((item instanceof ProjectileWeaponItem) || (item instanceof TridentItem)) && minecraftClient.player.isUsingItem()) {
            this.num = 2;
        }
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        ServerboundUseItemPacket packet904 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundUseItemPacket ? (ServerboundUseItemPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet904 instanceof ServerboundUseItemPacket) {
            handleItemUse(packet904.getHand());
        }
        ServerboundUseItemOnPacket packet9042 = (sendImmediatelyEvent.getPacket()) instanceof ServerboundUseItemOnPacket ? (ServerboundUseItemOnPacket) (sendImmediatelyEvent.getPacket()) : null;
        if (packet9042 instanceof ServerboundUseItemOnPacket) {
            handleItemUse(packet9042.getHand());
        }
    }

    public EnderMan getEndermanEntity852() {
        // 26.2: ClientLevel inherits EntityGetter#getEntitiesOfClass(Class, AABB, Predicate) publicly.
        if (minecraftClient.level == null || minecraftClient.player == null) {
            return null;
        }
        net.minecraft.world.phys.AABB box = minecraftClient.player.getBoundingBox().inflate(64.0);
        java.util.List<EnderMan> list = minecraftClient.level.getEntitiesOfClass(EnderMan.class, box, e -> e.isAlive() && !e.isRemoved());
        EnderMan best = null;
        double bestDist = Double.MAX_VALUE;
        for (EnderMan em : list) {
            if (!isEndermanInCrosshairFOV(em)) {
                continue;
            }
            double d = minecraftClient.player.distanceToSqr(em);
            if (d < bestDist) {
                bestDist = d;
                best = em;
            }
        }
        if (best != null) {
            return best;
        }
        return list.isEmpty() ? null : list.get(0);
    }

    public boolean isEndermanInCrosshairFOV(EnderMan endermanEntity) {
        // 26.2: Entity#getForward() replaces getRotationVec; dot with direction-to-target checks FOV.
        if (minecraftClient.player == null || endermanEntity == null) {
            return false;
        }
        Vec3 look = minecraftClient.player.getLookAngle().normalize();
        Vec3 toTarget = endermanEntity.getEyePosition().subtract(minecraftClient.player.getEyePosition()).normalize();
        return look.dot(toTarget) > 0.0;
    }

    public void handleItemUse(InteractionHand hand) {
        if (minecraftClient.player == null) {
            return;
        }
        ItemStack stackInHand = minecraftClient.player.getItemInHand(hand);
        if (stackInHand.isEmpty()) {
            return;
        }
        Item item = stackInHand.getItem();
        if ((item instanceof BlockItem) || BlockPlacer.isThrowableItem(item)) {
            this.num = 2;
        }
    }

    public boolean hasModifiedRotation() {
        return this.flag;
    }

    public void resetActionTicks() {
        this.num = 2;
    }

    public void markRotationModified() {
        this.flag = true;
    }
}