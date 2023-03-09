package lemke.christof.lit.commands;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusTest extends BaseTest {
    @Test
    public void testListsUntrackedFilesInNameOrder() throws IOException {
        create("file.txt", "foo");
        create("another.txt", "foo");

        lit.status();
        assertEquals("""
                ?? another.txt
                ?? file.txt
                """, output());
    }

}
