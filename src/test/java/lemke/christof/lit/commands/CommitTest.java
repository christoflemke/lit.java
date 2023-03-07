package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Environment;
import lemke.christof.lit.Refs;
import lemke.christof.lit.Workspace;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommitTest {
    @Test
    public void testCommit() throws IOException {
        Path tempDirectory = Files.createTempDirectory("test-");

        Files.writeString(tempDirectory.resolve("foo.txt"), "foo");
        Files.createDirectories(tempDirectory.resolve("bin"));
        Files.writeString(tempDirectory.resolve("bin").resolve("bar.txt"), "bar");

        Workspace ws = new Workspace(tempDirectory);
        Database db = new Database(tempDirectory);
        Environment env = new Environment();
        Refs refs = new Refs(tempDirectory);

        CommitCommand command = new CommitCommand(ws, db, env, refs);

        command.run();
    }
}
