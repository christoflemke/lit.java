package lemke.christof.lit.database;

import java.nio.file.Path;
import java.util.Comparator;

public record Entry (Path name, Oid oid, String mode) {

    public Path relativePath() {
        return name.getFileName();
    }

    public static Comparator<Entry> byName = Comparator.comparing(o -> o.name);
}
