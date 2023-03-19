package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Git;
import lemke.christof.lit.Lit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffTest extends BaseTest {
    @BeforeEach
    public void setup() {
        git.init();
    }

    @Nested
    class IndexWorkspace {
        @Test
        public void diffDeleted() {
            write("deleted");
            lit.add(".");
            lit.commit();

            delete("deleted");

            Lit.LitCommand litCommand = lit.diff();

            assertEquals("""
                             diff --git a/deleted b/deleted
                             deleted file mode 100644
                             index e69de2..000000
                             --- deleted
                             +++ /dev/null
                             """, litCommand.output());
        }

        @Test
        public void diffModified() {
            write("modified", "123");
            lit.add(".");
            lit.commit();

            write("modified", "456");

            Lit.LitCommand litCommand = lit.diff();

            assertEquals("""
                             diff --git a/modified b/modified
                             index d80088..ee2b83 100644
                             --- modified
                             +++ modified
                             """, litCommand.output());
        }
    }

    @Nested
    class HeadIndex {
        @Test
        public void diffModified() {
            write("modified", "123");
            lit.add("modified");
            lit.commit();
            write("modified", "456");
            lit.add("modified");

            Lit.LitCommand litCommand = lit.diffCached();

            assertEquals("""
                             diff --git a/modified b/modified
                             index d80088..ee2b83 100644
                             --- modified
                             +++ modified
                             """, litCommand.output());
        }

        @Test
        public void diffAdded() {
            write("added", "123");
            lit.add("added");

            Lit.LitCommand litCommand = lit.diffCached();

//            assertEquals("""
//                             diff --git a/added b/added
//                             new file mode 100644
//                             index 000000..d80088
//                             --- /dev/null
//                             +++ added
//                               """, litCommand.output());
            validateDiffCached("""
                             diff --git a/added b/added
                             new file mode 100644
                             index 000000..d80088
                             --- /dev/null
                             +++ added
                               """);
        }
    }

    void validateDiffCached(String expected) {
        Lit.LitCommand litCommand = lit.diffCached();
        Git.GitCommand gitCommand = git.diffCached();
        assertEquals(gitCommand.output(), litCommand.output());
        assertEquals(expected, litCommand.output());
    }
}
