package lemke.christof.lit;

import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class IndexTest extends BaseTest {

    @Test void minimalIndex() {
        write("test");
        Index index = repo.createIndex();
        index.add(Path.of("test"));
        index.commit();

        Index loaded = repo.createIndex();
        loaded.load();

        assertEquals(1, loaded.entries().size());
    }

    @Test void testIndex() throws IOException {
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


    @Test void updateTest() {
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

    @Test void updateContent() {
        {
            write("test", "foo");
            Index idx = repo.createIndex();
            idx.add(Path.of("test"));
            idx.commit();
        }
        final Oid expectedOid;
        {
            write("test", "bar");
            Index idx = repo.createIndex();
            idx.load();
            Blob blob = idx.add(Path.of("test"));
            expectedOid = blob.oid();
            idx.commit();
        }
        {
            Index idx = repo.createIndex();
            idx.load();
            Optional<Index.Entry> entry = idx.get(Path.of("test"));
            assertEquals(expectedOid, entry.get().oid());
        }
    }
}
