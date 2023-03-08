package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class IndexTest {
    @Test
    public void testIndex() throws IOException {
        Path root = Files.createTempDirectory("test-");
        Files.createDirectories(root.resolve(".git"));

        Path path = root.resolve("test");
        Files.writeString(path, "foo");

        Index index = new Index(root);
        index.add(root.relativize(path), "12348705fdbd3608cddbe904b67c731f3234c45b");
        index.commit();

        long size = Files.size(root.resolve(".git").resolve("index"));
        assertEquals(104, size);

        Index fromDisk = new Index(root);
        fromDisk.load();

        assertEquals(index.entries(), fromDisk.entries());
    }


    @Test
    public void updateTest() throws IOException {
        Path root = Files.createTempDirectory("test-");
        Files.createDirectories(root.resolve(".git"));

        {
            Path path = root.resolve("test");
            Files.writeString(path, "foo");
            Index index = new Index(root);
            index.add(root.relativize(path), "12348705fdbd3608cddbe904b67c731f3234c45b");
            index.commit();
            assertEquals(1, index.entries().size());
        }

        {
            Path path = root.resolve("test2");
            Files.writeString(path, "bar");
            Index index = new Index(root);
            index.load();
            index.add(root.relativize(path), "56788705fdbd3608cddbe904b67c731f3234c45b");
            index.commit();
            assertEquals(2, index.entries().size());
        }
    }
}
