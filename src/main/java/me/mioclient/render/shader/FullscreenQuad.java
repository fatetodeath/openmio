package me.mioclient.render.shader;

import me.mioclient.render.mesh.MeshBuilder;

import com.mojang.blaze3d.vertex.PoseStack;

public class FullscreenQuad {
    public static MeshBuilder byteBufferSearchHelper4;
    public static final PoseStack matrixStack = new PoseStack();

    public static void init() {
        byteBufferSearchHelper4 = new MeshBuilder(MeshBuilder.Mode_2.Triangles, MeshBuilder.Mode.Vec2);
        byteBufferSearchHelper4.beginBuild();
        byteBufferSearchHelper4.submitQuad(byteBufferSearchHelper4.writeScreenPosition((-1.0d), (-1.0d)).nextVertexIndex(), byteBufferSearchHelper4.writeScreenPosition((-1.0d), 1.0d).nextVertexIndex(), byteBufferSearchHelper4.writeScreenPosition(1.0d, 1.0d).nextVertexIndex(), byteBufferSearchHelper4.writeScreenPosition(1.0d, (-1.0d)).nextVertexIndex());
        byteBufferSearchHelper4.endBuild();
    }

    public static void beginOutline() {
        byteBufferSearchHelper4.beginDraw(matrixStack);
    }

    public static void drawBuffer() {
        byteBufferSearchHelper4.drawMesh(matrixStack);
    }

    public static void do866() {
        byteBufferSearchHelper4.do866();
    }
}
