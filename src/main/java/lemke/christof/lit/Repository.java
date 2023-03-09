package lemke.christof.lit;

import java.nio.file.Path;

public record Repository (Workspace ws, Database db, Refs refs, Environment env, IO io) {
    public static Repository create(Path root) {
        Workspace ws = new Workspace(root);
        return new Repository(ws, new Database(root), new Refs(root), new Environment(), new IO(System.in, System.out, System.err));
    }

    public Index createIndex() {
        return new Index(ws);
    }
}
