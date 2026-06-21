package myau.util.shader;

/**
 * Compatibility facade for modules that optionally draw blur/bloom regions.
 *
 * <p>The shader pipeline in this tree is implemented under {@code impl.rise}; older modules still
 * call this utility name. These methods intentionally no-op instead of failing compilation when the
 * optional post-processing bridge is unavailable.</p>
 */
public final class BlurUtils {
    private BlurUtils() {
    }

    public static void prepareBlur() {
    }

    public static void blurEnd(int passes, float radius) {
    }

    public static void prepareBloom() {
    }

    public static void bloomEnd(int passes, float radius) {
    }
}
