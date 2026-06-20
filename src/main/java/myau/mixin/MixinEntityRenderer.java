package myau.mixin;

import myau.Myau;
import myau.data.Box;
import myau.event.EventManager;
import myau.events.PickEvent;
import myau.events.RaytraceEvent;
import myau.events.Render3DEvent;
import myau.module.modules.*;
import myau.module.modules.combat.*;
import myau.module.modules.movement.*;
import myau.module.modules.render.*;
import myau.module.modules.player.*;
import myau.module.modules.misc.*;
import myau.module.modules.latency.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import myau.motionblur.MotionBlurShaderHook;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityRenderer.class}, priority = 9999)
public abstract class MixinEntityRenderer implements MotionBlurShaderHook {
    @Unique
    private Box<Integer> slot = null;
    @Unique
    private Box<ItemStack> using = null;
    @Unique
    private Box<Integer> useCount = null;
    @Shadow
    private Minecraft mc;
    @Shadow
    private float thirdPersonDistance;
    @Shadow
    private float thirdPersonDistanceTemp;
    @Shadow
    private ShaderGroup theShaderGroup;
    @Unique
    private ShaderGroup myau$motionBlurShader;
    @Unique
    private boolean myau$freeLookRestoreRotation;
    @Unique
    private float myau$freeLookYaw;
    @Unique
    private float myau$freeLookPitch;
    @Unique
    private float myau$freeLookPrevYaw;
    @Unique
    private float myau$freeLookPrevPitch;

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("HEAD")}
    )
    private void updateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.mc.thePlayer != null) {
            KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
            if (killAura.isEnabled() && killAura.isBlocking()) {
                this.using = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUse());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.mc.thePlayer.inventory.getCurrentItem());
                this.useCount = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUseCount());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(69000);
            }
        }
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("RETURN")}
    )
    private void postUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.mc.thePlayer.inventory.currentItem = this.slot.value;
            this.slot = null;
        }
        if (this.using != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.using.value);
            this.using = null;
        }
        if (this.useCount != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(this.useCount.value);
            this.useCount = null;
        }
    }

    @Inject(
            method = {"isShaderActive"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void myau$isMotionBlurShaderActive(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callbackInfo) {
        if (this.myau$motionBlurShader != null && OpenGlHelper.shadersSupported) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(
            method = {"getShaderGroup"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void myau$getMotionBlurShaderGroup(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ShaderGroup> callbackInfo) {
        if (this.myau$motionBlurShader != null && OpenGlHelper.shadersSupported && this.theShaderGroup == null) {
            callbackInfo.setReturnValue(this.myau$motionBlurShader);
        }
    }

    @Inject(
            method = {"updateShaderGroupSize"},
            at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;createBindEntityOutlineFbs(II)V")}
    )
    private void myau$updateMotionBlurShaderSize(int width, int height, CallbackInfo callbackInfo) {
        if (this.myau$motionBlurShader != null) {
            this.myau$motionBlurShader.createBindFramebuffers(width, height);
        }
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderEntityOutlineFramebuffer()V", shift = At.Shift.AFTER)}
    )
    private void myau$renderMotionBlurShader(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
        if (this.myau$motionBlurShader != null) {
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.loadIdentity();
            this.myau$motionBlurShader.loadShaderGroup(partialTicks);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public ShaderGroup myau$getMotionBlurShader() {
        return this.myau$motionBlurShader;
    }

    @Override
    public void myau$setMotionBlurShader(ShaderGroup shaderGroup) {
        this.myau$motionBlurShader = shaderGroup;
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("HEAD")}
    )
    private void updateRenderer(CallbackInfo callbackInfo) {
        AutoBlockIn autoBlockIn = (AutoBlockIn) Myau.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn.isEnabled() && autoBlockIn.itemSpoof.getValue()) {
            int slot = autoBlockIn.getSlot();
            if (slot >= 0) {
                this.slot = new Box<>(this.mc.thePlayer.inventory.currentItem);
                this.mc.thePlayer.inventory.currentItem = slot;
            }
        }
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("RETURN")}
    )
    private void postUpdateRenderer(CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.mc.thePlayer.inventory.currentItem = this.slot.value;
            this.slot = null;
        }
    }

    @Inject(
            method = {"renderWorldPass"},
            at = {@At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z",
                    shift = At.Shift.BEFORE
            )}
    )
    private void renderWorldPass(int integer, float float2, long long3, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(float2));
    }

    @ModifyConstant(
            method = {"hurtCameraEffect"},
            constant = {@Constant(
                    floatValue = 14.0F,
                    ordinal = 0
            )}
    )
    private float hurtCameraEffect(float float1) {
        if (Myau.moduleManager == null) {
            return float1;
        } else {
            NoHurtCam noHurtCam = (NoHurtCam) Myau.moduleManager.modules.get(NoHurtCam.class);
            return noHurtCam.isEnabled() ? float1 * (float) noHurtCam.multiplier.getValue().intValue() / 100.0F : float1;
        }
    }

    @ModifyConstant(
            method = {"getMouseOver"},
            constant = {@Constant(
                    doubleValue = 3.0,
                    ordinal = 1
            )}
    )
    private double getMouseOver(double range) {
        PickEvent event = new PickEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @ModifyVariable(
            method = {"getMouseOver"},
            at = @At("STORE"),
            name = {"d0"}
    )
    private double storeMouseOver(double range) {
        RaytraceEvent event = new RaytraceEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @Inject(
            method = {"getMouseOver"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0
            )},
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void a(
            float float1,
            CallbackInfo callbackInfo,
            Entity entity,
            double double4,
            double double5,
            Vec3 vec36,
            boolean boolean7,
            int integer8,
            Vec3 vec39,
            Vec3 vec310,
            Vec3 vec311,
            float float12,
            List<Entity> list,
            double double14,
            int integer15
    ) {
        if (Myau.moduleManager != null) {
            GhostHand event = (GhostHand) Myau.moduleManager.modules.get(GhostHand.class);
            if (event.isEnabled()) {
                list.removeIf(event::shouldSkip);
            }
        }
    }

    @Inject(
            method = {"orientCamera"},
            at = {@At("HEAD")}
    )
    private void orientFreeLook(float partialTicks, CallbackInfo callbackInfo) {
        if (Myau.moduleManager == null || this.mc.thePlayer == null) {
            return;
        }
        FreeLook freeLook = (FreeLook) Myau.moduleManager.modules.get(FreeLook.class);
        if (freeLook != null && freeLook.isFreeLooking()) {
            EntityPlayerSP player = this.mc.thePlayer;
            this.myau$freeLookYaw = player.rotationYaw;
            this.myau$freeLookPitch = player.rotationPitch;
            this.myau$freeLookPrevYaw = player.prevRotationYaw;
            this.myau$freeLookPrevPitch = player.prevRotationPitch;
            this.myau$freeLookRestoreRotation = true;
            player.prevRotationYaw = player.rotationYaw = freeLook.getCameraYaw();
            player.prevRotationPitch = player.rotationPitch = MathHelper.clamp_float(freeLook.getCameraPitch(), -90.0F, 90.0F);
            if (this.mc.gameSettings.thirdPersonView == 0) {
                this.mc.gameSettings.thirdPersonView = 1;
            }
            this.thirdPersonDistanceTemp = this.thirdPersonDistance;
        }
    }

    @Inject(
            method = {"orientCamera"},
            at = {@At("RETURN")}
    )
    private void restoreFreeLook(float partialTicks, CallbackInfo callbackInfo) {
        if (this.myau$freeLookRestoreRotation && this.mc.thePlayer != null) {
            EntityPlayerSP player = this.mc.thePlayer;
            player.rotationYaw = this.myau$freeLookYaw;
            player.rotationPitch = this.myau$freeLookPitch;
            player.prevRotationYaw = this.myau$freeLookPrevYaw;
            player.prevRotationPitch = this.myau$freeLookPrevPitch;
            this.myau$freeLookRestoreRotation = false;
        }
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"
            )
    )
    private double v(Vec3 vec31, Vec3 vec32) {
        if (Myau.moduleManager == null) {
            return vec31.distanceTo(vec32);
        } else {
            return Myau.moduleManager.modules.get(ViewClip.class).isEnabled() ? (double) this.thirdPersonDistance : vec31.distanceTo(vec32);
        }
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
            )
    )
    private Material x(Block block) {
        if (Myau.moduleManager == null) {
            return block.getMaterial();
        } else {
            return Myau.moduleManager.modules.get(ViewClip.class).isEnabled() ? Material.air : block.getMaterial();
        }
    }

    @Redirect(
            method = {"updateFogColor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean y(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean q(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityLivingBase).getActivePotionsMap().containsKey(potion.id);
    }

    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean c(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.confusion && Myau.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) Myau.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ((IAccessorEntityLivingBase) entityPlayerSP).getActivePotionsMap().containsKey(potion.id);
    }
}
