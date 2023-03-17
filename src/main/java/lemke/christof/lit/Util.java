package lemke.christof.lit;

public class Util {
    public static String pad(int amount) {
        return repeat(" ", amount);
    }

    public static String repeat(String s, int amount) {
        String result = "";
        for (int i = 0; i < amount; i++) {
            result += s;
        }
        return result;
    }

    public static String rightPad(String s, int labelWidth) {
        if (s.length() <= labelWidth) {
            return s + pad(labelWidth - s.length());
        } else {
            return s;
        }
    }
}
