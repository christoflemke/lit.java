package lemke.christof.lit.model;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommitTest {
    Author author = new Author("Christof Lemke", "doesnotexist@gmail.com", ZonedDateTime.parse("2023-03-05T10:24:12+01:00"));
    Author committer = new Author("Christof Lemke", "doesnotexist@gmail.com", ZonedDateTime.parse("2023-03-05T10:24:11+01:00"));
    Commit commit = new Commit(null,"88e38705fdbd3608cddbe904b67c731f3234c45b", author, committer, "first commit");

    @Test
    public void testOid() {
        assertEquals("42f46a330ec74552df5bc383abf72931e194360d", commit.oid());
    }
}
