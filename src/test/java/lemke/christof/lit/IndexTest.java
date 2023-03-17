package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class IndexTest extends BaseTest {

    @Test
    public void minimalIndex() {
        write("test");
        Index index = repo.createIndex();
        index.add(Path.of("test"));
        index.commit();

        Index loaded = repo.createIndex();
        loaded.load();

        assertEquals(1, loaded.entries().size());
    }

    @Test
    public void testIndex() throws IOException {
        write("test", "foo");

        Index index = repo.createIndex();
        index.add(Path.of("test"));
        index.commit();

        long size = Files.size(index.indexPath());
        assertEquals(104, size);

        Index fromDisk = repo.createIndex();
        fromDisk.load();

        assertEquals(index.entries(), fromDisk.entries());
    }


    @Test
    public void updateTest() throws IOException {
        {
            write("test", "foo");
            Index index = repo.createIndex();
            index.add(Path.of("test"));
            index.commit();
            assertEquals(1, index.entries().size());
        }

        {
            write("test2", "bar");
            Index index = repo.createIndex();
            index.load();
            index.add(Path.of("test2"));
            index.commit();
            assertEquals(2, index.entries().size());
        }
    }
}
