package lemke.christof.lit;

import lemke.christof.lit.status.Status;
import lemke.christof.lit.status.StatusBuilder;

import java.nio.file.Path;

public record Repository (Workspace ws, Database db, Refs refs, Environment env, IO io) {
    public static Repository create(Path root) {
        Workspace ws = new Workspace(root);
        return new Repository(ws, new Database(root), new Refs(root), Environment.createDefault(), IO.createDefault());
    }

    public Index createIndex() {
        return new Index(ws);
    }

    public Repository withIO(IO replacementIO) {
        return new Repository(ws, db, refs, env, replacementIO);
    }

    public Repository withEnv(Environment replacementEnv) {
        return new Repository(ws, db, refs, replacementEnv, io);
    }

    public Status status() {
        Index idx = createIndex();
        StatusBuilder status = new StatusBuilder(this, idx);
        idx.withLock(() -> {
            idx.load();
            status.computeChanges();
            idx.commit();
            return null;
        });
        return new Status(status.changed(), status.indexChanges(), status.workspaceChanges(), status.untrackedFiles());
    }
}
