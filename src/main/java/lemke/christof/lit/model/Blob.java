package lemke.christof.lit.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record Blob (byte[] data) implements DbObject {
    @Override
    public ObjectType type() {
        return ObjectType.BLOB;
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
        if (obj instanceof Blob blob) {
            return Arrays.equals(this.data, blob.data);
        }
        return false;
    }

    public String stringData() {
        return new String(data, StandardCharsets.UTF_8);
    }
}
