package lemke.christof.lit.model;

import lemke.christof.lit.Util;

import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Optional;

public record Oid(String value) {
    public static Oid of(String value) {
        String trimmed = value.trim();
        if(trimmed.length() != 40) {
            throw new RuntimeException("Oid should always 40 characters: "+trimmed);
        }
        return new Oid(trimmed);
    }

    public static Optional<Oid> ofNullable(String nullableString) {
        if(nullableString == null) {
            return Optional.empty();
        }
        return Optional.of(Oid.of(nullableString));
    }

    public static Oid fromBytes(byte[] bytes) {
        return Oid.of(HexFormat.of().formatHex(bytes));
    }

    private Path dirPath(Path parent) {
        return parent.resolve(value.substring(0, 2));
    }

    private Path filePath(Path parent) {
        return parent.resolve(value.substring(2));
    }

    public Path objectPath(Path objectsPath) {
        Path dirPath = dirPath(objectsPath);
        return filePath(dirPath);
    }

    public String shortOid() {
        return value.substring(0, 7);
    }

    public byte[] toHexBytes() {
        return HexFormat.of().parseHex(value);
    }

    @Override public String toString() {
        return value;
    }
}
