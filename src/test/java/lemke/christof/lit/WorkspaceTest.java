package lemke.christof.lit;

import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.Entry;
import lemke.christof.lit.model.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkspaceTest {
    @Test
    public void testListFiles() throws IOException {
        Path tmpDir = Files.createTempDirectory("test");
        Files.createFile(tmpDir.resolve("foo.txt"));
        assertEquals(
                List.of(Path.of("foo.txt")),
                new Workspace(tmpDir).listFiles()
        );
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        Path tmpDir = Files.createTempDirectory("test");
        Files.writeString(tmpDir.resolve("foo.txt"), "foo");
        Files.createDirectories(tmpDir.resolve("bin"));
        Files.writeString(tmpDir.resolve("bin").resolve("bar.txt"), "bar");

        Workspace.BuildResult result = new Workspace(tmpDir).buildTree();

        Entry binEntry = new Entry(Path.of("bin"), "894306874c6757044f9df1b03119638a0fef743d");
        Entry fooEntry = new Entry(Path.of("foo.txt"), "19102815663d23f8b75a47e7a01965dcdc96468c");
        Entry barEntry = new Entry(Path.of("bin/bar.txt"), "ba0e162e1c47469e3fe4b393a8bf8c569f302116");

        Tree expectedTree = new Tree(List.of(
                binEntry,
                fooEntry
        ));

        System.out.println(result.toString());
        assertEquals(expectedTree, result.root());

        assertEquals(2, result.blobs().size(), 2);
        assertEquals(result.blobs().get(0), Blob.fromString("bar"));
        assertEquals(result.blobs().get(1), Blob.fromString("foo"));

        assertEquals(2, result.trees().size());
        assertEquals(
                new Tree(List.of(barEntry)),
                result.trees().get(0)
        );
        assertEquals(
                new Tree(List.of(
                        binEntry,
                        fooEntry
                )),
                result.trees().get(1)
        );
    }
}
