package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchTest extends BaseTest {
    @BeforeEach void setup() {
        git.init();
        write("a");
        git.add(".");
        git.commit();
        write("b");
        git.add(".");
        git.commit();
    }

    @Test void branchOfMaster() throws IOException {
        lit.branch("test");
        Path branchPath = this.root.resolve(".git").resolve("refs").resolve("heads").resolve("test");
        assertTrue(branchPath.toFile().exists());
        assertEquals(this.repo.refs().readHead().get()+"\n", Files.readString(branchPath));
    }

    @Test void branchFromSha() throws IOException {
        Oid oid = repo.refs().readHead().get();
        lit.branch("test", oid.toString());
        Path branchPath = this.root.resolve(".git").resolve("refs").resolve("heads").resolve("test");
        assertTrue(branchPath.toFile().exists());
        assertEquals(this.repo.refs().readHead().get()+"\n", Files.readString(branchPath));
    }

    @Test void branchRelative() {
        lit.branch("test", "master^");
        Path branchPath = this.root.resolve(".git").resolve("refs").resolve("heads").resolve("test");
        assertTrue(branchPath.toFile().exists());
    }
}
