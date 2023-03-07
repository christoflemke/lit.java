package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
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
}
