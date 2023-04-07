package lemke.christof.lit;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Environment {
    static Environment createDefault() {
        return key -> System.getenv(key);
    }

    Pattern DATETIME_PATTERN = Pattern.compile("(\\d+) (.*)");

    String get(String key);

    default ZonedDateTime getDate(String envConst) {
        String authorDateString = get(envConst);
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
            Instant now = Instant.now();
            ZoneId zone = ZoneOffset.systemDefault();
            ZoneOffset offset = zone.getRules().getOffset(now);
            return ZonedDateTime.ofInstant(now, offset);
        }
    }

}
