package lemke.christof.lit.model;

import com.google.common.collect.ImmutableMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface Revision permits Revision.Ref, Revision.Parent, Revision.Ancestor {
    record Ref(String name) implements Revision {}
    record Parent(Revision rev) implements Revision {}
    record Ancestor(Revision rev, int n) implements Revision {}

    Pattern PARENT = Pattern.compile("^(.+)\\^$");
    Pattern ANCESTOR = Pattern.compile("^(.+)~(\\d+)$");
    ImmutableMap<String, String> REF_ALIASES = ImmutableMap.of("@", "HEAD");
    Pattern INVALID_BRANCH_NAME = Pattern.compile("""
            ^\\.| # begins with "."
            \\.\\.| # includes ".."
            [\\x00-\\x20] # includes control characters
            [:?\\[^~\\s]| # includes ":", "?", "[", "\", "^", "~", SP, or TAB]
            /$| # ends with "/"
            \\.lock$| # ends with ".lock"
            @\\{ # contains "@{"
            """, Pattern.COMMENTS);

    static Revision from(String name) {
        {
            Matcher matcher = PARENT.matcher(name);
            if (matcher.matches()) {
                Revision rev = Revision.from(matcher.group(1));
                return rev == null ? null : new Parent(rev);
            }
        }
        {
            Matcher matcher = ANCESTOR.matcher(name);
            if (matcher.matches()) {
                Revision rev = Revision.from(matcher.group(1));
                int number = Integer.parseInt(matcher.group(2));
                return rev == null ? null : new Ancestor(rev, number);
            }
        }
        if(!INVALID_BRANCH_NAME.matcher(name).matches()) {
            name = REF_ALIASES.getOrDefault(name, name);
            return new Ref(name);
        }
        return null;
    }
}
