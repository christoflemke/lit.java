package lemke.christof.lit;

import lemke.christof.lit.commands.ShowHeadCommand;
import org.junit.jupiter.api.Test;

public class ShowHeadTest extends BaseTest {
    @Test
    public void showHead() {
        write("1.txt");
        lit.add("1.txt");
        lit.commit();

        new ShowHeadCommand(repo).run(new String[] {});

        write("a/2.txt");
        lit.add("a/2.txt");
        lit.commit();

        new ShowHeadCommand(repo).run(new String[] {});
    }
}
