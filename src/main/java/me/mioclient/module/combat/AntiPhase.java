package me.mioclient.module.combat;

import me.mioclient.event.events.player.RotationTickEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.module.combat.antiphase.FacePlateTargetResolver;

import me.mioclient.module.combat.antiphase.AntiPhaseMode;
import me.mioclient.render.shape.BlockHighlightRenderer;
import me.mioclient.util.inventory.InventoryUtils;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.combat.antiphase.AntiPhasePlacement;
import me.mioclient.module.combat.antiphase.AntiPhaseTargetResolver;
import me.mioclient.module.combat.antiphase.LadderTargetResolver;
import me.mioclient.module.combat.antiphase.PermissiveLadderTargetResolver;
import me.mioclient.module.combat.antiphase.SolidBlockTargetResolver;
import me.mioclient.network.PacketSender;

import java.awt.Color;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import me.mioclient.module.movement.NoSlow;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class AntiPhase extends Module {
    /* Resolved lazily — static init ran before module registry was ready, leaving field null. */
    private static NoSlow noSlow;

    public static NoSlow getNoSlow() {
        if (noSlow == null && ClientServices.moduleInjector != null) {
            try {
                noSlow = (NoSlow) ClientServices.moduleInjector.getModule(NoSlow.class);
            } catch (Exception e) {
            }
        }
        return noSlow;
    }
    public Setting<AntiPhaseMode> material = add(new EnumSetting<>("Material", AntiPhaseMode.LADDER));
    public Setting<Float> range = add(new NumberSetting<>("Range", 4.5f, 0.1f, 6.0f));
    public Setting<Integer> delay = add(new NumberSetting<>("Delay", 50, 0, 250).setSuffix("ms"));
    public Setting<Integer> bpt = add(new NumberSetting<>("BPT", 6, 1, 6));
    public Setting<Boolean> strictDirection = add(new BooleanSetting("StrictDirection", false));
    public Setting<Boolean> rotate = add(new BooleanSetting("Rotate", false));
    public Setting<Boolean> noCrawl = add(new BooleanSetting("NoCrawl", false));
    public Setting<Boolean> render = add(new BooleanSetting("Render", false).setVisible());
    public Setting<Color> fill = add(new ColorSetting("Fill", new Color(146643455, true), v -> this.render.isShown()));
    public Setting<Color> outline = add(new ColorSetting("Outline", new Color(1539152383, true), v -> this.render.isShown()));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f).requiresSettings(this.render));
    public Setting<Boolean> fade = add(new BooleanSetting("Fade", true).requiresSettings(this.render));
    public Setting<Float> fadeTime = add(new NumberSetting<>("FadeTime", 1.0f, 0.1f, 1.0f).setSuffix("s").requiresSettings(this.render, this.fade));
    public final Stopwatch stopwatch;

    public AntiPhase() {
        super("AntiPhase", "Prevents your enemies from pearl-phasing into nearest block.", Category.COMBAT, new String[0]);
        this.stopwatch = new Stopwatch();
        ClientServices.antiPhaseBlockHelper.register(new BlockHighlightRenderer.Record(this, this.fill, this.outline, this.lineWidth, this.fadeTime, () -> {
            return true;
        }, this.fade, 450));
    }

    @Listen
    public void onRotationTick(RotationTickEvent holeSnapEvent) {
        AntiPhasePlacement antiPhaseData844;
        NoSlow noSlow_lcl = getNoSlow();
        if (!this.stopwatch.hasElapsedAndResetMs(this.delay.getValue().intValue()) || (noSlow_lcl != null && noSlow_lcl.shouldApply())) {
            return;
        }
        int i = 0;
        for (Player playerEntity : minecraftClient.level.players()) {
            if (isValidAntiPhaseTarget(playerEntity) && (antiPhaseData844 = getAntiPhaseHelper_3268().resolveTarget(this, playerEntity)) != null) {
                if (placeAntiPhaseBlock(antiPhaseData844)) {
                    if (this.rotate.getValue().booleanValue()) {
                        ClientServices.RotationManager.requestRotations(RotationManager.getRotationsTo(antiPhaseData844.getFaceCenter()), 25);
                    }
                    if (this.render.getValue().booleanValue()) {
                        ClientServices.antiPhaseBlockHelper.addHighlight(this, antiPhaseData844.getBox());
                    }
                    i++;
                }
                if (i >= this.bpt.getValue().intValue()) {
                    return;
                }
            }
        }
    }

    public boolean placeAntiPhaseBlock(AntiPhasePlacement antiPhaseData) {
        int i = InventoryUtils.findInHotbar(this.material.getValue().getDesiredItem());
        int i2 = minecraftClient.player.getInventory().getSelectedSlot();
        if (i == -1) {
            return false;
        }
        InventoryUtils.do438(i);
        BlockHitResult blockHitResult = new BlockHitResult(antiPhaseData.getFaceCenter(), antiPhaseData.getDirection(), antiPhaseData.getBlockPos(), false);
        ClientServices.antiPhaseHelper.withSneakPress(() -> {
            PacketSender.sendInteractBlock(InteractionHand.MAIN_HAND, blockHitResult);
        });
        PacketSender.sendHandSwing(InteractionHand.MAIN_HAND);
        InventoryUtils.do438(i2);
        return true;
    }

    public boolean isValidAntiPhaseTarget(Player playerEntity) {
        if (!playerEntity.isAlive() || minecraftClient.player == playerEntity || playerEntity.isSwimming() || ClientServices.socialManager.isFriendEntity(playerEntity) || PlayerUtils.getFeetPositions((LivingEntity) playerEntity).size() != 1 || hasItemAtPosition(PlayerUtils.getRoundedBlockPos((LivingEntity) playerEntity)) || !PlayerUtils.hasArmor((LivingEntity) playerEntity)) {
            return false;
        }
        return (!this.noCrawl.getValue().booleanValue() || playerEntity.getBoundingBox().getYsize() > 1.0d) && playerEntity.distanceTo(minecraftClient.player) <= this.range.getValue().floatValue();
    }

    public boolean hasItemAtPosition(BlockPos blockPos) {
        AABB box = new AABB(blockPos);
        return !minecraftClient.level.getEntities(EntityTypeTest.forClass(ItemFrame.class), box, entity -> entity.getBoundingBox().intersects(box)).isEmpty();
    }

    public AntiPhaseTargetResolver getAntiPhaseHelper_3268() {
        switch (this.material.getValue()) {
            case LADDER:
                return new LadderTargetResolver();
            case SCAFFOLDING:
                return new SolidBlockTargetResolver();
            case ITEMFRAME:
                return new FacePlateTargetResolver();
            case VINE:
                return new PermissiveLadderTargetResolver();
            default:
                throw new MatchException((String) null, (Throwable) null);
        }
    }
}