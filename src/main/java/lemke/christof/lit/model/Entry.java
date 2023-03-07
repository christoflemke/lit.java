package lemke.christof.lit.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public record Entry (Path name, String oid) {

    private boolean executable() {
        return Files.isExecutable(name);
    }

    public Path relativePath() {
        return name.getFileName();
    }

    public String mode() {
        if (Files.isDirectory(name)) {
            return "40000";
        } else if(executable()) {
            return "100755";
        } else {
            return "100644";
        }
    }

    public static Comparator<Entry> byName = new Comparator<Entry>() {
        @Override
        public int compare(Entry o1, Entry o2) {
            return o1.name.compareTo(o2.name);
        }
    };
}
