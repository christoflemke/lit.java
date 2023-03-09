package lemke.christof.lit.commands;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusTest extends BaseTest {
    @Test
    public void testListsUntrackedFilesInNameOrder() {
        create("file.txt");
        create("another.txt");

        lit.status();
        assertEquals("""
                ?? another.txt
                ?? file.txt
                """, output());
    }

    @Test
    public void testListsFilesAsUntrackedIfTheyAreNotInTheIndex() {
        create("committed.txt");
        lit.add("committed.txt");
        lit.commit();

        create("uncommitted.txt");

        lit.status();
        assertEquals("""
                ?? uncommitted.txt
                """, output());
    }
}
