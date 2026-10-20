package me.mioclient.module.render;

import me.mioclient.api.ClientServices;
import me.mioclient.api.NamedValue;
import me.mioclient.util.text.StringTemplate;
import me.mioclient.util.world.WorldUtils;

import me.mioclient.mode.setting.HoleSnapMode;
import me.mioclient.render.font.ClientTextRenderer;
import me.mioclient.render.shape.BoxRenderer;
import me.mioclient.util.math.ColorUtils;
import me.mioclient.util.math.MathConstants;
import me.mioclient.util.math.MathUtils;
import me.mioclient.util.math.Vec3dUtils;
import me.mioclient.util.world.Waypoint;

import me.mioclient.api.ClientAccessor;
import me.mioclient.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import me.mioclient.event.events.render.MatrixStackEvent;
import me.mioclient.manager.RotationManager;
import me.mioclient.api.Category;
import me.mioclient.api.Setting;
import me.mioclient.event.Listen;
import me.mioclient.event.events.client.TickEvent;
import me.mioclient.module.Module;
import me.mioclient.module.client.Fonts;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL32C;
import me.mioclient.setting.BooleanSetting;
import me.mioclient.setting.ColorSetting;
import me.mioclient.setting.EnumSetting;
import me.mioclient.setting.NumberSetting;

public class Waypoints extends Module {
    public static Fonts fonts = (Fonts) ClientServices.moduleInjector.getModule(Fonts.class);
    public Setting<Float> distance = add(new NumberSetting<>("Distance", 101.0f, 0.1f, 101.0f).setSuffix("k").setHoleSnapConfig("Unlimited", HoleSnapMode.MAX));
    public Setting<Float> lineWidth = add(new NumberSetting<>("LineWidth", 1.0f, 0.1f, 3.0f));
    public Setting<Boolean> box = add(new BooleanSetting("AABB", true));
    public Setting<Boolean> beam = add(new BooleanSetting("Beam", false));
    public Setting<Boolean> tracers = add(new BooleanSetting("Tracers", false));
    public Setting<Boolean> name = add(new BooleanSetting("Name", true));
    public Setting<WaypointsMode> info = add(new EnumSetting<>("Info", WaypointsMode.NONE, v -> this.name.getValue().booleanValue()));
    public Setting<Float> textScale = add(new NumberSetting<>("TextScale", 1.0f, 0.5f, 3.0f, v -> this.name.getValue().booleanValue()));
    public Setting<Boolean> textBackground = add(new BooleanSetting("TextBackground", true, v -> this.name.getValue().booleanValue()));
    public Setting<Boolean> eyesAlign = add(new BooleanSetting("EyesAlign", false).requiresSettings(this.name));
    public Setting<Color> color = add(new ColorSetting("Color", new Color(-457336321, true)).setThemeSync(true));
    public Setting<Integer> beamAlpha = add(new NumberSetting<>("BeamAlpha", 40, 0, 255));
    public final CopyOnWriteArrayList<Waypoint> copyOnWriteArrayList;

    
    private static enum WaypointsMode implements NamedValue {
        NONE("None"),
        waypointsMode("Coords") {
            @Override
            public String formatInfo(Vec3 vec3d) {
                return String.format(Locale.US, "X: %.1f, Y: %.1f, Z: %.1f", Double.valueOf(vec3d.x()), Double.valueOf(vec3d.y()), Double.valueOf(vec3d.z()));
            }
        },
        waypointsMode2("Distance") {
            @Override
            public String formatInfo(Vec3 vec3d) {
                return String.format("%.1fm", Double.valueOf(vec3d.distanceTo(ClientAccessor.minecraftClient.player.position()))).replaceAll(",", ".");
            }
        };

        public final String name;

        WaypointsMode(String str2) {
            this.name = str2;
        }

        @Override // me.mioclient.api.NamedValue
        public String getName() {
            return this.name;
        }

        public String formatInfo(Vec3 vec3d) {
            return "";
        }
    }

    public Waypoints() {
        super("Waypoints", "Renders certain places you mark using ;waypoints.", Category.RENDER, new String[0]);
        this.copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        this.distance.setHoleSnapConfig("Unlimited", HoleSnapMode.MAX);
        setDrawn(false);
    }

    @Listen
    public void onTick(TickEvent tickEvent) {
        if (minecraftClient.player.tickCount % 2 == 0) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (Waypoint waypointsEnumSettingHelper : ClientServices.waypointsManager.getRegistry()) {
            if (waypointsEnumSettingHelper != null && (waypointsEnumSettingHelper.getServer() == null || waypointsEnumSettingHelper.getDimension() == null || waypointsEnumSettingHelper.getName() == null)) {
                arrayList.add(waypointsEnumSettingHelper);
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            ClientServices.waypointsManager.unregister((Waypoint) it.next());
        }
        this.copyOnWriteArrayList.clear();
        for (Waypoint waypointsEnumSettingHelper2 : ClientServices.waypointsManager.getRegistry()) {
            if (waypointsEnumSettingHelper2 != null && waypointsEnumSettingHelper2.getDimension() != null && waypointsEnumSettingHelper2.getName() != null && waypointsEnumSettingHelper2.getServer() != null && waypointsEnumSettingHelper2.isToggled()) {
                if (minecraftClient.getCurrentServer() != null || !waypointsEnumSettingHelper2.getServer().equalsIgnoreCase("singleplayer")) {
                    if (minecraftClient.getCurrentServer() != null) {
                        if (waypointsEnumSettingHelper2.getServer().equalsIgnoreCase(minecraftClient.getCurrentServer().ip)) {
                        }
                    }
                }
                this.copyOnWriteArrayList.add(waypointsEnumSettingHelper2);
            }
        }
        this.copyOnWriteArrayList.sort(Comparator.comparing(waypointsEnumSettingHelper3 -> {
            return Double.valueOf(minecraftClient.player.distanceToSqr(waypointsEnumSettingHelper3.toVec3d()));
        }));
    }

    @Listen
    public void onRenderWorld(MatrixStackEvent.Inner_3 inner_3) {
        if (minecraftClient.gui.hud.isHidden()) {
            return;
        }
        Camera camera = minecraftClient.gameRenderer.mainCamera();
        Vec3 add = new Vec3(0.0d, 0.0d, 1.0d).xRot(-((float) Math.toRadians(camera.xRot()))).yRot(-((float) Math.toRadians(camera.yRot()))).add(minecraftClient.getEntityRenderDispatcher().camera.position());
        Iterator<Waypoint> it = this.copyOnWriteArrayList.iterator();
        while (it.hasNext()) {
            Waypoint next = it.next();
            if (next != null && next.getDimension() != null && next.getName() != null && next.getServer() != null) {
                if (next.toVec3d().distanceTo(minecraftClient.gameRenderer.mainCamera().position()) <= this.distance.getValue().floatValue() * 1000.0f || this.distance.getValue().floatValue() == 101.0f) {
                    Vec3 vec3d2100 = toCurrentDimension(next);
                    if (vec3d2100 != null) {
                        Vec3 vec3d = vec3d2100;
                        if (Vec3dUtils.getHorizontalDistance(minecraftClient.player.position(), vec3d2100) >= 120.0f) {
                            float[] floatArray2484 = RotationManager.getRotationsTo(vec3d2100);
                            Vec3 lerpedPos = minecraftClient.player.getPosition(RenderUtil.getPartialTick());
                            vec3d = new Vec3(lerpedPos.x + (100.0f * ((float) Math.cos(Math.toRadians(floatArray2484[0] + MathConstants.num2)))), vec3d2100.y, lerpedPos.z + (100.0f * ((float) Math.sin(Math.toRadians(floatArray2484[0] + MathConstants.num2)))));
                        }
                        if (this.beam.getValue().booleanValue()) {
                            BoxRenderer.drawFilledBox(inner_3.getMatrixStack(), new AABB(vec3d.add(-MathConstants.val3, -MathConstants.val3, -MathConstants.val3), vec3d.add(MathConstants.val3, MathConstants.val3, MathConstants.val3)).setMaxY(minecraftClient.level.getMaxY()).setMinY(minecraftClient.level.getMinY()), ColorUtils.withAlpha(this.color.getValue(), this.beamAlpha.getValue().intValue()));
                        }
                        if (this.box.getValue().booleanValue()) {
                            BoxRenderer.drawBoxOutline(inner_3.getMatrixStack(), new AABB(vec3d2100.add(-MathConstants.val3, -MathConstants.val3, -MathConstants.val3), vec3d2100.add(MathConstants.val3, MathConstants.val3, MathConstants.val3)), this.color.getValue(), this.lineWidth.getValue().floatValue());
                        }
                        if (this.tracers.getValue().booleanValue()) {
                            GL32C.glLineWidth(this.lineWidth.getValue().floatValue());
                            RenderUtil.INSTANCE.drawDebugLine(inner_3.getMatrixStack(), add, vec3d, this.color.getValue());
                            GL32C.glLineWidth(1.0f);
                        }
                        if (this.name.getValue().booleanValue()) {
                            if (this.eyesAlign.getValue().booleanValue()) {
                                vec3d = vec3d.with(Direction.Axis.Y, Mth.lerp(inner_3.getTickDelta(), minecraftClient.player.yo, minecraftClient.player.getY()) + minecraftClient.player.getEyeHeight(minecraftClient.player.getPose()));
                            }
                            String string2921 = new StringTemplate().arg(this.info.getValue().formatInfo(vec3d2100)).arg(next.getName()).format("\u0001 \u0001");
                            double d = MathUtils.getDistanceScale(minecraftClient.gameRenderer.mainCamera().position(), vec3d, this.textScale.getValue().floatValue());
                            me.mioclient.render.shape.BoxRenderer.submitNameTag(inner_3.getMatrixStack(), vec3d, 0.0d, net.minecraft.network.chat.Component.literal(string2921), inner_3.cameraState, this.textScale.getValue().floatValue());
                        }
                    }
                }
            }
        }
    }

    public static Vec3 toCurrentDimension(Waypoint waypointsEnumSettingHelper) {
        Vec3 vec3d1303 = waypointsEnumSettingHelper.toVec3d();
        String lowerCase = WorldUtils.getDimension().getDisplayName().toLowerCase();
        if (waypointsEnumSettingHelper.getDimension().equals("overworld") && lowerCase.equals("nether")) {
            return vec3d1303.multiply(0.125d, 1.0d, 0.125d);
        }
        if (waypointsEnumSettingHelper.getDimension().equals("nether") && lowerCase.equals("overworld")) {
            return vec3d1303.multiply(8.0d, 1.0d, 8.0d);
        }
        if (waypointsEnumSettingHelper.getDimension().equals(lowerCase)) {
            return vec3d1303;
        }
        return null;
    }
}