package lemke.christof.lit.commands;

import lemke.christof.lit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class CommitTest {

    private Repository repo;

    @BeforeEach
    public void setup() throws IOException {
        Path root = Files.createTempDirectory("test-");
        repo = Repository.create(root);
        new InitCommand().run(new String[] {root.toString()});
    }

    void create(String path, String data) throws IOException {
        Path fooPath = repo.ws().resolve(path);
        Files.createDirectories(fooPath.getParent());
        Files.writeString(fooPath, data);
    }

    void add(String... files) {
        new AddCommand(repo).run(files);
    }

    private void commit() {
        new CommitCommand(repo).run(new String[]{});
    }

    @Test
    public void testCommit() throws IOException {
        create("foo.txt", "foo");
        create("bin/bar.txt", "bar");

        add("foo.txt", "bin/bar.txt");
        commit();
    }
}
