package lemke.christof.lit.refs;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RefAstTest {
    @Test void simpleRef() {
        assertEquals(
            Optional.of(new Ref("main")),
            RefAst.parse("main")
        );
    }

    @Test void parentRef() {
        assertEquals(
            Optional.of(new Parent(new Ref("main"))),
            RefAst.parse("main^")
        );
    }

    @Test void ancestorRef() {
        assertEquals(
            Optional.of(new Ancestor(new Ref("main"), 2)),
            RefAst.parse("main~2")
        );
    }

    @Test void recursiveRef() {
        assertEquals(
            Optional.of(new Ancestor(
                new Parent(
                    new Ref("main")
                ), 2)),
            RefAst.parse("main^~2")
        );
    }
}
