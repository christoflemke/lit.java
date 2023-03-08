package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class IndexTest {
    @Test
    public void testIndex() throws IOException {
        Path directory = Files.createTempDirectory("test-");
        Files.createDirectories(directory.resolve(".git"));
        Path path = directory.resolve("test");
        Files.writeString(path, "foo");
        Index index = new Index(directory);
        index.add(directory.relativize(path), "12348705fdbd3608cddbe904b67c731f3234c45b");
        index.commit();

        long size = Files.size(directory.resolve(".git").resolve("index"));
        assertEquals(104, size);
    }
}
