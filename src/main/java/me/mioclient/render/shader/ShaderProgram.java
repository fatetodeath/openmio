package me.mioclient.render.shader;

import me.mioclient.util.text.StringTemplate;

import me.mioclient.render.mesh.GlStateWrapper;

import me.mioclient.api.ClientAccessor;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.awt.Color;
import org.joml.Matrix4f;

public class ShaderProgram implements ClientAccessor {
    public static ShaderProgram framebufferHelper_3;
    public final int num;
    public final Object2IntMap<String> object2IntMap = new Object2IntOpenHashMap();

    public ShaderProgram(String str, String str2) {
        int i = GlStateWrapper.createShader(35633);
        GlStateWrapper.setShaderSource(i, loadSource(str));
        String string291 = GlStateWrapper.compileShader(i);
        if (string291 != null) {
            throw new java.lang.RuntimeException(new StringTemplate().arg(string291).arg(str).format("Failed to compile vertex shader (1): 1"));
        }
        int i2 = GlStateWrapper.createShader(35632);
        GlStateWrapper.setShaderSource(i2, loadSource(str2));
        String string2912 = GlStateWrapper.compileShader(i2);
        if (string2912 != null) {
            throw new java.lang.RuntimeException(new StringTemplate().arg(string2912).arg(str2).format("Failed to compile fragment shader (1): 1"));
        }
        this.num = GlStateWrapper.createProgram();
        String string293 = GlStateWrapper.linkProgram(this.num, i, i2);
        if (string293 != null) {
            throw new java.lang.RuntimeException(new StringTemplate().arg(string293).format("Failed to link program: 1"));
        }
        GlStateWrapper.deleteShader(i);
        GlStateWrapper.deleteShader(i2);
    }

    public String loadSource(String str) {
        try {
            java.io.InputStream in = getClass().getResourceAsStream(new StringTemplate().arg(str).format("/assets/mio/shaders1"));
            if (in == null) {
                return "";
            }
            return org.apache.commons.io.IOUtils.toString(in, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public void use() {
        GlStateWrapper.useProgram(this.num);
        framebufferHelper_3 = this;
    }

    public int getUniformLocation(String str) {
        if (this.object2IntMap.containsKey(str)) {
            return this.object2IntMap.getInt(str);
        }
        int i = GlStateWrapper.findUniformLocation(this.num, str);
        this.object2IntMap.put(str, i);
        return i;
    }

    public void setUniformBool(String str, boolean z) {
        GlStateWrapper.uploadUniform1i(getUniformLocation(str), z ? 1 : 0);
    }

    public void setUniformInt(String str, int i) {
        GlStateWrapper.uploadUniform1i(getUniformLocation(str), i);
    }

    public void setUniformFloat(String str, double d) {
        GlStateWrapper.uploadUniform1f(getUniformLocation(str), (float) d);
    }

    public void setUniformVec2f(String str, double d, double d2) {
        GlStateWrapper.uploadUniform2f(getUniformLocation(str), (float) d, (float) d2);
    }

    public void setUniformColor(String str, Color color) {
        GlStateWrapper.uploadUniform4f(getUniformLocation(str), color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);
    }

    public void setUniformMatrix(String str, Matrix4f matrix4f) {
        GlStateWrapper.uploadUniformMatrix(getUniformLocation(str), matrix4f);
    }

    public void setProjectionUniforms() {
        setUniformMatrix("u_Proj", new Matrix4f());
        try {
            setUniformMatrix("u_ModelView", new Matrix4f(RenderSystem.getModelViewStack()));
        } catch (Exception ignored) {
            setUniformMatrix("u_ModelView", new Matrix4f());
        }
    }
}
