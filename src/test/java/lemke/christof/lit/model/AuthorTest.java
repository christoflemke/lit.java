package lemke.christof.lit.model;

import lemke.christof.lit.Environment;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorTest {
    @Test
    public void testDateParsing() {
        Map<String, String> envMap = Map.of(
                "GIT_AUTHOR_NAME", "me",
                "GIT_AUTHOR_EMAIL", "me@github.com",
                "GIT_AUTHOR_DATE", "1678008252 +0100"
        );
        Environment env = new Environment() {
            @Override
            public String getEnv(String key) {
                return envMap.get(key);
            }
        };
        Author author = Author.createAuthor(env);
        assertEquals("me", author.name());
        assertEquals("me@github.com", author.email());
        assertEquals(ZonedDateTime.parse("2023-03-05T10:24:12+01:00"), author.dateTime());
        assertEquals("me <me@github.com> 1678008252 +0100", author.toString());
    }
}
