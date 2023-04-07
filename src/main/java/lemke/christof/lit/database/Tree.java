package lemke.christof.lit.database;

import lemke.christof.lit.FileMode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public record Tree(Map<Path, Entry> entries) implements DbObject {

    public static Tree fromList(Collection<Entry> entries) {
        return new Tree(entries.stream().collect(Collectors.toMap(Entry::name, identity())));
    }

    @Override
    public ObjectType type() {
        return ObjectType.TREE;
    }

    @Override
    public byte[] data() {
        List<byte[]> chunks = entries.values().stream()
            .sorted(Entry.byName)
            .map(e -> {
                byte[] mode = e.mode().toString().getBytes(StandardCharsets.UTF_8);
                byte[] oid = e.oid().toHexBytes();
                byte[] name = e.relativePath().toString().getBytes(StandardCharsets.UTF_8);

                int length = mode.length + 1 + name.length + 1 + oid.length;
                byte[] bytes = new byte[length];
                ByteBuffer buffer = ByteBuffer.wrap(bytes);

                buffer.put(mode);
                buffer.put(" ".getBytes(StandardCharsets.UTF_8));
                buffer.put(name);
                buffer.put((byte) 0);
                buffer.put(oid);
                return bytes;
            }).collect(Collectors.toList());
        int length = chunks.stream().map(b -> b.length).reduce((l1, l2) -> l1 + l2).orElseGet(() -> 0);
        byte[] result = new byte[length];
        ByteBuffer wrap = ByteBuffer.wrap(result);
        for (byte[] bs : chunks) {
            wrap.put(bs);
        }
        return result;
    }

    private static byte[] readUntil(ByteBuffer buffer, char stopByte) {
        int start = buffer.position();
        int stop = start;
        while (buffer.hasRemaining() && buffer.get() != stopByte) {
            stop++;
        }
        buffer.position(start);
        byte[] bytes = new byte[stop - start];
        buffer.get(bytes);
        buffer.get();
        return bytes;
    }

    public static Tree fromBytes(byte[] data) {
        ByteBuffer buff = ByteBuffer.wrap(data);
        Map<Path, Entry> entries = new TreeMap<>();
        while (buff.hasRemaining()) {
            byte[] mode = readUntil(buff, ' ');
            byte[] path = readUntil(buff, '\0');
            byte[] oid = new byte[20];
            buff.get(oid);
            String modeString = new String(mode);
            String pathString = new String(path);
            Oid oidString = Oid.fromBytes(oid);
            Path p = Path.of(pathString);
            entries.put(p, new Entry(p, oidString, FileMode.fromString(modeString)));
        }
        return new Tree(entries);
    }

    @Override
    public String toString() {
        return type() + " " + oid();
    }
}
