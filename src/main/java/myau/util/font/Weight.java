package myau.util.font;

public enum Weight {
    NONE(0, "Regular", "Normal"),
    LIGHT(1, "Light", "Thin"),
    MEDIUM(2, "Medium"),
    SEMI_BOLD(3, "SemiBold"),
    BOLD(4, "Bold", "Black");

    private final int num;
    private final String[] aliases;

    Weight(int num, String... aliases) {
        this.num = num;
        this.aliases = aliases;
    }

    public int getNum() {
        return num;
    }

    public String[] getAliases() {
        return aliases;
    }
}
