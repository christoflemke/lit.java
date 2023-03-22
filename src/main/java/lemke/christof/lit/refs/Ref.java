package lemke.christof.lit.refs;

import java.util.Optional;

record Ref(String name) implements RefAst {
    @Override public Optional<String> resolve(Context context) {
        return context.readRef(name);
    }
}
