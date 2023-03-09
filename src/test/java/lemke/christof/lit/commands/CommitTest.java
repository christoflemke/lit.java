package lemke.christof.lit.commands;

import lemke.christof.lit.Index;
import lemke.christof.lit.Lit;
import lemke.christof.lit.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class CommitTest {

    private Repository repo;
    private Lit lit;

    @BeforeEach
    public void setup() throws IOException {
        Path root = Files.createTempDirectory("test-");
        repo = Repository.create(root);
        lit = new Lit(repo);
        lit.init(root.toString());
    }

    void create(String path, String data) throws IOException {
        Path fooPath = repo.ws().resolve(path);
        Files.createDirectories(fooPath.getParent());
        Files.writeString(fooPath, data);
    }

    @Test
    public void testCommit() throws IOException {
        create("foo.txt", "foo");
        create("bin/bar.txt", "bar");

        lit.add("foo.txt", "bin/bar.txt");
        lit.commit();

        Index index = repo.createIndex();
        index.load();

        assertEquals(2, index.entries().size());
    }
}
