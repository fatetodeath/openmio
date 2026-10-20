package me.mioclient.module.render.esp;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.UUID;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.module.render.ESP;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.manager.RotationManager;

public class EspOutlineRenderer implements ClientAccessor {
    public final ESP eSP;
    public final HashMap<UUID, String> hashMap = new HashMap<>();

    public EspOutlineRenderer(ESP esp) {
        this.eSP = esp;
    }

    public boolean shouldRenderEntity(Entity entity) {
        if (entity == null || entity.getBoundingBox() == null || !RotationManager.isInFrustum(entity.getBoundingBox())) {
            return false;
        }
        Vec3 pos = minecraftClient.gameRenderer.mainCamera().position();
        if (!(entity instanceof ItemEntity) || entity.position().distanceTo(pos) <= this.eSP.range.getValue().intValue()) {
            return ((entity instanceof Player) && this.eSP.players.getValue().booleanValue() && entity != minecraftClient.player) || ((entity instanceof ItemEntity) && this.eSP.items.getValue().booleanValue() && this.eSP.mode.getValue() != ESPRenderMode.TEXT) || (((entity instanceof ThrownExperienceBottle) && this.eSP.exp.getValue().booleanValue()) || ((((entity instanceof Animal) || (entity instanceof AbstractFish) || (entity instanceof Squid)) && this.eSP.animals.getValue().booleanValue()) || (((entity instanceof Monster) && this.eSP.hostiles.getValue().booleanValue()) || ((entity instanceof ThrownEnderpearl) && this.eSP.pearls.getValue().booleanValue()))));
        }
        return false;
    }

    public boolean shouldHighlightBlockEntity(BlockEntity blockEntity) {
        if (blockEntity != null && RotationManager.isInFrustum(new AABB(blockEntity.getBlockPos()))) {
            return (((blockEntity instanceof ChestBlockEntity) || (blockEntity instanceof BarrelBlockEntity)) && this.eSP.chests.getValue().booleanValue()) || (((blockEntity instanceof EnderChestBlockEntity) && this.eSP.eChests.getValue().booleanValue()) || (((blockEntity instanceof ShulkerBoxBlockEntity) && this.eSP.shulkers.getValue().booleanValue()) || (((blockEntity instanceof AbstractFurnaceBlockEntity) && this.eSP.furnaces.getValue().booleanValue()) || (((blockEntity instanceof DispenserBlockEntity) && this.eSP.dispensers.getValue().booleanValue()) || (((blockEntity instanceof HopperBlockEntity) && this.eSP.hoppers.getValue().booleanValue()) || (((blockEntity instanceof SignBlockEntity) && this.eSP.signs.getValue().booleanValue()) || ((blockEntity instanceof DecoratedPotBlockEntity) && this.eSP.pots.getValue().booleanValue())))))));
        }
        return false;
    }

    public boolean hasBlockEntityEsp() {
        return this.eSP.chests.getValue().booleanValue() || this.eSP.eChests.getValue().booleanValue() || this.eSP.shulkers.getValue().booleanValue() || this.eSP.beds.getValue().booleanValue() || this.eSP.signs.getValue().booleanValue() || this.eSP.dispensers.getValue().booleanValue() || this.eSP.hoppers.getValue().booleanValue() || this.eSP.furnaces.getValue().booleanValue();
    }

    public void renderBillboardLabel(MatrixStackEvent.Inner_3 inner_3, String str, Vec3 vec3d, float f, Color color, Color color2) {
        /* 26.2 backend: world-anchored labels go through vanilla's nametag pipeline
           (bg plate, distance scaling and depth handled by vanilla). */
        if (inner_3 == null || str == null || vec3d == null || str.isEmpty()) {
            return;
        }
        me.mioclient.render.shape.BoxRenderer.submitNameTag(inner_3.getMatrixStack(), vec3d, 0.0d, net.minecraft.network.chat.Component.literal(str), inner_3.cameraState, f);
    }

    public void fetchProfileName(UUID uuid) {
        try {
            HttpClient newHttpClient = HttpClient.newHttpClient();
            try {
                JsonObject asJsonObject = JsonParser.parseString((String) newHttpClient.send(HttpRequest.newBuilder().uri(new URI(new StringTemplate().arg(uuid.toString().replace("-", "")).format("https://sessionserver.mojang.com/session/minecraft/profile/\u0001"))).timeout(Duration.of(3L, ChronoUnit.SECONDS)).GET().build(), HttpResponse.BodyHandlers.ofString()).body()).getAsJsonObject();
                if (asJsonObject.has("name")) {
                    this.hashMap.put(uuid, asJsonObject.get("name").getAsString());
                }
                if (newHttpClient != null) {
                    newHttpClient.close();
                }
            } finally {
            }
        } catch (Exception e) {
        }
    }

    public String getOwnerName(Entity entity) {
        UUID uuid = null;
        if (!this.eSP.mobOwner.getValue().booleanValue()) {
            return null;
        }
        if (entity instanceof ThrownEnderpearl) {
            ThrownEnderpearl enderPearlEntity = (ThrownEnderpearl) entity;
            if (enderPearlEntity.getOwner() != null) {
                return enderPearlEntity.getOwner().getName().getString();
            }
        }
        if (entity instanceof TamableAnimal) {
            TamableAnimal tamable = (TamableAnimal) entity;
            if (tamable.getOwner() != null) {
                return tamable.getOwner().getName().getString();
            }
            if (tamable.getOwnerReference() != null) {
                uuid = tamable.getOwnerReference().getUUID();
            }
        }
        if (uuid == null) {
            return null;
        }
        Player playerByUuid = minecraftClient.level.getPlayerByUUID(uuid);
        if (playerByUuid != null) {
            String name = playerByUuid.getGameProfile().name();
            this.hashMap.putIfAbsent(uuid, name);
            return name;
        }
        if (this.hashMap.containsKey(uuid)) {
            return this.hashMap.get(uuid);
        }
        this.hashMap.put(uuid, null);
        UUID uuid2 = uuid;
        executorService.submit(() -> {
            fetchProfileName(uuid2);
        });
        return null;
    }

    public Color getEntityColor(Entity entity, boolean z) {
        Color color = Color.white;
        if (entity instanceof Player) {
            color = z ? this.eSP.fill6.getValue() : this.eSP.outline4.getValue();
        } else if (entity instanceof ItemEntity) {
            color = z ? this.eSP.fill4.getValue() : this.eSP.outline3.getValue();
        } else if (entity instanceof ThrownExperienceBottle) {
            color = z ? this.eSP.fill.getValue() : this.eSP.outline5.getValue();
        } else if ((entity instanceof Animal) || (entity instanceof AbstractFish) || (entity instanceof Squid)) {
            color = z ? this.eSP.fill2.getValue() : this.eSP.outline6.getValue();
        } else if (entity instanceof Monster) {
            color = z ? this.eSP.fill5.getValue() : this.eSP.outline2.getValue();
        } else if (entity instanceof ThrownEnderpearl) {
            color = z ? this.eSP.fill3.getValue() : this.eSP.outline.getValue();
        }
        int clamp = (int) Mth.clamp(MathUtils.interpolateWithTick(entity.tickCount - 1, entity.tickCount) * 20.0f, 0.0f, color.getAlpha());
        if ((entity instanceof ThrownEnderpearl) || (entity instanceof ThrownExperienceBottle)) {
            clamp = color.getAlpha();
        }
        return ColorUtils.withAlpha(color, clamp);
    }

    public Color getBlockEntityColor(BlockEntity blockEntity) {
        int renderColor = minecraftClient.level.getBlockState(blockEntity.getBlockPos()).getMapColor(minecraftClient.level, blockEntity.getBlockPos()).calculateARGBColor(MapColor.Brightness.HIGH);
        if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return new Color(255, 0, 175);
        }
        if (blockEntity instanceof EnderChestBlockEntity) {
            return new Color(125, 40, 180);
        }
        return new Color(renderColor & 255, (renderColor >> 8) & 255, (renderColor >> 16) & 255).brighter();
    }
}