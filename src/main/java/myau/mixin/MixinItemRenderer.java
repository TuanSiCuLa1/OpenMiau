package myau.mixin;

import myau.module.modules.render.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow
    private float prevEquippedProgress;

    @Shadow
    private float equippedProgress;

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Shadow
    protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    /**
     * @author CCBlueX, ported to Miau
     * @reason Add configurable blocking animations.
     */
    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        float equipProgress = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
        EntityPlayerSP player = this.mc.thePlayer;
        float swingProgress = player.getSwingProgress(partialTicks);
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

        this.rotateArroundXAndY(pitch, yaw);
        this.setLightMapFromPlayer(player);
        this.rotateWithPlayerRotations(player, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        if (this.itemToRender != null) {
            if (this.itemToRender.getItem() instanceof ItemMap) {
                this.renderItemMap(player, pitch, equipProgress, swingProgress);
            } else if (player.getItemInUseCount() > 0) {
                EnumAction action = this.itemToRender.getItemUseAction();
                if (action == EnumAction.NONE) {
                    this.transformFirstPersonItem(equipProgress, 0.0F);
                } else if (action == EnumAction.EAT || action == EnumAction.DRINK) {
                    this.performDrinking(player, partialTicks);
                    this.transformFirstPersonItem(equipProgress, swingProgress);
                } else if (action == EnumAction.BLOCK) {
                    if (!Animations.apply(swingProgress, equipProgress, player)) {
                        this.transformFirstPersonItem(equipProgress, swingProgress);
                        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                    }
                } else if (action == EnumAction.BOW) {
                    this.transformFirstPersonItem(equipProgress, swingProgress);
                    this.doBowTransformations(partialTicks, player);
                }
            } else {
                if (!Animations.isOddSwing()) {
                    this.doItemUsedTransformations(swingProgress);
                }
                this.transformFirstPersonItem(equipProgress, swingProgress);
            }

            this.renderItem(player, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        } else if (!player.isInvisible()) {
            this.renderPlayerArm(player, equipProgress, swingProgress);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }
}
