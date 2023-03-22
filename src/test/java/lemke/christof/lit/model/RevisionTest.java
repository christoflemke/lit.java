package lemke.christof.lit.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RevisionTest {
    @Test void simpleRef() {
        assertEquals(
            new Revision.Ref("main"),
            Revision.from("main")
        );
    }

    @Test void parentRef() {
        assertEquals(
            new Revision.Parent(new Revision.Ref("main")),
            Revision.from("main^")
        );
    }

    @Test void ancestorRef() {
        assertEquals(
            new Revision.Ancestor(new Revision.Ref("main"), 2),
            Revision.from("main~2")
        );
    }

    @Test void recursiveRef() {
        assertEquals(
            new Revision.Ancestor(
                new Revision.Parent(
                    new Revision.Ref("main")
                ), 2),
            Revision.from("main^~2")
        );
    }
}
