package me.mioclient.render.mesh;

import me.mioclient.render.shader.ShaderProgram;

import me.mioclient.api.ClientAccessor;

import java.awt.Color;
import java.nio.ByteBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fStack;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

public class MeshBuilder implements ClientAccessor {
    public boolean flag = false;
    public double val = 1.0d;
    public final Mode_2 mode_2;
    public final int num;
    public final int num2;
    public final int num3;
    public final int num4;
    public ByteBuffer byteBuffer;
    public long num5;
    public long num6;
    public ByteBuffer byteBuffer2;
    public long num7;
    public int num8;
    public int num9;
    public boolean flag2;
    public boolean flag3;
    public double val2;
    public double val3;
    public boolean flag4;

    public enum Mode {
        Float(1, 4, false),
        Vec2(2, 4, false),
        Vec3(3, 4, false),
        Color(4, 1, true);

        public final int num;
        public final int num2;
        public final boolean flag;

        Mode(int i, int i2, boolean z) {
            this.num = i;
            this.num2 = i * i2;
            this.flag = z;
        }

        public int getVertexStride() {
            return this == Color ? 5121 : 5126;
        }
    }

    public enum Mode_2 {
        Lines(2),
        Triangles(3);

        public final int num;

        Mode_2(int i) {
            this.num = i;
        }

        public int getIndexCount() {
            switch (this) {
                case Lines:
                    return 1;
                case Triangles:
                    return 4;
                default:
                    throw new MatchException((String) null, (Throwable) null);
            }
        }
    }

    public MeshBuilder(Mode_2 mode_2, Mode... modeArr) {
        int i = 0;
        for (Mode mode : modeArr) {
            i += mode.num2;
        }
        this.mode_2 = mode_2;
        this.num = i * mode_2.num;
        this.byteBuffer = BufferUtils.createByteBuffer(this.num * 256 * 4);
        this.num5 = MemoryUtil.memAddress0(this.byteBuffer);
        this.byteBuffer2 = BufferUtils.createByteBuffer(mode_2.num * 512 * 4);
        this.num7 = MemoryUtil.memAddress0(this.byteBuffer2);
        this.num2 = GlStateWrapper.generateVertexArray();
        GlStateWrapper.bindVertexArray(this.num2);
        this.num3 = GlStateWrapper.generateBuffer();
        GlStateWrapper.bindArrayBuffer(this.num3);
        this.num4 = GlStateWrapper.generateBuffer();
        GlStateWrapper.bindElementBuffer(this.num4);
        int i2 = 0;
        for (int i3 = 0; i3 < modeArr.length; i3++) {
            Mode mode2 = modeArr[i3];
            GlStateWrapper.enableVertexAttribute(i3);
            GlStateWrapper.setVertexAttributePointer(i3, mode2.num, mode2.getVertexStride(), mode2.flag, i, i2);
            i2 += mode2.num2;
        }
        GlStateWrapper.bindVertexArray(0);
        GlStateWrapper.bindArrayBuffer(0);
        GlStateWrapper.bindElementBuffer(0);
    }

    public void resetCounters() {
        GlStateWrapper.deleteBuffer(this.num4);
        GlStateWrapper.deleteBuffer(this.num3);
        GlStateWrapper.deleteVertexArray(this.num2);
    }

    public void beginBuild() {
        if (this.flag2) {
            throw new IllegalStateException("Mesh.end() called while already building.");
        }
        this.num6 = this.num5;
        this.num8 = 0;
        this.num9 = 0;
        this.flag2 = true;
        Vec3 pos = minecraftClient.gameRenderer.mainCamera().position();
        this.val2 = pos.x;
        this.val3 = pos.z;
    }

    public MeshBuilder writeWorldPosition(double d, double d2, double d3) {
        long j = this.num6;
        MemoryUtil.memPutFloat(j, (float) (d - this.val2));
        MemoryUtil.memPutFloat(j + 4, (float) d2);
        MemoryUtil.memPutFloat(j + 8, (float) (d3 - this.val3));
        this.num6 += 12;
        return this;
    }

    public MeshBuilder writeScreenPosition(double d, double d2) {
        long j = this.num6;
        MemoryUtil.memPutFloat(j, (float) d);
        MemoryUtil.memPutFloat(j + 4, (float) d2);
        this.num6 += 8;
        return this;
    }

    public MeshBuilder writeColor(Color color) {
        long j = this.num6;
        MemoryUtil.memPutByte(j, (byte) color.getRed());
        MemoryUtil.memPutByte(j + 1, (byte) color.getGreen());
        MemoryUtil.memPutByte(j + 2, (byte) color.getBlue());
        MemoryUtil.memPutByte(j + 3, (byte) (color.getAlpha() * ((float) this.val)));
        this.num6 += 4;
        return this;
    }

    public int nextVertexIndex() {
        int i = this.num8;
        this.num8 = i + 1;
        return i;
    }

    public void submitVertex(int i, int i2) {
        long j = this.num7 + (this.num9 * 4);
        MemoryUtil.memPutInt(j, i);
        MemoryUtil.memPutInt(j + 4, i2);
        this.num9 += 2;
        ensureCapacity();
    }

    public void submitQuad(int i, int i2, int i3, int i4) {
        long j = this.num7 + (this.num9 * 4);
        MemoryUtil.memPutInt(j, i);
        MemoryUtil.memPutInt(j + 4, i2);
        MemoryUtil.memPutInt(j + 8, i3);
        MemoryUtil.memPutInt(j + 12, i3);
        MemoryUtil.memPutInt(j + 16, i4);
        MemoryUtil.memPutInt(j + 20, i);
        this.num9 += 6;
        ensureCapacity();
    }

    public void ensureCapacity() {
        if ((this.num8 + 1) * this.num >= this.byteBuffer.capacity()) {
            int i = getWrittenBytes();
            int capacity = this.byteBuffer.capacity() * 2;
            if (capacity % this.num != 0) {
                capacity += capacity % this.num;
            }
            ByteBuffer createByteBuffer = BufferUtils.createByteBuffer(capacity);
            MemoryUtil.memCopy(MemoryUtil.memAddress0(this.byteBuffer), MemoryUtil.memAddress0(createByteBuffer), i);
            this.byteBuffer = createByteBuffer;
            this.num5 = MemoryUtil.memAddress0(this.byteBuffer);
            this.num6 = this.num5 + i;
        }
        if (this.num9 * 4 >= this.byteBuffer2.capacity()) {
            int capacity2 = this.byteBuffer2.capacity() * 2;
            if (capacity2 % this.mode_2.num != 0) {
                capacity2 += capacity2 % (this.mode_2.num * 4);
            }
            ByteBuffer createByteBuffer2 = BufferUtils.createByteBuffer(capacity2);
            MemoryUtil.memCopy(MemoryUtil.memAddress0(this.byteBuffer2), MemoryUtil.memAddress0(createByteBuffer2), this.num9 * 4);
            this.byteBuffer2 = createByteBuffer2;
            this.num7 = MemoryUtil.memAddress0(this.byteBuffer2);
        }
    }

    public void endBuild() {
        if (!this.flag2) {
            throw new IllegalStateException("Mesh.end() called while not building.");
        }
        if (this.num9 > 0) {
            GlStateWrapper.bindArrayBuffer(this.num3);
            GlStateWrapper.uploadBufferData(34962, this.byteBuffer.limit(getWrittenBytes()), 35048);
            GlStateWrapper.bindArrayBuffer(0);
            GlStateWrapper.bindElementBuffer(this.num4);
            GlStateWrapper.uploadBufferData(34963, this.byteBuffer2.limit(this.num9 * 4), 35048);
            GlStateWrapper.bindElementBuffer(0);
        }
        this.flag2 = false;
    }

    public void beginDraw(PoseStack matrixStack) {
        GlStateWrapper.saveState();
        if (this.flag) {
            GlStateWrapper.enableDepthTest();
        } else {
            GlStateWrapper.disableDepthTest();
        }
        GlStateWrapper.enableBlend();
        GlStateWrapper.disableCull();
        GlStateWrapper.enableSmoothing();
        if (this.flag3) {
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            if (matrixStack != null) {
                modelViewStack.mul(matrixStack.last().pose());
            }
            modelViewStack.translate(0.0f, (float) (-minecraftClient.gameRenderer.mainCamera().position().y), 0.0f);
        }
        this.flag4 = true;
    }

    public void drawMesh(PoseStack matrixStack) {
        if (this.flag2) {
            endBuild();
        }
        if (this.num9 > 0) {
            boolean z = this.flag4;
            if (!z) {
                beginDraw(matrixStack);
            }
            finishBuild();
            if (ShaderProgram.framebufferHelper_3 != null) {
                ShaderProgram.framebufferHelper_3.setProjectionUniforms();
            }
            GlStateWrapper.bindVertexArray(this.num2);
            GlStateWrapper.drawElements(this.mode_2.getIndexCount(), this.num9, 5125);
            GlStateWrapper.bindVertexArray(0);
            if (z) {
                return;
            }
            do866();
        }
    }

    public void do866() {
        if (this.flag3) {
            RenderSystem.getModelViewStack().popMatrix();
        }
        GlStateWrapper.restoreState();
        this.flag4 = false;
    }

    public boolean isBuilding() {
        return this.flag2;
    }

    public void finishBuild() {
    }

    public int getWrittenBytes() {
        return (int) (this.num6 - this.num5);
    }
}
