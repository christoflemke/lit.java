package lemke.christof.lit;

import java.io.*;

public interface IO {
    InputStream in();

    BufferedWriter out();

    BufferedWriter err();

    public static IO createDefault() {
        return new IO() {
            @Override
            public InputStream in() {
                return System.in;
            }

            @Override
            public BufferedWriter out() {
                return new BufferedWriter(new OutputStreamWriter(System.out));
            }

            @Override
            public BufferedWriter err() {
                return new BufferedWriter(new OutputStreamWriter(System.err));
            }
        };
    }
}
