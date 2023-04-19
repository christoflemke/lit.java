package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Lit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowHeadTest extends BaseTest {
    @Test
    public void showHead() {
        write("1.txt");
        lit.add("1.txt");
        write("a/2.txt");
        lit.add("a/2.txt");
        lit.commit();


        Lit.LitCommand litCommand = lit.showHead();
//7ac5e0a102bcab3eac0109c283592d4a70bbb1b5
        assertEquals("""
                         commit 488c62f95f9336af7068695900b10d576eb3afa0
                           tree 7ac5e0a102bcab3eac0109c283592d4a70bbb1b5
                             blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391
                             tree cb264b7ec91c6256e91b945dde3f5703fddb1ba3
                               blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391
                         """, litCommand.output());
    }
}
