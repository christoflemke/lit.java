package lemke.christof.lit.refs;

import java.util.Optional;

public interface Context {
    public Optional<String> readRef(String name);

    public String commitParent(String resolve);
}
