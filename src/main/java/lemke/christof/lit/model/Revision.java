package lemke.christof.lit.model;

import lemke.christof.lit.Repository;
import lemke.christof.lit.refs.Context;
import lemke.christof.lit.refs.RefAst;

import java.util.Optional;

public class Revision implements Context {

    private final Repository repo;
    private final Optional<RefAst> query;
    private final String expression;

    public Revision(Repository repo, String expression) {
        this.repo = repo;
        this.expression = expression;
        this.query = RefAst.parse(expression);
    }

    public String resolve() throws InvalidObjectException {
        Optional<String> oid = query.flatMap(q -> q.resolve(this));
        return oid.orElseThrow(() -> new InvalidObjectException("No valid object name: "+expression));

    }

    static class InvalidObjectException extends Exception {
        public InvalidObjectException(String s) {
            super(s);
        }
    }

    @Override public Optional<String> readRef(String name) {
        return repo.refs().readRef(name);
    }

    @Override public String commitParent(String oid) {
        DbObject o = repo.db().read(oid);
        if (o instanceof Commit commit) {
            return commit.parent();
        } else {
            throw new RuntimeException("Expected oid to point to commit "+oid);
        }
    }
}

