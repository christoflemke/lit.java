package lemke.christof.lit.refs;

import lemke.christof.lit.database.Oid;

import java.util.Optional;

record Ancestor(RefAst rev, int n) implements RefAst {
    @Override public Optional<Oid> resolve(Context context) {
        Optional<Oid> oid = rev.resolve(context);
        for (int i = 0; i < n; i++) {
            oid = oid.flatMap(context::commitParent);
        }
        return oid;
    }
}
