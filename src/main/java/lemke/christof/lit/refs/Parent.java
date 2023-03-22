package lemke.christof.lit.refs;

import java.util.Optional;

record Parent(RefAst rev) implements RefAst {
    @Override public Optional<String> resolve(Context context) {
        return rev.resolve(context).map(context::commitParent);
    }
}
