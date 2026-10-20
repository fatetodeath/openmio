package me.mioclient.module.misc;

import me.mioclient.event.events.network.PacketReceiveEvent;
import me.mioclient.event.events.network.PacketSendEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.util.text.StringTemplate;

import me.mioclient.util.client.ChatUtils;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.time.Stopwatch;
import me.mioclient.util.world.BlockPlacer;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.events.interaction.BreakBlockEvent;
import me.mioclient.event.events.player.FinishUsingEvent;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.chat.Component;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.NumberSetting;

public class Announcer extends Module {
    public Setting<Boolean> clientSide = add(new BooleanSetting("ClientSide", false).setVisible());
    public Setting<Boolean> compact = add(new BooleanSetting("Compact", false).requiresSettings(this.clientSide));
    public Setting<Boolean> joins = add(new BooleanSetting("Joins", false).setVisible());
    public Setting<Boolean> friends = add(new BooleanSetting("Friends", true).requiresSettings(this.joins));
    public Setting<Boolean> enemies = add(new BooleanSetting("Enemies", true).requiresSettings(this.joins));
    public Setting<Boolean> others = add(new BooleanSetting("Others", true).requiresSettings(this.joins));
    public Setting<Boolean> move = add(new BooleanSetting("Move", true));
    public Setting<Boolean> break_ = add(new BooleanSetting("Break", true));
    public Setting<Boolean> eat = add(new BooleanSetting("Eat", true));
    public Setting<Double> delay = add(new NumberSetting<>("Delay", 10.0, 1.0, 30.0).setSuffix("s"));
    public final Stopwatch stopwatch;
    public double val;
    public int num;
    public int num2;

    public Announcer() {
        super("Announcer", "Lets the others know what you're doing.", Category.MISC, new String[0]);
        this.stopwatch = new Stopwatch();
        setDrawn(false);
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        this.num = 0;
        this.num2 = 0;
        this.stopwatch.reset();
    }

    @Listen
    public void onPacketSend(PacketSendEvent sendImmediatelyEvent) {
        if (sendImmediatelyEvent.getPacket() instanceof ServerboundChatPacket) {
            this.stopwatch.reset();
        }
    }

    @Listen
    public void onPacketReceive(PacketReceiveEvent channelRead0Event) {
        if (this.joins.getValue().booleanValue()) {
            if (this.stopwatch.hasElapsed(this.delay.getValue().doubleValue(), TimeUnit.SECONDS)) {
                ClientboundPlayerInfoUpdatePacket packet904 = (channelRead0Event.getPacket()) instanceof ClientboundPlayerInfoUpdatePacket ? (ClientboundPlayerInfoUpdatePacket) (channelRead0Event.getPacket()) : null;
                if (packet904 instanceof ClientboundPlayerInfoUpdatePacket) {
                    for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet904.entries()) {
                        if (entry.profile() == null) {
                            continue;
                        }
                        if (entry.listed()) {
                            String name = entry.profile().name();
                            if (!name.equals(minecraftClient.player.getName().getString())) {
                                boolean isFriend = ClientServices.socialManager.isFriend(name);
                                boolean isEnemy = ClientServices.socialManager.isEnemy(name);
                                if (this.friends.getValue().booleanValue() || !isFriend) {
                                    if (this.enemies.getValue().booleanValue() || !isEnemy) {
                                        if (this.others.getValue().booleanValue() || isFriend || isEnemy) {
                                            if (this.clientSide.getValue().booleanValue() && this.compact.getValue().booleanValue()) {
                                                String string2921 = new StringTemplate().arg(name).format("\u0001 joined");
                                                if (isFriend) {
                                                    string2921 = new StringTemplate().arg(string2921).format("\u0001 [friend]");
                                                } else if (isEnemy) {
                                                    string2921 = new StringTemplate().arg(string2921).format("\u0001 [enemy]");
                                                }
                                                String str = string2921;
                                                minecraftClient.execute(() -> {
                                                    sendChatMessage(str, ChatUtils.createSignature(-254721));
                                                });
                                            } else {
                                                if (isFriend) {
                                                    name = new StringTemplate().arg(name).format("My friend \u0001");
                                                }
                                                String str2 = name;
                                                minecraftClient.execute(() -> {
                                                    sendChatMessage(new StringTemplate().arg(str2).format("\u0001 has joined the game!"), ChatUtils.createSignature(-254721));
                                                });
                                            }
                                            this.stopwatch.reset();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ClientboundPlayerInfoRemovePacket packet9042 = (channelRead0Event.getPacket()) instanceof ClientboundPlayerInfoRemovePacket ? (ClientboundPlayerInfoRemovePacket) (channelRead0Event.getPacket()) : null;
                if (packet9042 instanceof ClientboundPlayerInfoRemovePacket) {
                    for (UUID uuid : packet9042.profileIds()) {
                        for (PlayerInfo playerListEntry : minecraftClient.player.connection.getListedOnlinePlayers()) {
                            if (playerListEntry.getProfile().id().equals(uuid)) {
                                String name2 = playerListEntry.getProfile().name();
                                if (this.clientSide.getValue().booleanValue() && this.compact.getValue().booleanValue()) {
                                    String string29212 = new StringTemplate().arg(name2).format("\u0001 left");
                                    if (ClientServices.socialManager.isFriend(string29212)) {
                                        string29212 = new StringTemplate().arg(string29212).format("\u0001 [friend]");
                                    } else if (ClientServices.socialManager.isEnemy(string29212)) {
                                        string29212 = new StringTemplate().arg(string29212).format("\u0001 [enemy]");
                                    }
                                    String str3 = string29212;
                                    minecraftClient.execute(() -> {
                                        sendChatMessage(str3, ChatUtils.createSignature(-254722));
                                    });
                                } else {
                                    if (ClientServices.socialManager.isFriend(name2)) {
                                        name2 = new StringTemplate().arg(name2).format("My friend \u0001");
                                    }
                                    String str4 = name2;
                                    minecraftClient.execute(() -> {
                                        sendChatMessage(new StringTemplate().arg(str4).format("\u0001 has left the game!"), ChatUtils.createSignature(-254722));
                                    });
                                }
                                this.stopwatch.reset();
                            }
                        }
                    }
                }
            }
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (isNotInGame()) {
            return;
        }
        this.val += (ClientServices.feetPlaceHelper.getInstantSpeed() / 3.6d) / 20.0d;
        if (this.val >= 1000.0d) {
            this.val = 0.0d;
        }
        if (!this.move.getValue().booleanValue() || this.val < 1.0d) {
            return;
        }
        if (this.stopwatch.hasElapsedMs((long) (1000.0d * this.delay.getValue().doubleValue()))) {
            sendModuleMessage(getRandomMoveMessage().replace("{blocks}", new DecimalFormat("0.00").format(this.val)));
            this.val = 0.0d;
            this.stopwatch.reset();
        }
    }

    @Listen
    public void onFinishUsing(FinishUsingEvent finishUsingEvent) {
        if (isNotInGame()) {
            return;
        }
        int i = MathUtils.randomInt(1, 6);
        if (this.eat.getValue().booleanValue()) {
            if (finishUsingEvent.getItemStack().has(DataComponents.FOOD)) {
                this.num++;
                if (this.num >= i) {
                    if (this.stopwatch.hasElapsedMs((long) (1000.0d * this.delay.getValue().doubleValue()))) {
                        String itemName = finishUsingEvent.getItemStack().getItem().toString();
                        sendModuleMessage(getRandomEatMessage().replace("{amount}", String.valueOf(this.num)).replace("{name}", itemName));
                        this.num = 0;
                        this.stopwatch.reset();
                    }
                }
            }
        }
    }

    @Listen
    public void onBreakBlock(BreakBlockEvent breakBlockEvent) {
        if (isNotInGame()) {
            return;
        }
        int i = MathUtils.randomInt(1, 6);
        this.num2++;
        if (this.break_.getValue().booleanValue() && this.num2 >= i && this.stopwatch.hasElapsedMs((long) (1000.0d * this.delay.getValue().doubleValue()))) {
            String[] strArr = {BlockPlacer.getBlockAt(breakBlockEvent.getBlockPos()).toString(), "Air", "Bedrock", "Barrier"};
            sendModuleMessage(getRandomBreakMessage().replace("{amount}", String.valueOf(this.num2)).replace("{name}", strArr[new Random().nextInt(strArr.length)]));
            this.num2 = 0;
            this.stopwatch.reset();
        }
    }

    public String getRandomMoveMessage() {
        String[] strArr = {"I just flew over {blocks} blocks thanks to me.mioclient.me!", "Ð¯ Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ñ‡Ñ‚Ð¾ Ð¿Ñ€Ð¾Ð»ÐµÑ‚ÐµÐ» Ð½Ð°Ð´ {blocks} Ð±Ð»Ð¾ÐºÐ°Ð¼Ð¸ Ñ Ð¿Ð¾Ð¼Ð¾Ñ‰ÑŒÑŽ me.mioclient.me!", "me.mioclient.me sayesinde {blocks} blok uÃ§tum!", "æˆ‘åˆšåˆšç”¨ me.mioclient.me èµ°äº† {blocks} ç±³!", "Dank me.mioclient.me bin ich gerade Ã¼ber {blocks} BlÃ¶cke geflogen!", "Jag hoppade precis Ã¶ver {blocks} blocks tack vare me.mioclient.me!", "WÅ‚aÅ›nie przeleciaÅ‚em ponad {blocks} bloki dziÄ™ki me.mioclient.me!", "Es tikko nolidoju {blocks} blokus, paldies me.mioclient.me!", "Ð¯ Ñ‰Ð¾Ð¹Ð½Ð¾ Ð¿Ñ€Ð¾Ð»ÐµÑ‚Ñ–Ð² Ð¿Ð¾Ð½Ð°Ð´ {blocks} Ð±Ð»Ð¾ÐºÐ°Ð¼Ð¸ Ð·Ð°Ð²Ð´ÑÐºÐ¸ me.mioclient.me!", "I just fwew ovew {blocks} bwoccs thanks to miocwient.me! :3", "Ho appena camminato per {blocks} blocchi grazie a me.mioclient.me!", "×¢×›×©×™×• ×¢×¤×ª×™ {blocks} ×”×•×“×•×ª ×œ me.mioclient.me!", "PrÃ¡vÄ› jsem proletÄ›l {blocks} blokÅ¯ dÃ­ky me.mioclient.me!", "Lensin juuri yli {blocks} blokkia me.mioclient.me:n ansiosta!", "Ravnokar sem preletel {blocks} blokov v zahvali me.mioclient.me!", "Ø£Ù†Ø§ Ù‡Ù„Ù‚ Ø·Ø§ÙŠØ± ÙÙˆÙ‚ {blocks} Ø¨Ù„ÙˆÙƒØ³ Ø¨ÙØ¶Ù„ me.mioclient.me!"};
        return strArr[new Random().nextInt(strArr.length)];
    }

    public String getRandomBreakMessage() {
        String[] strArr = {"I just destroyed {amount} {name} with the power of me.mioclient.me!", "Ð¯ Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ñ‡Ñ‚Ð¾ Ñ€Ð°Ð·Ñ€ÑƒÑˆÐ¸Ð» {amount} {name} Ñ Ð¿Ð¾Ð¼Ð¾Ñ‰ÑŒÑŽ me.mioclient.me!", "Az Ã¶nce {amount} tane {name} kÄ±rdÄ±m. TeÅžekkÃ¼rler me.mioclient.me!", "æˆ‘åˆšåˆšç”¨ me.mioclient.me ç ´åäº† {amount} {name}!", "Ich habe gerade {amount} {name} mit der Kraft von me.mioclient.me zerstÃ¶rt!", "Jag fÃ¶rstÃ¶rde precis {amount} {name} tack vare me.mioclient.me!", "WÅ‚aÅ›nie zniszczyÅ‚em {amount} {name} za pomocÄ… me.mioclient.me", "Es tikko salauzu {amount} {name} ar spÄ“ku me.mioclient.me!", "Ð¯ Ñ‰Ð¾Ð¹Ð½Ð¾ Ð·Ð½Ð¸Ñ‰Ð¸Ð² {amount} {name} Ð·Ð° Ð´Ð¾Ð¿Ð¾Ð¼Ð¾Ð³Ð¾ÑŽ me.mioclient.me!", "I just destwoyed {amount} {name} with the powew of miocwient.me! :3", "Ho appena distrutto {amount} {name} grazie al potere di me.mioclient.me!", "×‘×“×™×•×§ ×—×¦×‘×ª×™ {amount} {name} ×‘×¢×–×¨×ª ×”×›×•×— ×©×œ me.mioclient.me!", "PrÃ¡vÄ› jsem zniÄil {amount} {name} dÃ­ky me.mioclient.me!", "Rikoin juuri {amount} {name}ia me.mioclient.me:n ansiosta!", "Ravnokar sem uniÄil {amount} {name} z moÄjo me.mioclient.me", "Ø£Ù†Ø§ Ù‡Ù„Ù‚ Ø¯Ù…Ù‘Ø±Øª {amount} {name} Ø¨Ù‚ÙˆØ© me.mioclient.me!"};
        return strArr[new Random().nextInt(strArr.length)];
    }

    public String getRandomEatMessage() {
        String[] strArr = {"I just ate {amount} {name} thanks to me.mioclient.me!", "Ð¯ Ñ‚Ð¾Ð»ÑŒÐºÐ¾ Ñ‡Ñ‚Ð¾ ÑÑŠÐµÐ» {amount} {name} Ñ Ð¿Ð¾Ð¼Ð¾Ñ‰ÑŒÑŽ me.mioclient.me!", "Tam olarak {amount} tane {name} yedim. TeÅžekkÃ¼rler me.mioclient.me", "æˆ‘åˆšç”¨ me.mioclient.me åƒäº† {amount} ä¸ª {name}!", "Ich habe gerade {amount} {name} dank me.mioclient.me gegessen!", "Jag Ã¥t precis {amount} {name} tack vare me.mioclient.me", "WÅ‚aÅ›nie zjadÅ‚em {amount} {name} dziÄ™ki me.mioclient.me", "Es tikko apÄ“du {amount} {name} ar me.mioclient.me spÄ“ku!", "Ð¯ Ñ‰Ð¾Ð¹Ð½Ð¾ Ð·â€™Ñ—Ð² {amount} {name} Ð·Ð°Ð²Ð´ÑÐºÐ¸ me.mioclient.me!", "I just ate {amount} {name} thanks to miocwient.me! ^-^", "Ho appena mangiato {amount} {name} grazie a me.mioclient.me!", "×›×¨×’×¢ ××›×œ×ª×™ {amount} {name} ×”×•×“×•×ª ×œmioclient.me!", "PrÃ¡vÄ› jsem snÄ›dl {amount} {name} dÃ­ky me.mioclient.me", "SÃ¶in juuri {amount} {name}a me.mioclient.me:n ansiosta!", "Ravnokar sem pojedel {amount} {name} v zahvali me.mioclient.me", "Ø£Ù†Ø§ Ù‡Ù„Ù‚ Ø£ÙƒÙ„Øª {amount} {name} Ø¨ÙØ¶Ù„ miocliet.me!"};
        return strArr[new Random().nextInt(strArr.length)];
    }

    public void sendModuleMessage(String str) {
        sendChatMessage(str, ChatUtils.signatureOfModule(this));
    }

    public void sendChatMessage(String str, MessageSignature messageSignature) {
        if (!this.clientSide.getValue().booleanValue()) {
            minecraftClient.player.connection.sendChat(str);
        } else {
            // 26.2: Style#withColor(int) still available; keep client-side echo plain (matches chat HUD path).
            ChatUtils.sendMessage(Component.literal(str), messageSignature);
        }
    }
}