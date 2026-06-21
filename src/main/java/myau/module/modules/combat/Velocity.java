package myau.module.modules.combat;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.*;
import myau.mixin.IAccessorEntity;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.module.modules.combat.velocity.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public class Velocity extends Module {
    public static final Minecraft mc = Minecraft.getMinecraft();

    public int chanceCounter = 0;
    public int delayChanceCounter = 0;
    public boolean pendingExplosion = false;
    public boolean allowNext = true;
    public boolean jumpFlag = false;
    public boolean reverseFlag = false;
    public boolean delayActive = false;

    public boolean shouldJump = false;
    public int jumpCooldown = 0;
    public boolean hasReceivedVelocity = false;
    public int legitSmartJumpCount = 0;
    public int intaveTick = 0;
    public int intaveDamageTick = 0;

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
        "OMDelay", "Reverse", "LegitTest", "LegitSmart", "IntaveReduce", "Grimtest", "JumpReset",
        "Standard", "AAC", "Bounce", "BufferAbuse", "Delay", "Grim", "GrimReduce", "Ground", "Intave",
        "Karhu", "Legit", "MMC", "Matrix", "Redesky", "Tick", "UniversoCraft", "Vulcan", "WatchdogPrediction", "Watchdog"
    });
    
    public final IntProperty delayTicks = new IntProperty("delay-ticks", 3, 1, 20, () -> mode.getModeString().equals("OMDelay"));
    public final PercentProperty delayChance = new PercentProperty("delay-chance", 100, () -> mode.getModeString().equals("OMDelay"));
    public final IntProperty legitSmartJumpLimit = new IntProperty("legit-smart-jump-limit", 2, 1, 5, () -> mode.getModeString().equals("LegitSmart"));
    public final FloatProperty intaveReduceFactor = new FloatProperty("intave-reduce-factor", 0.6F, 0.6F, 1.0F, () -> mode.getModeString().equals("IntaveReduce"));
    public final IntProperty intaveReduceHurtTime = new IntProperty("intave-reduce-hurt-time", 9, 1, 10, () -> mode.getModeString().equals("IntaveReduce"));
    public final PercentProperty chance = new PercentProperty("chance", 100, () -> mode.getModeString().equals("Legit") || mode.getModeString().equals("LegitTest") || mode.getModeString().equals("LegitSmart"));
    public final PercentProperty horizontal = new PercentProperty("horizontal", 0, () -> {
        String m = mode.getModeString();
        return m.equals("Standard") || m.equals("BufferAbuse") || m.equals("Redesky") || m.equals("Vulcan");
    });
    public final PercentProperty vertical = new PercentProperty("vertical", 100, () -> {
        String m = mode.getModeString();
        return m.equals("Standard") || m.equals("BufferAbuse") || m.equals("Redesky") || m.equals("Vulcan");
    });
    public final PercentProperty explosionHorizontal = new PercentProperty("explosions-horizontal", 100, () -> mode.getModeString().equals("Standard"));
    public final PercentProperty explosionVertical = new PercentProperty("explosions-vertical", 100, () -> mode.getModeString().equals("Standard"));
    public final IntProperty grimReduceJumpLimit = new IntProperty("grim-reduce-jump-limit", 2, 1, 5, () -> mode.getModeString().equals("Grimtest"));
    public final BooleanProperty fakeCheck = new BooleanProperty("fake-check", true);
    public final BooleanProperty debugLog = new BooleanProperty("debug-log", false);
    
    public final BooleanProperty onSwing = new BooleanProperty("on-swing", false);

    public final List<VelocityMode> modes = new ArrayList<>();

    public boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    public boolean canDelay() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        return mc.thePlayer.onGround && (!killAura.isEnabled() || !killAura.shouldAutoBlock());
    }

    public Velocity() {
        super("Velocity", false);

        // OM Legacy Modes
        modes.add(new OMDelayVelocity("OMDelay", this));
        modes.add(new ReverseVelocity("Reverse", this));
        modes.add(new LegitTestVelocity("LegitTest", this));
        modes.add(new LegitSmartVelocity("LegitSmart", this));
        modes.add(new IntaveReduceVelocity("IntaveReduce", this));
        modes.add(new GrimTestVelocity("Grimtest", this));
        modes.add(new JumpResetVelocity("JumpReset", this));

        // Rise Modes
        modes.add(new StandardVelocity("Standard", this));
        modes.add(new AACVelocity("AAC", this));
        modes.add(new BounceVelocity("Bounce", this));
        modes.add(new BufferAbuseVelocity("BufferAbuse", this));
        modes.add(new DelayVelocity("Delay", this));
        modes.add(new GrimVelocity("Grim", this));
        modes.add(new GrimReduceVelocity("GrimReduce", this));
        modes.add(new GroundVelocity("Ground", this));
        modes.add(new IntaveVelocity("Intave", this));
        modes.add(new KarhuVelocity("Karhu", this));
        modes.add(new LegitVelocity("Legit", this));
        modes.add(new MMCVelocity("MMC", this));
        modes.add(new MatrixVelocity("Matrix", this));
        modes.add(new RedeskyVelocity("Redesky", this));
        modes.add(new TickVelocity("Tick", this));
        modes.add(new UniversoCraftVelocity("UniversoCraft", this));
        modes.add(new VulcanVelocity("Vulcan", this));
        modes.add(new WatchdogPredictionVelocity("WatchdogPrediction", this));
        modes.add(new WatchdogVelocity("Watchdog", this));

    }

    public VelocityMode getActiveMode() {
        return modes.stream().filter(m -> m.getName().equals(mode.getModeString())).findFirst().orElse(modes.get(0));
    }

    @Override
    public void onEnabled() {
        getActiveMode().onEnable();
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onKnockback(event);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onUpdate(event);
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onLivingUpdate(event);
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onStrafe(event);
        }
    }

    @EventTarget
    public void onJump(JumpEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onJump(event);
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onRender3D(event);
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onMoveInput(event);
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onAttack(event);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled()) {
            getActiveMode().onPacket(event);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onDisabled() {
        getActiveMode().onDisable();
        this.pendingExplosion = false;
        this.allowNext = true;
        this.shouldJump = false;
        this.jumpCooldown = 0;
        this.hasReceivedVelocity = false;
        this.legitSmartJumpCount = 0;
        this.intaveTick = 0;
        this.intaveDamageTick = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
