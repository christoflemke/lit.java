package lemke.christof.lit.model;

import lemke.christof.lit.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RevisionTest extends BaseTest {
    @BeforeEach public void setup() {
        git.init();

        for (var c : List.of("a", "b", "c")) {
            write(c, c);
            git.add(c);
            git.commit();
        }
    }

    @Test public void test() throws Revision.InvalidObjectException {
        assertAll(
            () -> assertEquals("bc6bfb138843dbc0cf43a508168991bf684c4754", revision("master")),
            () -> assertEquals("949f94fd94b9c80686e96b0476f8a130ed77731d", revision("master^")),
            () -> assertEquals("949f94fd94b9c80686e96b0476f8a130ed77731d", revision("master~1")),
            () -> assertEquals("5c26f7f6ffab8fc9d5ad1e88875a26ef7a7b2831", revision("master~2"))
        );
    }

    private String revision(String name) throws Revision.InvalidObjectException {
        return new Revision(repo, name).resolve();
    }
}
