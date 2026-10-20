package me.mioclient.module.client;

import me.mioclient.event.events.client.CoordLogReceiveEvent;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.setting.SoundIdentifierSetting;
import me.mioclient.util.client.SoundIdentifier;

import me.mioclient.network.irc.CoordLogEntry;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.math.Vec3dUtils;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.KillstreakSoundManager;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.event.events.network.ChatEvent;
import me.mioclient.module.Module;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;
import me.mioclient.setting.StringSetting;

public class IRC extends Module {
    public static final Identifier identifier = Identifier.fromNamespaceAndPath("mio", "textures/warning.png");
    public Setting<Boolean> capes = add(new BooleanSetting("Capes", true));
    public Setting<Boolean> gradient = add(new BooleanSetting("Gradient", true));
    public Setting<Boolean> chat = add(new BooleanSetting("Chat", true).setVisible());
    public Setting<String> prefix = add(new StringSetting("Prefix", "!!").requiresSettings(this.chat));
    public Setting<Boolean> chatSound = add(new BooleanSetting("ChatSound", false).setVisible().requiresSettings(this.chat));
    public Setting<SoundIdentifier> mode = add(new SoundIdentifierSetting("Mode", new SoundIdentifier("system", "steam"), v -> this.chat.isShown() && this.chatSound.isShown()));
    public Setting<Float> volume2 = add(new NumberSetting<>("Volume", 0.5f, 0.0f, 1.0f, v -> this.chat.isShown() && this.chatSound.isShown()));
    public Setting<Boolean> pings = add(new BooleanSetting("Pings", true).setVisible());
    public Setting<IRCMode> info = add(new EnumSetting<>("Info", IRCMode.DISTANCE).requiresSettings(this.pings));
    public Setting<Boolean> pingSound = add(new BooleanSetting("PingSound", false).setVisible().requiresSettings(this.pings));
    public Setting<SoundIdentifier> mode2 = add(new SoundIdentifierSetting("Mode", new SoundIdentifier("system", "css")).requiresSettings(this.pings, this.pingSound));
    public Setting<Float> volume = add(new NumberSetting<>("Volume", 0.5f, 0.0f, 1.0f).requiresSettings(this.pings, this.pingSound));
    public int num;

    
    public enum IRCMode implements NamedValue {
        DISTANCE("Distance"),
        COORDS("Coords"),
        NONE("None");

        public final String name;

        IRCMode(String str) {
            this.name = str;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public String getString2864(CoordLogEntry spawnTimeHelper) {
            if (this == DISTANCE) {
                return "%.1fm".formatted(Double.valueOf(ClientAccessor.minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(spawnTimeHelper.getBlockPos()))));
            }
            Vec3 centerPos = Vec3.atCenterOf(spawnTimeHelper.getBlockPos());
            return "X:%.1f, Y:%.1f, Z:%.1f".formatted(Double.valueOf(centerPos.x), Double.valueOf(centerPos.y), Double.valueOf(centerPos.z));
        }

        public boolean is594() {
            return this != NONE;
        }
    }

    
    public enum Mode implements NamedValue {
        NONE("none", null),
        MIO("mio", Identifier.fromNamespaceAndPath("mio", "capes/mio.png")),
        PEPSI("pepsi", Identifier.fromNamespaceAndPath("mio", "capes/pepsi.png")),
        NIGHTLY("nightly", Identifier.fromNamespaceAndPath("mio", "capes/nightly.png")),
        TETRIS("tetris", Identifier.fromNamespaceAndPath("mio", "capes/tetris.png"));

        public final String name;
        public final Identifier identifier;

        Mode(String str, Identifier identifier) {
            this.name = str;
            this.identifier = identifier;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public Identifier getIdentifier2642() {
            return this.identifier;
        }
    }

    public IRC() {
        super("IRC", "Client user chat.", Category.CLIENT, new String[0]);
        setDrawn(false);
        this.capes.setChangeCallback(() -> {
            ClientServices.nameTagsManager.addFriend(getCapeTexture());
        });
        this.mode.setConfigName("C-SoundMode");
        this.volume2.setConfigName("C-SoundVolume");
        this.mode2.setConfigName("P-SoundMode");
        this.volume.setConfigName("P-SoundVolume");
    }

    @Override // me.mioclient.module.Module
    public void onEnable() {
        ClientServices.nameTagsManager.connectToServer();
    }

    @Override // me.mioclient.module.Module
    public void onDisable() {
        ClientServices.nameTagsManager.disconnectFromServer();
    }

    @Listen
    public void onCoordLogReceive(CoordLogReceiveEvent spawnTimeHelperEvent) {
        if (((float) minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(spawnTimeHelperEvent.getEntry().getBlockPos()))) >= 9999.0f) {
            spawnTimeHelperEvent.cancel();
            return;
        }
        if (this.pingSound.getValue().booleanValue() && spawnTimeHelperEvent.getEntry().isOnSameServer()) {
            KillstreakSoundManager KillstreakSoundManager = ClientServices.killstreakSoundManager;
            SoundIdentifier value = this.mode2.getValue();
            KillstreakSoundManager.playSoundAt(value, Vec3.atCenterOf(spawnTimeHelperEvent.getEntry().getBlockPos()), this.volume.getValue().floatValue());
        }
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        ClientServices.nameTagsManager.getCoordLogEntries().removeIf(spawnTimeHelper -> {
            return System.currentTimeMillis() - spawnTimeHelper.getSpawnTime() > 7500;
        });
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (this.pings.getValue().booleanValue()) {
            synchronized (ClientServices.nameTagsManager.getCoordLogEntries()) {
                for (CoordLogEntry spawnTimeHelper : ClientServices.nameTagsManager.getCoordLogEntries()) {
                    if (spawnTimeHelper.isOnSameServer()) {
                        float distanceTo = (float) minecraftClient.player.getEyePosition().distanceTo(Vec3.atCenterOf(spawnTimeHelper.getBlockPos()));
                        if (distanceTo < 9999.0f) {
                            Vec3 of = Vec3.atCenterOf(spawnTimeHelper.getBlockPos());
                            Vec3 vec3d = of;
                            if (distanceTo >= MathConstants.num2) {
                                float[] floatArray2484 = RotationManager.getRotationsTo(of);
                                vec3d = new Vec3(minecraftClient.player.getX() + (70.0f * ((float) Math.cos(Math.toRadians(floatArray2484[0] + MathConstants.num2)))), of.y, minecraftClient.player.getZ() + (70.0f * ((float) Math.sin(Math.toRadians(floatArray2484[0] + MathConstants.num2)))));
                            }
                            float clamp = Mth.clamp(1.0f - (((float) ((System.currentTimeMillis() - spawnTimeHelper.getSpawnTime()) - 6500)) / 1000.0f), 0.0f, 1.0f);
                            float f = easeOutElastic(Mth.clamp(((float) (System.currentTimeMillis() - spawnTimeHelper.getSpawnTime())) / 1500.0f, 0.0f, 1.0f));
                            float f2 = cubic(clamp);
                            float min = Math.min(Vec3dUtils.getHorizontalDistance(minecraftClient.player.getPosition(inner_3.getTickDelta()), of), 6.0f) / 6.0f;
                            Vec3 add = vec3d.add(0.5d, 0.5d, 0.5d);
                            double d = MathUtils.getDistanceScale(minecraftClient.gameRenderer.mainCamera().position(), add, 3.0d);
                            BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), new AABB(add.add(-MathConstants.val3, -MathConstants.val3, -MathConstants.val3), add.add(MathConstants.val3, MathConstants.val3, MathConstants.val3)).setMaxY(Mth.lerp(f, add.y, minecraftClient.level.getMaxY())).setMinY(Mth.lerp(f, add.y, minecraftClient.level.getMinY())), ColorUtils.withAlphaPercent(ColorUtils.getGradientColor(Color.red, Color.yellow, 1000.0d, 0.0d), 0.3f * f * f2 * min));
                            RenderUtil.INSTANCE.drawTextureBillboard(inner_3.getDrawContext(), add, 256.0f, 256.0f, (d / 25.6d) * f * f2, ColorUtils.getGradientColor(Color.red, Color.yellow, 1000.0d, 0.0d), identifier);
                            if (this.info.getValue().is594()) {
                                String string2864 = this.info.getValue().getString2864(spawnTimeHelper);
                                me.mioclient.render.shape.BoxRenderer.submitNameTag(inner_3.getMatrixStack(), add, 0.0d, net.minecraft.network.chat.Component.literal(string2864), inner_3.cameraState, 1.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    public float easeOutElastic(float f) {
        double longBitsToDouble = (2.0d * MathConstants.val) / 3.0d;
        if (f == 0.0f) {
            return 0.0f;
        }
        return (float) (f == 1.0f ? 1.0d : (Math.pow(2.0d, (-10.0f) * f) * Math.sin(((f * 10.0f) - 0.75d) * longBitsToDouble)) + 1.0d);
    }

    public float cubic(float f) {
        return f * f * f;
    }

    @Listen
    public void onChat(ChatEvent event) {
        if (this.chat.getValue().booleanValue() && event.getMessage().length() > this.prefix.getValue().length()) {
            if (event.getMessage().startsWith(this.prefix.getValue())) {
                event.cancel();
                ClientServices.nameTagsManager.joinChannel(event.getMessage().substring(this.prefix.getValue().length()).trim());
            }
        }
    }

    public String getCapeTexture() {
        if (!this.capes.getValue().booleanValue()) {
            return "none";
        }
        switch (ClientServices.welcomerHelper.getRank()) {
            case 3:
                return "pepsi";
            case 5:
                return "tetris";
            default:
                return "mio";
        }
    }
}