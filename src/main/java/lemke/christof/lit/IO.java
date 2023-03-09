package lemke.christof.lit;

import java.io.InputStream;
import java.io.OutputStream;

public record IO (InputStream in, OutputStream out, OutputStream err) {
}
