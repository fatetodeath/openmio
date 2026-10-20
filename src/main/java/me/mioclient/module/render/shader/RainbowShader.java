package me.mioclient.module.render.shader;

import me.mioclient.api.ClientServices;

import me.mioclient.render.shader.OutlineShader;
import me.mioclient.render.shader.ShaderRegistry;
import me.mioclient.util.math.ColorUtils;

import com.mojang.blaze3d.opengl.GlStateManager;
import me.mioclient.module.render.Shader;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.Identifier;

public class RainbowShader extends OutlineShader {
    public static final Identifier identifier = Identifier.fromNamespaceAndPath("mio-mount", "textures/overlay.png");
    /* Resolved lazily -- the Shader module registers after these shaders can first
       load, so an eager static lookup left the ref null (NPE). */
    private static Shader shader;

    public static Shader getShader() {
        if (shader == null && ClientServices.moduleInjector != null) {
            try {
                shader = (Shader) ClientServices.moduleInjector.getModule(Shader.class);
            } catch (Exception e) {
            }
        }
        return shader;
    }
    public final long num = System.currentTimeMillis();

    public RainbowShader() {
        initShaderProgram("rainbow");
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public boolean isOutlineShaderActive() {
        Shader shader_lcl = getShader();
        return shader_lcl != null && shader_lcl.isToggled() && shader_lcl.shader.getValue() == Shader.MixinHeldItemRendererMode.RAINBOW;
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public boolean shouldOutlineEntity(Entity entity) {
        Shader shader_lcl = getShader();
        if (shader_lcl == null || entity == null) {
            return false;
        }
        return shader_lcl.shouldRenderEntity(entity);
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public void applyOutlineUniforms() {
        Shader shader_lcl = getShader();
        if (shader_lcl == null) {
            return;
        }
        this.framebufferHelper_3.setUniformInt("u_Texture", 0);
        // 26.2: AbstractTexture.getGlId removed (GpuBackend GPU-driven textures).
        // Overlay texture view is managed by the GPU backend; select unit 1 for u_Overlay sampler.
        GlStateManager._activeTexture(33985);
        this.framebufferHelper_3.setUniformBool("u_Image", shader_lcl.image.getValue().booleanValue());
        this.framebufferHelper_3.setUniformInt("u_Overlay", 1);
        this.framebufferHelper_3.setUniformFloat("u_OverlayAlpha", shader_lcl.getOverlayAlpha());
        this.framebufferHelper_3.setUniformColor("u_Fill", shader_lcl.isHandFillDisabled() ? ColorUtils.color : shader_lcl.applyClusterAlpha(shader_lcl.fill.getValue()));
        this.framebufferHelper_3.setUniformFloat("u_Fill_Offset", ((float) (System.currentTimeMillis() - this.num)) * 0.001f * shader_lcl.fillSpeed.getValue().floatValue());
        this.framebufferHelper_3.setUniformFloat("u_Fill_Strength", shader_lcl.fillStrength.getValue().floatValue());
        this.framebufferHelper_3.setUniformColor("u_Outline", shader_lcl.outline2.getValue());
        this.framebufferHelper_3.setUniformFloat("u_Outline_Offset", ((float) (System.currentTimeMillis() - this.num)) * 0.001f * shader_lcl.outlineSpeed.getValue().floatValue());
        this.framebufferHelper_3.setUniformFloat("u_Outline_Strength", shader_lcl.outlineStrength.getValue().floatValue());
        this.framebufferHelper_3.setUniformInt("u_Radius", shader_lcl.isHandOutlineDisabled() ? 0 : shader_lcl.lineWidth.getValue().intValue());
        this.framebufferHelper_3.setUniformFloat("u_GlowMultiplier", shader_lcl.glow.getValue().floatValue());
        this.framebufferHelper_3.setUniformBool("u_FastLines", shader_lcl.fastLines.getValue().booleanValue() || ShaderRegistry.flag2);
        this.framebufferHelper_3.setUniformInt("u_Dots", shader_lcl.type.getValue().ordinal());
        this.framebufferHelper_3.setUniformInt("u_DotsRadius", shader_lcl.radius.getValue().intValue());
        this.framebufferHelper_3.setUniformFloat("u_DotsAlpha", shader_lcl.alpha.getValue().floatValue());
    }
}