package me.mioclient.module.client;

import me.mioclient.event.events.entity.TotemPopEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.io.FileIO;

import me.mioclient.manager.ConfigManager;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import me.mioclient.render.RenderConstants;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.client.EnableEvent;
import me.mioclient.event.Listen;
import me.mioclient.module.Module;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.apache.commons.io.FileUtils;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.setting.StringSetting;

public class Notifications extends Module {
    /* 26.2 port: resolved lazily — eager static init ran before the module registry was ready, leaving iRC null. */
    private static IRC iRC;

    public static IRC getIRC() {
        if (iRC == null && ClientServices.moduleInjector != null) {
            try {
                iRC = (IRC) ClientServices.moduleInjector.getModule(IRC.class);
            } catch (Exception e) {
            }
        }
        return iRC;
    }
    public Setting<Color> watermark = add(new ColorSetting("Watermark", new Color(-4351489, true)).setThemeSync(true));
    public Setting<Color> brackets = add(new ColorSetting("Brackets", new Color(-4351489, true)).setThemeSync(true));
    public Setting<Boolean> modules = add(new BooleanSetting("Modules", true));
    public Setting<Boolean> totemPops = add(new BooleanSetting("TotemPops", false).setVisible());
    public Setting<Boolean> self = add(new BooleanSetting("Self", false).requiresSettings(this.totemPops));
    public Setting<Color> primary = add(new ColorSetting("Primary", new Color(-2828, true), v -> this.totemPops.isShown()));
    public Setting<Color> secondary = add(new ColorSetting("Secondary", new Color(-4351489, true), v -> this.totemPops.isShown()).setThemeSync(true));
    public Setting<Boolean> sayChat = add(new BooleanSetting("SayChat", false, v -> this.totemPops.isShown()).setVisible());
    public Setting<String> path = add(new StringSetting("Path", "totempop.txt", v -> this.totemPops.isShown() && this.sayChat.isShown()));
    public Setting<Float> delay = add(new NumberSetting<>("Delay", 0.0f, 0.0f, 15.0f, v -> this.totemPops.isShown() && this.sayChat.isShown()).setSuffix("s"));
    public Setting<Boolean> random = add(new BooleanSetting("Random", true, v -> this.totemPops.isShown() && this.sayChat.isShown()));
    public Setting<Boolean> refresh = add(new BooleanSetting("Refresh", false, v -> this.totemPops.isShown() && this.sayChat.isShown()));
    public int num;
    public final List<String> list;
    public final Random random2;

    public Notifications() {
        super("Notifications", "Lets you know about what's going on in chat.", Category.CLIENT, new String[0]);
        this.num = 0;
        this.list = new ArrayList();
        this.random2 = new Random();
        setDrawn(false);
        this.sayChat.setChangeCallback(() -> {
            if (this.sayChat.getValue().booleanValue()) {
                reloadTotemPopFile();
            }
        });
        this.refresh.setChangeCallback(() -> {
            if (this.refresh.getValue().booleanValue()) {
                this.refresh.setValue(false);
                reloadTotemPopFile();
            }
        });
        this.random.setChangeCallback(() -> {
            this.num = 0;
        });
        this.path.setChangeCallback(this::reloadTotemPopFile);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        reloadTotemPopFile();
    }

    @Listen
    public void onTotemPop(TotemPopEvent playerEntityEvent) {
        boolean z = minecraftClient.player == playerEntityEvent.getPlayer();
        if (this.totemPops.getValue().booleanValue()) {
            if (!z || this.self.getValue().booleanValue()) {
                String name = z ? "You" : playerEntityEvent.getName();
                int i = playerEntityEvent.getPops();
                if (playerEntityEvent.getType() != TotemPopEvent.LogoutSpotsHelperMode.TOTEM_POP) {
                    sendTotemPopMessage(name, " has died after popping ", i);
                    return;
                }
                sendTotemPopMessage(name, new StringTemplate().arg(z ? " have" : " has").format("\u0001 popped "), i);
                if (ClientServices.socialManager.isFriend(name) || z || !this.sayChat.getValue().booleanValue() || this.list.isEmpty()) {
                    return;
                }
                executorService.submit(() -> {
                    try {
                        if (this.delay.getValue().floatValue() > 0.0f) {
                            Thread.sleep((long) ((this.delay.getValue().floatValue() * 1000.0f) + this.random2.nextLong(500L)));
                        }
                        String str = null;
                        if (!this.random.getValue().booleanValue()) {
                            str = this.list.get(this.num);
                            this.num = (this.num + 1) % this.list.size();
                        } else if (this.list.size() == 1) {
                            str = this.list.get(0);
                        } else {
                            while (str == null) {
                                int nextInt = this.random2.nextInt(this.list.size());
                                if (this.num != nextInt) {
                                    this.num = nextInt;
                                    str = this.list.get(nextInt);
                                }
                            }
                        }
                        String replace = str.replace("{name}", name).replace("{totems}", Integer.toString(i));
                        IRC iRC_lcl = getIRC();
                        if (iRC_lcl == null || !iRC_lcl.isToggled() || !replace.startsWith(iRC_lcl.prefix.getValue())) {
                            ChatUtils.sendChatOrCommand(replace);
                        }
                    } catch (Exception e) {
                    }
                });
            }
        }
    }

    @Listen
    public void onModuleToggle(EnableEvent enableEvent) {
        if (isNotInGame() || !this.modules.getValue().booleanValue() || (enableEvent.getModule() instanceof HUD)) {
            return;
        }
        MutableComponent empty = Component.empty();
        if (enableEvent.getModule().isToggled()) {
            empty.append(Component.literal("[").withStyle(style -> {
                return style.withColor(getPopStateColor(true).darker().hashCode());
            }));
            empty.append(Component.literal("+").withStyle(style2 -> {
                return style2.withColor(getPopStateColor(true).hashCode());
            }));
            empty.append(Component.literal("] ").withStyle(style3 -> {
                return style3.withColor(getPopStateColor(true).darker().hashCode());
            }));
        } else {
            empty.append(Component.literal("[").withStyle(style4 -> {
                return style4.withColor(getPopStateColor(false).darker().hashCode());
            }));
            empty.append(Component.literal("-").withStyle(style5 -> {
                return style5.withColor(getPopStateColor(false).hashCode());
            }));
            empty.append(Component.literal("] ").withStyle(style6 -> {
                return style6.withColor(getPopStateColor(false).darker().hashCode());
            }));
        }
        empty.append(Component.literal(ClientServices.notificationsHelper.getModuleDisplayName(enableEvent.getModule())));
        ClientServices.tooltipsHelper.schedule(() -> {
            ChatUtils.sendMessage((Component) empty, ChatUtils.signatureOfModule(enableEvent.getModule()));
        }, 0);
    }

    public String getTotemCountSuffix(int i) {
        return i == 1 ? " totem." : " totems.";
    }

    public void sendTotemPopMessage(String str, String str2, int i) {
        if (minecraftClient.options.chatVisibility().get() == ChatVisiblity.HIDDEN) {
            return;
        }
        ChatUtils.sendPrefixedMessage(Component.empty().append(Component.literal(str).withStyle(style -> {
            return style.withColor(this.primary.getValue().hashCode());
        })).append(Component.literal(str2).withStyle(style2 -> {
            return style2.withColor(this.secondary.getValue().hashCode());
        })).append(Component.literal(String.valueOf(i)).withStyle(style3 -> {
            return style3.withColor(this.primary.getValue().hashCode());
        })).append(Component.literal(getTotemCountSuffix(i)).withStyle(style4 -> {
            return style4.withColor(this.secondary.getValue().hashCode());
        })), ChatUtils.createSignature(Math.abs(str.hashCode()) * (-1)), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode2);
    }

    public Color getPopStateColor(boolean z) {
        return z ? new Color(0, 190, 50) : new Color(RenderConstants.num, 0, 0);
    }

    public Color getWatermarkColor() {
        return this.watermark.getValue();
    }

    public Color getBracketColor() {
        return this.brackets.getValue();
    }

    public void reloadTotemPopFile() {
        try {
            Path path1566 = FileIO.resolvePath(ConfigManager.path.resolve(this.path.getValue()), ".txt");
            if (!path1566.toFile().exists()) {
                throw new RuntimeException("Invalid totem pop file path");
            }
            this.num = 0;
            this.list.clear();
            this.list.addAll(FileUtils.readLines(path1566.toFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            if (this.sayChat.getValue().booleanValue()) {
                ChatUtils.sendPrefixedMessage(Component.literal("Failed to open totem pop file").withStyle(style -> {
                    return style.withColor(ChatFormatting.RED);
                }), ChatUtils.createSignature(e.toString().hashCode()), ChatUtils.MixinClientConnectionMode.mixinClientConnectionMode3);
                e.printStackTrace();
            }
        }
    }
}