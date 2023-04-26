package lemke.christof.lit;

import lemke.christof.lit.database.Oid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RefTest extends BaseTest {
    @BeforeEach
    public void setUp() {
        git.init();
        write("a");
        git.add("a");
        git.commit();
    }

    private static final Oid NULL_OID = Oid.of(Util.repeat("0", 40));

    @Test
    public void createInvalidBranch() {
        assertAll(
            invalidBranchName(".something"),
            invalidBranchName("something..something"),
            invalidBranchName("something\u0010 something"),
            invalidBranchName("something:something"),
            invalidBranchName("something?something"),
            invalidBranchName("something[something"),
            invalidBranchName("something^something"),
            invalidBranchName("something~something"),
            invalidBranchName("something something"),
            invalidBranchName("something/"),
            invalidBranchName("something.lock"),
            invalidBranchName("something@{something")
        );
        repo.refs().createBranch("master1", NULL_OID);
        repo.refs().createBranch("main", NULL_OID);
    }

    @Test public void refByName() {
        Oid master = repo.refs().resolveCommit("master");
        assertEquals(Oid.of("a1823e229db49387ddf87dfd48331f1b5712489c"), master);
    }

    @Test public void refByPrefix() {
        Oid master = repo.refs().resolveCommit("a182");
        assertEquals(Oid.of("a1823e229db49387ddf87dfd48331f1b5712489c"), master);
    }

    @Test public void refPrefixTooShort() {
        assertEquals(Optional.empty(), repo.refs().resolveBranchName("a"));
    }

    private Executable invalidBranchName(String branchName) {
        return () -> assertThrows(RuntimeException.class, () -> repo.refs().createBranch(branchName, NULL_OID), branchName + " should not be valid");
    }
}
