package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlobTest {

    @Test
    public void testOid() {
        Blob blob = Blob.fromString("hello\n");
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", blob.oid());
    }

    @Test
    public void testBytes() throws UnsupportedEncodingException {
        Blob blob = Blob.fromString("hello\n");
        byte[] expected = HexFormat.of().parseHex("626c6f6220360068656c6c6f0a");
        byte[] actual = blob.diskData();
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testCompressedBytes() {
        Blob blob = Blob.fromString("hello\n");
        String expected = "78014bcac94f523063c848cdc9c9e702001dc50414";
        String actual = HexFormat.of().formatHex(blob.compressedData());
        assertEquals(expected, actual);
    }
}
