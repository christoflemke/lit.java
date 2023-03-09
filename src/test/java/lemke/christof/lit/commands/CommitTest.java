package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommitTest {
    @Test
    public void testCommit() throws IOException {
        Path root = Files.createTempDirectory("test-");

        Path fooPath = root.resolve("foo.txt");
        Files.writeString(fooPath, "foo");
        Files.createDirectories(root.resolve("bin"));
        Path barPath = root.resolve("bin").resolve("bar.txt");
        Files.writeString(barPath, "bar");

        Workspace ws = new Workspace(root);
        Database db = new Database(root);
        Environment env = new Environment();
        Refs refs = new Refs(root);

        String[] args = {"add", ws.toRelativePath(fooPath).toString(), ws.toRelativePath(barPath).toString()};
        new InitCommand(new String[] {"init", root.toString()}).run();
        new AddCommand(ws, db, new Index(ws), args).run();
        CommitCommand command = new CommitCommand(ws, db, env, refs);

        command.run();
    }
}
