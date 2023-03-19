package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record Blob (byte[] data) implements DbObject {
    @Override
    public String type() {
        return "blob";
    }

    public static Blob fromString(String s) {
        return new Blob(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        return type() + " " + oid();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Blob) {
            return Arrays.equals(this.data, ((Blob) obj).data);
        }
        return false;
    }

    public String stringData() {
        return new String(data, StandardCharsets.UTF_8);
    }
}
