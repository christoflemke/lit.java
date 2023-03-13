package lemke.christof.lit;

import java.io.*;

public record IO(InputStream in, PrintStream out, PrintStream err) {
    public static IO createDefault() {
        return new IO(System.in, System.out, System.err);
    }

    public IO withIn(InputStream replacement) {
        return new IO(replacement, out, err);
    }

    public IO withOut(PrintStream replacement) {
        return new IO(in, replacement, err);
    }

    public IO withErr(PrintStream replacement) {
        return new IO(in, out, replacement);
    }
}
