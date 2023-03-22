package lemke.christof.lit.refs;

import com.google.common.collect.ImmutableMap;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface RefAst permits Ref, Parent, Ancestor {
    Optional<String> resolve(Context context);

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


    static Optional<RefAst> parse(String name) {
        {
            Matcher matcher = PARENT.matcher(name);
            if (matcher.matches()) {
                Optional<RefAst> rev = RefAst.parse(matcher.group(1));
                return rev.map(r -> new Parent(r));
            }
        }
        {
            Matcher matcher = ANCESTOR.matcher(name);
            if (matcher.matches()) {
                Optional<RefAst> rev = RefAst.parse(matcher.group(1));
                int number = Integer.parseInt(matcher.group(2));
                return rev.map(r -> new Ancestor(r, number));
            }
        }
        if (!INVALID_BRANCH_NAME.matcher(name).matches()) {
            name = REF_ALIASES.getOrDefault(name, name);
            return Optional.of(new Ref(name));
        }
        return Optional.empty();
    }
}

