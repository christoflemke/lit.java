package lemke.christof.lit.refs;

import java.util.Optional;

record Ancestor(RefAst rev, int n) implements RefAst {
    @Override public Optional<String> resolve(Context context) {
        Optional<String> oid = rev.resolve(context);
        for (int i = 0; i < n; i++) {
            oid = oid.map(context::commitParent);
        }
        return oid;
    }
}
