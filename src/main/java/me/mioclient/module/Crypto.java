package me.mioclient.module;

import me.mioclient.api.NamedValue;

import me.mioclient.render.hud.HudTextLine;
import me.mioclient.render.hud.TextHudElement;
import me.mioclient.util.time.Stopwatch;

import me.mioclient.api.ClientAccessor;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.StringSetting;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class Crypto extends me.mioclient.module.HudModule {
    public static final String string = "MioClient/2.0";
    public Setting<String> setting;
    public Setting<CryptoMode> setting2;
    public final HttpClient httpClient;
    public String string2;
    public ChatFormatting formatting;
    public final Stopwatch stopwatch;

    
    private enum CryptoMode implements NamedValue {
        USD("USD"),
        EUR("EUR"),
        RUB("RUB"),
        CNY("CNY"),
        TRY("TRY"),
        JPY("JPY"),
        PLN("PLN"),
        BRL("BRL");

        public final String name;

        CryptoMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }
    }

    public Crypto() {
        super("Crypto", new String[0]);
        this.setting = add(new StringSetting("Coin", "BTC"));
        this.setting2 = add(new EnumSetting("Fiat", CryptoMode.USD));
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
        this.string2 = "0.00";
        this.formatting = ChatFormatting.WHITE;
        this.stopwatch = new Stopwatch();
        setElement(new TextHudElement(this, new HudTextLine(() -> {
            return Component.literal("%s %s%s".formatted(this.setting.getValue().toUpperCase(), this.formatting, this.string2));
        }, () -> {
            return true;
        })));
        this.setting.setChangeCallback(() -> {
            this.formatting = ChatFormatting.WHITE;
            this.string2 = "0.00";
            if (isValidCoinSymbol()) {
                this.stopwatch.setTime(5000L);
            } else {
                this.setting.setValue("UNKNOWN");
            }
        });
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (this.stopwatch.hasElapsed(5.0d, TimeUnit.SECONDS)) {
            executorService.submit(this::fetchExchangeRate);
            this.stopwatch.reset();
        }
    }

    public void fetchExchangeRate() {
        if (!isValidCoinSymbol()) {
            this.setting.setValue("UNKNOWN");
            this.string2 = "0.00";
            this.formatting = ChatFormatting.WHITE;
            return;
        }
        String upperCase = this.setting.getValue().toUpperCase(Locale.ROOT);
        String upperCase2 = this.setting2.getValue().getName().toUpperCase(Locale.ROOT);
        try {
            HttpResponse send = this.httpClient.send(HttpRequest.newBuilder().GET().uri(new URI("https://api.coinconvert.net/convert/%s/%s?amount=1".formatted(upperCase, upperCase2))).header("Accept", "application/json").header("User-Agent", "MioClient/2.0").build(), HttpResponse.BodyHandlers.ofString());
            if (send.statusCode() != 200) {
                this.string2 = "UNKNOWN";
                this.formatting = ChatFormatting.WHITE;
                return;
            }
            JsonObject jsonObject = (JsonObject) ClientAccessor.gson.fromJson((String) send.body(), JsonObject.class);
            if (jsonObject.has("status") && jsonObject.has(upperCase2)) {
                String formatted = "%.2f".formatted(Float.valueOf(jsonObject.get(upperCase2).getAsFloat()));
                float parseFloat = Float.parseFloat(formatted);
                if (!this.string2.equalsIgnoreCase("0.00") && !this.string2.equalsIgnoreCase("UNKNOWN")) {
                    do213(parseFloat);
                }
                this.string2 = formatted;
            }
        } catch (Exception e) {
        }
    }

    public void do213(float f) {
        float parseFloat = Float.parseFloat(this.string2);
        if (Math.abs(f - parseFloat) < 0.01d) {
            this.formatting = ChatFormatting.WHITE;
        } else if (parseFloat > f) {
            this.formatting = ChatFormatting.RED;
        } else if (parseFloat < f) {
            this.formatting = ChatFormatting.GREEN;
        }
    }

    public boolean isValidCoinSymbol() {
        String value = this.setting.getValue();
        return !value.isEmpty() && value.matches("[a-zA-Z]+");
    }
}