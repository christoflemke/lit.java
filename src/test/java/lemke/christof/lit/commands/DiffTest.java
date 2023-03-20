package lemke.christof.lit.commands;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.Git;
import lemke.christof.lit.Lit;
import lemke.christof.lit.diff.Diff;
import lemke.christof.lit.diff.Edit;
import lemke.christof.lit.diff.Hunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffTest extends BaseTest {
    @BeforeEach
    public void setup() {
        git.init();
    }

    @Nested
    class IndexWorkspace {
        @Test
        public void diffDeleted() {
            write("deleted", "hello\n");
            lit.add(".");
            lit.commit();

            delete("deleted");

            validate("""
                         diff --git a/deleted b/deleted
                         deleted file mode 100644
                         index ce01362..0000000
                         --- a/deleted
                         +++ /dev/null
                         @@ -1 +0,0 @@
                         -hello
                         """);
        }

        @Test
        public void diffModified() {
            write("modified", "abc\n123\n1\n");
            lit.add(".");
            lit.commit();

            write("modified", "abc\n456\n1\n");

            Lit.LitCommand litCommand = lit.diff();

            assertEquals("""
                             diff --git a/modified b/modified
                             index 6d123cb..054b7f5 100644
                             --- a/modified
                             +++ b/modified
                             @@ -1,3 +1,3 @@
                              abc
                             -123
                             +456
                              1
                             """, litCommand.output());
        }

        void validate(String expected) {
            Lit.LitCommand litCommand = lit.diff();
            Git.GitCommand gitCommand = git.diff();
            String gitOutput = gitCommand.output();
            String litOutput = litCommand.output();
            assertEquals(gitOutput, litOutput);
            assertEquals(expected, litOutput);
        }
    }

    @Nested
    class HeadIndex {
        @Test
        public void diffModified() {
            write("modified", "abc\n123\n1\n");
            git.add("modified");
            git.commit();
            write("modified", "abc\n456\n1\n");
            git.add("modified");

            validate("""
                         diff --git a/modified b/modified
                         index 6d123cb..054b7f5 100644
                         --- a/modified
                         +++ b/modified
                         @@ -1,3 +1,3 @@
                          abc
                         -123
                         +456
                          1
                         """);
        }

        @Test
        public void diffAdded() {
            write("added", "123\n456\n");
            lit.add("added");

            validate("""
                          diff --git a/added b/added
                          new file mode 100644
                          index 0000000..ce8c77d
                          --- /dev/null
                          +++ b/added
                          @@ -0,0 +1,2 @@
                          +123
                          +456
                          """);
        }

        void validate(String expected) {
            Lit.LitCommand litCommand = lit.diffCached();
            Git.GitCommand gitCommand = git.diffCached();
            String gitOutput = gitCommand.output();
            String litOutput = litCommand.output();
            assertEquals(gitOutput, litOutput);
            assertEquals(expected, litOutput);
        }
    }

    @Nested
    public class JitTests {
        final String doc = input("the quick brown fox jumps over the lazy dog");

        @Test
        public void itDetectsADeletionAtTheStart() {
            String changed = input("quick brown fox jumps over the lazy dog");

            List<Hunk> hunks = Diff.diffHunks(doc, changed);

            assertEquals(1, hunks.size());
            validateHunk(
                hunks.get(0),
                "@@ -1,4 +1,3 @@",
                List.of(
                    "-the", " quick", " brown", " fox"
                )
            );
        }

        @Test
        public void itDetectsAnInsertionAtTheStart() {
            String changed = input("so the quick brown fox jumps over the lazy dog");

            List<Hunk> hunks = Diff.diffHunks(doc, changed);

            assertEquals(1, hunks.size());
            validateHunk(
                hunks.get(0),
                "@@ -1,3 +1,4 @@",
                List.of(
                    "+so", " the", " quick", " brown"
                )
            );
        }

        @Test
        public void itDetectsAChangeSkippingTheStartAndEnd() {
            String changed = input("the quick brown fox leaps right over the lazy dog");

            List<Hunk> hunks = Diff.diffHunks(doc, changed);

            assertEquals(1, hunks.size());
            validateHunk(
                hunks.get(0),
                "@@ -2,7 +2,8 @@",
                List.of(
                    " quick", " brown", " fox", "-jumps", "+leaps", "+right", " over", " the", " lazy"
                )
            );
        }

        @Test
        public void itPutsNearbyChangesInTheSameHunk() {
            String changed = input("the brown fox jumps over the lazy cat");

            List<Hunk> hunks = Diff.diffHunks(doc, changed);

            assertEquals(1, hunks.size());
            validateHunk(
                hunks.get(0),
                "@@ -1,9 +1,8 @@",
                List.of(
                    " the", "-quick", " brown", " fox", " jumps", " over", " the", " lazy", "-dog", "+cat"
                )
            );
        }

        @Test
        public void itPutsDistantChangesInDifferentHunks() {
            String changed = input("a quick brown fox jumps over the lazy cat");

            List<Hunk> hunks = Diff.diffHunks(doc, changed);

            assertEquals(2, hunks.size());
            validateHunk(
                hunks.get(0),
                "@@ -1,4 +1,4 @@",
                List.of(
                    "-the", "+a", " quick", " brown", " fox"
                )
            );
            validateHunk(
                hunks.get(1),
                "@@ -6,4 +6,4 @@",
                List.of(
                    " over", " the", " lazy", "-dog", "+cat"
                )
            );
        }

        private void validateHunk(Hunk hunk, String expectedHeader, List<String> expectedEdits) {
            assertEquals(expectedHeader, hunk.header());
            assertEquals(
                expectedEdits
                , editsToStrings(hunk.edits())
            );
        }

        private String input(String s) {
            return s.replace(" ", "\n");
        }

        private List<String> editsToStrings(List<Edit> edits) {
            return edits.stream().map(edit -> edit.toString().stripTrailing()).toList();
        }
    }
}
