package lemke.christof.lit.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusTest extends BaseTest {
    @Test
    public void listsUntrackedFilesInNameOrder() {
        write("file.txt");
        write("another.txt");

        lit.status();
        assertEquals("""
                ?? another.txt
                ?? file.txt
                """, output());
    }

    @Test
    public void ignoresTrackedFiles() {
        write("committed.txt");
        lit.add("committed.txt");
        lit.commit();

        lit.status();
        assertEquals("", output());
    }

    @Test
    public void listsFilesAsUntrackedIfTheyAreNotInTheIndex() {
        write("committed.txt");
        lit.add("committed.txt");
        lit.commit();

        write("uncommitted.txt");

        lit.status();
        assertEquals("""
                ?? uncommitted.txt
                """, output());
    }

    @Test
    public void listsUntrackedDirectoriesNotTheirContents() {
        write("file.txt");
        write("dir/another.txt");

        lit.status();
        assertEquals("""
                ?? dir/
                ?? file.txt
                """, output());
    }

    @Test
    public void listsUntrackedFilesInsideTrackedDirectories() {
        write("a/b/inner.txt");
        lit.add(".");
        lit.commit();

        write("a/outer.txt");
        write("a/b/c/file.txt");

        lit.status();
        assertEquals("""
                ?? a/b/c/
                ?? a/outer.txt
                """, output());
    }

    @Nested
    public class IndexDifference extends BaseTest{
        @BeforeEach
        public void setup() {
            write("1.txt", "one");
            write("a/2.txt", "two");
            write("a/b/3.txt", "tree");
            lit.add(".");
            lit.commit();
        }

        @Test
        public void printsNothingIfNoFilesAreChanged() {
            lit.status();
            assertEquals("", output());
        }

        @Test
        public void reportsFilesWithModifiedContent() {
            write("1.txt", "changed");
            write("a/2.txt", "modified");

            lit.status();
            assertEquals("""
                     M 1.txt
                     M a/2.txt
                    """, output());
        }

    }
}
