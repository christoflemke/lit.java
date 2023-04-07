package lemke.christof.lit;

import lemke.christof.lit.database.Commit;
import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HintedExceptionTest extends BaseTest {

    private Oid master;
    private Oid treeOid;

    @BeforeEach void setup() {
        lit.init();
        write("foo");
        write("bar");
        lit.add(".");
        lit.commit();
        master = repo.refs().readHead().get();
        Commit commit = (Commit) repo.db().read(master);
        treeOid = commit.treeOid();
    }


    @Test void fromShaCandidates() {
        RuntimeException exception = HintedException.fromShaCandidates(
            repo.db(),
            List.of(master, treeOid),
            "acab"
        );
        assertEquals("""
                         error: short SHA1 acab is ambiguous
                         hint: The candidates are:
                         hint:   29a31ca commit 2023-03-05 - commit message
                         hint:   ea41dba tree
                         fatal: Not a valid object name: 'acab'""", exception.getMessage());
    }

    @Test void errorWhenResolvingToTree() {
        HintedException e = assertThrows(
            HintedException.class,
            () -> this.repo.refs().resolveCommit("ea41dba")
        );
        assertEquals("""
                         error: object ea41dba10b54a794284e0be009a11f0ff3716a28 is a tree, not a commit
                         fatal: Not a valid object name: 'ea41dba'""", e.getMessage());
    }

    @Test void errorUnknownObject() {
        HintedException e = assertThrows(HintedException.class, () -> this.repo.refs().resolveCommit("1234"));
        assertEquals("fatal: Not a valid object name: '1234'", e.getMessage());
    }

}
