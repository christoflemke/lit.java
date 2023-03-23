package lemke.christof.lit.refs;

import lemke.christof.lit.model.Oid;

import java.util.Optional;

record Ref(String name) implements RefAst {
    @Override public Optional<Oid> resolve(Context context) {
        return context.readRef(name);
    }
}
