package myau.module.modules.movement.noslow;

import myau.module.modules.movement.NoSlow;
import myau.events.UpdateEvent;
import myau.events.PacketEvent;
import myau.events.RightClickMouseEvent;
import net.minecraft.client.Minecraft;

public abstract class NoSlowMode {
    protected final String name;
    protected final NoSlow parent;
    protected static final Minecraft mc = Minecraft.getMinecraft();

    public NoSlowMode(String name, NoSlow parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public NoSlow getParent() {
        return parent;
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onUpdate(UpdateEvent event) {}
    public void onPacket(PacketEvent event) {}
    public void onRightClick(RightClickMouseEvent event) {}
}
