package lemke.christof.lit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

public class RefTest extends BaseTest {
    @BeforeEach
    public void setUp() {
        git.init();
    }

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
        repo.refs().createBranch("master");
        repo.refs().createBranch("main");
    }

    private Executable invalidBranchName(String branchName) {
        return () -> assertThrows(RuntimeException.class, () -> repo.refs().createBranch(branchName), branchName + " should not be valid");
    }
}
