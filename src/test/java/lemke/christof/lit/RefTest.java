package lemke.christof.lit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            invalidBranchName("something\0x10 something"),
            invalidBranchName("something:something"),
            invalidBranchName("something?something"),
            invalidBranchName("something[something"),
            invalidBranchName("something^something"),
            invalidBranchName("something~something"),
            invalidBranchName("something something")
        );

    }

    private Executable invalidBranchName(String branchName) {
        return () -> assertThrows(RuntimeException.class, () -> repo.refs().createBranch(branchName));
    }
}
