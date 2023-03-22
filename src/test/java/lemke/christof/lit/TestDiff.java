package lemke.christof.lit;

import lemke.christof.lit.diff.Diff;
import lemke.christof.lit.diff.Edit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDiff {

    @Test
    public void addLine() {
        List<Edit> diff = Diff.diff("", "a");
        assertEquals("+a", editsToString(diff));
    }

    @Test
    void removeLine() {
        List<Edit> diff = Diff.diff("a", "");
        assertEquals("-a", editsToString(diff));
    }

    @Test
    void noDiff() {
        List<Edit> diff = Diff.diff("a", "a");
        assertEquals(" a", editsToString(diff));
    }

    @Test void diffEmpty() {
        List<Edit> diff = Diff.diff("", "");
        assertEquals("", editsToString(diff));
    }

    @Test void andNowForSomethingCompletelyDifferent() {
        List<Edit> diff = Diff.diff("a", "b");
        assertEquals("-a+b", editsToString(diff));
    }

    @Test void moreDifferent() {
        List<Edit> diff = Diff.diff("1\n2\n3\n", "a\nb\nc\n");
        assertEquals("""
                         -1
                         -2
                         -3
                         +a
                         +b
                         +c
                         """, editsToString(diff));
    }

    @Test
    public void test() {
        String a = """
            A
            B
            C
            A
            B
            B
            A
            """;
        String b = """
            C
            B
            A
            B
            A
            C
            """;

        List<Edit> diff = Diff.diff(a, b);
        System.out.println(diff);
        assertEquals("""
                         -A
                         -B
                          C
                         +B
                          A
                          B
                         -B
                          A
                         +C
                         """, editsToString(diff));
    }

    private static String editsToString(List<Edit> diff) {
        return diff.stream().map(e -> e.toString()).collect(Collectors.joining(""));
    }

}
