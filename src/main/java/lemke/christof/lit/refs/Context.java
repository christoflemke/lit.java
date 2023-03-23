package lemke.christof.lit.refs;

import lemke.christof.lit.model.Oid;

import java.util.Optional;

public interface Context {
    public Optional<Oid> readRef(String name);

    public Optional<Oid> commitParent(Oid resolve);
}
