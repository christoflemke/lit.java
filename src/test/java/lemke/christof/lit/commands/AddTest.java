package lemke.christof.lit.commands;

import lemke.christof.lit.Database;
import lemke.christof.lit.Index;
import lemke.christof.lit.Repository;
import lemke.christof.lit.Workspace;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class AddTest {
    @Test
    public void testAdd() throws Exception {
        Path root = Files.createTempDirectory("test-");
        Files.createDirectories(root.resolve(".git"));
        Workspace ws = new Workspace(root);

        Path path = root.resolve("test");
        Files.writeString(path, "foo");

        Path sub = root.resolve("sub");
        Files.createDirectories(sub);
        Path subFile = sub.resolve("test.file");
        Files.writeString(subFile, "abc");

        Repository repo = Repository.create(root);

        String[] args = {"test", "sub"};
        new AddCommand(repo).run(args);
    }
}
