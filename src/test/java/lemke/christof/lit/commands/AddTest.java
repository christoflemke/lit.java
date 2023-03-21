package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Lit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class AddTest extends BaseTest {
    @Test
    public void testAdd() throws Exception {
        write("test", "foo");
        write("sub/test.file", "abc");

        Lit.LitCommand command = lit.add("test", "sub");

        assertEquals("", command.output());
    }
}
