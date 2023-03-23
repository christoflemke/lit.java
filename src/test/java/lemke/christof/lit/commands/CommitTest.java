package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Index;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CommitTest extends BaseTest {

    @Test
    public void testCommit() throws IOException {
        write("foo.txt", "foo");
        write("bin/bar.txt", "bar");

        lit.add("foo.txt", "bin/bar.txt");
        lit.commit();

        Index index = repo.createIndex();
        index.load();

        assertEquals(2, index.entries().size());
    }

    @Test public void commitWithParent() {
        lit.init();
        write("a");
        lit.add("a");
        lit.commit();

        write("b");
        lit.add("b");
        lit.commit();
        System.out.println("");
    }
}
