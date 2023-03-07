package lemke.christof.lit.model;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
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
            byte[] oid = HexFormat.of().parseHex(e.oid());
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

    @Override
    public String toString() {
        return oid() + " entries: "+entries;
    }
}
