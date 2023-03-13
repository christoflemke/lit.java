package lemke.christof.lit;

import java.nio.file.Path;

public class TestUtil {
    public static Path projectRoot() {
        String path = TestUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        return Path.of(path).getParent().getParent().getParent().getParent();
    }
}
