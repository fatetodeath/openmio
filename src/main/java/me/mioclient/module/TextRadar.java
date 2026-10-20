package me.mioclient.module;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;

import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.hud.ResizableHudElement;
import me.mioclient.util.player.EntityUtil;
import me.mioclient.util.player.PlayerUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.render.NameTags;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;

public class TextRadar extends me.mioclient.module.HudModule {
    public Setting<TextRadarMode> setting;
    public Setting<Boolean> setting2;
    public Setting<Boolean> setting3;
    public Setting<Boolean> setting4;
    public Setting<Boolean> setting5;
    public Setting<Boolean> setting6;
    public Setting<Boolean> setting7;
    public Setting<Boolean> setting8;
    public Setting<Integer> setting9;
    public Setting<Boolean> setting10;
    public Setting<Boolean> setting11;
    public Setting<Boolean> setting12;
    public final List<Player> list;

    
    public enum TextRadarMode implements NamedValue {
        DISTANCE("Distance"),
        ANGLE("Angle");

        public final String name;

        TextRadarMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public TextRadar() {
        super("TextRadar", "playerlist");
        this.setting = add(new EnumSetting("Sort", TextRadarMode.DISTANCE));
        this.setting2 = add(new BooleanSetting("Health", true));
        this.setting3 = add(new BooleanSetting("Distance", false));
        this.setting4 = add(new BooleanSetting("TotemPops", false));
        this.setting5 = add(new BooleanSetting("FriendColor", true));
        this.setting6 = add(new BooleanSetting("EnemyColor", true));
        this.setting7 = add(new BooleanSetting("Armor", false));
        this.setting8 = add(new BooleanSetting("Limit", false).setVisible());
        this.setting9 = add(new NumberSetting("Max", 8, 1, 32).requiresSettings(this.setting8));
        this.setting10 = add(new BooleanSetting("Ignore", false).setDependent());
        this.setting11 = add(new BooleanSetting("Friends", false).requiresSettings(this.setting10));
        this.setting12 = add(new BooleanSetting("Nakeds", false).requiresSettings(this.setting10));
        this.list = new ArrayList();
        setElement(new ResizableHudElement(this));
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.list.clear();
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        synchronized (this.list) {
            this.list.clear();
            Stream limit = minecraftClient.level.players().stream().filter((v1) -> {
                return shouldShowPlayer(v1);
            }).sorted(Comparator.comparing((v1) -> {
                return getSortValue(v1);
            })).limit(this.setting8.getValue().booleanValue() ? this.setting9.getValue().intValue() : Long.MAX_VALUE);
            List<Player> list = this.list;
            Objects.requireNonNull(list);
            limit.forEachOrdered((v1) -> {
                list.add((net.minecraft.world.entity.player.Player) v1);
            });
        }
    }

    @Override // me.mioclient.module.HudModule
    public void renderContent(GuiGraphicsExtractor drawContext) {
        float f = this.moduleListSearchHelper4.getAlignedY(ClientTextRenderer.INSTANCE.getFontHeight()) - this.moduleListSearchHelper4.getY();
        for (Player playerEntity : this.list) {
            Color color3018 = getHudColor(f);
            if (ClientServices.socialManager.isFriendEntity(playerEntity) && this.setting5.getValue().booleanValue()) {
                color3018 = ClientServices.socialManager.getSocialColor(playerEntity.getGameProfile().name(), color3018);
            } else if (ClientServices.socialManager.isEnemyEntity(playerEntity) && this.setting6.getValue().booleanValue()) {
                color3018 = ClientServices.socialManager.getSocialColor(playerEntity.getGameProfile().name(), color3018);
            }
            String string2646 = formatPlayerEntry(playerEntity);
            ClientTextRenderer.INSTANCE.drawTextShadow(drawContext, string2646, this.moduleListSearchHelper4.getAlignedX(ClientTextRenderer.INSTANCE.getStringWidth(string2646)) - this.moduleListSearchHelper4.getX(), f, color3018);
            f += (ClientTextRenderer.INSTANCE.getFontHeight() + 1) * this.moduleListSearchHelper4.getVerticalSide();
        }
    }

    @Override // me.mioclient.module.HudModule
    public float[] getSize() {
        float f = 0.0f;
        float f2 = 0.0f;
        Iterator<Player> it = this.list.iterator();
        while (it.hasNext()) {
            float f3 = ClientTextRenderer.INSTANCE.getStringWidth(formatPlayerEntry(it.next()));
            f += ClientTextRenderer.INSTANCE.getFontHeight() + 1;
            if (f3 > f2) {
                f2 = f3;
            }
        }
        return new float[]{f2, f};
    }

    public String formatPlayerEntry(Player playerEntity) {
        int i;
        StringBuilder sb = new StringBuilder();
        float f = EntityUtil.getEntityHealth((Entity) playerEntity);
        if (this.setting2.getValue().booleanValue()) {
            sb.append(getHealthColor(f)).append(Math.round(f)).append(" ");
        }
        sb.append(ChatFormatting.RESET);
        sb.append(ClientServices.notificationsHelper.getPlayerDisplayName(playerEntity.getGameProfile().name()));
        if (this.setting4.getValue().booleanValue() && (i = ClientServices.logoutSpotsHelper.getPopCount(playerEntity)) > 0) {
            sb.append(NameTags.getTotemPopColor(i)).append(" -").append(i);
        }
        if (this.setting3.getValue().booleanValue()) {
            sb.append(ChatFormatting.WHITE).append(" ").append(Math.round(minecraftClient.player.distanceTo((Entity) playerEntity))).append("m");
        }
        if (this.setting7.getValue().booleanValue()) {
            String str = "G";
            Iterator it = java.util.List.of(playerEntity.getItemBySlot(EquipmentSlot.HEAD), playerEntity.getItemBySlot(EquipmentSlot.CHEST), playerEntity.getItemBySlot(EquipmentSlot.LEGS), playerEntity.getItemBySlot(EquipmentSlot.FEET)).iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ItemStack itemStack = (ItemStack) it.next();
                Item item = itemStack.getItem();
                if (isDiamondOrNetheriteArmor(item)) {
                    if (!itemStack.isEnchanted()) {
                        str = "";
                    }
                } else {
                    if (itemStack.getItem() == Items.ELYTRA) {
                        str = "W";
                        break;
                    }
                    str = "";
                }
            }
            if (!str.isEmpty()) {
                sb.append(ChatFormatting.WHITE);
                sb.append(" [");
                sb.append(str);
                sb.append("]");
            }
        }
        return sb.toString();
    }

    public ChatFormatting getHealthColor(double d) {
        return d >= 20.0d ? ChatFormatting.GREEN : d >= 16.0d ? ChatFormatting.DARK_GREEN : d >= 10.0d ? ChatFormatting.GOLD : d >= 4.0d ? ChatFormatting.RED : ChatFormatting.DARK_RED;
    }

    public double getSortValue(Entity entity) {
        return this.setting.getValue() == TextRadarMode.DISTANCE ? entity.distanceToSqr(minecraftClient.player) : Mth.degreesDifferenceAbs(minecraftClient.gameRenderer.mainCamera().yRot(), RotationManager.getRotationsTo(entity)[0]);
    }

    private static boolean isDiamondOrNetheriteArmor(Item item) {
        return item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS
                || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
    }

    public boolean shouldShowPlayer(Player playerEntity) {
        if (!isInHudEditor() && minecraftClient.player == playerEntity) {
            return false;
        }
        if (this.setting11.getValue().booleanValue() && ClientServices.socialManager.isFriendEntity(playerEntity)) {
            return false;
        }
        return !this.setting12.getValue().booleanValue() || PlayerUtils.hasArmor((LivingEntity) playerEntity);
    }
}