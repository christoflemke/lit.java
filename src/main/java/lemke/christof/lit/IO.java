package lemke.christof.lit;

import java.io.*;

public interface IO {
    InputStream in();

    PrintStream out();

    PrintStream err();

    public static IO createDefault() {
        return new IO() {
            @Override
            public InputStream in() {
                return System.in;
            }

            @Override
            public PrintStream out() {
                return System.out;
            }

            @Override
            public PrintStream err() {
                return System.err;
            }
        };
    }
}
