package lemke.christof.lit;

public class Util {
    public static String pad(int amount) {
        String result = "";
        for (int i = 0; i < amount; i++) {
            result += " ";
        }
        return result;
    }

    public static String rightPad(String s, int labelWidth) {
        if (s.length() <= labelWidth) {
            return s;
        } else {
            return s + pad(labelWidth - s.length());
        }
    }
}
