package lemke.christof.lit;

import lemke.christof.lit.model.Blob;
import lemke.christof.lit.model.Entry;
import lemke.christof.lit.model.Tree;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkspaceTest {
    @Test
    public void testListFiles() throws IOException {
        Path tmpDir = Files.createTempDirectory("test");
        Files.createFile(tmpDir.resolve("foo.txt"));
        assertEquals(
                List.of(Path.of("foo.txt")),
                new Workspace(tmpDir).listFiles().toList()
        );
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        Path root = Files.createTempDirectory("test");
        Path fooPath = root.resolve("foo.txt");
        Files.writeString(fooPath, "foo");
        Files.createDirectories(root.resolve("bin"));
        Path barPath = root.resolve("bin").resolve("bar.txt");
        Files.writeString(barPath, "bar");

        Workspace ws = new Workspace(root);
        Index idx = new Index(ws);
        idx.add(ws.toRelativePath(fooPath));
        idx.add(ws.toRelativePath(barPath));

        Workspace.BuildResult result = ws.buildTree(idx);

        Entry binEntry = new Entry(Path.of("bin"), "894306874c6757044f9df1b03119638a0fef743d", "40000");
        Entry fooEntry = new Entry(Path.of("foo.txt"), "19102815663d23f8b75a47e7a01965dcdc96468c", "100644");
        Entry barEntry = new Entry(Path.of("bin/bar.txt"), "ba0e162e1c47469e3fe4b393a8bf8c569f302116", "100644");

        Tree expectedTree = new Tree(List.of(
                binEntry,
                fooEntry
        ));

        System.out.println(result);
        assertEquals(expectedTree, result.root());

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
