package lemke.christof.lit.model;

import lemke.christof.lit.Environment;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public record Author (String name, String email, ZonedDateTime dateTime) {

    @Override
    public String toString() {
        String zone = dateTime.getZone().toString().replace(":", "");
        String dateString = String.valueOf(dateTime.toInstant().getEpochSecond()) + " " + zone;
        return name + " <" + email + "> " +dateString;
    }

    public static Author createAuthor(Environment env) {
        return new Author(
                env.get("GIT_AUTHOR_NAME"),
                env.get("GIT_AUTHOR_EMAIL"),
                env.getDate("GIT_AUTHOR_DATE")
        );
    }

    public static Author createCommitter(Environment env) {
        return new Author(
                env.get("GIT_AUTHOR_NAME"),
                env.get("GIT_AUTHOR_EMAIL"),
                env.getDate("GIT_COMMITTER_DATE")
        );
    }

    static DateTimeFormatter dateFormat() {
        return DateTimeFormatter.ofPattern("%s %z");
    }
}
