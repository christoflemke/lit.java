package lemke.christof.lit.model;

import java.nio.file.Files;
import java.nio.file.Path;

public record Entry (Path name, String oid) {

    private boolean executable() {
        return Files.isExecutable(name);
    }


    public String mode() {
        return executable() ? "100755" : "100644";
    }
}
