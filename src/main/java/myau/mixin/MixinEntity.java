package myau.mixin;

import myau.Myau;
import myau.event.EventManager;
import myau.events.KnockbackEvent;
import myau.events.SafeWalkEvent;
import myau.module.modules.FreeLook;
import myau.util.ITruePosition;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(value = {Entity.class}, priority = 9999)
public abstract class MixinEntity implements ITruePosition {
    @Shadow
    public World worldObj;
    @Shadow
    public double posX;
    @Shadow
    public double posY;
    @Shadow
    public double posZ;
    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public float rotationPitch;
    @Shadow
    public float prevRotationYaw;
    @Shadow
    public float prevRotationPitch;
    @Shadow
    public boolean onGround;

    @Shadow
    public int serverPosX;

    @Shadow
    public int serverPosY;

    @Shadow
    public int serverPosZ;

    @Unique
    private double trueX;

    @Unique
    private double trueY;

    @Unique
    private double trueZ;

    @Unique
    private boolean truePos;

    @Shadow
    public boolean isRiding() {
        return false;
    }

    @Inject(
            method = {"setVelocity"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setVelocity(double double1, double double2, double double3, CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            KnockbackEvent event = new KnockbackEvent(double1, double2, double3);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
                this.motionX = event.getX();
                this.motionY = event.getY();
                this.motionZ = event.getZ();
            }
        }
    }

    @Inject(
            method = {"setAngles"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void setAngles(float yaw, float pitch, CallbackInfo callbackInfo) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            if (Myau.moduleManager != null) {
                FreeLook freeLook = (FreeLook) Myau.moduleManager.modules.get(FreeLook.class);
                if (freeLook != null && freeLook.isFreeLooking()) {
                    freeLook.updateCamera(yaw, pitch);
                    callbackInfo.cancel();
                    return;
                }
            }
            if (Myau.rotationManager != null && Myau.rotationManager.isRotated()) {
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    private void initTruePosition(World world, CallbackInfo callbackInfo) {
        updateTruePositionFromCurrent();
    }

    @Inject(method = {"setPosition"}, at = {@At("RETURN")})
    private void setPositionTrue(double x, double y, double z, CallbackInfo callbackInfo) {
        updateTruePositionFromCurrent();
    }

    @Inject(method = {"setPositionAndRotation"}, at = {@At("RETURN")})
    private void setPositionAndRotationTrue(double x, double y, double z, float yaw, float pitch, CallbackInfo callbackInfo) {
        updateTruePositionFromCurrent();
    }

    @Inject(method = {"setPositionAndRotation2"}, at = {@At("HEAD")})
    private void setPositionAndRotation2True(double x, double y, double z, float yaw, float pitch, int increments, boolean teleport, CallbackInfo callbackInfo) {
        this.trueX = x;
        this.trueY = y;
        this.trueZ = z;
        this.truePos = true;
    }

    @Unique
    private void updateTruePositionFromCurrent() {
        this.trueX = this.posX;
        this.trueY = this.posY;
        this.trueZ = this.posZ;
        this.truePos = true;
    }

    @Override
    public double getTrueX() {
        return this.trueX;
    }

    @Override
    public double getTrueY() {
        return this.trueY;
    }

    @Override
    public double getTrueZ() {
        return this.trueZ;
    }

    @Override
    public void setTrueX(double trueX) {
        this.trueX = trueX;
    }

    @Override
    public void setTrueY(double trueY) {
        this.trueY = trueY;
    }

    @Override
    public void setTrueZ(double trueZ) {
        this.trueZ = trueZ;
    }

    @Override
    public boolean isTruePos() {
        return this.truePos;
    }

    @Override
    public void setTruePos(boolean truePos) {
        this.truePos = truePos;
    }

    @ModifyVariable(
            method = {"moveEntity"},
            ordinal = 0,
            at = @At("STORE"),
            name = {"flag"}
    )
    private boolean moveEntity(boolean boolean1) {
        if ((Entity) ((Object) this) instanceof EntityPlayerSP) {
            SafeWalkEvent event = new SafeWalkEvent(boolean1);
            EventManager.call(event);
            return event.isSafeWalk();
        } else {
            return boolean1;
        }
    }
}
