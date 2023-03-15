package lemke.christof.lit;

public class Diff {
    public static void diff(String a, String b) {
        new Meyers(a.split("\n"), b.split("\n")).shortestEdit();
    }
}
