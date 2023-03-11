package lemke.christof.lit.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public record Entry (Path name, String oid, String mode) {

    public Path relativePath() {
        return name.getFileName();
    }

    public static Comparator<Entry> byName = Comparator.comparing(o -> o.name);
}
