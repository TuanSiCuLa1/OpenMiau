package myau;

public final class ClientInfo {
    public static final String NAME = "Miau";
    public static final String VERSION = "${version}";
    public static final String MC_VERSION = "${mcversion}";
    public static final String GIT_COMMIT = "${gitCommit}";
    public static final boolean GITHUB_BUILD = Boolean.parseBoolean("${githubBuild}");

    private ClientInfo() {
    }

    public static String getBuildChannel() {
        return GITHUB_BUILD ? "dev" : "main";
    }

    public static String getDisplayVersion() {
        if (GITHUB_BUILD) {
            return NAME + " (dev) " + getGitVersion() + " | MC " + MC_VERSION;
        }
        return NAME + " (main) " + VERSION + " | MC " + MC_VERSION;
    }

    public static String getClickGuiVersion() {
        return GITHUB_BUILD ? getGitVersion() : VERSION;
    }

    public static String getGitVersion() {
        String commit = GIT_COMMIT != null && !GIT_COMMIT.isEmpty() && !"unknown".equalsIgnoreCase(GIT_COMMIT)
                ? GIT_COMMIT
                : "unknown";
        return "git-" + commit;
    }
}
