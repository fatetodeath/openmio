package me.mioclient.module.render;

import me.mioclient.api.ClientServices;

import me.mioclient.render.mesh.TrajectoriesVertexConsumer;
import me.mioclient.render.RenderUtil;
import me.mioclient.util.math.MathConstants;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.model.player.PlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import me.mioclient.setting.ColorSetting;

public class Skeleton extends Module {
    public static Animations animations = (Animations) ClientServices.moduleInjector.getModule(Animations.class);
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-4351489, true)).setThemeSync(true));

    public Skeleton() {
        super("Skeleton", "Draws skeletons for players.", Category.RENDER, new String[0]);
        setDrawn(false);
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        PoseStack matrixStack472 = inner_3.getMatrixStack();
        float f = inner_3.getTickDelta();
        matrixStack472.pushPose();
        for (AbstractClientPlayer abstractClientPlayerEntity : minecraftClient.level.players()) {
            if (!abstractClientPlayerEntity.isRemoved() && abstractClientPlayerEntity != minecraftClient.player) {
                Color value = this.color.getValue();
                Vec3 vec3d2641 = getCameraRelativePos((Entity) abstractClientPlayerEntity, f);
                PlayerModel model = (PlayerModel) ((AvatarRenderer) minecraftClient.getEntityRenderDispatcher().getRenderer(abstractClientPlayerEntity)).getModel();
                float lerpAngleDegrees = Mth.rotLerp(f, abstractClientPlayerEntity.yBodyRotO, abstractClientPlayerEntity.yBodyRot);
                float lerp = Mth.lerp(f, abstractClientPlayerEntity.xRotO, abstractClientPlayerEntity.getXRot());
                if (!animations.isStaticAnimationEnabled()) {
                    float lerpAngleDegrees2 = Mth.rotLerp(f, abstractClientPlayerEntity.yHeadRotO, abstractClientPlayerEntity.yHeadRot) - Mth.rotLerp(f, abstractClientPlayerEntity.yBodyRotO, abstractClientPlayerEntity.yBodyRot);
                    float mio$getAnimationProgress = (float) abstractClientPlayerEntity.tickCount + f;
                    float f2 = 0.0f;
                    float f3 = 0.0f;
                    if (!abstractClientPlayerEntity.isPassenger() && abstractClientPlayerEntity.isAlive()) {
                        f2 = Math.min(abstractClientPlayerEntity.walkAnimation.speed(f), 1.0f);
                        f3 = abstractClientPlayerEntity.walkAnimation.position(f);
                        if (abstractClientPlayerEntity.isBaby()) {
                            f3 *= 3.0f;
                        }
                    }
                    model.head.yRot = lerpAngleDegrees2 * 0.017453292f;
                    model.head.xRot = lerp * 0.017453292f;
                    model.leftLeg.xRot = Mth.cos(f3 * 0.6662f) * 1.4f * f2;
                    model.rightLeg.xRot = Mth.cos(f3 * 0.6662f + (float) Math.PI) * 1.4f * f2;
                    model.leftArm.xRot = Mth.cos(f3 * 0.6662f) * 2.0f * f2 * 0.5f;
                    model.rightArm.xRot = Mth.cos(f3 * 0.6662f + (float) Math.PI) * 2.0f * f2 * 0.5f;
                }
                boolean isInSwimmingPose = abstractClientPlayerEntity.isSwimming();
                boolean z = abstractClientPlayerEntity.isShiftKeyDown() || animations.isSneakAnimationEnabled();
                boolean isFallFlying = abstractClientPlayerEntity.isFallFlying();
                ModelPart modelPart = model.head;
                ModelPart modelPart2 = model.leftArm;
                ModelPart modelPart3 = model.rightArm;
                ModelPart modelPart4 = model.leftLeg;
                ModelPart modelPart5 = model.rightLeg;
                matrixStack472.translate(vec3d2641.x, vec3d2641.y, vec3d2641.z);
                if (isInSwimmingPose) {
                    matrixStack472.translate(0.0f, 0.35f, 0.0f);
                }
                matrixStack472.mulPose(new Quaternionf().setAngleAxis(((lerpAngleDegrees + 180.0f) * MathConstants.val) / 180.0d, 0.0d, (-1.0d), 0.0d));
                if (isInSwimmingPose || isFallFlying) {
                    matrixStack472.mulPose(new Quaternionf().setAngleAxis(((MathConstants.num2 + lerp) * MathConstants.val) / 180.0d, (-1.0d), 0.0d, 0.0d));
                }
                if (isInSwimmingPose) {
                    matrixStack472.translate(0.0f, (-0.95f), 0.0f);
                }
                if (animations.isPlayerScaleModified()) {
                    matrixStack472.scale(animations.playerScale.getValue().floatValue(), animations.playerScale.getValue().floatValue(), animations.playerScale.getValue().floatValue());
                }
                PoseStack.Pose pose = matrixStack472.last();
                int rgb = value.getRGB();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose, 0.0f, z ? 0.6f : 0.7f, z ? 0.23f : 0.0f, 0.0f, z ? 1.05f : 1.4f, 0.0f, rgb);
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose, (-0.37f), z ? 1.05f : 1.35f, 0.0f, 0.37f, z ? 1.05f : 1.35f, 0.0f, rgb);
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose, (-0.15f), z ? 0.6f : 0.7f, z ? 0.23f : 0.0f, 0.15f, z ? 0.6f : 0.7f, z ? 0.23f : 0.0f, rgb);
                matrixStack472.pushPose();
                matrixStack472.translate(0.0f, z ? 1.05f : 1.4f, 0.0f);
                applyModelPartRotation(matrixStack472, modelPart);
                PoseStack.Pose pose2 = matrixStack472.last();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose2, 0.0f, 0.0f, 0.0f, 0.0f, 0.15f, 0.0f, rgb);
                matrixStack472.popPose();
                matrixStack472.pushPose();
                matrixStack472.translate(0.15f, z ? 0.6f : 0.7f, z ? 0.23f : 0.0f);
                applyModelPartRotation(matrixStack472, modelPart5);
                PoseStack.Pose pose3 = matrixStack472.last();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose3, 0.0f, 0.0f, 0.0f, 0.0f, (-0.6f), 0.0f, rgb);
                matrixStack472.popPose();
                matrixStack472.pushPose();
                matrixStack472.translate((-0.15f), z ? 0.6f : 0.7f, z ? 0.23f : 0.0f);
                applyModelPartRotation(matrixStack472, modelPart4);
                PoseStack.Pose pose4 = matrixStack472.last();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose4, 0.0f, 0.0f, 0.0f, 0.0f, (-0.6f), 0.0f, rgb);
                matrixStack472.popPose();
                matrixStack472.pushPose();
                matrixStack472.translate(0.37f, z ? 1.05f : 1.35f, 0.0f);
                applyModelPartRotation(matrixStack472, modelPart3);
                PoseStack.Pose pose5 = matrixStack472.last();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose5, 0.0f, 0.0f, 0.0f, 0.0f, (-0.55f), 0.0f, rgb);
                matrixStack472.popPose();
                matrixStack472.pushPose();
                matrixStack472.translate((-0.37f), z ? 1.05f : 1.35f, 0.0f);
                applyModelPartRotation(matrixStack472, modelPart2);
                PoseStack.Pose pose6 = matrixStack472.last();
                me.mioclient.render.shape.BoxRenderer.submitLinePair(matrixStack472, pose6, 0.0f, 0.0f, 0.0f, 0.0f, (-0.55f), 0.0f, rgb);
                matrixStack472.popPose();
                if (isInSwimmingPose) {
                    matrixStack472.translate(0.0f, 0.95f, 0.0f);
                }
                if (isInSwimmingPose || isFallFlying) {
                    matrixStack472.mulPose(new Quaternionf().setAngleAxis(((MathConstants.num2 + lerp) * MathConstants.val) / 180.0d, 1.0d, 0.0d, 0.0d));
                }
                if (isInSwimmingPose) {
                    matrixStack472.translate(0.0f, (-0.35f), 0.0f);
                }
                matrixStack472.mulPose(new Quaternionf().setAngleAxis(((lerpAngleDegrees + 180.0f) * MathConstants.val) / 180.0d, 0.0d, 1.0d, 0.0d));
                matrixStack472.translate(-vec3d2641.x, -vec3d2641.y, -vec3d2641.z);
            }
        }
        matrixStack472.popPose();
    }

    public void applyModelPartRotation(PoseStack matrixStack, ModelPart modelPart) {
        if (modelPart.zRot != 0.0f) {
            matrixStack.mulPose(Axis.ZP.rotation(modelPart.zRot));
        }
        if (modelPart.yRot != 0.0f) {
            matrixStack.mulPose(Axis.YN.rotation(modelPart.yRot));
        }
        if (modelPart.xRot != 0.0f) {
            matrixStack.mulPose(Axis.XN.rotation(modelPart.xRot));
        }
    }

    public Vec3 getCameraRelativePos(Entity entity, double d) {
        return new Vec3((entity.xo + ((entity.getX() - entity.xo) * d)) - minecraftClient.getEntityRenderDispatcher().camera.position().x, (entity.yo + ((entity.getY() - entity.yo) * d)) - minecraftClient.getEntityRenderDispatcher().camera.position().y, (entity.zo + ((entity.getZ() - entity.zo) * d)) - minecraftClient.getEntityRenderDispatcher().camera.position().z);
    }
}