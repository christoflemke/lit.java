package lemke.christof.lit.model;

import lemke.christof.lit.Environment;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Author (String name, String email, ZonedDateTime dateTime) {

    @Override
    public String toString() {
        String zone = dateTime.getZone().toString().replace(":", "");
        String dateString = dateTime.toInstant().getEpochSecond() + " " + zone;
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

    private static final Pattern pattern = Pattern.compile("([^<]+)<([^>]+)> (\\d+) (.+)");
    public static Author fromString(String s) {
        Matcher matcher = pattern.matcher(s);
        if(!matcher.matches()) {
            throw new RuntimeException("Unable to parse author string: "+s);
        }
        String name = matcher.group(1).stripTrailing();
        String email = matcher.group(2);
        String time = matcher.group(3);
        String zone = matcher.group(4);
        Instant instant = Instant.ofEpochSecond(Long.parseLong(time));
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.of(zone));
        return new Author(name, email, dateTime);
    }

    public String shortDate() {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
