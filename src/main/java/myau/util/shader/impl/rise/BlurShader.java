package myau.util.shader.impl.rise;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.util.List;

public class BlurShader extends RiseShader {

    protected static final Minecraft mc = Minecraft.getMinecraft();
    private final RiseShaderProgram blurProgram = new RiseShaderProgram("blur.frag", "vertex.vsh");
    private Framebuffer inputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private Framebuffer outputFramebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
    private GaussianKernel gaussianKernel = new GaussianKernel(0);

    public final static int DEFAULT_RADIUS = 12;
    public final static float DEFAULT_COMPRESSION = 3.0F;

    private int radius = DEFAULT_RADIUS;
    private float compression = DEFAULT_COMPRESSION;

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public float getCompression() {
        return compression;
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
                this.inputFramebuffer.bindFramebuffer(true);

                for (Runnable r : runnable) {
                    r.run();
                }

                mc.getFramebuffer().bindFramebuffer(true);
                break;
            }
            case OVERLAY: {
                this.inputFramebuffer.bindFramebuffer(true);

                for (Runnable r : runnable) {
                    r.run();
                }

                final int programId = this.blurProgram.getProgramId();

                this.outputFramebuffer.bindFramebuffer(true);
                this.blurProgram.start();

                if (this.gaussianKernel.getSize() != radius) {
                    this.gaussianKernel = new GaussianKernel(radius);
                    this.gaussianKernel.compute();

                    final FloatBuffer buffer = BufferUtils.createFloatBuffer(radius);
                    buffer.put(this.gaussianKernel.getKernel());
                    buffer.flip();

                    ShaderUniforms.uniform1f(programId, "u_radius", radius);
                    ShaderUniforms.uniformFB(programId, "u_kernel", buffer);
                    ShaderUniforms.uniform1i(programId, "u_diffuse_sampler", 0);
                    ShaderUniforms.uniform1i(programId, "u_other_sampler", 20);
                }

                ShaderUniforms.uniform2f(programId, "u_texel_size", 1.0F / mc.displayWidth, 1.0F / mc.displayHeight);
                ShaderUniforms.uniform2f(programId, "u_direction", compression, 0.0F);

                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
                mc.getFramebuffer().bindFramebufferTexture();
                RiseShaderProgram.drawQuad();

                mc.getFramebuffer().bindFramebuffer(true);
                ShaderUniforms.uniform2f(programId, "u_direction", 0.0F, compression);

                outputFramebuffer.bindFramebufferTexture();

                GL13.glActiveTexture(GL13.GL_TEXTURE20);
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
        this.setActive(false);

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
