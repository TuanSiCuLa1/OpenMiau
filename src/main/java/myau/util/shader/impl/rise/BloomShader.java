package myau.util.shader.impl.rise;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.util.List;

public class BloomShader extends RiseShader {

    protected static final Minecraft mc = Minecraft.getMinecraft();
    private final RiseShaderProgram bloomProgram = new RiseShaderProgram("bloom.frag", "vertex.vsh");
    private Framebuffer inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private Framebuffer outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private GaussianKernel gaussianKernel = new GaussianKernel(0);
    
    private int radius = 14;
    private float compression = 2.0F;

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public void setCompression(float compression) {
        this.compression = compression;
    }

    @Override
    public void run(final ShaderRenderType type, final float partialTicks, List<Runnable> runnable) {
        if (!Display.isVisible()) {
            return;
        }

        switch (type) {
            case CAMERA: {
                RendererLivingEntity.NAME_TAG_RANGE = 0;
                RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 0;

                this.inputFramebuffer.bindFramebuffer(true);
                for (Runnable runnable1 : runnable) {
                    runnable1.run();
                }
                mc.getFramebuffer().bindFramebuffer(true);

                RendererLivingEntity.NAME_TAG_RANGE = 64;
                RendererLivingEntity.NAME_TAG_RANGE_SNEAK = 32;

                RenderHelper.disableStandardItemLighting();
                mc.entityRenderer.disableLightmap();
                break;
            }

            case OVERLAY: {
                this.inputFramebuffer.bindFramebuffer(true);

                for (Runnable r : runnable) {
                    r.run();
                }

                final int programId = this.bloomProgram.getProgramId();

                this.outputFramebuffer.bindFramebuffer(true);
                this.bloomProgram.start();

                if (this.gaussianKernel.getSize() != radius) {
                    this.gaussianKernel = new GaussianKernel(radius);
                    this.gaussianKernel.compute();

                    final FloatBuffer buffer = BufferUtils.createFloatBuffer(radius);
                    buffer.put(this.gaussianKernel.getKernel());
                    buffer.flip();

                    ShaderUniforms.uniform1f(programId, "u_radius", radius);
                    ShaderUniforms.uniformFB(programId, "u_kernel", buffer);
                    ShaderUniforms.uniform1i(programId, "u_diffuse_sampler", 0);
                    ShaderUniforms.uniform1i(programId, "u_other_sampler", 16);
                }

                ShaderUniforms.uniform2f(programId, "u_texel_size", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
                ShaderUniforms.uniform2f(programId, "u_direction", compression, 0.0F);

                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_SRC_ALPHA);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
                inputFramebuffer.bindFramebufferTexture();
                RiseShaderProgram.drawQuad();

                mc.getFramebuffer().bindFramebuffer(true);
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                ShaderUniforms.uniform2f(programId, "u_direction", 0.0F, compression);
                outputFramebuffer.bindFramebufferTexture();
                GL13.glActiveTexture(GL13.GL_TEXTURE16);
                inputFramebuffer.bindFramebufferTexture();
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                RiseShaderProgram.drawQuad();
                GlStateManager.disableBlend();

                RiseShaderProgram.stop();
                break;
            }
        }
    }

    @Override
    public void update() {
        int width = mc.displayWidth;
        int height = mc.displayHeight;

        if (inputFramebuffer.framebufferWidth != width || inputFramebuffer.framebufferHeight != height) {
            inputFramebuffer.deleteFramebuffer();
            inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        } else {
            inputFramebuffer.framebufferClear();
        }

        if (outputFramebuffer.framebufferWidth != width || outputFramebuffer.framebufferHeight != height) {
            outputFramebuffer.deleteFramebuffer();
            outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
        } else {
            outputFramebuffer.framebufferClear();
        }

        inputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        outputFramebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
    }
}
