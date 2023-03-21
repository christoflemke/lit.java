package lemke.christof.lit;

public enum Color {
    GREEN(32),
    RED(31),
    BOLD(1),
    CYAN(36);

    public static final String RESET_CODE = "\u001B[m";
    final int code;

    Color(int code) {
        this.code = code;
    }

    public String format(String in, boolean useColor) {
        if (useColor) {
            return "\u001B[" + code + "m" + in + RESET_CODE;
        } else {
            return in;
        }
    }

    public static String justReset(String in, boolean useColor) {
        if(useColor) {
            return in + RESET_CODE;
        } else {
            return in;
        }
    }
}
