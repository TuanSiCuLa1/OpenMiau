package myau.module;

import myau.Myau;
import myau.module.modules.render.HUD;
import myau.property.Property;
import myau.property.PropertyManager;
import java.util.List;
import java.util.ArrayList;
import myau.util.client.KeyBindUtil;
import myau.notification.NotificationManager;
import myau.notification.NotificationType;

public abstract class Module {
    protected final String name;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected final boolean defaultHidden;
    protected boolean enabled;
    protected int key;
    protected boolean hidden;

    public Module(String name, boolean enabled) {
        this(name, enabled, false);
    }

    public Module(String name, boolean enabled, boolean hidden) {
        this.name = name;
        this.enabled = this.defaultEnabled = enabled;
        this.key = this.defaultKey = 0;
        this.hidden = this.defaultHidden = hidden;
    }

    public String getName() {
        return this.name;
    }

    public String formatModule() {
        return String.format(
                "%s%s &r(%s&r)",
                this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key)),
                this.name,
                this.enabled ? "&a&lON" : "&c&lOFF"
        );
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
                Myau.notificationManager.builder(NotificationType.SUCCESS).title(this.name).description("was enabled.").duration(2000).buildAndPublish();
            } else {
                this.onDisabled();
                Myau.notificationManager.builder(NotificationType.ERROR).title(this.name).description("was disabled.").duration(2000).buildAndPublish();
            }
        }
    }

    public List<Property<?>> getValues() {
        ArrayList<Property<?>> props = Myau.propertyManager.properties.get(this.getClass());
        if (props == null) return new ArrayList<>();
        return props;
    }

    public boolean toggle() {
        boolean enabled = !this.enabled;
        this.setEnabled(enabled);
        if (this.enabled == enabled) {
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            if (hud != null && hud.toggleSound.getValue()) {
                Myau.moduleManager.playSound();
            }
            return true;
        } else {
            return false;
        }
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean boolean1) {
        this.hidden = boolean1;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }

    public String getCategory() {
        String packageName = this.getClass().getPackage().getName();
        String prefix = "myau.module.modules.";
        if (!packageName.startsWith(prefix)) {
            return null;
        }
        String categoryKey = packageName.substring(prefix.length());
        int nestedPackage = categoryKey.indexOf('.');
        if (nestedPackage >= 0) {
            categoryKey = categoryKey.substring(0, nestedPackage);
        }
        return categoryKey;
    }
}
