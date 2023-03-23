package lemke.christof.lit.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public interface DbObject {

    String type();

    byte[] data();

    default String hexData() {
        return HexFormat.of().formatHex(diskData());
    }

    default byte[] diskData() {
        ByteBuffer
                buffer = ByteBuffer.allocateDirect(data().length + 20)
                .put((type() + " " + data().length).getBytes(StandardCharsets.UTF_8))
                .put((byte) 0)
                .put(data());

        byte[] result = new byte[buffer.position()];
        buffer.limit(buffer.position());
        buffer.rewind();
        buffer.get(result);
        return result;
    }

    default byte[] compressedData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream deflate = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_SPEED));
        try {
            deflate.write(diskData());
            deflate.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return out.toByteArray();
    }

    default Oid oid() {
        byte[] sum = sha1().digest(diskData());
        return Oid.fromBytes(sum);
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
