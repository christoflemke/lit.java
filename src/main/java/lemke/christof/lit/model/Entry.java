package lemke.christof.lit.model;

import java.nio.file.Path;

public record Entry (Path name, String oid) {
    public String mode() {
        return "100644";
    }
}
