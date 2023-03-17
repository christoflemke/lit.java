package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
public class DiffTest extends BaseTest {

    @Test
    public void diffDeleted() {
        write("deleted");
        lit.add(".");
        lit.commit();

        delete("deleted");

        lit.diff();

        assertEquals("""
                         diff --git a/deleted b/deleted
                         deleted file mode 100644
                         index e69de2..000000
                         --- deleted
                         +++ /dev/null
                         """, lit.output());
    }

    @Test
    public void diffModified() {
        write("modified", "123");
        lit.add(".");
        lit.commit();

        write("modified", "456");

        lit.diff();

        assertEquals("""
                         diff --git a/modified b/modified
                         index d80088..ee2b83 100644
                         --- modified
                         +++ modified
                         """, lit.output());
    }
}
