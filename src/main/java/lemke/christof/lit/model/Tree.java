package lemke.christof.lit.model;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public record Tree (List<Entry> entries) implements DbObject {
    @Override
    public String type() {
        return "tree";
    }

    @Override
    public byte[] data() {
        List<byte[]> chunks =  entries.stream().sorted(Entry.byName).map(e -> {
            byte[] mode = e.mode().getBytes(StandardCharsets.UTF_8);
            byte[] oid = e.oid().toHexBytes();
            byte[] name = e.relativePath().toString().getBytes(StandardCharsets.UTF_8);

            int length = mode.length + 1 + name.length + 1 + oid.length;
            byte[] bytes = new byte[length];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);

            buffer.put(mode);
            buffer.put(" ".getBytes(StandardCharsets.UTF_8));
            buffer.put(name);
            buffer.put((byte)0);
            buffer.put(oid);
            return bytes;
        }).collect(Collectors.toList());
        int length = chunks.stream().map(b -> b.length).reduce((l1, l2) -> l1 + l2).orElseGet(() -> 0);
        byte[] result = new byte[length];
        ByteBuffer wrap = ByteBuffer.wrap(result);
        for(byte[] bs : chunks) {
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
        List<Entry> entries = new ArrayList<>();
        while (buff.hasRemaining()) {
            byte[] mode = readUntil(buff, ' ');
            byte[] path = readUntil(buff, '\0');
            byte[] oid = new byte[20];
            buff.get(oid);
            String modeString = new String(mode);
            String pathString = new String(path);
            Oid oidString = Oid.fromBytes(oid);
            entries.add(new Entry(Path.of(pathString), oidString, modeString));
        }
        return new Tree(entries);
    }

    @Override
    public String toString() {
        return type() + " " + oid();
    }
}
