package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class StatusTest extends BaseTest {
    @Test
    public void listsUntrackedFilesInNameOrder() {
        write("file.txt");
        write("another.txt");
        lit.add(".");
        lit.commit();
        write("added.txt");
        lit.add("added.txt");

        lit.statusPorcelain();
        assertEquals("""
                         A  added.txt
                         """, output());
    }

    @Test
    public void ignoresTrackedFiles() {
        write("committed.txt");
        lit.add("committed.txt");
        lit.commit();

        lit.statusPorcelain();
        assertEquals("", output());
    }

    @Test
    public void listsFilesAsUntrackedIfTheyAreNotInTheIndex() {
        write("committed.txt");
        lit.add("committed.txt");
        lit.commit();

        write("uncommitted.txt");

        lit.statusPorcelain();
        assertEquals("""
                         ?? uncommitted.txt
                         """, output());
    }

    @Test
    public void listsUntrackedDirectoriesNotTheirContents() {
        write("file.txt");
        write("dir/another.txt");

        lit.statusPorcelain();
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

        lit.statusPorcelain();
        assertEquals("""
                         ?? a/b/c/
                         ?? a/outer.txt
                         """, output());
    }

    @Test
    public void updatesIndexOnTimestampChange() {
        write("test");
        lit.add(".");

        Index index = repo.createIndex();
        index.load();
        Index.Entry entry = index.entries().stream().findFirst().get();
        int mtime_nano = entry.stat().mtime_nano();

        touch("test");
        lit.statusPorcelain();

        index = repo.createIndex();
        index.load();
        entry = index.entries().stream().findFirst().get();
        int mtime_nanoAfter = entry.stat().mtime_nano();
        assertNotEquals(mtime_nano, mtime_nanoAfter);
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
            lit.statusPorcelain();
            assertEquals("", output());
        }

        @Test
        public void reportsFilesWithModifiedContent() {
            write("1.txt", "changed");
            write("a/2.txt", "modified");

            lit.statusPorcelain();
            assertEquals("""
                              M 1.txt
                              M a/2.txt
                             """, output());
        }

        @Test
        public void itReportsFilesThatChangedMode() {
            makeExecutable("a/2.txt");

            lit.statusPorcelain();

            assertEquals("""
                              M a/2.txt
                             """, output());
        }

        @Test
        public void itReportsFilesWithUnchangedSize() {
            write("a/b/3.txt", "hello");

            lit.statusPorcelain();

            assertEquals("""
                              M a/b/3.txt
                             """, output());
        }

        @Test
        public void itDoesNotReportFilesIfOnlyTheTimestampHasChanges() {
            touch("1.txt");

            lit.statusPorcelain();

            assertEquals("", output());
        }

        @Test
        public void itReportsDeletedFiles() {
            delete("a/2.txt");

            lit.statusPorcelain();

            assertEquals("""
                              D a/2.txt
                             """, output());
        }

        @Test
        public void itReportsFilesInDeletedDirectories() {
            delete("a");

            lit.statusPorcelain();

            assertEquals("""
                              D a/2.txt
                              D a/b/3.txt
                             """, output());
        }


        @Test
        public void itReportsAFileAddedToATrackedDirectory() {
            write("a/4.txt", "four");
            lit.add(".");

            lit.statusPorcelain();

            assertEquals("""
                             A  a/4.txt
                             """, output());
        }

        @Test
        public void itReportsAFileAddedToAnUntrackedDirectory() {
            write("d/e/5.txt", "five");
            lit.add(".");

            lit.statusPorcelain();

            assertEquals("""
                             A  d/e/5.txt
                             """, output());
        }
    }
}
