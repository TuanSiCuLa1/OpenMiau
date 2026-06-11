package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;

public class RPC extends Module {
    public final BooleanProperty showServer = new BooleanProperty("show-server", true);
    public final BooleanProperty showModulesCount = new BooleanProperty("show-modules-count", true);

    public RPC() {
        super("RPC", true, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            if (!Myau.discordRichPresence.isRunning()) {
                Myau.discordRichPresence.start(this);
            } else {
                Myau.discordRichPresence.update(this);
            }
        }
    }

    @Override
    public void onEnabled() {
        Myau.discordRichPresence.start(this);
    }

    @Override
    public void onDisabled() {
        Myau.discordRichPresence.stop();
    }
}
