package lemke.christof.lit.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeTest {

    Blob helloBlob = Blob.fromString("hello\n");
    Blob worldBlob = Blob.fromString("world\n");
    Entry helloEntry = new Entry(Path.of("hello.txt"), helloBlob.oid(), "100644");
    Entry worldEntry = new Entry(Path.of("world.txt"), worldBlob.oid(), "100644");
    Tree tree = new Tree(List.of(helloEntry, worldEntry));

    @Test
    public void testData() {
        String expected =
               "74726565203734003130303634342068"+
                        "656c6c6f2e74787400ce013625030ba8"+
                        "dba906f756967f9e9ca394464a313030"+
                        "36343420776f726c642e74787400cc62"+
                        "8ccd10742baea8241c5924df992b5c01"+
                        "9f71";
        assertEquals(expected, tree.hexData());
    }

    @Test
    public void testOid() {
        assertEquals("88e38705fdbd3608cddbe904b67c731f3234c45b", tree.oid().value());
    }
}
