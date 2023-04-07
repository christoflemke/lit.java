package lemke.christof.lit.refs;

import lemke.christof.lit.Database;
import lemke.christof.lit.HintedException;
import lemke.christof.lit.Refs;
import lemke.christof.lit.database.Commit;
import lemke.christof.lit.database.DbObject;
import lemke.christof.lit.database.Oid;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Revision {

    private final Database db;
    private final Refs refs;
    private final Optional<RefAst> query;
    private final String expression;
    private final List<String> errors = new ArrayList<>();

    public Revision(Database db, Refs refs, String expression) {
        this.db = db;
        this.refs = refs;
        this.expression = expression;
        this.query = RefAst.parse(expression);
    }

    public Oid resolve(Optional<DbObject.ObjectType> objectType) {
        Optional<Oid> oid = query
            .flatMap(q -> q.resolve(new RevisionContext()))
            .flatMap(o -> validateType(o, objectType));

        return oid.orElseThrow(() -> HintedException.fromErrors("Not a valid object name: '"+expression+"'", this.errors));
    }

    private Optional<Oid> validateType(Oid oid, Optional<DbObject.ObjectType> objectType) {
        if(objectType.isEmpty()) {
            return Optional.of(oid);
        }
        DbObject object = db.read(oid);
        if(object.type() == objectType.get()) {
            return Optional.of(oid);
        }
        errors.add("object " + oid + " is a " + object.type() + ", not a commit");
        return Optional.empty();
    }

    private class RevisionContext implements Context {
        @Override public Optional<Oid> readRef(String name) {
            Optional<Oid> oid = refs.readRef(name);
            if (oid.isPresent()) {
                return oid;
            }

            List<Oid> candidates = db.prefixMatch(name);
            if (candidates.size() == 1) {
                return Optional.of(candidates.get(0));
            }
            if (candidates.size() > 1) {
                throw HintedException.fromShaCandidates(db, candidates, name);
            }
            return Optional.empty();
        }

        @Override public Optional<Oid> commitParent(Oid oid) {
            DbObject o = db.read(oid);
            if (o instanceof Commit commit) {
                return commit.parent();
            } else {
                errors.add("object " + oid + " is a " + o.type() + ", not a commit");
                return Optional.empty();
            }
        }
    }
}

