package lemke.christof.lit;

import org.junit.jupiter.api.Test;

import java.util.List;

public class TestDiff {

    @Test
    public void test() {
        String[] a = "ABCABBA".split("");
        String[] b = "CBABAC".split("");

        Meyers meyers = new Meyers(a, b);
        List<Meyers.Edit> diff = meyers.diff();
        System.out.println(diff);
    }

}
