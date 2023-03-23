package lemke.christof.lit.refs;

import lemke.christof.lit.model.Oid;

import java.util.Optional;

record Parent(RefAst rev) implements RefAst {
    @Override public Optional<Oid> resolve(Context context) {
        return rev.resolve(context).flatMap(context::commitParent);
    }
}
