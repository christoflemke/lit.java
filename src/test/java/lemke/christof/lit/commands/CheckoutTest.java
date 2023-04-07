package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutTest extends BaseTest {
    @BeforeEach void setup() {
        git.init();
        write("a/foo.txt", "foo");
        write("c/modified.txt", "a");
        git.add(".");
        git.commit();
        git.branch("test");
        write("b/bar.txt", "bar");
        delete("a/foo.txt");
        write("c/modified.txt", "b");
        git.add(".");
        git.commit();
    }

    @Test void checkoutBranch() throws IOException {
        lit.checkout("test");
        assertTrue(Files.exists(root.resolve("a/foo.txt")));
        assertFalse(Files.exists(root.resolve("b/bar.txt")));
        assertEquals("a", Files.readString(root.resolve("c/modified.txt")));
    }

}
