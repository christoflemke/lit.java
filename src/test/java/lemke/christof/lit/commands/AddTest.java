package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Index;
import lemke.christof.lit.Workspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class AddTest {
    @Test
    public void testAdd() throws Exception {
        Path root = Files.createTempDirectory("test-");
        Files.createDirectories(root.resolve(".git"));

        Path path = root.resolve("test");
        Files.writeString(path, "foo");

        Path sub = root.resolve("sub");
        Files.createDirectories(sub);
        Path subFile = sub.resolve("test.file");
        Files.writeString(subFile, "abc");

        String[] args = {"add", "test", "sub"};
        AddCommand command = new AddCommand(new Workspace(root), new Database(root), new Index(root), args);

        command.run();
    }
}
