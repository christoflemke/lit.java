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

        assertEquals("""
                        commit 2635aba50b5031bbd96997f4b33393b28c847fe1
                          tree bc342f35c92dbd0b376b0be6f78c641563863950
                            blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391
                            tree cb264b7ec91c6256e91b945dde3f5703fddb1ba3
                              blob e69de29bb2d1d6434b8b29ae775ad8c2e48c5391
                        """, litCommand.output());
    }
}
