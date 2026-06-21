package myau.module.modules.combat.velocity;

import myau.events.KnockbackEvent;
import myau.events.LivingUpdateEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.events.MoveInputEvent;
import myau.events.AttackEvent;
import myau.events.StrafeEvent;
import myau.events.JumpEvent;
import myau.events.Render3DEvent;
import myau.module.modules.combat.Velocity;
import net.minecraft.client.Minecraft;

public abstract class VelocityMode {
    protected final String name;
    protected final Velocity parent;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public VelocityMode(String name, Velocity parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public Velocity getParent() {
        return parent;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onUpdate(UpdateEvent event) {}
    public void onPacket(PacketEvent event) {}
    public void onKnockback(KnockbackEvent event) {}
    public void onLivingUpdate(LivingUpdateEvent event) {}
    public void onMoveInput(MoveInputEvent event) {}
    public void onAttack(AttackEvent event) {}
    public void onStrafe(StrafeEvent event) {}
    public void onJump(JumpEvent event) {}
    public void onRender3D(Render3DEvent event) {}
}
