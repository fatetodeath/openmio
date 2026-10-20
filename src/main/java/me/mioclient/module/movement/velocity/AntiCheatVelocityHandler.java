package me.mioclient.module.movement.velocity;

import me.mioclient.event.events.network.PacketReceiveEvent;

import me.mioclient.api.ClientServices;

import me.mioclient.util.math.BoxMath;
import me.mioclient.util.player.PlayerUtils;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.module.movement.velocity.VelocityModeHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import me.mioclient.event.events.network.ExplosionVelocityEvent;
import me.mioclient.event.events.player.MotionEvent;
import me.mioclient.module.movement.FastWeb;
import me.mioclient.module.movement.Velocity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class AntiCheatVelocityHandler extends VelocityModeHandler {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving refs null (NPE). */
    private static FastWeb fastWeb;

    public static FastWeb getFastWeb() {
        if (fastWeb == null && ClientServices.moduleInjector != null) {
            try {
                fastWeb = (FastWeb) ClientServices.moduleInjector.getModule(FastWeb.class);
            } catch (Exception e) {
            }
        }
        return fastWeb;
    }
    public final List<Long> list;
    public final Stopwatch stopwatch;
    public boolean flag;

    public AntiCheatVelocityHandler(Velocity velocity) {
        super(velocity);
        this.list = Collections.synchronizedList(new ArrayList());
        this.stopwatch = new Stopwatch();
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void handlePacketReceive(PacketReceiveEvent channelRead0Event) {
        if (channelRead0Event.getPacket() instanceof ClientboundSetEntityMotionPacket entityVelocityUpdateS2CPacket) {
            if (entityVelocityUpdateS2CPacket.id() == minecraftClient.player.getId() && is951()) {
                FastWeb fastWeb_lcl = getFastWeb();
                boolean z = fastWeb_lcl != null && fastWeb_lcl.isToggled() && fastWeb_lcl.isGrimMode() && PlayerUtils.isInCobweb(minecraftClient.player);
                Vec3 m = entityVelocityUpdateS2CPacket.movement();
                // 26.2 port: record immutable - cancel + apply zeroed/inverted motion directly.
                double ny = m.y;
                if ((this.flag || z) && m.y > 0.0d) {
                    ny = -m.y;
                }
                channelRead0Event.cancel();
                minecraftClient.player.setDeltaMovement(new Vec3(0.0d, ny, 0.0d));
            }
        }
        if (channelRead0Event.getPacket() instanceof ClientboundTeleportEntityPacket) {
            this.list.add(Long.valueOf(System.currentTimeMillis()));
        }
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void handleMotion(MotionEvent motionEvent) {
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void handleExplosionVelocity(ExplosionVelocityEvent explosionVelocityEvent) {
        if (is951()) {
            explosionVelocityEvent.setPlayerVelocityX(0.0f);
            explosionVelocityEvent.setPlayerVelocityY(0.0f);
            explosionVelocityEvent.setPlayerVelocityZ(0.0f);
        }
    }

    @Override // me.mioclient.module.movement.velocity.VelocityModeHandler
    public void onUpdate() {
        this.flag = isNearGroundCollision();
        this.list.removeIf(l -> {
            return System.currentTimeMillis() > l.longValue() + 1000;
        });
        if (this.list.size() > 5) {
            this.stopwatch.reset();
        }
    }

    public boolean is951() {
        FastWeb fastWeb_lcl = getFastWeb();
        if (fastWeb_lcl != null && fastWeb_lcl.isToggled() && fastWeb_lcl.isGrimMode() && PlayerUtils.isInCobweb(minecraftClient.player)) {
            return true;
        }
        return this.stopwatch.hasElapsedMs(500L) && this.flag;
    }

    public static boolean isNearGroundCollision() {
        return is1886();
    }

    public static boolean isHorizontallyColliding() {
        HashSet<BlockPos> hashSet = new HashSet<>();
        hashSet.add(getBlockPosAtOffset(1.0f, 0.0f));
        hashSet.add(getBlockPosAtOffset((-1.0f), 0.0f));
        hashSet.add(getBlockPosAtOffset(0.0f, 1.0f));
        hashSet.add(getBlockPosAtOffset(0.0f, (-1.0f)));
        return hashSet.stream().filter(AntiCheatVelocityHandler::hasCollisionShape).count() >= 2;
    }

    public static boolean is1886() {
        // 26.2: BlockPos.stream -> betweenClosedStream, AABB.shrink -> deflate,
        // AABB.withMaxY -> setMaxY, AABB.offset -> move
        return BlockPos.betweenClosedStream(minecraftClient.player.getBoundingBox().deflate(BoxMath.val, 0.0d, BoxMath.val).setMaxY(minecraftClient.player.getY()).move(0.0d, 0.10000000149011612d, 0.0d)).anyMatch(AntiCheatVelocityHandler::hasCollisionShape);
    }

    public static BlockPos getBlockPosAtOffset(float f, float f2) {
        LocalPlayer clientPlayerEntity = minecraftClient.player;
        float lengthX = (float) (clientPlayerEntity.getBoundingBox().getXsize() / 2.0d);
        return BlockPos.containing(clientPlayerEntity.getX() + (f * lengthX * 1.000001f), Math.round(clientPlayerEntity.getY()), clientPlayerEntity.getZ() + (f2 * lengthX * 1.000001f));
    }

    public static boolean hasCollisionShape(BlockPos blockPos) {
        return !minecraftClient.level.getBlockState(blockPos).getCollisionShape(minecraftClient.level, blockPos).isEmpty();
    }
}