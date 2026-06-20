package myau.module;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.modules.render.ClickGUI;
import myau.module.modules.render.HUD;
import myau.util.client.ChatUtil;
import myau.util.client.SoundUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModuleManager {
    private static final Logger LOGGER = Logger.getLogger(ModuleManager.class.getName());
    private static final String MODULE_PACKAGE = "myau.module.modules.";
    private static final Map<String, String> CATEGORY_NAMES = new LinkedHashMap<>();

    static {
        CATEGORY_NAMES.put("combat", "Combat");
        CATEGORY_NAMES.put("movement", "Movement");
        CATEGORY_NAMES.put("render", "Render");
        CATEGORY_NAMES.put("player", "Player");
        CATEGORY_NAMES.put("misc", "Misc");
        CATEGORY_NAMES.put("latency", "Latency");
        CATEGORY_NAMES.put("minigames", "Minigames");
        CATEGORY_NAMES.put("target", "Target");
    }

    private boolean sound = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public LinkedHashMap<String, List<Module>> getModulesByCategory() {
        LinkedHashMap<String, List<Module>> categories = new LinkedHashMap<>();
        for (String categoryName : CATEGORY_NAMES.values()) {
            categories.put(categoryName, new ArrayList<>());
        }

        for (Module module : this.modules.values()) {
            String categoryName = getCategoryName(module);
            if (categoryName != null) {
                categories.get(categoryName).add(module);
            }
        }

        Comparator<Module> byName = Comparator.comparing(module -> module.getName().toLowerCase());
        categories.values().forEach(modules -> modules.sort(byName));
        categories.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return categories;
    }

    private static String getCategoryName(Module module) {
        String packageName = module.getClass().getPackage().getName();
        if (!packageName.startsWith(MODULE_PACKAGE)) {
            return null;
        }

        String categoryKey = packageName.substring(MODULE_PACKAGE.length());
        int nestedPackage = categoryKey.indexOf('.');
        if (nestedPackage >= 0) {
            categoryKey = categoryKey.substring(0, nestedPackage);
        }
        return CATEGORY_NAMES.get(categoryKey);
    }

    public void playSound() {
        this.sound = true;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        List<Module> matchedModules = new ArrayList<>();
        for (Module module : new ArrayList<>(this.modules.values())) {
            if (module.getKey() == event.getKey()) {
                matchedModules.add(module);
            }
        }

        for (Module module : matchedModules) {
            try {
                boolean shouldNotify = module.toggle();
                HUD hud = (HUD) this.modules.get(HUD.class);
                if (hud != null && shouldNotify) {
                    shouldNotify = hud.toggleAlerts.getValue();
                }
                if (module instanceof ClickGUI) {
                    shouldNotify = false;
                }
                if (shouldNotify) {
                    String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                    ChatUtil.display("%s: %s&r", module.getName(), status);
                }
            } catch (Throwable throwable) {
                LOGGER.log(Level.WARNING, String.format("Failed to toggle module on key press: module=%s key=%d", module.getName(), event.getKey()), throwable);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                SoundUtil.playSound("random.click");
            }
        }
    }
}
