package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
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
    public class IndexDifference extends BaseTest {
        @BeforeEach
        public void setup() {
            write("1.txt", "one");
            write("a/2.txt", "two");
            write("a/b/3.txt", "three");
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

        @Test
        public void itReportsFilesThatChangedMode() {
            makeExecutable("a/2.txt");

            lit.status();

            assertEquals("""
                              M a/2.txt
                             """, output());
        }

        @Test
        public void itReportsFilesWithUnchangedSize() {
            write("a/b/3.txt", "hello");

            lit.status();

            assertEquals("""
                              M a/b/3.txt
                             """, output());
        }

        @Test
        public void itDoesNotReportFilesIfOnlyTheTimestampHasChanges() {
            touch("1.txt");

            lit.status();

            assertEquals("", output());
        }

        @Test
        public void itReportsDeletedFiles() {
            delete("a/2.txt");

            lit.status();

            assertEquals("""
                              D a/2.txt
                             """, output());
        }

        @Test
        public void itReportsFilesInDeletedDirectories() {
            delete("a");

            lit.status();

            assertEquals("""
                              D a/2.txt
                              D a/b/3.txt
                             """, output());
        }


        @Test
        public void itReportsAFileAddedToATrackedDirectory() {
            write("a/4.txt", "four");
            lit.add(".");

            lit.status();

            assertEquals("""
                             A  a/4.txt
                             """, output());
        }

        @Test
        public void itReportsAFileAddedToAnUntrackedDirectory() {
            write("d/e/5.txt", "five");
            lit.add(".");

            lit.status();

            assertEquals("""
                             A  d/e/5.txt
                             """, output());
        }
    }
}
