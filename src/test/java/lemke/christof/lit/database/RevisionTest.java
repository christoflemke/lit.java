package lemke.christof.lit.database;

import lemke.christof.lit.BaseTest;
import lemke.christof.lit.refs.Revision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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

    @Test public void test() {
        assertAll(
            () -> assertEquals("bc6bfb138843dbc0cf43a508168991bf684c4754", revision("master")),
            () -> assertEquals("949f94fd94b9c80686e96b0476f8a130ed77731d", revision("master^")),
            () -> assertEquals("949f94fd94b9c80686e96b0476f8a130ed77731d", revision("master~1")),
            () -> assertEquals("5c26f7f6ffab8fc9d5ad1e88875a26ef7a7b2831", revision("master~2"))
        );
    }

    private String revision(String name) {
        return new Revision(repo.db(), repo.refs(), name).resolve(Optional.of(DbObject.ObjectType.COMMIT)).value();
    }
}
