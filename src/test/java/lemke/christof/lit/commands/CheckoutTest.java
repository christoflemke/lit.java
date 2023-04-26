package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Index;
import lemke.christof.lit.Lit;
import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

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

    @Test void headIsUpdated() throws IOException {
        lit.checkout("test");

        assertEquals("ref: refs/heads/test\n", Files.readString(root.resolve(".git").resolve("HEAD")));
    }

    @Test void indexIsUpdated() {
        lit.checkout("test");

        Index index = repo.createIndex();
        index.load();
        assertTrue(index.get(Path.of("b/bar.txt")).isEmpty());
        assertTrue(index.get(Path.of("a/foo.txt")).isPresent());
        assertTrue(index.get(Path.of("c/modified.txt")).isPresent());
        assertEquals(
            repo.ws().read(Path.of("c/modified.txt")).get().oid(),
            index.get(Path.of("c/modified.txt")).get().oid()
        );
    }

    @Nested
    static class Ported extends BaseTest {
        Map<String, String> baseFiles = Map.of(
            "1.txt", "1",
            "outer/2.txt", "2",
            "outer/inner/3.txt", "3"
        );

        @BeforeEach void setup() {
            baseFiles.entrySet().forEach(e -> write(e.getKey(), e.getValue()));
            git.init();
            git.add(".");
            git.commit();
        }

        @Test void itUpdatesAChangedFile() {
            write("1.txt", "changed");
            Lit.LitCommand command = commitAndCheckout("@^");

            assertWorkspace(baseFiles);
            assertEquals("", command.output());
        }

        @Test void itFailsToUpdateAModifiedFile() {
            addCommit();

            write("1.txt", "conflict");
            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itFailsToUpdateAModifiedEqualFile() {
            addCommit();

            write("1.txt", "1");
            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itFailsToUpdateAModifiedModeFile() {
            addCommit();

            makeExecutable("1.txt");
            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itRestoresADeletedFile() {
            addCommit();

            delete("1.txt");
            lit.checkout("@^");

            assertWorkspace(baseFiles);
        }

        @Test void itRestoresFilesFromADeletedDirectory() {
            write("outer/inner/3.txt", "changed");
            commitAll();

            delete("outer");
            lit.checkout("@^");

            assertWorkspace(Map.of(
                "1.txt", "1",
                "outer/inner/3.txt", "3"
            ));
//            assertStatus("""
//                              D outer/2.txt""");
        }

        @Test void itFailsToUpdateAStagedFile() {
            addCommit();

            write("1.txt", "conflict");
            lit.add(".");

            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itUpdatesAStagedEqualFile() {
            addCommit();

            write("1.txt", "1");
            lit.add(".");

            Lit.LitCommand command = lit.checkout("@^");
            assertWorkspace(baseFiles);
            assertEquals("", command.output());
        }

        @Test void itFailsToUpdateAStagedModeFile() {
            addCommit();

            makeExecutable("1.txt");
            lit.add("1.txt");

            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itFailsToUpdateAnUnIndexedFile() {
            addCommit();

            delete("1.txt");
            delete(".git/index");
            lit.add(".");

            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        @Test void itFailsToUpdateAnUnIndexedAndUnTrackedFile() {
            addCommit();

            delete("1.txt");
            delete(".git/index");
            lit.add(".");
            write("1.txt", "conflict");

            assertStaleFile("1.txt", () -> lit.checkout("@^"));
        }

        private void addCommit() {
            write("1.txt", "changed");
            commitAll();
        }

        private void assertStatus(String expected) {
            assertEquals(expected, lit.statusPorcelain().output());
        }

        private void assertStaleFile(String file, Executable command) {
            RuntimeException exception = assertThrows(RuntimeException.class, command);
            assertEquals("""
                             error: Your local changes to the following files would be overwritten by checkout:
                             	%s
                             Please commit your changes or stash them before you switch branches.
                             Aborting""".formatted(file), exception.getMessage());
        }

        private Lit.LitCommand commitAndCheckout(String ref) {
            commitAll();
            return lit.checkout(ref);
        }

        private void commitAll() {
            lit.add(".");
            git.commit();
        }

        private void assertWorkspace(Map<String, String> baseFiles) {
            Map<String, String> workspaceFiles = new TreeMap<>();
            baseFiles.keySet().forEach(f -> {
                String wsText = repo.ws().readString(Path.of(f)).orElseGet(() -> "missing");
                workspaceFiles.put(f, wsText);
            });
            assertEquals(baseFiles, workspaceFiles);
        }

    }
}
