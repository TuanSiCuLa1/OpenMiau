package myau.module.modules.movement;

import myau.event.EventTarget;
import myau.events.SafeWalkEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty onlyGround = new BooleanProperty("only-ground", false);
    public final BooleanProperty pitchLimit = new BooleanProperty("pitch", false);
    public final IntProperty pitchMax = new IntProperty("pitch-max", 90, 0, 90, this.pitchLimit::getValue);
    public final IntProperty pitchMin = new IntProperty("pitch-min", 0, 0, 90, this.pitchLimit::getValue);

    public SafeWalk() {
        super("SafeWalk", false);
    }

    private boolean canSafeWalk() {
        if (mc.thePlayer == null) {
            return false;
        }
        if (this.onlyGround.getValue() && !mc.thePlayer.onGround) {
            return false;
        }
        return !this.pitchLimit.getValue() || mc.thePlayer.rotationPitch < this.pitchMax.getValue() && mc.thePlayer.rotationPitch > this.pitchMin.getValue();
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled() && canSafeWalk()) {
            event.setSafeWalk(true);
        }
    }

    @Override
    public void verifyValue(String name) {
        switch (name) {
            case "pitch-min":
                if (this.pitchMin.getValue() > this.pitchMax.getValue()) {
                    this.pitchMax.setValue(this.pitchMin.getValue());
                }
                break;
            case "pitch-max":
                if (this.pitchMin.getValue() > this.pitchMax.getValue()) {
                    this.pitchMin.setValue(this.pitchMax.getValue());
                }
                break;
        }
    }
}