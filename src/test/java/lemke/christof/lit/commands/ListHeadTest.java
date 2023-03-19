package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Lit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ListHeadTest extends BaseTest {
    @Test
    public void listHead() {
        write("1.txt");
        lit.add("1.txt");
        write("a/2.txt");
        lit.add("a/2.txt");
        lit.commit();

        Lit.LitCommand litCommand = lit.listHead();

        assertEquals("""
                         100644 e69de29bb2d1d6434b8b29ae775ad8c2e48c5391 1.txt
                         100644 e69de29bb2d1d6434b8b29ae775ad8c2e48c5391 a/2.txt
                         """, litCommand.output());
    }
}
