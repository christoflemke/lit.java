package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Index;
import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutTest extends BaseTest {
    @BeforeEach void setup() {
        git.init();
        write("a/foo.txt", "foo");
        write("c/modified.txt", "a");
        git.add(".");
        git.commit();
        git.branch("test");
        write("b/bar.txt", "bar");
        delete("a/foo.txt");
        write("c/modified.txt", "b");
        git.add(".");
        git.commit();
    }

    @Test void workspaceIsUpdated() throws IOException {
        lit.checkout("test");

        assertTrue(Files.exists(root.resolve("a/foo.txt")));
        assertFalse(Files.exists(root.resolve("b/bar.txt")));
        assertEquals("a", Files.readString(root.resolve("c/modified.txt")));
    }

    @Test void headIsUpdated() {
        Oid expected = repo.refs().resolveCommit("test");

        lit.checkout("test");

        assertEquals(expected, repo.refs().readHead().get());
    }

    @Test void indexIsUpdated() {
        lit.checkout("test");

        Index index = repo.createIndex();
        index.load();
        assertTrue(index.get(Path.of("b/bar.txt")).isEmpty());
        assertTrue(index.get(Path.of("a/foo.txt")).isPresent());
        assertTrue(index.get(Path.of("c/modified.txt")).isPresent());
        assertEquals(
            repo.ws().read(Path.of("c/modified.txt")).oid(),
            index.get(Path.of("c/modified.txt")).get().oid()
        );
    }
}
