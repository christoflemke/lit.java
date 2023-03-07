package lemke.christof.lit;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Environment {
    public static final Pattern DATETIME_PATTERN = Pattern.compile("(\\d+) (.*)");

    public String getEnv(String key) {
        return System.getenv(key);
    }

    public ZonedDateTime getDate(String envConst) {
        String authorDateString = getEnv(envConst);
        if (authorDateString != null) {
            Matcher matcher = DATETIME_PATTERN.matcher(authorDateString);
            if (matcher.matches()) {
                long epoch = Long.parseLong(matcher.group(1)) * 1000;
                String offset = matcher.group(2);
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.of(offset));
            }
            else {
                throw new RuntimeException("Failed to parse author time: "+authorDateString);
            }
        } else {
            return ZonedDateTime.now();
        }
    }

}
