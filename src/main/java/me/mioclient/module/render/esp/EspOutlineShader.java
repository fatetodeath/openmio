package me.mioclient.module.render.esp;

import me.mioclient.api.ClientServices;

import me.mioclient.render.shader.OutlineShader;

import me.mioclient.module.render.ESP;
import net.minecraft.world.entity.Entity;

public class EspOutlineShader extends OutlineShader {
    public static ESP eSP = (ESP) ClientServices.moduleInjector.getModule(ESP.class);

    public EspOutlineShader() {
        initShaderProgram("outline");
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public boolean isOutlineShaderActive() {
        return eSP.isToggled() && eSP.eSPSearchHelper4_2.hasBlockEntityEsp();
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public boolean shouldOutlineEntity(Entity entity) {
        return false;
    }

    @Override // me.mioclient.render.shader.PostProcessEffect
    public void applyOutlineUniforms() {
        this.framebufferHelper_3.setUniformInt("u_Texture", 0);
        this.framebufferHelper_3.setUniformInt("u_Width", 2);
        this.framebufferHelper_3.setUniformBool("u_FastLines", true);
        this.framebufferHelper_3.setUniformFloat("u_GlowMultiplier", 1.0d);
    }
}