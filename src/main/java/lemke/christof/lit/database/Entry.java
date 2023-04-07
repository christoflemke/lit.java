package lemke.christof.lit.database;

import lemke.christof.lit.FileMode;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

public record Entry (Path name, Oid oid, FileMode mode) {

    public Path relativePath() {
        return name.getFileName();
    }

    public static Comparator<Entry> byName = Comparator.comparing(o -> o.name);

    public boolean isTree() {
        return mode == FileMode.DIRECTORY;
    }

    public boolean isNotATree() {
        return !isTree();
    }
}
