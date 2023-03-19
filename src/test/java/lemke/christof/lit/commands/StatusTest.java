package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Git;
import lemke.christof.lit.Index;
import lemke.christof.lit.Lit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class StatusTest extends BaseTest {

    @BeforeEach
    public void setup() {
        git.init();
    }

    @Test
    public void listsUntrackedFilesInNameOrder() {
        write("file.txt");
        write("another.txt");
        git.add(".");
        git.commit();
        write("added.txt");
        lit.add("added.txt");

        validateStatus("""
                           A  added.txt
                           """);
    }

    @Test
    public void ignoresTrackedFiles() {
        write("committed.txt");
        git.add("committed.txt");
        git.commit();

        validateStatus("");
    }

    @Test
    public void listsFilesAsUntrackedIfTheyAreNotInTheIndex() {
        write("committed.txt");
        git.add("committed.txt");
        git.commit();

        write("uncommitted.txt");

        validateStatus("""
                           ?? uncommitted.txt
                           """);
    }

    @Test
    public void listsUntrackedDirectoriesNotTheirContents() {
        write("file.txt");
        write("dir/another.txt");

        validateStatus("""
                           ?? dir/
                           ?? file.txt
                           """);
    }

    @Test
    public void listsUntrackedFilesInsideTrackedDirectories() {
        write("a/b/inner.txt");
        git.add(".");
        git.commit();

        write("a/outer.txt");
        write("a/b/c/file.txt");

        validateStatus("""
                           ?? a/b/c/
                           ?? a/outer.txt
                           """);
    }

    @Test
    public void updatesIndexOnTimestampChange() {
        write("test");
        git.add(".");

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
    public class IndexDifference {
        @BeforeEach
        public void setup() {
            git.init();
            write("1.txt", "one");
            write("a/2.txt", "two");
            write("a/b/3.txt", "three");
            git.add(".");
            git.commit();
        }

        @Test
        public void printsNothingIfNoFilesAreChanged() {
            validateStatus("");
        }

        @Test
        public void reportsFilesWithModifiedContent() {
            write("1.txt", "changed");
            write("a/2.txt", "modified");

            validateStatus("""
                                M 1.txt
                                M a/2.txt
                               """);
        }

        @Test
        public void itReportsFilesThatChangedMode() {
            makeExecutable("a/2.txt");

            validateStatus("""
                                M a/2.txt
                               """);
        }

        @Test
        public void itReportsFilesWithUnchangedSize() {
            write("a/b/3.txt", "hello");

            validateStatus("""
                                M a/b/3.txt
                               """);
        }

        @Test
        public void itDoesNotReportFilesIfOnlyTheTimestampHasChanges() {
            touch("1.txt");

            validateStatus("");
        }

        @Test
        public void itReportsDeletedFiles() {
            delete("a/2.txt");

            Lit.LitCommand litCommand = lit.statusPorcelain();
            Git.GitCommand gitCommand = git.statusPorcelain();

            validateStatus("""
                                D a/2.txt
                               """);
        }

        @Test
        public void itReportsFilesInDeletedDirectories() {
            delete("a");

            validateStatus("""
                                D a/2.txt
                                D a/b/3.txt
                               """);
        }


        @Test
        public void itReportsAFileAddedToATrackedDirectory() {
            write("a/4.txt", "four");
            lit.add(".");

            validateStatus("""
                               A  a/4.txt
                               """);
        }

        @Test
        public void itReportsAFileAddedToAnUntrackedDirectory() {
            write("d/e/5.txt", "five");
            lit.add(".");

            Lit.LitCommand litCommand = lit.statusPorcelain();
            Git.GitCommand gitCommand = git.statusPorcelain();

            assertEquals(gitCommand.output(), litCommand.output());
            validateStatus("""
                               A  d/e/5.txt
                               """);
        }
    }

    @Nested
    public class IndexHead {
        @Test
        public void deletedInIndex() {
            write("deleted");
            git.add("deleted");
            git.commit();

            git.delete("deleted");

            validateStatus("""
                             D  deleted
                             """);
        }

        @Test
        public void modifiedInIndex() {
            write("modified", "123");
            git.add("modified");
            git.commit();

            write("modified", "456");
            git.add(".");

            validateStatus("""
                               M  modified
                               """);
        }

        @Test
        public void addedInIndex() {
            write("added");
            git.add("added");

            validateStatus("""
                               A  added
                               """);
        }
    }

    private void validateStatus(String expected) {
        Git.GitCommand gitCommand = git.statusPorcelain();
        Lit.LitCommand litCommand = lit.statusPorcelain();
        assertEquals(expected, litCommand.output());
        assertEquals(gitCommand.output(), litCommand.output());
    }

}
