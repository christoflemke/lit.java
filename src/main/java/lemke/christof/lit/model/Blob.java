package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;

public record Blob (byte[] data) implements DbObject {
    @Override
    public String type() {
        return "blob";
    }

    static Blob fromString(String s) {
        return new Blob(s.getBytes(StandardCharsets.UTF_8));
    }
}
